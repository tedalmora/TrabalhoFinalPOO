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
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Camada de "service" que encapsula as ferramentas de diagnóstico de rede
 * (ping, traceroute, MTR simplificado). Toda a lógica de invocar processos
 * do sistema operacional e fazer o parsing da saída fica concentrada aqui,
 * de modo que o restante do sistema trabalhe com objetos Java limpos.
 *
 * Pontos importantes:
 *  - Funciona em Windows e Linux (detecta o SO em tempo de execução);
 *  - "MTR" é implementado de forma portátil: enviamos N pings e calculamos
 *    média + perda, satisfazendo a descrição do trabalho ("combinando
 *    latência e perda de pacotes");
 *  - As chamadas são bloqueantes e devem ser executadas em threads de
 *    background — quem orquestra isso é o pacote `monitor`.
 */
public final class FerramentaRede {

    // True quando estamos rodando em Windows. Definido uma única vez.
    private static final boolean WINDOWS =
            System.getProperty("os.name", "").toLowerCase().contains("win");

    // Charset usado para ler a saída dos comandos. Em Windows o "ping"
    // tradicional usa a code page do console (CP850/CP1252 em PT-BR), mas
    // como só procuramos números (latência, %perda) o default funciona bem.
    private static final Charset CHARSET = Charset.defaultCharset();

    private FerramentaRede() {
        // Classe utilitária — não deve ser instanciada.
    }

    /**
     * Executa um ping com a quantidade de pacotes informada.
     *
     * @param host       endereço IP ou hostname.
     * @param quantidade número de pacotes a enviar (tipicamente 4).
     */
    public static ResultadoPing ping(String host, int quantidade) {
        // Monta a linha de comando de acordo com o SO.
        List<String> comando = new ArrayList<>();
        comando.add("ping");
        if (WINDOWS) {
            comando.add("-n");
            comando.add(String.valueOf(quantidade));
            // -w define o timeout por resposta, em ms.
            comando.add("-w");
            comando.add("1500");
        } else {
            comando.add("-c");
            comando.add(String.valueOf(quantidade));
            // -W define o timeout por resposta, em segundos.
            comando.add("-W");
            comando.add("2");
        }
        comando.add(host);

        List<String> linhas = executarComando(comando, 15);

        // Parsing: percorre as linhas e extrai latências individuais
        // (procurando "time=X" ou "tempo=X"). Para a perda, tentamos
        // primeiro ler o "X% loss"/"X% packet loss"/"X% de perda";
        // se não achar, calculamos a partir das respostas encontradas.
        double somaLatencias = 0;
        int respostas = 0;
        double perdaPercentual = -1;

        // Aceita "time=12.3 ms", "time<1ms", "tempo=12ms".
        Pattern patLatencia = Pattern.compile(
                "(?:time|tempo)[=<]([0-9]+(?:[.,][0-9]+)?)", Pattern.CASE_INSENSITIVE);
        // Aceita "12% loss", "12% packet loss", "12% de perda".
        Pattern patPerda = Pattern.compile(
                "([0-9]+)% (?:packet )?(?:loss|perda|perdidos)", Pattern.CASE_INSENSITIVE);

        for (String linha : linhas) {
            Matcher mLat = patLatencia.matcher(linha);
            if (mLat.find()) {
                try {
                    somaLatencias += Double.parseDouble(mLat.group(1).replace(',', '.'));
                    respostas++;
                } catch (NumberFormatException ignored) {
                    // Ignora linhas com formato inesperado.
                }
            }
            Matcher mPerda = patPerda.matcher(linha);
            if (mPerda.find()) {
                try {
                    perdaPercentual = Double.parseDouble(mPerda.group(1));
                } catch (NumberFormatException ignored) {
                    // Mantém perdaPercentual = -1 e usa o cálculo abaixo.
                }
            }
        }

        // Fallback: se o parser de perda não achou, calcula manualmente.
        if (perdaPercentual < 0) {
            int perdidos = quantidade - respostas;
            perdaPercentual = (perdidos * 100.0) / Math.max(1, quantidade);
        }

        boolean alcancavel = respostas > 0;
        double latenciaMedia = alcancavel ? (somaLatencias / respostas) : -1.0;

        return new ResultadoPing(alcancavel, latenciaMedia, perdaPercentual, linhas);
    }

    /**
     * Executa um traceroute e devolve a lista de saltos como strings
     * (cada string é uma linha de hop crua, com IP/hostname e latências).
     */
    public static List<String> traceroute(String host) {
        List<String> comando = new ArrayList<>();
        if (WINDOWS) {
            comando.add("tracert");
            // -d evita resolução de DNS (mais rápido e estável).
            comando.add("-d");
            // -h limita o número máximo de saltos (evita travar em rotas longas).
            comando.add("-h");
            comando.add("20");
        } else {
            comando.add("traceroute");
            comando.add("-n");
            comando.add("-m");
            comando.add("20");
        }
        comando.add(host);

        List<String> bruto = executarComando(comando, 40);
        List<String> hops = new ArrayList<>();

        // Filtra apenas linhas que parecem de hop (começam com número ou
        // espaços+número). Isso descarta o cabeçalho do comando.
        Pattern patLinhaHop = Pattern.compile("^\\s*\\d+\\s+.*");
        for (String linha : bruto) {
            if (patLinhaHop.matcher(linha).matches()) {
                hops.add(linha.trim());
            }
        }
        return hops;
    }

    /**
     * Implementação portátil de MTR. A descrição do trabalho diz: "MTR
     * combinando latência e perda de pacotes". Aqui executamos um ping
     * com mais pacotes (8) e devolvemos o mesmo objeto ResultadoPing, que
     * já contém latência média + perda — exatamente o que o MTR resume.
     */
    public static ResultadoPing mtrSimplificado(String host) {
        return ping(host, 8);
    }

    // -----------------------------------------------------------------
    // Resolução DNS
    // -----------------------------------------------------------------

    /**
     * Resolução DNS DIRETA: dado um hostname, devolve todos os IPs
     * associados pela API {@link InetAddress#getAllByName(String)}.
     *
     * Se o argumento já for um IP literal (ex.: "8.8.8.8"), a JVM apenas
     * o converte em InetAddress sem consultar o DNS — nesse caso a
     * resolução "acerta" instantaneamente e devolve o próprio IP. Isso
     * é coerente: nada a resolver.
     */
    public static ResultadoDns resolverDnsDireto(String host) {
        long inicio = System.currentTimeMillis();
        try {
            InetAddress[] enderecos = InetAddress.getAllByName(host);
            List<String> ips = new ArrayList<>();
            for (InetAddress a : enderecos) {
                ips.add(a.getHostAddress());
            }
            long fim = System.currentTimeMillis();
            return new ResultadoDns(true, true, ips, fim - inicio, null);
        } catch (Exception e) {
            long fim = System.currentTimeMillis();
            return new ResultadoDns(true, false, null, fim - inicio,
                    e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    /**
     * Resolução DNS REVERSA (PTR): dado um IP, descobre o hostname
     * canônico. Usamos um truque para forçar o lookup reverso: criamos
     * o InetAddress a partir do array de bytes do IP, garantindo que
     * o hostname interno fica nulo e {@link InetAddress#getCanonicalHostName()}
     * realmente consulta o servidor DNS (caso contrário ele apenas
     * devolveria o IP em string).
     *
     * Quando o IP não tem PTR configurado, a JVM devolve o próprio IP
     * em string como "hostname canônico" — nesse caso reportamos
     * que não houve resolução real.
     */
    public static ResultadoDns resolverDnsReverso(String host) {
        long inicio = System.currentTimeMillis();
        try {
            // Primeiro garantimos ter um IP literal. Se host for um
            // nome, resolvemos para IP e usamos esse IP no lookup PTR.
            InetAddress alvo = InetAddress.getByName(host);
            // Reconstrói o InetAddress só com os bytes — assim a JVM
            // é obrigada a consultar o DNS para obter o nome canônico.
            InetAddress somenteIp = InetAddress.getByAddress(alvo.getAddress());
            String nome = somenteIp.getCanonicalHostName();
            long fim = System.currentTimeMillis();
            // Se o "nome canônico" for igual ao IP, não houve PTR.
            if (nome == null || nome.equals(somenteIp.getHostAddress())) {
                return new ResultadoDns(false, false,
                        Arrays.asList(somenteIp.getHostAddress()),
                        fim - inicio,
                        "IP sem registro PTR");
            }
            return new ResultadoDns(false, true,
                    Arrays.asList(nome), fim - inicio, null);
        } catch (Exception e) {
            long fim = System.currentTimeMillis();
            return new ResultadoDns(false, false, null, fim - inicio,
                    e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    // -----------------------------------------------------------------
    // Teste de porta TCP
    // -----------------------------------------------------------------

    /**
     * Tenta abrir uma conexão TCP no host/porta informados. Mede o tempo
     * gasto até o handshake completar (ou até dar timeout).
     *
     * Diferentemente do ping (ICMP), o teste de porta atravessa firewalls
     * que bloqueiam ICMP mas permitem o serviço — por isso é um excelente
     * complemento ao ping.
     *
     * @param host       IP ou hostname.
     * @param porta      porta TCP a testar (1..65535).
     * @param timeoutMs  tempo máximo de espera, em milissegundos.
     */
    public static ResultadoTcp testarPortaTcp(String host, int porta, int timeoutMs) {
        long inicio = System.currentTimeMillis();
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, porta), timeoutMs);
            long fim = System.currentTimeMillis();
            return new ResultadoTcp(porta, true, fim - inicio, null);
        } catch (Exception e) {
            long fim = System.currentTimeMillis();
            return new ResultadoTcp(porta, false, fim - inicio,
                    e.getClass().getSimpleName());
        }
    }

    // -----------------------------------------------------------------
    // Teste HTTP HEAD (com fallback para GET)
    // -----------------------------------------------------------------

    /**
     * Faz uma requisição HTTP HEAD ao host. Estratégia:
     *  1. Tenta http://host primeiro (ou https://host se a porta sugerir);
     *  2. Se o servidor responder "método não permitido" (405) faz GET;
     *  3. Se http falhar de cara, tenta https como fallback.
     *
     * Sucesso = qualquer resposta HTTP (mesmo 4xx/5xx) — significa que o
     * servidor está vivo. Falha total = exceção de I/O (sem resposta).
     *
     * @param host       IP ou hostname.
     * @param portaTcp   porta cadastrada para o dispositivo (pode ser null).
     *                   Usada como dica: 443 → https; demais → tenta http
     *                   e depois https.
     */
    public static ResultadoHttp testarHttp(String host, Integer portaTcp) {
        // Define a ordem dos esquemas a tentar com base na porta cadastrada.
        List<String> urls = new ArrayList<>();
        if (portaTcp != null && portaTcp == 443) {
            urls.add("https://" + host);
            urls.add("http://" + host);
        } else if (portaTcp != null && portaTcp != 80) {
            // Porta customizada: inclui-a explicitamente na URL.
            urls.add("http://" + host + ":" + portaTcp);
            urls.add("https://" + host + ":" + portaTcp);
        } else {
            urls.add("http://" + host);
            urls.add("https://" + host);
        }

        ResultadoHttp ultimoErro = null;
        for (String url : urls) {
            ResultadoHttp r = tentarHttp(url);
            if (r.isSucesso()) {
                return r;
            }
            ultimoErro = r;
        }
        return ultimoErro != null
                ? ultimoErro
                : new ResultadoHttp(false, -1, "sem URL para testar", "", 0);
    }

    /** Faz uma única tentativa HTTP (HEAD; cai para GET se necessário). */
    private static ResultadoHttp tentarHttp(String urlTexto) {
        long inicio = System.currentTimeMillis();
        HttpURLConnection conn = null;
        try {
            // Em Java moderno, o construtor new URL(String) está deprecado.
            // O caminho recomendado é URI → URL.
            URL url = URI.create(urlTexto).toURL();
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("HEAD");
            conn.setConnectTimeout(4000);
            conn.setReadTimeout(4000);
            conn.setInstanceFollowRedirects(true);
            // User-Agent neutro: alguns servidores recusam UA vazio.
            conn.setRequestProperty("User-Agent", "GerenciadorRede/1.0");
            int status = conn.getResponseCode();
            String msg = conn.getResponseMessage();

            // 405 "Method Not Allowed" → tenta GET no mesmo URL.
            if (status == HttpURLConnection.HTTP_BAD_METHOD) {
                conn.disconnect();
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(4000);
                conn.setReadTimeout(4000);
                conn.setInstanceFollowRedirects(true);
                conn.setRequestProperty("User-Agent", "GerenciadorRede/1.0");
                status = conn.getResponseCode();
                msg = conn.getResponseMessage();
            }

            long fim = System.currentTimeMillis();
            return new ResultadoHttp(true, status,
                    msg == null ? "" : msg, urlTexto, fim - inicio);
        } catch (Exception e) {
            long fim = System.currentTimeMillis();
            return new ResultadoHttp(false, -1,
                    e.getClass().getSimpleName() + ": " + e.getMessage(),
                    urlTexto, fim - inicio);
        } finally {
            if (conn != null) {
                try { conn.disconnect(); } catch (Exception ignored) { }
            }
        }
    }

    // -----------------------------------------------------------------
    // Auxiliares internos
    // -----------------------------------------------------------------

    /**
     * Executa um processo do SO e devolve a saída como lista de linhas.
     * Há um timeout para evitar que travas no SO congelem a thread do
     * monitor (ex.: traceroute para um IP inalcançável).
     */
    private static List<String> executarComando(List<String> comando, long timeoutSegundos) {
        List<String> linhas = new ArrayList<>();
        Process proc = null;
        try {
            ProcessBuilder pb = new ProcessBuilder(comando);
            // Junta stderr e stdout para não perdermos diagnósticos do SO.
            pb.redirectErrorStream(true);
            proc = pb.start();

            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(proc.getInputStream(), CHARSET))) {
                String linha;
                while ((linha = br.readLine()) != null) {
                    linhas.add(linha);
                }
            }

            boolean terminou = proc.waitFor(timeoutSegundos, TimeUnit.SECONDS);
            if (!terminou) {
                // Mata o processo se passou do tempo limite.
                proc.destroyForcibly();
            }
        } catch (Exception e) {
            // Em caso de erro (comando não encontrado, falta de permissão),
            // devolvemos o que conseguimos ler. O chamador interpretará a
            // ausência de respostas como falha do dispositivo.
            linhas.add("[erro] " + e.getMessage());
        } finally {
            if (proc != null && proc.isAlive()) {
                proc.destroyForcibly();
            }
        }
        return linhas;
    }
}
