package service;

import java.util.Collections;
import java.util.List;

/**
 * Resultado de uma resolução DNS (direta ou reversa).
 *
 *  - direto    : true  = hostname → IPs;  false = IP → hostname (PTR);
 *  - sucesso   : true se a resolução completou sem erro;
 *  - nomes     : nomes/IPs encontrados (depende do tipo);
 *  - tempoMs   : tempo gasto na consulta;
 *  - erro      : mensagem amigável quando sucesso == false.
 */
public class ResultadoDns {

    private final boolean direto;
    private final boolean sucesso;
    private final List<String> nomes;
    private final long tempoMs;
    private final String erro;

    public ResultadoDns(boolean direto, boolean sucesso, List<String> nomes,
                        long tempoMs, String erro) {
        this.direto = direto;
        this.sucesso = sucesso;
        this.nomes = nomes == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(nomes);
        this.tempoMs = tempoMs;
        this.erro = erro;
    }

    public boolean isDireto()  { return direto; }
    public boolean isSucesso() { return sucesso; }
    public List<String> getNomes() { return nomes; }
    public long getTempoMs() { return tempoMs; }
    public String getErro() { return erro; }

    /** Texto pronto para exibição na GUI. */
    public String resumo() {
        if (!sucesso) {
            return "Falhou (" + erro + ")";
        }
        if (nomes.isEmpty()) {
            return "Sem resultado";
        }
        return String.join(", ", nomes) + "  (" + tempoMs + " ms)";
    }
}
