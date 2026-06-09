package service;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

/**
 * Informações resumidas sobre uma interface de rede local. Usada pela aba
 * "Interfaces de Rede" da GUI para listar as placas do computador.
 */
public class InterfaceRedeInfo {

    private final String nome;
    private final String descricao;
    private final boolean ativa;
    private final boolean loopback;
    private final List<String> enderecos;

    public InterfaceRedeInfo(String nome,
                             String descricao,
                             boolean ativa,
                             boolean loopback,
                             List<String> enderecos) {
        this.nome = nome;
        this.descricao = descricao;
        this.ativa = ativa;
        this.loopback = loopback;
        this.enderecos = enderecos == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(enderecos);
    }

    public String getNome() { return nome; }
    public String getDescricao() { return descricao; }
    public boolean isAtiva() { return ativa; }
    public boolean isLoopback() { return loopback; }
    public List<String> getEnderecos() { return enderecos; }

    /**
     * Constrói a lista de interfaces locais consultando a API
     * {@link NetworkInterface} da JVM. Esse método é estático e estático-amigável
     * porque a coleta é leve e pode ser feita on-demand pela GUI.
     */
    public static List<InterfaceRedeInfo> listarLocais() {
        List<InterfaceRedeInfo> resultado = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
            while (ifaces.hasMoreElements()) {
                NetworkInterface ni = ifaces.nextElement();

                List<String> enderecos = new ArrayList<>();
                Enumeration<InetAddress> addrs = ni.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    enderecos.add(addrs.nextElement().getHostAddress());
                }

                resultado.add(new InterfaceRedeInfo(
                        ni.getName(),
                        ni.getDisplayName(),
                        ni.isUp(),
                        ni.isLoopback(),
                        enderecos
                ));
            }
        } catch (Exception e) {
            // Se houver problema ao listar interfaces, devolve lista vazia
            // — a GUI deve tratar esse caso (ex.: exibir aviso).
        }
        return resultado;
    }
}
