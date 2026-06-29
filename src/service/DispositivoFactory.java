package service;

import model.DispositivoRede;
import model.Firewall;
import model.Roteador;
import model.Servidor;
import model.SwitchRede;

/*
Centraliza a criação das subclasses de DispositivoRede. A GUI não precisa conhecer as classes concretas.
Passa o enum e recebe um DispositivoRede pronto.
*/
public final class DispositivoFactory {

    // tipos de dispositivos que podem ser criados
    // a ui usa tambem pra popular o combo box de tipos de dispositivos
    public enum TipoDispositivo {
        ROTEADOR("Roteador"),
        SWITCH("Switch"),
        FIREWALL("Firewall"),
        SERVIDOR("Servidor");

        private String rotulo;

        TipoDispositivo(String rotulo) { 
            this.rotulo = rotulo; 
        }

        public String getRotulo() { 
            return rotulo; 
        }

        @Override 
        public String toString() { 
            return rotulo; 
        }
    }

    private DispositivoFactory() { }

    public static DispositivoRede criar(TipoDispositivo tipo, String nome, String ip) {
        if (tipo == null)
            // se tipo == null, jogo exceção, pq não faz sentido criar um dispositivo sem tipo
            throw new IllegalArgumentException("Tipo é obrigatório.");
        if (nome == null)
            throw new IllegalArgumentException("Nome é obrigatório.");
        if (ip == null)
            throw new IllegalArgumentException("IP/host é obrigatório.");

        String n = nome.trim(); // removo espaços em branco do começo e do fim
        String i = ip.trim(); 

        // polimorfismo
        DispositivoRede d;
        switch (tipo) {
            // crio a depender do tipo informado.
            case ROTEADOR: 
                d = new Roteador(n, i); 
                break;
            case SWITCH:   
                d = new SwitchRede(n, i); 
                break;
            case FIREWALL: 
                d = new Firewall(n, i); 
                break;
            case SERVIDOR: 
                d = new Servidor(n, i); 
                break;
            // se for outro tipo
            default: throw new IllegalArgumentException("Tipo inválido.");
        }
        return d;
    }

    // descobre o tipo de um dispositivo já existente (polimorfismo)
    public static TipoDispositivo tipoDe(DispositivoRede d) {
        for (TipoDispositivo t : TipoDispositivo.values()) {
            if (t.getRotulo().equalsIgnoreCase(d.tipoDispositivo())) return t;
        }
        return TipoDispositivo.SERVIDOR;
    }
}
