package model;

// Switch (em LAN espera-se latência muito baixa).
public class SwitchRede extends DispositivoRede {

    public SwitchRede(String nome, String enderecoIp) {
        super(nome, enderecoIp);
    }

    @Override
    public String tipoDispositivo() { 
        return "Switch"; 
    }

    @Override
    public String diagnosticoEspecifico(MetricaRede m) {
        if (m == null) return "Sem dados coletados ainda.";
        if (!m.isAlcancavel())
            return "Switch não respondeu — verifique alimentação ou IP de gerência.";
        if (m.getLatenciaMediaMs() > 20)
            return "Switch respondendo com latência incomum para LAN (" + m.getLatenciaFormatada() + ").";
        return "Switch operando normalmente na LAN.";
    }
}
