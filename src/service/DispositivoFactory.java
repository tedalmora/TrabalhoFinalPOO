package service;

import model.DispositivoRede;
import model.Firewall;
import model.Roteador;
import model.Servidor;
import model.SwitchRede;

/**
 * Implementação do padrão de projeto FACTORY METHOD.
 *
 * O objetivo do Factory é centralizar a criação de objetos de uma
 * família (no caso, subclasses de {@link DispositivoRede}) escondendo
 * do código cliente os detalhes de qual classe concreta instanciar.
 * Assim, a GUI não precisa importar diretamente as classes Roteador,
 * SwitchRede, Firewall e Servidor: ela passa o tipo desejado como
 * string e recebe um DispositivoRede já configurado.
 *
 * Vantagens dessa abordagem:
 *  - Acrescentar um novo tipo (ex.: AccessPoint) exige mexer apenas
 *    aqui e no enum {@link TipoDispositivo}, sem alterar a GUI;
 *  - Garante validação centralizada (ex.: nome e IP obrigatórios);
 *  - Mantém o princípio Aberto/Fechado (Open/Closed).
 */
public final class DispositivoFactory {

    /**
     * Enumeração dos tipos suportados. É usada pelo combo box da GUI
     * para apresentar as opções ao usuário.
     */
    public enum TipoDispositivo {
        ROTEADOR("Roteador"),
        SWITCH("Switch"),
        FIREWALL("Firewall"),
        SERVIDOR("Servidor");

        private final String rotulo;

        TipoDispositivo(String rotulo) {
            this.rotulo = rotulo;
        }

        /** Texto amigável exibido na interface gráfica. */
        public String getRotulo() {
            return rotulo;
        }

        @Override
        public String toString() {
            return rotulo;
        }
    }

    private DispositivoFactory() {
        // Classe utilitária — não pode ser instanciada.
    }

    /**
     * Cria um novo dispositivo do tipo solicitado, sem porta TCP nem
     * verificação HTTP. Atalho usado quando o chamador não precisa
     * desses testes extras.
     */
    public static DispositivoRede criar(TipoDispositivo tipo, String nome, String ip) {
        return criar(tipo, nome, ip, null, false);
    }

    /**
     * Versão completa: permite informar a porta TCP a monitorar
     * (null = não monitorar) e se a verificação HTTP deve ser feita.
     *
     * @throws IllegalArgumentException se nome/ip estiverem vazios,
     *                                  porta fora do range, ou tipo desconhecido.
     */
    public static DispositivoRede criar(TipoDispositivo tipo,
                                        String nome,
                                        String ip,
                                        Integer portaTcp,
                                        boolean verificarHttp) {
        if (tipo == null) {
            throw new IllegalArgumentException("Tipo do dispositivo é obrigatório.");
        }
        if (nome == null || nome.trim().isEmpty()) {
            throw new IllegalArgumentException("Nome do dispositivo é obrigatório.");
        }
        if (ip == null || ip.trim().isEmpty()) {
            throw new IllegalArgumentException("Endereço IP/hostname é obrigatório.");
        }
        if (portaTcp != null && (portaTcp < 1 || portaTcp > 65535)) {
            throw new IllegalArgumentException(
                    "Porta TCP deve estar entre 1 e 65535.");
        }

        String nomeLimpo = nome.trim();
        String ipLimpo = ip.trim();

        // O switch escolhe a classe concreta. Este é o ponto onde o
        // polimorfismo se "constrói": a partir daqui o resto do sistema
        // trabalha apenas com a referência DispositivoRede.
        DispositivoRede d;
        switch (tipo) {
            case ROTEADOR: d = new Roteador(nomeLimpo, ipLimpo); break;
            case SWITCH:   d = new SwitchRede(nomeLimpo, ipLimpo); break;
            case FIREWALL: d = new Firewall(nomeLimpo, ipLimpo); break;
            case SERVIDOR: d = new Servidor(nomeLimpo, ipLimpo); break;
            default:
                throw new IllegalArgumentException("Tipo não suportado: " + tipo);
        }
        d.setPortaTcp(portaTcp);
        d.setVerificarHttp(verificarHttp);
        return d;
    }

    /**
     * Faz o caminho inverso: dado um dispositivo já existente, devolve
     * o enum equivalente. Útil para preencher o combo box do diálogo
     * de edição com o tipo correto pré-selecionado.
     */
    public static TipoDispositivo tipoDe(DispositivoRede dispositivo) {
        if (dispositivo instanceof Roteador)   return TipoDispositivo.ROTEADOR;
        if (dispositivo instanceof SwitchRede) return TipoDispositivo.SWITCH;
        if (dispositivo instanceof Firewall)   return TipoDispositivo.FIREWALL;
        if (dispositivo instanceof Servidor)   return TipoDispositivo.SERVIDOR;
        throw new IllegalArgumentException(
                "Dispositivo desconhecido: " + dispositivo.getClass().getName());
    }
}
