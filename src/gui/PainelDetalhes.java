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

import model.DispositivoRede;
import model.MetricaRede;
import model.StatusDispositivo;
import service.ResultadoDns;
import service.ResultadoHttp;
import service.ResultadoTcp;

/**
 * Painel lateral que exibe os DETALHES do dispositivo selecionado na
 * tabela principal: status atual, latência, perda, resultados de DNS
 * (direto/reverso), porta TCP, HTTP, diagnóstico textual e a rota
 * completa retornada pelo traceroute.
 *
 * Sempre que o usuário muda a seleção da tabela OU quando uma métrica
 * do dispositivo selecionado é atualizada, a janela principal chama
 * {@link #exibir(DispositivoRede)} aqui.
 */
public class PainelDetalhes extends JPanel {

    private final JLabel lblTitulo = new JLabel(" ");
    private final JLabel lblTipo = new JLabel(" ");
    private final JLabel lblStatus = new JLabel(" ");
    private final JLabel lblLatencia = new JLabel(" ");
    private final JLabel lblPerda = new JLabel(" ");
    private final JLabel lblAtualizado = new JLabel(" ");
    private final JLabel lblDnsDireto = new JLabel(" ");
    private final JLabel lblDnsReverso = new JLabel(" ");
    private final JLabel lblTcp = new JLabel(" ");
    private final JLabel lblHttp = new JLabel(" ");
    private final JTextArea areaDiagnostico = new JTextArea(3, 20);
    private final JTextArea areaRota = new JTextArea(10, 20);

    public PainelDetalhes() {
        super(new BorderLayout(6, 6));
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Detalhes do dispositivo"),
                BorderFactory.createEmptyBorder(6, 6, 6, 6)));

        lblTitulo.setFont(lblTitulo.getFont().deriveFont(Font.BOLD, 14f));

        JPanel topo = new JPanel(new BorderLayout(4, 4));
        topo.add(lblTitulo, BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(0, 2, 6, 4));
        grid.add(rotulo("Tipo:"));        grid.add(lblTipo);
        grid.add(rotulo("Status:"));      grid.add(lblStatus);
        grid.add(rotulo("Latência:"));    grid.add(lblLatencia);
        grid.add(rotulo("Perda:"));       grid.add(lblPerda);
        grid.add(rotulo("Atualizado:"));  grid.add(lblAtualizado);
        grid.add(rotulo("DNS direto:"));  grid.add(lblDnsDireto);
        grid.add(rotulo("DNS reverso:")); grid.add(lblDnsReverso);
        grid.add(rotulo("Porta TCP:"));   grid.add(lblTcp);
        grid.add(rotulo("HTTP:"));        grid.add(lblHttp);
        topo.add(grid, BorderLayout.CENTER);

        areaDiagnostico.setEditable(false);
        areaDiagnostico.setLineWrap(true);
        areaDiagnostico.setWrapStyleWord(true);
        areaDiagnostico.setBorder(BorderFactory.createTitledBorder("Diagnóstico"));

        areaRota.setEditable(false);
        areaRota.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane scrollRota = new JScrollPane(areaRota);
        scrollRota.setBorder(BorderFactory.createTitledBorder("Rota (traceroute)"));

        JPanel centro = new JPanel(new BorderLayout(6, 6));
        centro.add(areaDiagnostico, BorderLayout.NORTH);
        centro.add(scrollRota, BorderLayout.CENTER);

        add(topo, BorderLayout.NORTH);
        add(centro, BorderLayout.CENTER);

        limpar();
    }

    private static JLabel rotulo(String texto) {
        JLabel l = new JLabel(texto);
        l.setFont(l.getFont().deriveFont(Font.BOLD));
        return l;
    }

    /** Exibe os dados de um dispositivo. Aceita null para limpar. */
    public void exibir(DispositivoRede d) {
        if (d == null) {
            limpar();
            return;
        }
        lblTitulo.setText(d.getNome() + " — " + d.getEnderecoIp());
        lblTipo.setText(d.tipoDispositivo());

        MetricaRede m = d.getUltimaMetrica();
        if (m == null) {
            StatusDispositivo s = StatusDispositivo.DESCONHECIDO;
            lblStatus.setText(s.getDescricao());
            lblStatus.setForeground(s.getCor().darker());
            lblLatencia.setText("-");
            lblPerda.setText("-");
            lblAtualizado.setText("-");
            lblDnsDireto.setText("-");
            lblDnsReverso.setText("-");
            lblTcp.setText(d.getPortaTcp() == null
                    ? "(não configurado)"
                    : "aguardando teste na porta " + d.getPortaTcp());
            lblHttp.setText(d.isVerificarHttp() ? "aguardando teste" : "(desabilitado)");
            areaDiagnostico.setText("Aguardando primeira coleta...");
            areaRota.setText("");
        } else {
            lblStatus.setText(m.getStatus().getDescricao());
            lblStatus.setForeground(m.getStatus().getCor().darker());
            lblLatencia.setText(m.getLatenciaFormatada());
            lblPerda.setText(m.getPerdaFormatada());
            lblAtualizado.setText(m.getColetadaEmFormatada());

            lblDnsDireto.setText(formatarDns(m.getDnsDireto()));
            lblDnsReverso.setText(formatarDns(m.getDnsReverso()));
            lblTcp.setText(formatarTcp(d, m.getTcp()));
            lblHttp.setText(formatarHttp(d, m.getHttp()));

            areaDiagnostico.setText(m.getDiagnostico());
            if (m.getRotaTraceroute().isEmpty()) {
                areaRota.setText("(sem dados de rota — dispositivo inalcançável "
                        + "ou traceroute não pôde ser executado)");
            } else {
                areaRota.setText(String.join(System.lineSeparator(), m.getRotaTraceroute()));
                areaRota.setCaretPosition(0);
            }
        }
    }

    private void limpar() {
        lblTitulo.setText("Selecione um dispositivo na tabela");
        lblTipo.setText("-");
        lblStatus.setText("-");
        lblStatus.setForeground(Color.DARK_GRAY);
        lblLatencia.setText("-");
        lblPerda.setText("-");
        lblAtualizado.setText("-");
        lblDnsDireto.setText("-");
        lblDnsReverso.setText("-");
        lblTcp.setText("-");
        lblHttp.setText("-");
        areaDiagnostico.setText("");
        areaRota.setText("");
    }

    // -----------------------------------------------------------------
    // Formatadores para os novos campos
    // -----------------------------------------------------------------

    private static String formatarDns(ResultadoDns r) {
        if (r == null) return "-";
        return r.resumo();
    }

    private static String formatarTcp(DispositivoRede d, ResultadoTcp r) {
        if (d.getPortaTcp() == null) {
            return "(não configurado)";
        }
        if (r == null) return "-";
        return r.resumo();
    }

    private static String formatarHttp(DispositivoRede d, ResultadoHttp r) {
        if (!d.isVerificarHttp()) {
            return "(desabilitado)";
        }
        if (r == null) return "-";
        return r.resumo();
    }
}
