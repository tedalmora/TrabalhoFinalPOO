package gui;

import java.awt.BorderLayout;
import java.awt.Font;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

import service.RotaInfo;

/**
 * Painel que exibe a tabela de rotas IPv4 do sistema operacional local.
 * Funciona de forma análoga ao {@link PainelInterfaces}: tem um botão
 * "Atualizar" que recarrega os dados executando o comando do SO
 * (route print no Windows, netstat -rn no Linux).
 */
public class PainelRotas extends JPanel {

    private final ModeloRotas modelo = new ModeloRotas();

    public PainelRotas() {
        super(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JTable tabela = new JTable(modelo);
        tabela.setFillsViewportHeight(true);
        tabela.setRowHeight(22);
        tabela.setAutoCreateRowSorter(true);
        // Fonte monoespaçada combina com tabela de rotas (alinha IPs).
        tabela.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        JButton btnAtualizar = new JButton("Atualizar");
        btnAtualizar.addActionListener(e -> atualizar());

        JPanel topo = new JPanel(new BorderLayout());
        topo.add(new JLabel("Tabela de rotas IPv4 do computador local:"),
                BorderLayout.WEST);
        topo.add(btnAtualizar, BorderLayout.EAST);

        add(topo, BorderLayout.NORTH);
        add(new JScrollPane(tabela), BorderLayout.CENTER);

        atualizar();
    }

    /** Recarrega a tabela a partir do SO. */
    public final void atualizar() {
        modelo.atualizar(RotaInfo.listarLocais());
    }

    /** Modelo simples para alimentar a JTable de rotas. */
    private static class ModeloRotas extends AbstractTableModel {
        private static final String[] COLUNAS = {
                "Destino", "Máscara", "Gateway", "Interface", "Métrica"
        };
        private List<RotaInfo> dados = java.util.Collections.emptyList();

        void atualizar(List<RotaInfo> novos) {
            this.dados = novos;
            fireTableDataChanged();
        }

        @Override public int getRowCount() { return dados.size(); }
        @Override public int getColumnCount() { return COLUNAS.length; }
        @Override public String getColumnName(int c) { return COLUNAS[c]; }

        @Override
        public Object getValueAt(int r, int c) {
            RotaInfo i = dados.get(r);
            switch (c) {
                case 0: return i.getDestino();
                case 1: return i.getMascara();
                case 2: return i.getGateway();
                case 3: return i.getInterfaceLocal();
                case 4: return i.getMetrica();
                default: return "";
            }
        }
    }
}
