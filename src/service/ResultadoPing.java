package service;

/**
 * Resultado de um ping (também usado como "MTR simplificado").
 *
 *  - alcancavel: ao menos um pacote teve resposta;
 *  - latenciaMediaMs: média das respostas; -1 quando nenhuma;
 *  - perdaPercentual: percentual de pacotes perdidos (0..100).
 */
public class ResultadoPing {

    private final boolean alcancavel;
    private final double latenciaMediaMs;
    private final double perdaPercentual;

    public ResultadoPing(boolean alcancavel,
                         double latenciaMediaMs,
                         double perdaPercentual) {
        this.alcancavel = alcancavel;
        this.latenciaMediaMs = latenciaMediaMs;
        this.perdaPercentual = perdaPercentual;
    }

    public boolean isAlcancavel() { return alcancavel; }
    public double getLatenciaMediaMs() { return latenciaMediaMs; }
    public double getPerdaPercentual() { return perdaPercentual; }
}
