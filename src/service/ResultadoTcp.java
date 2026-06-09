package service;

/**
 * Resultado de um teste de porta TCP (conexão via {@link java.net.Socket}).
 *
 *  - porta       : porta testada (ex.: 80, 443, 22);
 *  - aberta      : true se a conexão TCP foi aceita dentro do timeout;
 *  - tempoMs     : tempo até estabelecer (ou falhar) a conexão;
 *  - erro        : mensagem amigável quando a porta está fechada/timeout.
 */
public class ResultadoTcp {

    private final int porta;
    private final boolean aberta;
    private final long tempoMs;
    private final String erro;

    public ResultadoTcp(int porta, boolean aberta, long tempoMs, String erro) {
        this.porta = porta;
        this.aberta = aberta;
        this.tempoMs = tempoMs;
        this.erro = erro;
    }

    public int getPorta()    { return porta; }
    public boolean isAberta() { return aberta; }
    public long getTempoMs() { return tempoMs; }
    public String getErro()  { return erro; }

    public String resumo() {
        if (aberta) {
            return "Porta " + porta + " ABERTA (" + tempoMs + " ms)";
        }
        return "Porta " + porta + " FECHADA (" + erro + ")";
    }
}
