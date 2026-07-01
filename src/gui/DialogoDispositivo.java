package gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;

import classes.DispositivoRede;
import servicos.DispositivoFactory;
import servicos.DispositivoFactory.TipoDispositivo;

/*
Dialogos para cadastro, edição e remoção de dispositivos.
 */
public class DialogoDispositivo extends JDialog {

    private JTextField campoNome = new JTextField(20);
    private JTextField campoIp = new JTextField(20);
    private JComboBox<TipoDispositivo> comboTipo = new JComboBox<>(TipoDispositivo.values());

    // emEdicao serve para saber se o dialogo foi aberto para criar um novo dispositivo (emEdicao == null) ou para editar um dispositivo existente (emEdicao != null). Se for edição, os campos serão preenchidos com os dados do dispositivo e o tipo não poderá ser alterado.
    private DispositivoRede emEdicao;

    private boolean confirmado = false;
    private DispositivoRede resultado;

    // Construtor do diálogo. Se emEdicao for null, o diálogo será para criar um novo dispositivo. Se emEdicao for diferente de null, o diálogo será para editar o dispositivo existente.
    public DialogoDispositivo(java.awt.Frame parent, DispositivoRede emEdicao) {
        super(parent, true); // modal
        this.emEdicao = emEdicao;
        setTitle(emEdicao == null ? "Novo dispositivo" : "Editar dispositivo");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        construirInterface();
        preencherCamposSeEdicao();

        pack();// ajusta o tamanho da janela para caber os componentes
        setMinimumSize(new Dimension(380, getHeight()));
        setLocationRelativeTo(parent); // centraliza a janela em relação à janela pai
    }

    // monta o formulario
    private void construirInterface() {
        // fraço um grid pra posicionar os labels e campos de texto
        JPanel form = new JPanel(new GridLayout(3, 2, 5, 5));
        form.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        // Linha 0: Tipo
        form.add(new JLabel("Tipo:", SwingConstants.RIGHT));
        form.add(comboTipo);

        // Linha 1: Nome
        form.add(new JLabel("Nome:", SwingConstants.RIGHT));
        form.add(campoNome);

        // Linha 2: IP / hostname
        form.add(new JLabel("IP/Host:", SwingConstants.RIGHT));
        form.add(campoIp);

        // Botões
        JPanel botoes = new JPanel();
        JButton btnOk = new JButton(new AbstractAction("Confirmar") {
            @Override public void actionPerformed(ActionEvent e) { confirmar(); } //jogo pra o método confirmar() que valida os campos e cria/edita o dispositivo
        });
        JButton btnCancel = new JButton(new AbstractAction("Cancelar") {
            @Override public void actionPerformed(ActionEvent e) { dispose(); } //jogo pra o método dispose() que fecha o diálogo
        }); //dispose é um metodo da propria JDialog
        botoes.add(btnOk);
        botoes.add(btnCancel);

        //adiciono os paineis a tela de dialogo criada
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(form, BorderLayout.CENTER);
        getContentPane().add(botoes, BorderLayout.SOUTH);
    }


    private void preencherCamposSeEdicao() {
        if (emEdicao != null) {
            campoNome.setText(emEdicao.getNome()); // preenche o campo de nome com o nome do dispositivo em edição
            campoIp.setText(emEdicao.getEnderecoIp()); // preenche o campo de IP com o IP do dispositivo em edição
            comboTipo.setSelectedItem(DispositivoFactory.tipoDe(emEdicao));// preenche o combo de tipo com o tipo do dispositivo em edição
            // Trocar o tipo de um dispositivo já existente complicaria
            // a vida do monitor (ID novo, métrica perdida). Mantemos
            // tipo fixo na edição.
            comboTipo.setEnabled(false);// desabilita o combo de tipo para que o usuário não possa alterar o tipo do dispositivo em edição
        }
        //se ele nao for edicao, voce nao preenche nada
    }

    private void confirmar() {
        try {
            //pego nome ip e tipo
            String nome = campoNome.getText();
            String ip = campoIp.getText();
            TipoDispositivo tipo = (TipoDispositivo) comboTipo.getSelectedItem();

            if (emEdicao == null) {
                // se nao for edicao, eu crio um novo dispositivo com os dados informados
                resultado = DispositivoFactory.criar(tipo, nome, ip);
            } else {
                // se for edição, eu so mudo nome e ip
                emEdicao.setNome(nome.trim());
                emEdicao.setEnderecoIp(ip.trim());
                resultado = emEdicao;
            }
            confirmado = true; // marco que o usuário confirmou a operação para que o chamador saiba que ele confirmou e nao cancelou
            dispose(); // fecho a janela
        } catch (IllegalArgumentException ex) { //se argumento invalido
            // mando uma mensagem de aviso
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Dados inválidos", JOptionPane.WARNING_MESSAGE);
        }
    }

    public boolean foiConfirmado() {
        return confirmado;
    }

    public DispositivoRede getResultado() {
        return resultado;
    }
}