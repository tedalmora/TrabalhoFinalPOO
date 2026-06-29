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

// baiscamente tenho uma tabela de service.InterfaceRedeInfo, que é a classe que encapsula as informações de cada interface de rede local, como nome, descrição, status, endereços IP, etc. A tabela é alimentada por um TableModel customizado (ModeloInterfaces) que implementa AbstractTableModel. O painel também tem um botão "Atualizar" que chama o método atualizar() para atualizar a lista de interfaces chamando InterfaceRedeInfo.listarLocais().

// aqui sao as interfaces do meu PC
public class PainelInterfaces extends JPanel {

    // modelo da tabela de interfaces, que é um TableModel customizado.
    private ModeloInterfaces modelo = new ModeloInterfaces();

    public PainelInterfaces() {
        super(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JTable tabela = new JTable(modelo); //tabela que vai exibir as interfaces de rede detectadas no computador local
        tabela.setFillsViewportHeight(true); //faz a tabela preencher o espaço disponível, mesmo que não haja linhas suficientes para isso
        tabela.setRowHeight(22);

        JButton btnAtualizar = new JButton("Atualizar");
        btnAtualizar.addActionListener(e -> atualizar());

        //parte superior do painel: label e botão de atualizar
        JPanel topo = new JPanel(new BorderLayout());
        topo.add(new JLabel("Interfaces de rede detectadas no computador local:"),BorderLayout.WEST);
        topo.add(btnAtualizar, BorderLayout.EAST);

        add(topo, BorderLayout.NORTH);
        add(new JScrollPane(tabela), BorderLayout.CENTER);
        atualizar(); //mando atualizar a lista de interfaces
    }

    // a funcao atualizar() usa a modeloatualiazr e pega a lista de interfaces locais chamando InterfaceRedeInfo.listarLocais()
    // interfaceredeinfo é a classe que encapsula as informações de cada interface de rede local, como nome, descrição, status, endereços IP, etc.
    public void atualizar() {
        modelo.atualizar(InterfaceRedeInfo.listarLocais());
    }

    // TableModel customizado que alimenta a JTable de interfaces de rede.
    private static class ModeloInterfaces extends AbstractTableModel {
        private static String[] COLUNAS = {
                "Nome", "Descrição", "Ativa", "Loopback", "Endereços"
        };

        // lista de interfaces de rede detectadas no computador local. Inicialmente vazia.
        private List<InterfaceRedeInfo> dados = java.util.Collections.emptyList();

        // atualiza a lista de interfaces e notifica a JTable que os dados mudaram, para que ela se redesenhe
        void atualizar(List<InterfaceRedeInfo> novos) {
            this.dados = novos;
            fireTableDataChanged();//tabela mudou, JTable precisa se redesenhar
        }

        @Override public int getRowCount() { return dados.size(); }
        @Override public int getColumnCount() { return COLUNAS.length; }
        @Override public String getColumnName(int c) { return COLUNAS[c]; }

        @Override
        // Retorna a classe da coluna, para que a JTable saiba como renderizar os dados. Colunas 2 e 3 são booleanas, as outras são strings.
        public Class<?> getColumnClass(int c) {
            return (c == 2 || c == 3) ? Boolean.class : String.class;
        }

        @Override
        // Retorna o valor de uma célula específica da tabela, baseado na linha e coluna.
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
