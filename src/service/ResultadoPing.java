package service;

import java.util.Collections;
import java.util.List;

/**
 * Resultado consolidado de um ping. Imutável, retornado por
 * {@link FerramentaRede#ping(String, int)}.
 *
 *  - alcancavel: ao menos um pacote teve resposta;
 *  - latenciaMediaMs: média das respostas; -1 quando nenhuma;
 *  - perdaPercentual: percentual de pacotes perdidos (0..100);
 *  - linhasBrutas: saída original do comando para exibição/debug.
 */
public class ResultadoPing {

    private final boolean alcancavel;
    private final double latenciaMediaMs;
    private final double perdaPercentual;
    private final List<String> linhasBrutas;

    public ResultadoPing(boolean alcancavel,
                         double latenciaMediaMs,
                         double perdaPercentual,
                         List<String> linhasBrutas) {
        this.alcancavel = alcancavel;
        this.latenciaMediaMs = latenciaMediaMs;
        this.perdaPercentual = perdaPercentual;
        this.linhasBrutas = linhasBrutas == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(linhasBrutas);
    }

    public boolean isAlcancavel() {
        return alcancavel;
    }

    public double getLatenciaMediaMs() {
        return latenciaMediaMs;
    }

    public double getPerdaPercentual() {
        return perdaPercentual;
    }

    public List<String> getLinhasBrutas() {
        return linhasBrutas;
    }
}
