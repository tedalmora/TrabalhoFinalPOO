package service;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

//Informações resumidas sobre uma interface de rede local. Usada pela aba "Interfaces de Rede" da GUI para listar as placas do computador.

public class InterfaceRedeInfo {

    private String nome;
    private String descricao;
    private boolean ativa;
    private boolean loopback;
    private List<String> enderecos;

    public InterfaceRedeInfo(String nome,String descricao,boolean ativa,boolean loopback,List<String> enderecos) {
        this.nome = nome;
        this.descricao = descricao;
        this.ativa = ativa;
        this.loopback = loopback;
        this.enderecos = enderecos == null ? Collections.emptyList() : enderecos;
    }

    public String getNome() { 
        return nome; 
    }

    public String getDescricao() { 
        return descricao; 
    }

    public boolean isAtiva() { 
        return ativa; 
    }

    public boolean isLoopback() { 
        return loopback; 
    }

    public List<String> getEnderecos() { 
        return enderecos; 
    }

    // Constrói a lista de interfaces locais consultando a API de netowrk da java
    public static List<InterfaceRedeInfo> listarLocais() {
        List<InterfaceRedeInfo> resultado = new ArrayList<>();
        try {
            //pega a lista de interfaces de rede do SO
            // é um enumeration pois a API de network da java é antiga e usa enumeration
            Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
            while (ifaces.hasMoreElements()) { // enquanto houver interfaces, pega a proxima
                NetworkInterface ni = ifaces.nextElement(); // pega a proxima interface

                List<String> enderecos = new ArrayList<>();

                //pega a lista de endereços IP da interface
                Enumeration<InetAddress> addrs = ni.getInetAddresses();
                while (addrs.hasMoreElements()) { //enquanto tem elemento, adiciona em enderecos
                    enderecos.add(addrs.nextElement().getHostAddress());
                }

                // adiciono na lista com as interfaces de resultado
                resultado.add(new InterfaceRedeInfo(ni.getName(),ni.getDisplayName(),ni.isUp(),ni.isLoopback(),enderecos));
            }
        } catch (Exception e) {
            // Se houver problema ao listar interfaces, faz nada
        }
        return resultado;
    }
}
