package service;

/**
 * Resultado de uma requisição HTTP HEAD (com fallback para GET).
 *
 *  - sucesso     : true se conseguimos qualquer resposta HTTP do servidor;
 *  - status      : código HTTP retornado (200, 301, 404, 500...);
 *  - mensagem    : "OK", "Not Found", etc. (ou descrição do erro);
 *  - urlUsada    : URL realmente requisitada (com esquema definido);
 *  - tempoMs     : tempo total da requisição.
 */
public class ResultadoHttp {

    private final boolean sucesso;
    private final int status;
    private final String mensagem;
    private final String urlUsada;
    private final long tempoMs;

    public ResultadoHttp(boolean sucesso, int status, String mensagem,
                         String urlUsada, long tempoMs) {
        this.sucesso = sucesso;
        this.status = status;
        this.mensagem = mensagem;
        this.urlUsada = urlUsada;
        this.tempoMs = tempoMs;
    }

    public boolean isSucesso() { return sucesso; }
    public int getStatus()     { return status; }
    public String getMensagem(){ return mensagem; }
    public String getUrlUsada(){ return urlUsada; }
    public long getTempoMs()   { return tempoMs; }

    public String resumo() {
        if (!sucesso) {
            return "Falhou: " + mensagem;
        }
        return "HTTP " + status + " " + mensagem
                + "  [" + urlUsada + "]  (" + tempoMs + " ms)";
    }
}
