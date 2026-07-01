package gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import classes.DispositivoRede;
import classes.MetricaRede;
import classes.StatusDispositivo;

/*
Painel lateral que exibe os DETALHES do dispositivo selecionado na
tabela principal: status atual, latência, perda, diagnóstico textual
e a rota completa retornada pelo traceroute.
*/
public class PainelDetalhes extends JPanel {

    private JLabel lblTitulo = new JLabel(" ");
    private JLabel lblTipo = new JLabel(" ");
    private JLabel lblStatus = new JLabel(" ");
    private JLabel lblLatencia = new JLabel(" ");
    private JLabel lblPerda = new JLabel(" ");
    private JLabel lblAtualizado = new JLabel(" ");
    private JTextArea areaDiagnostico = new JTextArea(3, 20);
    private JTextArea areaRota = new JTextArea(10, 20);

    public PainelDetalhes() {
        super(new BorderLayout(6, 6));
        setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder("Detalhes do dispositivo"),BorderFactory.createEmptyBorder(6, 6, 6, 6)));

        lblTitulo.setFont(lblTitulo.getFont().deriveFont(Font.BOLD, 14f));

        // topo do painel: título e grid de labels com tipo, status, latência, perda e atualizado
        JPanel topo = new JPanel(new BorderLayout(4, 4));
        topo.add(lblTitulo, BorderLayout.NORTH);

        // grid de labels com tipo, status, latência, perda e atualizado. nao joga so em topo porque quero que o topo fique com altura mínima, e o grid ocupe o resto do espaço
        JPanel grid = new JPanel(new GridLayout(0, 2, 6, 4));
        grid.add(rotulo("Tipo:"));        grid.add(lblTipo);
        grid.add(rotulo("Status:"));      grid.add(lblStatus);
        grid.add(rotulo("Latência:"));    grid.add(lblLatencia);
        grid.add(rotulo("Perda:"));       grid.add(lblPerda);
        grid.add(rotulo("Atualizado:"));  grid.add(lblAtualizado);
        topo.add(grid, BorderLayout.CENTER);

        // centro do painel: área de diagnóstico e área de rota (traceroute)
        areaDiagnostico.setEditable(false); //nao quero que o usuário edite, é só para exibir o diagnóstico textual retornado pelo monitor
        areaDiagnostico.setLineWrap(true); //quero que o texto quebre linha automaticamente, para não precisar de scroll horizontal
        areaDiagnostico.setWrapStyleWord(true); //quero que a quebra de linha respeite palavras, para não cortar palavras no meio
        areaDiagnostico.setBorder(BorderFactory.createTitledBorder("Diagnóstico")); //coloco um título na borda da área de diagnóstico

        areaRota.setEditable(false);
        areaRota.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane scrollRota = new JScrollPane(areaRota); //coloco a área de rota dentro de um JScrollPane, para permitir scroll vertical e horizontal
        scrollRota.setBorder(BorderFactory.createTitledBorder("Rota (traceroute)"));

        JPanel centro = new JPanel(new BorderLayout(6, 6)); //centro do painel, que vai conter a área de diagnóstico e a área de rota
        centro.add(areaDiagnostico, BorderLayout.NORTH); //coloco a área de diagnóstico no norte do centro, para que ela fique acima da área de rota
        centro.add(scrollRota, BorderLayout.CENTER); //coloco a área de rota no centro do centro, para que ela ocupe o resto do espaço disponível

        add(topo, BorderLayout.NORTH); //coloco o topo no norte do painel, para que ele fique acima do centro
        add(centro, BorderLayout.CENTER); //coloco o centro no centro do painel

        limpar(); //inicialmente não há dispositivo selecionado, então limpo os campos
    }


    private static JLabel rotulo(String texto) {
        JLabel l = new JLabel(texto);
        l.setFont(l.getFont().deriveFont(Font.BOLD));
        return l;
    }

    // Exibe os dados de um dispositivo. Aceita null para limpar.
    public void exibir(DispositivoRede d) {
        if (d == null) { //se sem dispositivo, limpa
            limpar();
            return;
        }

        lblTitulo.setText(d.getNome() + " — " + d.getEnderecoIp());
        lblTipo.setText(d.tipoDispositivo());

        MetricaRede m = d.getUltimaMetrica(); //acessa a última métrica coletada do dispositivo, que pode ser null se ainda não houver coleta
        if (m == null) {
            StatusDispositivo s = StatusDispositivo.DESCONHECIDO;
            lblStatus.setText(s.getDescricao());
            lblStatus.setForeground(s.getCor().darker());
            lblLatencia.setText("-");
            lblPerda.setText("-");
            lblAtualizado.setText("-");
            areaDiagnostico.setText("Aguardando primeira coleta...");
            areaRota.setText("");
        } else {
            // status, latência, perda e atualizado são exibidos com base na última métrica coletada
            lblStatus.setText(m.getStatus().getDescricao());
            lblStatus.setForeground(m.getStatus().getCor().darker());
            lblLatencia.setText(m.getLatenciaFormatada());
            lblPerda.setText(m.getPerdaFormatada());
            lblAtualizado.setText(m.getColetadaEmFormatada());

            areaDiagnostico.setText(m.getDiagnostico());
            if (m.getRotaTraceroute().isEmpty()) {
                areaRota.setText("(sem dados de rota — dispositivo inalcançável ou traceroute não pôde ser executado)");
            } else {
                areaRota.setText(String.join(System.lineSeparator(), m.getRotaTraceroute()));
                areaRota.setCaretPosition(0);
            }
        }
    }

    // limpa todos os campos do painel, deixando-os em branco ou com texto padrão
    private void limpar() {
        lblTitulo.setText("Selecione um dispositivo na tabela");
        lblTipo.setText("-");
        lblStatus.setText("-");
        lblStatus.setForeground(Color.DARK_GRAY);
        lblLatencia.setText("-");
        lblPerda.setText("-");
        lblAtualizado.setText("-");
        areaDiagnostico.setText("");
        areaRota.setText("");
    }
}
