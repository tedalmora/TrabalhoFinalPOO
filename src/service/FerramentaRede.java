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
 * Camada que encapsula as ferramentas de diagnóstico de rede:
 * ping e traceroute.
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
