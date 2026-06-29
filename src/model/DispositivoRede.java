package model;

import java.util.Objects;

/*
Classe abstrata que representa qualquer dispositivo de rede monitorado pelo
sistema. Classes concretas (Roteador, SwitchRede, Firewall, Servidor) herdam desta.

Atributos comuns a todos os tipos:
 - id: identificador único (gerado automaticamente);
 - nome: nome amigável (ex.: "Roteador da Sala");
 - enderecoIp: IP ou hostname usado nas ferramentas de diagnóstico;
 - ultimaMetrica: última métrica coletada (pode ser null).
*/
public abstract class DispositivoRede {

    // Gerador thread-safe de IDs sequenciais.
    private static int PROXIMO_ID = 1;

    private final int id;
    private String nome;
    private String enderecoIp;
    private volatile MetricaRede ultimaMetrica;

    protected DispositivoRede(String nome, String enderecoIp) {
        this.id = PROXIMO_ID++;
        this.nome = Objects.requireNonNull(nome, "nome não pode ser nulo"); //ja retorna 
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

    // Atalho para acesso ao status atual (mesmo sem métrica coletada)
    public StatusDispositivo getStatusAtual() {
        if (ultimaMetrica==null) {
            return StatusDispositivo.DESCONHECIDO;
        } else {
            return ultimaMetrica.getStatus();
        }
    }

    // Identifica o tipo do dispositivo em texto. Cada subclasse devolve algo como "Roteador", "Switch", etc.
    public abstract String tipoDispositivo();

    //cada subclasse tem seu diagnostico, e a metrica é passada para ela, que devolve uma string com o diagnostico
    public abstract String diagnosticoEspecifico(MetricaRede metrica);

    @Override
    public String toString() {
        return tipoDispositivo() + " " + nome + " (" + enderecoIp + ")";
    }
}
