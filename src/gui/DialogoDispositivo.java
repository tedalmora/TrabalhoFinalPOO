package gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
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

import model.DispositivoRede;
import service.DispositivoFactory;
import service.DispositivoFactory.TipoDispositivo;

/**
 * Diálogo modal usado tanto para CADASTRO quanto para EDIÇÃO de
 * dispositivos. Quando aberto sem um dispositivo, opera em modo "novo";
 * quando recebe um dispositivo no construtor, opera em modo "editar".
 *
 * O resultado é exposto por dois getters: {@link #foiConfirmado()} e
 * {@link #getResultado()} — a janela principal lê esses valores depois
 * que o diálogo é fechado.
 */
public class DialogoDispositivo extends JDialog {

    private final JTextField campoNome = new JTextField(20);
    private final JTextField campoIp = new JTextField(20);
    private final JComboBox<TipoDispositivo> comboTipo =
            new JComboBox<>(TipoDispositivo.values());

    // Quando estamos editando, guardamos a referência original para
    // alterá-la in-place ao confirmar (mantendo o mesmo ID e métrica).
    private final DispositivoRede emEdicao;

    private boolean confirmado = false;
    private DispositivoRede resultado;

    public DialogoDispositivo(java.awt.Frame parent, DispositivoRede emEdicao) {
        super(parent, true); // modal
        this.emEdicao = emEdicao;
        setTitle(emEdicao == null ? "Novo dispositivo" : "Editar dispositivo");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        construirInterface();
        preencherCamposSeEdicao();

        pack();
        setMinimumSize(new Dimension(380, getHeight()));
        setLocationRelativeTo(parent);
    }

    /** Monta o formulário com GridBagLayout para alinhar rótulos e campos. */
    private void construirInterface() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.anchor = GridBagConstraints.WEST;

        // Linha 0: Tipo
        c.gridx = 0; c.gridy = 0;
        form.add(new JLabel("Tipo:", SwingConstants.RIGHT), c);
        c.gridx = 1; c.fill = GridBagConstraints.HORIZONTAL; c.weightx = 1;
        form.add(comboTipo, c);

        // Linha 1: Nome
        c.gridx = 0; c.gridy = 1; c.fill = GridBagConstraints.NONE; c.weightx = 0;
        form.add(new JLabel("Nome:", SwingConstants.RIGHT), c);
        c.gridx = 1; c.fill = GridBagConstraints.HORIZONTAL; c.weightx = 1;
        form.add(campoNome, c);

        // Linha 2: IP / hostname
        c.gridx = 0; c.gridy = 2; c.fill = GridBagConstraints.NONE; c.weightx = 0;
        form.add(new JLabel("IP/Host:", SwingConstants.RIGHT), c);
        c.gridx = 1; c.fill = GridBagConstraints.HORIZONTAL; c.weightx = 1;
        form.add(campoIp, c);

        // Botões
        JPanel botoes = new JPanel();
        JButton btnOk = new JButton(new AbstractAction("Confirmar") {
            @Override public void actionPerformed(ActionEvent e) { confirmar(); }
        });
        JButton btnCancel = new JButton(new AbstractAction("Cancelar") {
            @Override public void actionPerformed(ActionEvent e) { dispose(); }
        });
        botoes.add(btnOk);
        botoes.add(btnCancel);

        // Faz "Enter" confirmar e "Esc" cancelar (UX básica).
        getRootPane().setDefaultButton(btnOk);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(form, BorderLayout.CENTER);
        getContentPane().add(botoes, BorderLayout.SOUTH);
    }

    private void preencherCamposSeEdicao() {
        if (emEdicao != null) {
            campoNome.setText(emEdicao.getNome());
            campoIp.setText(emEdicao.getEnderecoIp());
            comboTipo.setSelectedItem(DispositivoFactory.tipoDe(emEdicao));
            // Trocar o tipo de um dispositivo já existente complicaria
            // a vida do monitor (ID novo, métrica perdida). Mantemos
            // tipo fixo na edição.
            comboTipo.setEnabled(false);
        }
    }

    private void confirmar() {
        try {
            String nome = campoNome.getText();
            String ip = campoIp.getText();
            TipoDispositivo tipo = (TipoDispositivo) comboTipo.getSelectedItem();

            if (emEdicao == null) {
                resultado = DispositivoFactory.criar(tipo, nome, ip);
            } else {
                // Edição in-place: setters validam internamente.
                emEdicao.setNome(nome.trim());
                emEdicao.setEnderecoIp(ip.trim());
                resultado = emEdicao;
            }
            confirmado = true;
            dispose();
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(
                    this, ex.getMessage(), "Dados inválidos",
                    JOptionPane.WARNING_MESSAGE);
        }
    }

    public boolean foiConfirmado() {
        return confirmado;
    }

    public DispositivoRede getResultado() {
        return resultado;
    }
}
