package gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.table.DefaultTableCellRenderer;

import model.DispositivoRede;
import model.StatusDispositivo;
import monitor.DispositivoObserver;
import monitor.MonitorDispositivos;

/**
 * Janela principal: tabela de dispositivos, painel lateral de detalhes
 * e abas de Interfaces e Rotas. Implementa DispositivoObserver para
 * receber atualizações do monitor (e saltar para a EDT antes de mexer
 * em qualquer componente Swing).
 */
public class JanelaPrincipal extends JFrame implements DispositivoObserver {

    private final MonitorDispositivos monitor;
    private final ModeloDispositivos modelo = new ModeloDispositivos();
    private final JTable tabela = new JTable(modelo);
    private final PainelDetalhes painelDetalhes = new PainelDetalhes();

    public JanelaPrincipal(MonitorDispositivos monitor) {
        super("Gerenciador de Dispositivos de Rede");
        this.monitor = monitor;

        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                monitor.encerrar();
                dispose();
                System.exit(0);
            }
        });

        construirInterface();
        monitor.adicionarObservador(this);

        setSize(1050, 600);
        setLocationRelativeTo(null);
    }

    private void construirInterface() {
        setLayout(new BorderLayout());

        JToolBar barra = new JToolBar();
        barra.setFloatable(false);
        JButton btnAdd = new JButton("Adicionar");
        JButton btnEdit = new JButton("Editar");
        JButton btnDel = new JButton("Remover");
        btnAdd.addActionListener(e -> abrirDialogoNovo());
        btnEdit.addActionListener(e -> abrirDialogoEditar());
        btnDel.addActionListener(e -> removerSelecionado());
        barra.add(btnAdd); barra.add(btnEdit); barra.add(btnDel);

        tabela.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabela.setRowHeight(24);
        tabela.setFillsViewportHeight(true);
        tabela.setAutoCreateRowSorter(true);
        // Coluna 0 (Status) recebe coloração de fundo conforme o enum.
        tabela.getColumnModel().getColumn(0).setCellRenderer(new RendererStatus());
        tabela.getColumnModel().getColumn(0).setPreferredWidth(120);

        tabela.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) painelDetalhes.exibir(selecionado());
        });

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                new JScrollPane(tabela), painelDetalhes);
        split.setResizeWeight(0.65);
        split.setDividerLocation(680);

        JTabbedPane abas = new JTabbedPane();
        abas.addTab("Dispositivos", split);
        abas.addTab("Interfaces de Rede", new PainelInterfaces());
        abas.addTab("Tabela de Rotas", new PainelRotas());

        add(barra, BorderLayout.NORTH);
        add(abas, BorderLayout.CENTER);
    }

    // -----------------------------------------------------------------
    // Ações
    // -----------------------------------------------------------------

    private void abrirDialogoNovo() {
        DialogoDispositivo dlg = new DialogoDispositivo(this, null);
        dlg.setVisible(true);
        if (dlg.foiConfirmado()) {
            DispositivoRede novo = dlg.getResultado();
            monitor.adicionarDispositivo(novo);
            modelo.adicionar(novo);
        }
    }

    private void abrirDialogoEditar() {
        DispositivoRede sel = selecionado();
        if (sel == null) {
            avisar("Selecione um dispositivo para editar.");
            return;
        }
        DialogoDispositivo dlg = new DialogoDispositivo(this, sel);
        dlg.setVisible(true);
        if (dlg.foiConfirmado()) {
            modelo.atualizarLinha(modelo.linhaDe(sel));
            painelDetalhes.exibir(sel);
        }
    }

    private void removerSelecionado() {
        DispositivoRede sel = selecionado();
        if (sel == null) {
            avisar("Selecione um dispositivo para remover.");
            return;
        }
        int op = JOptionPane.showConfirmDialog(this,
                "Remover \"" + sel.getNome() + "\"?",
                "Confirmar", JOptionPane.YES_NO_OPTION);
        if (op == JOptionPane.YES_OPTION) {
            monitor.removerDispositivo(sel);
            modelo.remover(modelo.linhaDe(sel));
            painelDetalhes.exibir(null);
        }
    }

    private void avisar(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Aviso",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private DispositivoRede selecionado() {
        int row = tabela.getSelectedRow();
        if (row < 0) return null;
        return modelo.getDispositivo(tabela.convertRowIndexToModel(row));
    }

    // -----------------------------------------------------------------
    // Observer (chamado pelas threads do monitor)
    // -----------------------------------------------------------------

    @Override
    public void aoAtualizarDispositivo(DispositivoRede d) {
        // Roda na thread do monitor — salta para a EDT antes de mexer no Swing.
        SwingUtilities.invokeLater(() -> {
            int row = modelo.linhaDe(d);
            if (row >= 0) modelo.atualizarLinha(row);
            DispositivoRede sel = selecionado();
            if (sel != null && sel == d) painelDetalhes.exibir(d);
        });
    }

    // -----------------------------------------------------------------
    // Renderer da coluna Status: pinta o fundo com a cor do enum.
    // -----------------------------------------------------------------

    private static class RendererStatus extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object v,
                boolean sel, boolean foc, int row, int col) {
            super.getTableCellRendererComponent(t, v, sel, foc, row, col);
            StatusDispositivo s = (v instanceof StatusDispositivo)
                    ? (StatusDispositivo) v : StatusDispositivo.DESCONHECIDO;
            setText(s.getDescricao());
            setForeground(Color.DARK_GRAY);
            if (!sel) {
                Color c = s.getCor();
                setBackground(new Color(c.getRed(), c.getGreen(), c.getBlue(), 80));
                setOpaque(true);
            }
            return this;
        }
    }
}
