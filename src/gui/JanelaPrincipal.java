package gui;
import java.awt.EventQueue;

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
import javax.swing.WindowConstants;
import javax.swing.table.DefaultTableCellRenderer;

import model.DispositivoRede;
import model.StatusDispositivo;
import monitor.DispositivoObserver;
import monitor.MonitorDispositivos;

/*
Janela principal: tabela de dispositivos, painel lateral de detalhes
e abas de Interfaces e Rotas. Implementa DispositivoObserver para
receber atualizações do monitor.
*/

// minha janela prinicpla é um JFRame que implementa o observer, assim ela vai receber as atualizações do monitor de dispositivos, que é quem gerencia as threads de cada device
public class JanelaPrincipal extends JFrame implements DispositivoObserver {

    private MonitorDispositivos monitor; //monitora meus dispositivos 
    private ModeloDispositivos modelo = new ModeloDispositivos(); //modelo da tabela de dispositivos
    private JTable tabela = new JTable(modelo);
    private PainelDetalhes painelDetalhes = new PainelDetalhes(); //painel lateral que mostra os detalhes do device selecionado na tabela

    //mostro a janela principal, que é um JFrame, com a tabela de dispositivos, o painel lateral de detalhes e as abas de interfaces e rotas. A janela principal implementa o observer para receber atualizações do monitor de dispositivos.
    public JanelaPrincipal(MonitorDispositivos monitor) {
        super("Gerenciador de Dispositivos de Rede");
        this.monitor = monitor;

        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

        // quando o usuário fecha a janela, eu quero encerrar o monitor e sair da aplicação
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                monitor.encerrar();
                dispose(); // dispose faz a janela ser destruída, liberando recursos
                System.exit(0);
            }
        });

        // construo a interface gráfica da janela principal
        construirInterface();

        // registro a janela principal como observadora do monitor de dispositivos, assim ela vai receber as atualizações dos devices
        monitor.adicionarObservador(this);

        setSize(1050, 600);
        setLocationRelativeTo(null);
    }

    private void construirInterface() {
        setLayout(new BorderLayout());

        JToolBar barra = new JToolBar();
        barra.setFloatable(false); // não quero que a barra de ferramentas seja flutuante
        JButton btnAdd = new JButton("Adicionar");
        JButton btnEdit = new JButton("Editar");
        JButton btnDel = new JButton("Remover");

        // adiciono os botões da barra de ferramentas e defino as ações que eles vão executar
        btnAdd.addActionListener(e -> abrirDialogoNovo());
        btnEdit.addActionListener(e -> abrirDialogoEditar());
        btnDel.addActionListener(e -> removerSelecionado());
        barra.add(btnAdd); barra.add(btnEdit); barra.add(btnDel);

        // configuro a tabela de dispositivos: seleção única, altura das linhas, preenchimento do viewport, ordenação automática e renderização da coluna de status
        tabela.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabela.setRowHeight(24);
        tabela.setFillsViewportHeight(true); // faz a tabela preencher o espaço disponível
        tabela.setAutoCreateRowSorter(true); // permite ordenar as colunas clicando no cabeçalho
        // Coluna 0 (Status) recebe coloração de fundo conforme o enum.
        // isso acontece sempre que a tabela for redesenhada, por exemplo quando o usuário muda a seleção, ou quando o monitor atualiza a métrica de um dispositivo
        tabela.getColumnModel().getColumn(0).setCellRenderer(new RendererStatus()); //pego a coluna 0 da tabela, que é a coluna de status, e seto o renderer para a classe RendererStatus, que vai pintar o fundo da célula com a cor do enum StatusDispositivo
        tabela.getColumnModel().getColumn(0).setPreferredWidth(120);

        // quando o usuário muda a seleção da tabela, eu quero exibir os detalhes do dispositivo selecionado no painel lateral
        tabela.getSelectionModel().addListSelectionListener(e -> {
            painelDetalhes.exibir(selecionado());
        });

        // crio um split pane horizontal com a tabela de dispositivos à esquerda e o painel de detalhes à direita
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(tabela), painelDetalhes);
        split.setResizeWeight(0.65);// 65% da largura para a tabela, 35% para o painel de detalhes
        split.setDividerLocation(680);// posição inicial do divisor, mas o usuário pode arrastar para mudar

        // crio tabs: uma para a tabela de dispositivos e outra para as interfaces de rede
        JTabbedPane abas = new JTabbedPane();
        abas.addTab("Dispositivos", split);
        abas.addTab("Interfaces de Rede", new PainelInterfaces());

        // adiciono a barra de ferramentas no topo e as abas no centro da janela
        add(barra, BorderLayout.NORTH);
        add(abas, BorderLayout.CENTER);
    }

    // acoes

    private void abrirDialogoNovo() {
        //abro um dialogo novo
        DialogoDispositivo dlg = new DialogoDispositivo(this, null);
        dlg.setVisible(true);
        if (dlg.foiConfirmado()) { //espero o usuário confirmar a operação, se ele cancelar, não faço nada
            // quem cria o dispositivo é o dialogo, que chama a factory e valida os campos. Aqui eu só pego o resultado e adiciono no monitor e na tabela
            DispositivoRede novo = dlg.getResultado(); //pego novo dispositivo do dialogo
            monitor.adicionarDispositivo(novo);//coloco no monitor, ele vai criar a thread de coleta para esse dispositivo
            modelo.adicionar(novo);//coloco no modelo da tabela, começando a exibir na tabela
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
            modelo.atualizarLinha(modelo.linhaDe(sel));//atualizo a linha da tabela com os novos dados do dispositivo
            painelDetalhes.exibir(sel);//atualizo o painel de detalhes com os novos dados do dispositivo
        }
    }

    private void removerSelecionado() {
        DispositivoRede sel = selecionado();
        if (sel == null) {
            avisar("Selecione um dispositivo para remover.");
            return;
        }
        int op = JOptionPane.showConfirmDialog(this, "Remover \"" + sel.getNome() + "\"?","Confirmar", JOptionPane.YES_NO_OPTION);
        if (op == JOptionPane.YES_OPTION) {
            monitor.removerDispositivo(sel);
            modelo.remover(modelo.linhaDe(sel));
            painelDetalhes.exibir(null);
        }
    }

    //aviso para o usuário, mostrando uma mensagem em um JOptionPane
    private void avisar(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Aviso", JOptionPane.INFORMATION_MESSAGE);
    }

    // retorna um DispositivoRede selecionado na tabela, ou null se nenhum estiver selecionado
    private DispositivoRede selecionado() {
        int row = tabela.getSelectedRow(); //pega a linha selecionada na tabela, que é a linha visível, mas pode estar ordenada de forma diferente da linha do modelo
        if (row < 0) return null; // se não houver seleção, retorna null
        return modelo.getDispositivo(tabela.convertRowIndexToModel(row)); // converte o índice da linha visível para o índice da linha do modelo, e retorna o dispositivo correspondente
    }

    // Observer (chamado pelas threads do monitor)
    @Override
    public void aoAtualizarDispositivo(DispositivoRede d) {
        // eu mando as edições da tabela e do painel de detalhes para a thread de eventos do Swing, que é a thread que cuida da interface gráfica. Isso evita problemas de concorrência e garante que a interface seja atualizada corretamente.
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                // pego a linha do modelo correspondente ao dispositivo atualizado
                int row = modelo.linhaDe(d);

                // se o dispositivo atualizado estiver na tabela, atualizo a linha correspondente. Se não estiver, não faço nada.
                if (row >= 0) modelo.atualizarLinha(row);

                // se o dispositivo atualizado estiver selecionado, atualizo o painel de detalhes
                DispositivoRede sel = selecionado();
                if (sel != null && sel == d) {
                    painelDetalhes.exibir(d);
                }
            }
        });
    }


    // Renderer da coluna Status: pinta o fundo com a cor do enum.
    // classe interna privada que estende DefaultTableCellRenderer, usada para renderizar a coluna de status da tabela de dispositivos. Ela pega o valor da célula, que é um StatusDispositivo, e define o texto e a cor de fundo da célula de acordo com o status.
    private static class RendererStatus extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object v,boolean sel, boolean foc, int row, int col) {
            super.getTableCellRendererComponent(t, v, sel, foc, row, col); //chama o método da superclasse para configurar a célula com o texto e a seleção padrão
            // v é o valor da célula, que deve ser um StatusDispositivo. Se não for, uso DESCONHECIDO como padrão. Depois seto o texto da célula com a descrição do status, a cor do texto para cinza escuro e a cor de fundo para a cor do status com transparência se não estiver selecionada.
            StatusDispositivo s = (v instanceof StatusDispositivo) ? (StatusDispositivo) v : StatusDispositivo.DESCONHECIDO;
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
