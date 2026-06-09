package model;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Classe abstrata que representa qualquer dispositivo de rede monitorado pelo
 * sistema. É o coração do polimorfismo do trabalho: classes concretas
 * (Roteador, SwitchRede, Firewall, Servidor) herdam desta e podem
 * sobrescrever o método {@link #diagnosticoEspecifico(MetricaRede)} para
 * adicionar regras particulares a cada tipo de equipamento.
 *
 * Atributos comuns a todos os tipos:
 *  - id: identificador único (gerado automaticamente);
 *  - nome: nome amigável (ex.: "Roteador da Sala");
 *  - enderecoIp: IP ou hostname usado nas ferramentas de diagnóstico;
 *  - ultimaMetrica: última métrica coletada (pode ser null).
 *
 * Importante: a métrica é guardada como `volatile` porque é gravada pela
 * thread do monitor e lida pela thread do Swing (EDT). `volatile` garante
 * visibilidade entre threads para referências de objeto.
 */
public abstract class DispositivoRede {

    // Gerador thread-safe de IDs sequenciais.
    private static final AtomicInteger PROXIMO_ID = new AtomicInteger(1);

    private final int id;
    private String nome;
    private String enderecoIp;
    private volatile MetricaRede ultimaMetrica;

    // Porta TCP opcional a ser testada periodicamente. null = não testar.
    private Integer portaTcp;

    // Se true, o monitor faz HEAD/GET HTTP a cada ciclo de coleta.
    private boolean verificarHttp;

    protected DispositivoRede(String nome, String enderecoIp) {
        this.id = PROXIMO_ID.getAndIncrement();
        this.nome = Objects.requireNonNull(nome, "nome não pode ser nulo");
        this.enderecoIp = Objects.requireNonNull(enderecoIp, "ip não pode ser nulo");
    }

    public int getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = Objects.requireNonNull(nome);
    }

    public String getEnderecoIp() {
        return enderecoIp;
    }

    public void setEnderecoIp(String enderecoIp) {
        this.enderecoIp = Objects.requireNonNull(enderecoIp);
    }

    public MetricaRede getUltimaMetrica() {
        return ultimaMetrica;
    }

    public void setUltimaMetrica(MetricaRede ultimaMetrica) {
        this.ultimaMetrica = ultimaMetrica;
    }

    public Integer getPortaTcp() {
        return portaTcp;
    }

    public void setPortaTcp(Integer portaTcp) {
        // Aceita null (sem teste de porta) ou porta dentro do range válido.
        if (portaTcp != null && (portaTcp < 1 || portaTcp > 65535)) {
            throw new IllegalArgumentException(
                    "Porta TCP deve estar entre 1 e 65535: " + portaTcp);
        }
        this.portaTcp = portaTcp;
    }

    public boolean isVerificarHttp() {
        return verificarHttp;
    }

    public void setVerificarHttp(boolean verificarHttp) {
        this.verificarHttp = verificarHttp;
    }

    /** Atalho para acesso ao status atual (mesmo sem métrica coletada). */
    public StatusDispositivo getStatusAtual() {
        return ultimaMetrica == null
                ? StatusDispositivo.DESCONHECIDO
                : ultimaMetrica.getStatus();
    }

    /**
     * Identifica o tipo do dispositivo em texto. Cada subclasse devolve algo
     * como "Roteador", "Switch", etc. — usado na coluna "Tipo" da tabela.
     */
    public abstract String tipoDispositivo();

    /**
     * Diagnóstico específico do tipo de dispositivo. Cada subclasse pode
     * acrescentar regras próprias (ex.: roteador alerta sobre rota longa,
     * firewall alerta sobre bloqueio, servidor alerta sobre latência alta).
     *
     * Este método é o ponto principal do polimorfismo: o monitor não sabe
     * qual classe concreta está chamando, mas o comportamento certo é
     * executado em tempo de execução.
     */
    public abstract String diagnosticoEspecifico(MetricaRede metrica);

    @Override
    public String toString() {
        return tipoDispositivo() + " " + nome + " (" + enderecoIp + ")";
    }
}
