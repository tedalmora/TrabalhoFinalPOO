package model;

/** Firewall — ICMP pode estar bloqueado por política, e isso é esperado. */
public class Firewall extends DispositivoRede {

    public Firewall(String nome, String enderecoIp) {
        super(nome, enderecoIp);
    }

    @Override
    public String tipoDispositivo() { return "Firewall"; }

    @Override
    public String diagnosticoEspecifico(MetricaRede m) {
        if (m == null) return "Sem dados coletados ainda.";
        if (!m.isAlcancavel())
            return "Firewall não respondeu ao ICMP — pode estar offline ou "
                    + "configurado para descartar ping.";
        if (m.getPerdaPacotesPercentual() > 25)
            return "Firewall respondendo de forma intermitente (perda "
                    + m.getPerdaFormatada() + ").";
        return "Firewall ativo e respondendo aos pacotes ICMP.";
    }
}
