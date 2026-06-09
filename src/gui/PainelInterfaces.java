package gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import service.InterfaceRedeInfo;

/**
 * Painel que mostra as interfaces de rede locais do computador (placas
 * físicas, virtuais, loopback). É reconstruído sob demanda quando o
 * usuário clica em "Atualizar".
 */
public class PainelInterfaces extends JPanel {

    private final ModeloInterfaces modelo = new ModeloInterfaces();

    public PainelInterfaces() {
        super(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JTable tabela = new JTable(modelo);
        tabela.setFillsViewportHeight(true);
        tabela.setRowHeight(22);

        // Renderer simples: pinta a coluna "Ativa" de verde/vermelho.
        tabela.getColumnModel().getColumn(2).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel,
                                                           boolean foc, int row, int col) {
                Component comp = super.getTableCellRendererComponent(t, v, sel, foc, row, col);
                boolean ativa = Boolean.TRUE.equals(v);
                comp.setBackground(sel ? t.getSelectionBackground()
                        : (ativa ? new Color(220, 245, 220) : new Color(245, 220, 220)));
                setHorizontalAlignment(CENTER);
                setText(ativa ? "Sim" : "Não");
                return comp;
            }
        });

        JButton btnAtualizar = new JButton("Atualizar");
        btnAtualizar.addActionListener(e -> atualizar());

        JPanel topo = new JPanel(new BorderLayout());
        topo.add(new JLabel("Interfaces de rede detectadas no computador local:"),
                BorderLayout.WEST);
        topo.add(btnAtualizar, BorderLayout.EAST);

        add(topo, BorderLayout.NORTH);
        add(new JScrollPane(tabela), BorderLayout.CENTER);

        // Carga inicial.
        atualizar();
    }

    /** Recarrega a lista a partir da API NetworkInterface. */
    public final void atualizar() {
        modelo.atualizar(InterfaceRedeInfo.listarLocais());
    }

    /** TableModel customizado simples para a lista de interfaces. */
    private static class ModeloInterfaces extends AbstractTableModel {
        private static final String[] COLUNAS = {
                "Nome", "Descrição", "Ativa", "Loopback", "Endereços"
        };
        private List<InterfaceRedeInfo> dados = java.util.Collections.emptyList();

        void atualizar(List<InterfaceRedeInfo> novos) {
            this.dados = novos;
            fireTableDataChanged();
        }

        @Override public int getRowCount() { return dados.size(); }
        @Override public int getColumnCount() { return COLUNAS.length; }
        @Override public String getColumnName(int c) { return COLUNAS[c]; }

        @Override
        public Class<?> getColumnClass(int c) {
            return (c == 2 || c == 3) ? Boolean.class : String.class;
        }

        @Override
        public Object getValueAt(int r, int c) {
            InterfaceRedeInfo i = dados.get(r);
            switch (c) {
                case 0: return i.getNome();
                case 1: return i.getDescricao();
                case 2: return i.isAtiva();
                case 3: return i.isLoopback();
                case 4: return String.join(", ", i.getEnderecos());
                default: return "";
            }
        }
    }
}
