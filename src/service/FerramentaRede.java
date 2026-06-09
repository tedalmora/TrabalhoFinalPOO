package service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Camada que encapsula as ferramentas de diagnóstico de rede:
 * ping, traceroute, DNS direto/reverso, teste de porta TCP e HTTP HEAD/GET.
 * Detecta Windows vs. Linux em tempo de execução.
 *
 * Todas as chamadas são bloqueantes — quem orquestra é o MonitorDispositivos
 * em threads de background.
 */
public final class FerramentaRede {

    private static final boolean WINDOWS =
            System.getProperty("os.name", "").toLowerCase().contains("win");
    private static final Charset CHARSET = Charset.defaultCharset();

    private FerramentaRede() { }

    // -----------------------------------------------------------------
    // Ping (também usado como "MTR simplificado" quando qtd = 8)
    // -----------------------------------------------------------------

    public static ResultadoPing ping(String host, int quantidade) {
        List<String> cmd = new ArrayList<>();
        cmd.add("ping");
        if (WINDOWS) {
            cmd.add("-n"); cmd.add(String.valueOf(quantidade));
            cmd.add("-w"); cmd.add("1500");
        } else {
            cmd.add("-c"); cmd.add(String.valueOf(quantidade));
            cmd.add("-W"); cmd.add("2");
        }
        cmd.add(host);

        List<String> linhas = executar(cmd, 15);

        // Aceita "time=12.3 ms", "time<1ms", "tempo=12ms".
        Pattern patLat = Pattern.compile(
                "(?:time|tempo)[=<]([0-9]+(?:[.,][0-9]+)?)", Pattern.CASE_INSENSITIVE);
        Pattern patPerda = Pattern.compile(
                "([0-9]+)% (?:packet )?(?:loss|perda|perdidos)", Pattern.CASE_INSENSITIVE);

        double soma = 0;
        int respostas = 0;
        double perda = -1;
        for (String l : linhas) {
            Matcher m = patLat.matcher(l);
            if (m.find()) {
                try {
                    soma += Double.parseDouble(m.group(1).replace(',', '.'));
                    respostas++;
                } catch (NumberFormatException ignored) { }
            }
            Matcher mp = patPerda.matcher(l);
            if (mp.find()) {
                try { perda = Double.parseDouble(mp.group(1)); }
                catch (NumberFormatException ignored) { }
            }
        }
        // Fallback se o parser de % não pegou.
        if (perda < 0) {
            perda = ((quantidade - respostas) * 100.0) / Math.max(1, quantidade);
        }

        boolean alcancavel = respostas > 0;
        double media = alcancavel ? (soma / respostas) : -1.0;
        return new ResultadoPing(alcancavel, media, perda);
    }

    // -----------------------------------------------------------------
    // Traceroute
    // -----------------------------------------------------------------

    public static List<String> traceroute(String host) {
        List<String> cmd = new ArrayList<>();
        if (WINDOWS) {
            cmd.add("tracert"); cmd.add("-d"); cmd.add("-h"); cmd.add("20");
        } else {
            cmd.add("traceroute"); cmd.add("-n"); cmd.add("-m"); cmd.add("20");
        }
        cmd.add(host);

        List<String> bruto = executar(cmd, 40);
        List<String> hops = new ArrayList<>();
        Pattern patHop = Pattern.compile("^\\s*\\d+\\s+.*");
        for (String linha : bruto) {
            if (patHop.matcher(linha).matches()) hops.add(linha.trim());
        }
        return hops;
    }

    // -----------------------------------------------------------------
    // DNS direto e reverso
    // -----------------------------------------------------------------

    public static ResultadoDns resolverDnsDireto(String host) {
        long inicio = System.currentTimeMillis();
        try {
            InetAddress[] enderecos = InetAddress.getAllByName(host);
            List<String> ips = new ArrayList<>();
            for (InetAddress a : enderecos) ips.add(a.getHostAddress());
            return new ResultadoDns(true, true, ips,
                    System.currentTimeMillis() - inicio, null);
        } catch (Exception e) {
            return new ResultadoDns(true, false, null,
                    System.currentTimeMillis() - inicio, e.getClass().getSimpleName());
        }
    }

    /**
     * Reverso (PTR). O truque do getByAddress força a JVM a consultar
     * o DNS, em vez de devolver o hostname já em cache.
     */
    public static ResultadoDns resolverDnsReverso(String host) {
        long inicio = System.currentTimeMillis();
        try {
            InetAddress alvo = InetAddress.getByName(host);
            InetAddress soIp = InetAddress.getByAddress(alvo.getAddress());
            String nome = soIp.getCanonicalHostName();
            long tempo = System.currentTimeMillis() - inicio;
            if (nome == null || nome.equals(soIp.getHostAddress())) {
                return new ResultadoDns(false, false,
                        Arrays.asList(soIp.getHostAddress()), tempo,
                        "IP sem registro PTR");
            }
            return new ResultadoDns(false, true,
                    Arrays.asList(nome), tempo, null);
        } catch (Exception e) {
            return new ResultadoDns(false, false, null,
                    System.currentTimeMillis() - inicio, e.getClass().getSimpleName());
        }
    }

    // -----------------------------------------------------------------
    // Teste de porta TCP
    // -----------------------------------------------------------------

    /** Tenta abrir uma conexão TCP. Útil quando ICMP está bloqueado. */
    public static ResultadoTcp testarPortaTcp(String host, int porta, int timeoutMs) {
        long inicio = System.currentTimeMillis();
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress(host, porta), timeoutMs);
            return new ResultadoTcp(porta, true,
                    System.currentTimeMillis() - inicio, null);
        } catch (Exception e) {
            return new ResultadoTcp(porta, false,
                    System.currentTimeMillis() - inicio, e.getClass().getSimpleName());
        }
    }

    // -----------------------------------------------------------------
    // HTTP HEAD (com fallback para GET)
    // -----------------------------------------------------------------

    /**
     * Faz HEAD (e GET se for 405). Escolhe http ou https com base na porta:
     * 443 → https; outras → http. Sucesso = qualquer resposta HTTP.
     */
    public static ResultadoHttp testarHttp(String host, Integer portaTcp) {
        String url;
        if (portaTcp != null && portaTcp == 443) {
            url = "https://" + host;
        } else if (portaTcp != null && portaTcp != 80) {
            url = "http://" + host + ":" + portaTcp;
        } else {
            url = "http://" + host;
        }

        long inicio = System.currentTimeMillis();
        HttpURLConnection conn = null;
        try {
            URL u = URI.create(url).toURL();
            conn = abrirHttp(u, "HEAD");
            int status = conn.getResponseCode();
            String msg = conn.getResponseMessage();
            if (status == HttpURLConnection.HTTP_BAD_METHOD) {
                conn.disconnect();
                conn = abrirHttp(u, "GET");
                status = conn.getResponseCode();
                msg = conn.getResponseMessage();
            }
            return new ResultadoHttp(true, status, msg == null ? "" : msg,
                    url, System.currentTimeMillis() - inicio);
        } catch (Exception e) {
            return new ResultadoHttp(false, -1,
                    e.getClass().getSimpleName() + ": " + e.getMessage(),
                    url, System.currentTimeMillis() - inicio);
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private static HttpURLConnection abrirHttp(URL u, String metodo) throws Exception {
        HttpURLConnection c = (HttpURLConnection) u.openConnection();
        c.setRequestMethod(metodo);
        c.setConnectTimeout(4000);
        c.setReadTimeout(4000);
        c.setInstanceFollowRedirects(true);
        c.setRequestProperty("User-Agent", "GerenciadorRede/1.0");
        return c;
    }

    // -----------------------------------------------------------------
    // Execução de processos do SO
    // -----------------------------------------------------------------

    /**
     * Roda um comando do SO e devolve sua saída como lista de linhas.
     * Com timeout: se passar do limite, o processo é morto.
     */
    private static List<String> executar(List<String> cmd, long timeoutSeg) {
        List<String> linhas = new ArrayList<>();
        Process proc = null;
        try {
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            proc = pb.start();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(proc.getInputStream(), CHARSET))) {
                String l;
                while ((l = br.readLine()) != null) linhas.add(l);
            }
            if (!proc.waitFor(timeoutSeg, TimeUnit.SECONDS)) {
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
