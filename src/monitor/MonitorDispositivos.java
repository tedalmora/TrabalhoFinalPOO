package monitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import model.DispositivoRede;
import model.MetricaRede;
import model.StatusDispositivo;
import service.FerramentaRede;
import service.ResultadoDns;
import service.ResultadoHttp;
import service.ResultadoPing;
import service.ResultadoTcp;

/**
 * Núcleo de threads do sistema. Para cada dispositivo cadastrado,
 * agenda uma coleta periódica (ping, traceroute, DNS, TCP, HTTP) em
 * background e notifica observadores quando a métrica é atualizada.
 *
 * Usa um ScheduledExecutorService com threads daemon para não impedir
 * o encerramento da aplicação.
 */
public class MonitorDispositivos {

    /** Intervalo entre coletas, em segundos. */
    private static final int INTERVALO_SEGUNDOS = 15;

    private final ScheduledExecutorService pool =
            Executors.newScheduledThreadPool(4, r -> {
                Thread t = new Thread(r, "monitor-rede");
                t.setDaemon(true);
                return t;
            });

    private final List<DispositivoRede> dispositivos = new CopyOnWriteArrayList<>();
    private final List<DispositivoObserver> observadores = new CopyOnWriteArrayList<>();

    /** Adiciona um dispositivo e dispara seu monitoramento periódico. */
    public void adicionarDispositivo(DispositivoRede d) {
        dispositivos.add(d);
        pool.scheduleWithFixedDelay(
                () -> executarColeta(d), 0, INTERVALO_SEGUNDOS, TimeUnit.SECONDS);
    }

    public void removerDispositivo(DispositivoRede d) {
        dispositivos.remove(d);
        // A tarefa agendada continua existindo, mas como o dispositivo
        // saiu da lista da GUI ela apenas trabalha em vão até o shutdown.
        // Em um app pequeno isso é aceitável; o pool é finito (4 threads).
    }

    public List<DispositivoRede> getDispositivos() {
        return Collections.unmodifiableList(new ArrayList<>(dispositivos));
    }

    public void adicionarObservador(DispositivoObserver obs) {
        observadores.add(obs);
    }

    /** Encerra todas as threads do pool. Chamado ao fechar a janela. */
    public void encerrar() {
        pool.shutdownNow();
    }

    /** Executa uma coleta para o dispositivo informado (roda no pool). */
    private void executarColeta(DispositivoRede d) {
        try {
            // Ping com 8 pacotes — entrega latência média + perda (papel do MTR).
            ResultadoPing ping = FerramentaRede.ping(d.getEnderecoIp(), 8);

            // Traceroute só se o destino respondeu — senão é desperdício.
            List<String> rota = ping.isAlcancavel()
                    ? FerramentaRede.traceroute(d.getEnderecoIp())
                    : Collections.emptyList();

            ResultadoDns dnsDireto = FerramentaRede.resolverDnsDireto(d.getEnderecoIp());
            ResultadoDns dnsReverso = FerramentaRede.resolverDnsReverso(d.getEnderecoIp());

            ResultadoTcp tcp = d.getPortaTcp() == null
                    ? null
                    : FerramentaRede.testarPortaTcp(d.getEnderecoIp(), d.getPortaTcp(), 3000);

            ResultadoHttp http = d.isVerificarHttp()
                    ? FerramentaRede.testarHttp(d.getEnderecoIp(), d.getPortaTcp())
                    : null;

            StatusDispositivo status = interpretarStatus(ping, tcp, http);

            // Métrica preliminar para a subclasse calcular o diagnóstico.
            MetricaRede previa = new MetricaRede(ping.isAlcancavel(),
                    ping.getLatenciaMediaMs(), ping.getPerdaPercentual(),
                    rota, status, "", dnsDireto, dnsReverso, tcp, http);
            // Polimorfismo: cada tipo de dispositivo aplica suas regras.
            String diag = d.diagnosticoEspecifico(previa);

            MetricaRede metrica = new MetricaRede(ping.isAlcancavel(),
                    ping.getLatenciaMediaMs(), ping.getPerdaPercentual(),
                    rota, status, diag, dnsDireto, dnsReverso, tcp, http);
            d.setUltimaMetrica(metrica);

            for (DispositivoObserver obs : observadores) {
                obs.aoAtualizarDispositivo(d);
            }
        } catch (Exception e) {
            System.err.println("Erro ao monitorar " + d + ": " + e);
        }
    }

    /** Combina os resultados em um status (verde / amarelo / vermelho). */
    private StatusDispositivo interpretarStatus(ResultadoPing ping,
                                                ResultadoTcp tcp,
                                                ResultadoHttp http) {
        boolean pingOk = ping.isAlcancavel();
        boolean tcpOk = (tcp == null) || tcp.isAberta();
        boolean httpOk = (http == null) || (http.isSucesso() && http.getStatus() < 400);

        // Falha total: ping caiu e (sem TCP cadastrado OU TCP também fechado).
        if (!pingOk && !tcpOk) return StatusDispositivo.FALHA;
        if (!pingOk && tcp == null) return StatusDispositivo.FALHA;

        if (ping.getPerdaPercentual() > 0
                || ping.getLatenciaMediaMs() > 150
                || !tcpOk
                || !httpOk) {
            return StatusDispositivo.ATENCAO;
        }
        return StatusDispositivo.OK;
    }
}
