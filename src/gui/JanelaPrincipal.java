package gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.WindowConstants;
import javax.swing.table.DefaultTableCellRenderer;

import model.DispositivoRede;
import model.StatusDispositivo;
import monitor.MonitorDispositivos;
import observer.DispositivoObserver;

/**
 * Janela principal da aplicação. Junta tudo:
 *
 *  - JTable de dispositivos (alimentada por {@link ModeloDispositivos});
 *  - Painel lateral de detalhes (traceroute, diagnóstico);
 *  - Aba "Interfaces de Rede" listando placas do computador;
 *  - Barra de ferramentas (Adicionar/Editar/Remover/Coletar agora);
 *  - Indicação visual por cores na coluna "Status";
 *  - Implementa {@link DispositivoObserver}: quando o monitor termina
 *    uma coleta, esta janela atualiza a linha correspondente da tabela
 *    e, se for o dispositivo selecionado, atualiza o painel de detalhes.
 *
 * Toda atualização de UI feita a partir do monitor passa pela EDT via
 * {@link SwingUtilities#invokeLater(Runnable)} — regra fundamental do
 * Swing para evitar bugs intermitentes de concorrência.
 */
public class JanelaPrincipal extends JFrame implements DispositivoObserver {

    private final MonitorDispositivos monitor;
    private final ModeloDispositivos modelo = new ModeloDispositivos();
    private final JTable tabela = new JTable(modelo);
    private final PainelDetalhes painelDetalhes = new PainelDetalhes();
    private final JLabel lblRodape = new JLabel(" ");

    public JanelaPrincipal(MonitorDispositivos monitor) {
        super("Gerenciador de Dispositivos de Rede");
        this.monitor = monitor;

        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                // Encerramento limpo: para o pool de threads antes de sair.
                monitor.encerrar();
                dispose();
                System.exit(0);
            }
        });

        construirInterface();

        // Registra-se como observador para receber atualizações de métricas.
        monitor.adicionarObservador(this);

        setSize(1100, 620);
        setLocationRelativeTo(null);
    }

    private void construirInterface() {
        setLayout(new BorderLayout());

        // --------- Barra de ferramentas ---------
        JToolBar barra = new JToolBar();
        barra.setFloatable(false);

        JButton btnAdd = new JButton("Adicionar");
        btnAdd.addActionListener(e -> abrirDialogoNovo());

        JButton btnEdit = new JButton("Editar");
        btnEdit.addActionListener(e -> abrirDialogoEditar());

        JButton btnDel = new JButton("Remover");
        btnDel.addActionListener(e -> removerSelecionado());

        JButton btnAgora = new JButton("Coletar agora");
        btnAgora.addActionListener(e -> coletarAgora());

        barra.add(btnAdd);
        barra.add(btnEdit);
        barra.add(btnDel);
        barra.addSeparator();
        barra.add(btnAgora);

        // --------- Tabela de dispositivos ---------
        tabela.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabela.setRowHeight(26);
        tabela.setFillsViewportHeight(true);
        tabela.setAutoCreateRowSorter(true);

        // Renderer customizado para a coluna 0 (Status): desenha uma
        // bolinha colorida + texto, conforme o enum StatusDispositivo.
        tabela.getColumnModel().getColumn(0).setCellRenderer(new RendererStatus());
        tabela.getColumnModel().getColumn(0).setPreferredWidth(110);

        // Quando o usuário muda a seleção, atualizamos o painel de
        // detalhes na lateral direita.
        tabela.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            atualizarPainelDetalhesComSelecao();
        });

        JScrollPane scrollTabela = new JScrollPane(tabela);

        // --------- Aba principal (tabela + detalhes) ---------
        JSplitPane split = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT, scrollTabela, painelDetalhes);
        split.setResizeWeight(0.65);
        split.setDividerLocation(700);

        JTabbedPane abas = new JTabbedPane();
        abas.addTab("Dispositivos", split);
        abas.addTab("Interfaces de Rede", new PainelInterfaces());
        abas.addTab("Tabela de Rotas", new PainelRotas());

        // --------- Rodapé ---------
        JPanel rodape = new JPanel(new FlowLayout(FlowLayout.LEFT));
        rodape.setBorder(BorderFactory.createEtchedBorder());
        rodape.add(lblRodape);
        atualizarRodape();

        // Atualiza o relógio do rodapé a cada segundo (apenas estético).
        Timer t = new Timer(1000, e -> atualizarRodape());
        t.start();

        add(barra, BorderLayout.NORTH);
        add(abas, BorderLayout.CENTER);
        add(rodape, BorderLayout.SOUTH);
    }

    private void atualizarRodape() {
        lblRodape.setText(String.format(
                "Monitoramento a cada %d s   |   %d dispositivo(s) cadastrado(s)",
                monitor.getIntervaloSegundos(),
                modelo.getRowCount()));
    }

    // -----------------------------------------------------------------
    // Ações dos botões
    // -----------------------------------------------------------------

    private void abrirDialogoNovo() {
        DialogoDispositivo dlg = new DialogoDispositivo(this, null);
        dlg.setVisible(true);
        if (dlg.foiConfirmado()) {
            DispositivoRede novo = dlg.getResultado();
            monitor.adicionarDispositivo(novo);
            modelo.adicionar(novo);
            atualizarRodape();
        }
    }

    private void abrirDialogoEditar() {
        DispositivoRede sel = dispositivoSelecionado();
        if (sel == null) {
            JOptionPane.showMessageDialog(this,
                    "Selecione um dispositivo na tabela para editar.",
                    "Nada selecionado", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        DialogoDispositivo dlg = new DialogoDispositivo(this, sel);
        dlg.setVisible(true);
        if (dlg.foiConfirmado()) {
            // O monitor já tem a referência do dispositivo e segue
            // monitorando — apenas redesenhamos a linha.
            int row = modelo.linhaDe(sel);
            modelo.atualizarLinha(row);
            atualizarPainelDetalhesComSelecao();
        }
    }

    private void removerSelecionado() {
        DispositivoRede sel = dispositivoSelecionado();
        if (sel == null) {
            JOptionPane.showMessageDialog(this,
                    "Selecione um dispositivo na tabela para remover.",
                    "Nada selecionado", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int op = JOptionPane.showConfirmDialog(this,
                "Remover \"" + sel.getNome() + "\"?",
                "Confirmar", JOptionPane.YES_NO_OPTION);
        if (op == JOptionPane.YES_OPTION) {
            int row = modelo.linhaDe(sel);
            monitor.removerDispositivo(sel);
            modelo.remover(row);
            painelDetalhes.exibir(null);
            atualizarRodape();
        }
    }

    private void coletarAgora() {
        DispositivoRede sel = dispositivoSelecionado();
        if (sel == null) {
            JOptionPane.showMessageDialog(this,
                    "Selecione um dispositivo na tabela.",
                    "Nada selecionado", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        monitor.coletarAgora(sel);
    }

    private DispositivoRede dispositivoSelecionado() {
        int row = tabela.getSelectedRow();
        if (row < 0) return null;
        // Converte índice da view → model (necessário porque a tabela
        // permite ordenação pelo cabeçalho).
        int modelRow = tabela.convertRowIndexToModel(row);
        return modelo.getDispositivo(modelRow);
    }

    private void atualizarPainelDetalhesComSelecao() {
        painelDetalhes.exibir(dispositivoSelecionado());
    }

    // -----------------------------------------------------------------
    // DispositivoObserver — chamado pelas threads de monitoramento
    // -----------------------------------------------------------------

    @Override
    public void aoAtualizarDispositivo(DispositivoRede d) {
        // ATENÇÃO: este método executa na thread do monitor, NÃO na EDT.
        // Por isso encapsulamos toda a interação com Swing em invokeLater.
        SwingUtilities.invokeLater(() -> {
            int row = modelo.linhaDe(d);
            if (row >= 0) {
                modelo.atualizarLinha(row);
            }
            // Se o dispositivo atualizado está selecionado, refresca os
            // detalhes (latência nova, rota nova, etc.).
            DispositivoRede sel = dispositivoSelecionado();
            if (sel != null && sel.getId() == d.getId()) {
                painelDetalhes.exibir(d);
            }
        });
    }

    // -----------------------------------------------------------------
    // Renderer da coluna Status
    // -----------------------------------------------------------------

    /**
     * Desenha uma bola colorida seguida da descrição textual do status.
     * O fundo da célula recebe um leve tom da cor do status para reforço
     * visual (verde/amarelo/vermelho/cinza).
     */
    private static class RendererStatus extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean sel,
                                                       boolean foc, int row, int col) {
            super.getTableCellRendererComponent(t, v, sel, foc, row, col);

            StatusDispositivo s = (v instanceof StatusDispositivo)
                    ? (StatusDispositivo) v : StatusDispositivo.DESCONHECIDO;

            setText(s.getDescricao());
            setIcon(new BolinhaIcon(s.getCor()));
            setForeground(Color.DARK_GRAY);

            if (!sel) {
                // Aplica um tom claro da cor como background.
                Color base = s.getCor();
                Color tonalidade = new Color(base.getRed(), base.getGreen(),
                        base.getBlue(), 60);
                setBackground(tonalidade);
                setOpaque(true);
            }
            return this;
        }
    }

    /** Ícone circular simples usado pelo {@link RendererStatus}. */
    private static class BolinhaIcon implements javax.swing.Icon {
        private final Color cor;
        BolinhaIcon(Color cor) { this.cor = cor; }
        @Override public int getIconWidth() { return 14; }
        @Override public int getIconHeight() { return 14; }
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(cor);
            g2.fillOval(x, y, 12, 12);
            g2.setColor(cor.darker());
            g2.drawOval(x, y, 12, 12);
            g2.dispose();
        }
    }

    // Pequeno auxiliar usado por outras partes futuramente, se necessário.
    @SuppressWarnings("unused")
    private static Dimension dim(int w, int h) { return new Dimension(w, h); }
}
