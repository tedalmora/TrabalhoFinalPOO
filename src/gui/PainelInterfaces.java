package gui;

import java.awt.BorderLayout;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

import service.InterfaceRedeInfo;

/** Aba que lista as interfaces de rede locais do computador. */
public class PainelInterfaces extends JPanel {

    private final ModeloInterfaces modelo = new ModeloInterfaces();

    public PainelInterfaces() {
        super(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JTable tabela = new JTable(modelo);
        tabela.setFillsViewportHeight(true);
        tabela.setRowHeight(22);

        JButton btnAtualizar = new JButton("Atualizar");
        btnAtualizar.addActionListener(e -> atualizar());

        JPanel topo = new JPanel(new BorderLayout());
        topo.add(new JLabel("Interfaces de rede detectadas no computador local:"),
                BorderLayout.WEST);
        topo.add(btnAtualizar, BorderLayout.EAST);

        add(topo, BorderLayout.NORTH);
        add(new JScrollPane(tabela), BorderLayout.CENTER);
        atualizar();
    }

    public final void atualizar() {
        modelo.atualizar(InterfaceRedeInfo.listarLocais());
    }

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
