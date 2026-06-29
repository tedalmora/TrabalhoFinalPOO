package monitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ConcurrentHashMap;

import model.DispositivoRede;
import model.MetricaRede;
import model.StatusDispositivo;
import service.FerramentaRede;
import service.ResultadoPing;

/*
Núcleo de threads do sistema.
Cada dispositivo tem sua própria classe de thread (extends Thread) com loop em run(), sleep e interrupção.
Executa ping + traceroute, atualiza o objeto DispositivoRede e notifica os observadores (JanelaPrincipal).
*/
public class MonitorDispositivos {

    private static final int INTERVALO_MS = 15_000;

    private final List<DispositivoRede> dispositivos = new CopyOnWriteArrayList<>();
    private final List<DispositivoObserver> observadores = new CopyOnWriteArrayList<>();
    private final Map<Integer, ThreadColetaDispositivo> threadsPorDispositivo =
            new ConcurrentHashMap<>();

    /** Adiciona um dispositivo e dispara seu monitoramento periódico. */
    public void adicionarDispositivo(DispositivoRede d) {
        dispositivos.add(d);
        ThreadColetaDispositivo t = new ThreadColetaDispositivo(d);
        threadsPorDispositivo.put(d.getId(), t);
        t.start();
    }

    public void removerDispositivo(DispositivoRede d) {
        dispositivos.remove(d);
        ThreadColetaDispositivo t = threadsPorDispositivo.remove(d.getId());
        if (t != null) {
            t.encerrar();
        }
    }

    public List<DispositivoRede> getDispositivos() {
        return Collections.unmodifiableList(new ArrayList<>(dispositivos));
    }

    public void adicionarObservador(DispositivoObserver obs) {
        observadores.add(obs);
    }

    /** Encerra todas as threads de coleta. Chamado ao fechar a janela. */
    public void encerrar() {
        for (ThreadColetaDispositivo t : threadsPorDispositivo.values()) {
            t.encerrar();
        }
        threadsPorDispositivo.clear();
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

                StatusDispositivo status = interpretarStatus(ping);

            // Métrica preliminar para a subclasse calcular o diagnóstico.
            MetricaRede previa = new MetricaRede(ping.isAlcancavel(),
                    ping.getLatenciaMediaMs(), ping.getPerdaPercentual(),
                    rota, status, "");
            // Polimorfismo: cada tipo de dispositivo aplica suas regras.
            String diag = d.diagnosticoEspecifico(previa);

            MetricaRede metrica = new MetricaRede(ping.isAlcancavel(),
                    ping.getLatenciaMediaMs(), ping.getPerdaPercentual(),
                    rota, status, diag);
            d.setUltimaMetrica(metrica);

            for (DispositivoObserver obs : observadores) {
                obs.aoAtualizarDispositivo(d);
            }
        } catch (Exception e) {
            System.err.println("Erro ao monitorar " + d + ": " + e);
        }
    }

    /** Combina os resultados em um status (verde / amarelo / vermelho). */
    private StatusDispositivo interpretarStatus(ResultadoPing ping) {
        if (!ping.isAlcancavel()) return StatusDispositivo.FALHA;
        if (ping.getPerdaPercentual() > 0
                || ping.getLatenciaMediaMs() > 150) {
            return StatusDispositivo.ATENCAO;
        }
        return StatusDispositivo.OK;
    }

    /**
     * Thread por dispositivo, no estilo visto em aula (extends Thread).
     * Fica em loop coletando métricas até receber interrupção.
     */
    private class ThreadColetaDispositivo extends Thread {

        private final DispositivoRede dispositivo;
        private volatile boolean ativo = true;

        ThreadColetaDispositivo(DispositivoRede dispositivo) {
            super("monitor-" + dispositivo.getId());
            this.dispositivo = dispositivo;
            setDaemon(true);
        }

        @Override
        public void run() {
            while (ativo && !isInterrupted()) {
                executarColeta(dispositivo);
                try {
                    Thread.sleep(INTERVALO_MS);
                } catch (InterruptedException e) {
                    interrupt();
                }
            }
        }

        void encerrar() {
            ativo = false;
            interrupt();
        }
    }
}
