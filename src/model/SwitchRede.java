package model;

/**
 * Subclasse concreta para Switches. Usamos o nome "SwitchRede" porque
 * "switch" é uma palavra reservada da linguagem Java e não pode ser
 * usada como nome de classe.
 */
public class SwitchRede extends DispositivoRede {

    public SwitchRede(String nome, String enderecoIp) {
        super(nome, enderecoIp);
    }

    @Override
    public String tipoDispositivo() {
        return "Switch";
    }

    @Override
    public String diagnosticoEspecifico(MetricaRede metrica) {
        if (metrica == null) {
            return "Sem dados coletados ainda.";
        }
        if (!metrica.isAlcancavel()) {
            return "Switch não respondeu — equipamento pode estar desligado "
                    + "ou sem IP de gerência configurado.";
        }
        // Switches gerenciáveis costumam ficar na mesma LAN — esperam-se
        // latências bem baixas. Qualquer coisa acima de 20ms é suspeita.
        if (metrica.getLatenciaMediaMs() > 20) {
            return "Switch respondendo, porém com latência incomum para LAN ("
                    + metrica.getLatenciaFormatada() + ").";
        }
        return "Switch operando normalmente na LAN.";
    }
}
