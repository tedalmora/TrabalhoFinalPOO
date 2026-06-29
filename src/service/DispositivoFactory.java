package service;

import model.DispositivoRede;
import model.Firewall;
import model.Roteador;
import model.Servidor;
import model.SwitchRede;

/**
 * Padrão FACTORY METHOD: centraliza a criação das subclasses de
 * DispositivoRede. A GUI não precisa conhecer as classes concretas —
 * passa o enum {@link TipoDispositivo} e recebe um DispositivoRede pronto.
 */
public final class DispositivoFactory {

    /** Tipos de dispositivo suportados. Usado pelo combo do diálogo. */
    public enum TipoDispositivo {
        ROTEADOR("Roteador"),
        SWITCH("Switch"),
        FIREWALL("Firewall"),
        SERVIDOR("Servidor");

        private final String rotulo;
        TipoDispositivo(String rotulo) { this.rotulo = rotulo; }
        public String getRotulo() { return rotulo; }
        @Override public String toString() { return rotulo; }
    }

    private DispositivoFactory() { }

    public static DispositivoRede criar(TipoDispositivo tipo,
                                        String nome,
                                        String ip) {
        if (tipo == null)
            throw new IllegalArgumentException("Tipo é obrigatório.");
        if (nome == null || nome.trim().isEmpty())
            throw new IllegalArgumentException("Nome é obrigatório.");
        if (ip == null || ip.trim().isEmpty())
            throw new IllegalArgumentException("IP/host é obrigatório.");

        String n = nome.trim();
        String i = ip.trim();

        // Aqui é onde o polimorfismo se constrói: a partir deste ponto o
        // resto do sistema trabalha apenas com a referência DispositivoRede.
        DispositivoRede d;
        switch (tipo) {
            case ROTEADOR: d = new Roteador(n, i); break;
            case SWITCH:   d = new SwitchRede(n, i); break;
            case FIREWALL: d = new Firewall(n, i); break;
            case SERVIDOR: d = new Servidor(n, i); break;
            default: throw new IllegalArgumentException("Tipo inválido.");
        }
        return d;
    }

    /** Descobre o enum equivalente ao tipo declarado pelo dispositivo. */
    public static TipoDispositivo tipoDe(DispositivoRede d) {
        for (TipoDispositivo t : TipoDispositivo.values()) {
            if (t.getRotulo().equalsIgnoreCase(d.tipoDispositivo())) return t;
        }
        return TipoDispositivo.SERVIDOR;
    }
}
