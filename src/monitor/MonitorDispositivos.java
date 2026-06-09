package monitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import model.DispositivoRede;
import model.MetricaRede;
import model.StatusDispositivo;
import observer.DispositivoObserver;
import service.FerramentaRede;
import service.ResultadoDns;
import service.ResultadoHttp;
import service.ResultadoPing;
import service.ResultadoTcp;

/**
 * Núcleo de threading da aplicação.
 *
 * Responsabilidades:
 *  1. Manter a lista de dispositivos cadastrados;
 *  2. Para cada dispositivo, agendar uma tarefa periódica (em uma thread
 *     do pool) que executa ping/traceroute/MTR e atualiza a métrica;
 *  3. Avisar os observadores (Observer) sempre que uma métrica mudar;
 *  4. Manter a GUI responsiva: nada de I/O bloqueante na EDT.
 *
 * Decisões de implementação:
 *  - Usamos {@link ScheduledExecutorService} (pool de threads daemon)
 *    em vez de criar threads manualmente. O pool reaproveita threads e
 *    garante encerramento limpo no shutdown.
 *  - Estruturas concorrentes (ConcurrentHashMap, CopyOnWriteArrayList)
 *    evitam ConcurrentModificationException quando GUI e threads de
 *    monitoramento operam ao mesmo tempo.
 *  - O período de coleta é configurável (padrão: 15 segundos).
 */
public class MonitorDispositivos {

    /** Intervalo padrão entre coletas, em segundos. */
    public static final int INTERVALO_PADRAO_SEGUNDOS = 15;

    // Pool de threads de monitoramento. Threads são "daemon" para que o
    // processo possa encerrar mesmo se o pool estiver vivo.
    private final ScheduledExecutorService pool;

    // Lista global de dispositivos. CopyOnWriteArrayList é ideal aqui
    // porque leituras (pela GUI) são MUITO mais frequentes que escritas.
    private final List<DispositivoRede> dispositivos = new CopyOnWriteArrayList<>();

    // Mapa id -> tarefa agendada, para permitir cancelar a coleta de um
    // dispositivo específico quando ele for removido ou pausado.
    private final ConcurrentMap<Integer, ScheduledFuture<?>> tarefas =
            new ConcurrentHashMap<>();

    // Lista de observadores (GUI, loggers, etc.).
    private final List<DispositivoObserver> observadores = new CopyOnWriteArrayList<>();

    // Intervalo atual entre coletas (em segundos).
    private volatile int intervaloSegundos = INTERVALO_PADRAO_SEGUNDOS;

    public MonitorDispositivos() {
        // ThreadFactory customizada: nomes amigáveis para depuração e
        // todas marcadas como daemon.
        ThreadFactory factory = new ThreadFactory() {
            private final AtomicInteger contador = new AtomicInteger(1);
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "monitor-rede-" + contador.getAndIncrement());
                t.setDaemon(true);
                return t;
            }
        };
        // Pool com 4 threads — suficiente para monitorar dezenas de
        // dispositivos em paralelo sem sobrecarregar o computador.
        this.pool = Executors.newScheduledThreadPool(4, factory);
    }

    // -----------------------------------------------------------------
    // Gerenciamento de dispositivos
    // -----------------------------------------------------------------

    /** Adiciona um novo dispositivo e inicia sua thread de monitoramento. */
    public void adicionarDispositivo(DispositivoRede d) {
        dispositivos.add(d);
        agendarColeta(d);
    }

    /** Remove o dispositivo e cancela sua thread de monitoramento. */
    public void removerDispositivo(DispositivoRede d) {
        dispositivos.remove(d);
        ScheduledFuture<?> f = tarefas.remove(d.getId());
        if (f != null) {
            f.cancel(true);
        }
    }

    /**
     * Devolve uma cópia imutável da lista atual de dispositivos.
     * A cópia evita que a GUI itere sobre a lista enquanto o monitor
     * a modifica (apesar de CopyOnWriteArrayList ser seguro, devolver
     * uma cópia explícita deixa a intenção clara).
     */
    public List<DispositivoRede> getDispositivos() {
        return Collections.unmodifiableList(new ArrayList<>(dispositivos));
    }

    /** Força uma coleta imediata para um dispositivo (não espera o ciclo). */
    public void coletarAgora(DispositivoRede d) {
        pool.submit(() -> executarColeta(d));
    }

    // -----------------------------------------------------------------
    // Observers
    // -----------------------------------------------------------------

    public void adicionarObservador(DispositivoObserver obs) {
        observadores.add(obs);
    }

    public void removerObservador(DispositivoObserver obs) {
        observadores.remove(obs);
    }

    // -----------------------------------------------------------------
    // Configuração
    // -----------------------------------------------------------------

    public int getIntervaloSegundos() {
        return intervaloSegundos;
    }

    /**
     * Ajusta o intervalo entre coletas e reagenda todas as tarefas
     * existentes. Útil se você quiser um modo "monitoramento intensivo".
     */
    public void setIntervaloSegundos(int novoIntervalo) {
        if (novoIntervalo < 5) {
            // Limite mínimo para não bombardear a rede.
            novoIntervalo = 5;
        }
        this.intervaloSegundos = novoIntervalo;
        // Reagenda todas as tarefas existentes com o novo período.
        for (DispositivoRede d : dispositivos) {
            ScheduledFuture<?> f = tarefas.remove(d.getId());
            if (f != null) {
                f.cancel(false);
            }
            agendarColeta(d);
        }
    }

    /**
     * Encerra todas as threads do pool. Deve ser chamado quando a janela
     * principal é fechada (window listener).
     */
    public void encerrar() {
        pool.shutdownNow();
    }

    // -----------------------------------------------------------------
    // Internos
    // -----------------------------------------------------------------

    private void agendarColeta(DispositivoRede d) {
        // scheduleWithFixedDelay garante que a próxima coleta só começa
        // DEPOIS da anterior terminar — assim evitamos sobreposição em
        // dispositivos lentos para responder.
        ScheduledFuture<?> f = pool.scheduleWithFixedDelay(
                () -> executarColeta(d),
                0,                      // primeira coleta imediata
                intervaloSegundos,      // intervalo entre coletas
                TimeUnit.SECONDS
        );
        tarefas.put(d.getId(), f);
    }

    /**
     * Coração da coleta: executa ping (e traceroute), monta a métrica,
     * salva no dispositivo e notifica observadores.
     */
    private void executarColeta(DispositivoRede d) {
        try {
            // 1) Ping/MTR — mede latência e perda.
            ResultadoPing ping = FerramentaRede.mtrSimplificado(d.getEnderecoIp());

            // 2) Traceroute — só roda se o destino respondeu ao ping;
            //    senão é desperdício de tempo (e travaria a thread).
            List<String> rota;
            if (ping.isAlcancavel()) {
                rota = FerramentaRede.traceroute(d.getEnderecoIp());
            } else {
                rota = Collections.emptyList();
            }

            // 3) Resolução DNS direta e reversa — sempre executadas.
            //    São rápidas (geralmente < 50ms) e ajudam muito no
            //    diagnóstico (mostra se o nome resolve e qual é o PTR).
            ResultadoDns dnsDireto = FerramentaRede.resolverDnsDireto(d.getEnderecoIp());
            ResultadoDns dnsReverso = FerramentaRede.resolverDnsReverso(d.getEnderecoIp());

            // 4) Teste de porta TCP — apenas se o usuário cadastrou uma porta.
            ResultadoTcp tcp = null;
            if (d.getPortaTcp() != null) {
                tcp = FerramentaRede.testarPortaTcp(
                        d.getEnderecoIp(), d.getPortaTcp(), 3000);
            }

            // 5) Verificação HTTP — apenas se o usuário marcou a opção.
            ResultadoHttp http = null;
            if (d.isVerificarHttp()) {
                http = FerramentaRede.testarHttp(d.getEnderecoIp(), d.getPortaTcp());
            }

            // 6) Interpreta as métricas em um StatusDispositivo,
            //    considerando também os novos testes.
            StatusDispositivo status = interpretarStatus(ping, tcp, http);

            // 7) Pede ao dispositivo (polimorfismo!) o diagnóstico textual
            //    específico do seu tipo. Aqui o monitor não sabe se é
            //    Roteador/Switch/etc., mas chama o método certo.
            MetricaRede previa = new MetricaRede(
                    ping.isAlcancavel(),
                    ping.getLatenciaMediaMs(),
                    ping.getPerdaPercentual(),
                    rota,
                    status,
                    "", // diagnóstico será preenchido logo abaixo
                    dnsDireto, dnsReverso, tcp, http
            );
            String diag = d.diagnosticoEspecifico(previa);

            // 8) Monta a métrica final (já com o diagnóstico) e salva.
            MetricaRede metrica = new MetricaRede(
                    ping.isAlcancavel(),
                    ping.getLatenciaMediaMs(),
                    ping.getPerdaPercentual(),
                    rota,
                    status,
                    diag,
                    dnsDireto, dnsReverso, tcp, http
            );
            d.setUltimaMetrica(metrica);

            // 9) Notifica todos os observadores. Cada observador decide
            //    em qual thread realmente atualizar suas estruturas
            //    (a GUI, por exemplo, pula para a EDT via SwingUtilities).
            for (DispositivoObserver obs : observadores) {
                try {
                    obs.aoAtualizarDispositivo(d);
                } catch (RuntimeException e) {
                    // Não deixamos uma falha em um observador atrapalhar
                    // os demais nem matar a thread do monitor.
                    System.err.println("Falha ao notificar observador: " + e);
                }
            }
        } catch (Exception e) {
            // Qualquer erro inesperado é apenas registrado para que o
            // agendamento periódico continue funcionando.
            System.err.println("Erro ao monitorar " + d + ": " + e);
        }
    }

    /**
     * Traduz o resultado dos testes em um status para a GUI (cores).
     *
     * Regras combinadas:
     *  - FALHA  : ping falhou E (sem teste TCP OU teste TCP fechado);
     *  - ATENCAO: ping OK porém perda > 0 ou latência alta;
     *             ou TCP fechado quando ping OK;
     *             ou HTTP devolveu 5xx;
     *  - OK     : ping OK + sem perda + (TCP aberto se houver) +
     *             (HTTP < 400 se houver).
     */
    private StatusDispositivo interpretarStatus(ResultadoPing ping,
                                                ResultadoTcp tcp,
                                                ResultadoHttp http) {
        boolean pingOk = ping.isAlcancavel();
        boolean tcpOk = (tcp == null) || tcp.isAberta();
        boolean httpOk = (http == null) || (http.isSucesso() && http.getStatus() < 400);

        // Cenário de falha total: ping não respondeu e o TCP (se houver)
        // também falhou. Se o usuário só configurou TCP e ele está
        // aberto, ainda consideramos o dispositivo vivo.
        if (!pingOk && !tcpOk) {
            return StatusDispositivo.FALHA;
        }
        if (!pingOk && tcp == null) {
            return StatusDispositivo.FALHA;
        }
        // Cenários de atenção.
        if (ping.getPerdaPercentual() > 0
                || ping.getLatenciaMediaMs() > 150
                || !tcpOk
                || !httpOk) {
            return StatusDispositivo.ATENCAO;
        }
        return StatusDispositivo.OK;
    }

    /** Traduz o resultado do ping para o enum de status (cores da GUI). */
    @SuppressWarnings("unused")
    private StatusDispositivo interpretarStatus(ResultadoPing ping) {
        return interpretarStatus(ping, null, null);
    }
}
