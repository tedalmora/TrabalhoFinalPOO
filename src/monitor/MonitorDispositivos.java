package monitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import classes.DispositivoRede;
import classes.MetricaRede;
import classes.StatusDispositivo;
import servicos.FerramentaRede;
import servicos.ResultadoPing;

/*
Núcleo de threads do sistema.
Cada dispositivo tem sua própria classe de thread (extends Thread) com loop em run(), sleep e interrupção.
Executa ping + traceroute, atualiza o objeto DispositivoRede e notifica os observadores (JanelaPrincipal).
*/
public class MonitorDispositivos {

    private static int INTERVALO_MS = 15_000; // intervalo de coleta em milissegundos (15s)

    // Lista de dispositivos monitorados. Acesso sincronizado.
    private List<DispositivoRede> dispositivos =Collections.synchronizedList(new ArrayList<>());
    
    // lista de observadores (JanelaPrincipal) que serão notificados quando um dispositivo for atualizado. Acesso sincronizado.
    private List<DispositivoObserver> observadores = Collections.synchronizedList(new ArrayList<>());
    
    // mapa de threads de coleta, indexadas pelo ID do dispositivo. Acesso sincronizado.
    private Map<Integer, ThreadColetaDispositivo> threadsPorDispositivo = Collections.synchronizedMap(new HashMap<>());


    // Adiciona um dispositivo e dispara seu monitoramento periódico.
    public void adicionarDispositivo(DispositivoRede d) {
        dispositivos.add(d); // coloco na lista de dispositivos
        ThreadColetaDispositivo t = new ThreadColetaDispositivo(d); // crio a thread de coleta
        threadsPorDispositivo.put(d.getId(), t); // guardo a thread no mapa
        t.start(); // disparo da thread
    }

    // Remove um dispositivo e encerra seu monitoramento periódico.
    public void removerDispositivo(DispositivoRede d) {
        dispositivos.remove(d); // remove da lista de dispositivos
        ThreadColetaDispositivo t = threadsPorDispositivo.remove(d.getId()); // remove do mapa de threads
        if (t != null) {
            t.encerrar(); // encerra a thread de coleta
        }
    }

    public List<DispositivoRede> getDispositivos() {
        return dispositivos; // retorna a lista de dispositivos monitorados
    }

    public void adicionarObservador(DispositivoObserver obs) {
        observadores.add(obs); // adiciona um observador. na pratica, so a JanelaPrincipal vai se inscrever como observador
    }

    // Encerra todas as threads de coleta. Chamado ao fechar a janela.
    public void encerrar() {
        synchronized (threadsPorDispositivo) {
            for (ThreadColetaDispositivo t : threadsPorDispositivo.values()) {
                t.encerrar(); // encerra a thread de coleta
            }
            threadsPorDispositivo.clear(); // limpa o mapa de threads
        }
    }

    // Executa uma coleta para o dispositivo informado (roda no pool).
    private void executarColeta(DispositivoRede d) {
        try {
            // Ping com 8 pacotes: latência média + perda (papel do MTR)
            ResultadoPing ping = FerramentaRede.ping(d.getEnderecoIp(), 8);

            // Traceroute só se o destino respondeu
            List<String> rota = ping.isAlcancavel() ? FerramentaRede.traceroute(d.getEnderecoIp()) : Collections.emptyList();

            // ve status com base no ping
            StatusDispositivo status = interpretarStatus(ping);

            // Métrica preliminar para a subclasse calcular o diagnóstico
            MetricaRede previa = new MetricaRede(ping.isAlcancavel(), ping.getLatenciaMediaMs(), ping.getPerdaPercentual(), rota, status, "");

            // Polimorfismo: cada tipo de dispositivo aplica suas regras. Mando a metrica pro meu dispositivo pra ele mandar o diag de volta.
            String diag = d.diagnosticoEspecifico(previa);

            // faço a metrica fica
            MetricaRede metrica = new MetricaRede(ping.isAlcancavel(), ping.getLatenciaMediaMs(), ping.getPerdaPercentual(),rota, status, diag);
            
            // atualiazo no dispositivo
            d.setUltimaMetrica(metrica);

            // aviso os observadores
            synchronized (observadores) {
                for (DispositivoObserver obs : observadores) {
                    obs.aoAtualizarDispositivo(d);
                }
            }

        // se der algum erro
        } catch (Exception e) {
            System.err.println("Erro ao monitorar " + d + ": " + e);
        }
    }

    // Combina os resultados em um status (verde / amarelo / vermelho).
    private StatusDispositivo interpretarStatus(ResultadoPing ping) {
        if (!ping.isAlcancavel()) return StatusDispositivo.FALHA;
        if (ping.getPerdaPercentual() > 0 || ping.getLatenciaMediaMs() > 150) {
            return StatusDispositivo.ATENCAO;
        }
        return StatusDispositivo.OK;
    }

    /*
    Thread por dispositivo.
    Fica em loop coletando métricas até receber interrupção.
    */
    private class ThreadColetaDispositivo extends Thread {

        private DispositivoRede dispositivo; // disp d

        ThreadColetaDispositivo(DispositivoRede dispositivo) {
            super("monitor-" + dispositivo.getId()); // nome da thread, monitor-1, monitor-2, etc
            this.dispositivo = dispositivo;
            setDaemon(true); // thread daemon, nao impede o programa de fechar
        }

        @Override
        // Loop de coleta periódica. roda quando a thread é iniciada. chama executarColeta() e depois dorme INTERVALO_MS.
        public void run() {
            while (true) {
                executarColeta(dispositivo);
                try {
                    Thread.sleep(INTERVALO_MS);
                } catch (InterruptedException e) {
                    break; // encerra a thread
                }
            }
        }

        void encerrar() { // encerra a thread de coleta
            interrupt(); // acorda a thread se estiver dormindo com o sleep() acima. entra no catch dai e fecha a thread. se ela estiver rodando ainda, so vai acabar quando bater no catch.
        }
    }
}
