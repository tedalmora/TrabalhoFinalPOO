package model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

import service.ResultadoDns;
import service.ResultadoHttp;
import service.ResultadoTcp;

/**
 * Classe que agrupa as métricas coletadas para um dispositivo em um determinado
 * instante. É um "Value Object" (objeto imutável de transporte de dados) usado
 * pelo MonitorDispositivos para entregar resultados ao restante do sistema.
 *
 * As métricas combinam dados de:
 *  - Ping (alcançável, latência média);
 *  - MTR simplificado (perda percentual de pacotes);
 *  - Traceroute (lista de saltos até o destino);
 *  - DNS direto (hostname → IPs) e reverso (IP → hostname);
 *  - Teste de porta TCP (se o dispositivo tiver porta cadastrada);
 *  - Teste HTTP HEAD/GET (se o dispositivo tiver essa opção habilitada).
 *
 * Os campos novos podem ser null quando o teste correspondente não foi
 * executado (por exemplo, dispositivo sem porta TCP cadastrada).
 */
public class MetricaRede {

    // Indica se houve qualquer resposta de eco (ICMP) do destino.
    private final boolean alcancavel;

    // Latência média em milissegundos. Vale -1 quando não foi possível medir.
    private final double latenciaMediaMs;

    // Percentual de pacotes perdidos (0.0 a 100.0).
    private final double perdaPacotesPercentual;

    // Hops/saltos obtidos pelo traceroute. Pode estar vazio quando não rodado.
    private final List<String> rotaTraceroute;

    // Status interpretado das métricas (OK / ATENCAO / FALHA).
    private final StatusDispositivo status;

    // Pequeno diagnóstico textual ("Latência alta", "Sem resposta", etc.).
    private final String diagnostico;

    // Momento da coleta. Útil para exibir "última atualização" na GUI.
    private final LocalDateTime coletadaEm;

    // Resultados opcionais — null se o teste não foi executado.
    private final ResultadoDns dnsDireto;
    private final ResultadoDns dnsReverso;
    private final ResultadoTcp tcp;
    private final ResultadoHttp http;

    public MetricaRede(boolean alcancavel,
                       double latenciaMediaMs,
                       double perdaPacotesPercentual,
                       List<String> rotaTraceroute,
                       StatusDispositivo status,
                       String diagnostico,
                       ResultadoDns dnsDireto,
                       ResultadoDns dnsReverso,
                       ResultadoTcp tcp,
                       ResultadoHttp http) {
        this.alcancavel = alcancavel;
        this.latenciaMediaMs = latenciaMediaMs;
        this.perdaPacotesPercentual = perdaPacotesPercentual;
        // Defensive copy: garante imutabilidade externa da lista.
        this.rotaTraceroute = rotaTraceroute == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(rotaTraceroute);
        this.status = status;
        this.diagnostico = diagnostico;
        this.coletadaEm = LocalDateTime.now();
        this.dnsDireto = dnsDireto;
        this.dnsReverso = dnsReverso;
        this.tcp = tcp;
        this.http = http;
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

    public ResultadoDns getDnsDireto()  { return dnsDireto; }
    public ResultadoDns getDnsReverso() { return dnsReverso; }
    public ResultadoTcp getTcp()        { return tcp; }
    public ResultadoHttp getHttp()      { return http; }

    /** Retorna a hora da coleta formatada (HH:mm:ss) para exibição na GUI. */
    public String getColetadaEmFormatada() {
        return coletadaEm.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }

    /** Latência formatada com unidade. "-" quando não há medição válida. */
    public String getLatenciaFormatada() {
        if (latenciaMediaMs < 0) {
            return "-";
        }
        return String.format("%.1f ms", latenciaMediaMs);
    }

    /** Perda de pacotes formatada como percentual. */
    public String getPerdaFormatada() {
        return String.format("%.0f%%", perdaPacotesPercentual);
    }
}
