package service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Representa uma única entrada da tabela de rotas do sistema operacional.
 *
 * É preenchida pelo método estático {@link #listarLocais()}, que executa
 * o comando apropriado para o SO (`route print -4` no Windows ou
 * `netstat -rn` no Linux/macOS) e faz o parsing da saída.
 *
 * Campos:
 *  - destino  : rede de destino (ex.: "0.0.0.0", "192.168.1.0");
 *  - mascara  : máscara de sub-rede (ex.: "255.255.255.0");
 *  - gateway  : próximo salto (ex.: "192.168.1.1" ou "On-link");
 *  - interfaceLocal : IP da interface usada para essa rota;
 *  - metrica  : custo da rota (quanto menor, mais preferida).
 */
public class RotaInfo {

    private static final boolean WINDOWS =
            System.getProperty("os.name", "").toLowerCase().contains("win");

    private final String destino;
    private final String mascara;
    private final String gateway;
    private final String interfaceLocal;
    private final String metrica;

    public RotaInfo(String destino, String mascara, String gateway,
                    String interfaceLocal, String metrica) {
        this.destino = destino;
        this.mascara = mascara;
        this.gateway = gateway;
        this.interfaceLocal = interfaceLocal;
        this.metrica = metrica;
    }

    public String getDestino() { return destino; }
    public String getMascara() { return mascara; }
    public String getGateway() { return gateway; }
    public String getInterfaceLocal() { return interfaceLocal; }
    public String getMetrica() { return metrica; }

    /**
     * Lê a tabela de rotas IPv4 do sistema operacional. Em caso de
     * erro devolve lista vazia (o painel da GUI tratará).
     */
    public static List<RotaInfo> listarLocais() {
        List<String> linhas = executarComando(comandoDoSO(), 10);
        if (WINDOWS) {
            return parseWindows(linhas);
        }
        return parseUnix(linhas);
    }

    // -----------------------------------------------------------------
    // Parsing
    // -----------------------------------------------------------------

    /**
     * Parse da saída do `route print -4` no Windows. A seção que nos
     * interessa começa após o cabeçalho "Active Routes:" /
     * "Rotas ativas:" e termina em uma linha de "=====".
     *
     * Cada linha relevante tem 5 colunas:
     *   Destino   Máscara   Gateway   Interface   Métrica
     */
    private static List<RotaInfo> parseWindows(List<String> linhas) {
        List<RotaInfo> rotas = new ArrayList<>();
        boolean dentroDaTabela = false;

        // Aceita os cabeçalhos em inglês e em português.
        Pattern patInicio = Pattern.compile(
                "(Active Routes:|Rotas ativas:)", Pattern.CASE_INSENSITIVE);
        // Linha de separação ("====...") encerra a seção.
        Pattern patFim = Pattern.compile("^=+\\s*$");
        // 5 colunas separadas por espaços. Aceita "On-link" no gateway.
        Pattern patLinha = Pattern.compile(
                "^\\s*(\\S+)\\s+(\\S+)\\s+(\\S+|On-link|No-link)\\s+(\\S+)\\s+(\\d+)\\s*$");

        for (String linha : linhas) {
            if (!dentroDaTabela) {
                if (patInicio.matcher(linha).find()) {
                    dentroDaTabela = true;
                }
                continue;
            }
            if (patFim.matcher(linha).matches()) {
                // Saiu da tabela IPv4 — paramos para não pegar a IPv6.
                break;
            }
            Matcher m = patLinha.matcher(linha);
            if (m.matches()) {
                // Pula o cabeçalho "Network Destination ..." que também
                // bate na regex porque tem colunas que parecem dados.
                if (m.group(1).equalsIgnoreCase("Network")
                        || m.group(1).equalsIgnoreCase("Destino")) {
                    continue;
                }
                rotas.add(new RotaInfo(
                        m.group(1), m.group(2), m.group(3),
                        m.group(4), m.group(5)));
            }
        }
        return rotas;
    }

    /**
     * Parse da saída do `netstat -rn` em Linux/macOS. As colunas
     * costumam ser: Destination Gateway Genmask Flags MSS Window irtt Iface
     * (ou variantes; pegamos apenas as 4 primeiras + última quando dá).
     */
    private static List<RotaInfo> parseUnix(List<String> linhas) {
        List<RotaInfo> rotas = new ArrayList<>();
        boolean cabecalhoVisto = false;

        for (String linha : linhas) {
            String l = linha.trim();
            if (l.isEmpty()) continue;
            if (l.toLowerCase().startsWith("kernel ip routing")) continue;
            if (l.toLowerCase().startsWith("destination") || l.toLowerCase().startsWith("destino")) {
                cabecalhoVisto = true;
                continue;
            }
            if (!cabecalhoVisto) continue;

            String[] partes = l.split("\\s+");
            if (partes.length < 4) continue;

            String destino = partes[0];
            String gateway = partes[1];
            String mascara = partes[2];
            String iface   = partes[partes.length - 1];
            rotas.add(new RotaInfo(destino, mascara, gateway, iface, "-"));
        }
        return rotas;
    }

    // -----------------------------------------------------------------
    // Execução de comando externo
    // -----------------------------------------------------------------

    private static List<String> comandoDoSO() {
        List<String> cmd = new ArrayList<>();
        if (WINDOWS) {
            cmd.add("route");
            cmd.add("print");
            cmd.add("-4");
        } else {
            cmd.add("netstat");
            cmd.add("-rn");
        }
        return cmd;
    }

    private static List<String> executarComando(List<String> comando, long timeoutSegundos) {
        List<String> linhas = new ArrayList<>();
        Process proc = null;
        try {
            ProcessBuilder pb = new ProcessBuilder(comando);
            pb.redirectErrorStream(true);
            proc = pb.start();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(proc.getInputStream(), Charset.defaultCharset()))) {
                String linha;
                while ((linha = br.readLine()) != null) {
                    linhas.add(linha);
                }
            }
            if (!proc.waitFor(timeoutSegundos, TimeUnit.SECONDS)) {
                proc.destroyForcibly();
            }
        } catch (Exception e) {
            return Collections.emptyList();
        } finally {
            if (proc != null && proc.isAlive()) proc.destroyForcibly();
        }
        return linhas;
    }
}
