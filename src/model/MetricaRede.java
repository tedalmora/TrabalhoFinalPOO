package model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/*
Classe que agrupa as métricas coletadas para um dispositivo em um determinado
instante. É um "Value Object" (objeto imutável de transporte de dados) usado
pelo MonitorDispositivos para entregar resultados ao restante do sistema.
As métricas combinam dados de:
 - Ping (alcançável, latência média);
 - MTR simplificado (perda percentual de pacotes);
 - Traceroute (lista de saltos até o destino).

 A métrica é só o resultado. Quer dizer, não é responsável por coletar os dados. A coleta é feita pelo MonitorDispositivos, que cria uma instância de MetricaRede com os resultados obtidos.
 Quem vê o que cada um significa é o DispositivoRede, que interpreta os dados e gera um diagnóstico específico para cada tipo de dispositivo (Roteador, Switch, Firewall, Servidor).
*/
public class MetricaRede {

    // Indica se houve qualquer resposta de eco (ICMP) do destino.
    private boolean alcancavel;

    // Latência média em milissegundos. Vale -1 quando não foi possível medir.
    private double latenciaMediaMs;

    // Percentual de pacotes perdidos (0.0 a 100.0).
    private double perdaPacotesPercentual;

    // Hops/saltos obtidos pelo traceroute. Pode estar vazio quando não rodado.
    private List<String> rotaTraceroute;

    // Status interpretado das métricas (OK / ATENCAO / FALHA).
    private StatusDispositivo status;

    // Pequeno diagnóstico textual ("Latência alta", "Sem resposta", etc.).
    private String diagnostico;

    // Momento da coleta. Útil para exibir "última atualização" na GUI.
    private LocalDateTime coletadaEm;

    public MetricaRede(boolean alcancavel,double latenciaMediaMs,double perdaPacotesPercentual,List<String> rotaTraceroute,StatusDispositivo status,String diagnostico) {
        this.alcancavel = alcancavel;
        this.latenciaMediaMs = latenciaMediaMs;
        this.perdaPacotesPercentual = perdaPacotesPercentual;
        this.rotaTraceroute = rotaTraceroute;
        this.status = status;
        this.diagnostico = diagnostico;
        this.coletadaEm = LocalDateTime.now();
    }

    public boolean isAlcancavel() {
        return alcancavel;
    }

    public double getLatenciaMediaMs() {
        return latenciaMediaMs;
    }

    public double getPerdaPacotesPercentual() {
        return perdaPacotesPercentual;
    }

    public List<String> getRotaTraceroute() {
        return rotaTraceroute;
    }

    public StatusDispositivo getStatus() {
        return status;
    }

    public String getDiagnostico() {
        return diagnostico;
    }

    public LocalDateTime getColetadaEm() {
        return coletadaEm;
    }

    // Retorna a hora da coleta formatada (HH:mm:ss) para exibição na GUI.
    public String getColetadaEmFormatada() {
        return coletadaEm.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }

    // Latência formatada com unidade. "-" quando não há medição válida.
    public String getLatenciaFormatada() {
        if (latenciaMediaMs < 0) {
            return "-";
        }
        return String.format("%.1f ms", latenciaMediaMs);
    }

    // Perda de pacotes formatada como percentual.
    public String getPerdaFormatada() {
        return String.format("%.0f%%", perdaPacotesPercentual);
    }
}
