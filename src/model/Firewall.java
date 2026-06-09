package model;

/**
 * Subclasse concreta para Firewalls. Um firewall pode estar online mas
 * bloqueando ICMP — então só usamos perda de pacote como diagnóstico
 * complementar, não como única fonte de verdade.
 */
public class Firewall extends DispositivoRede {

    public Firewall(String nome, String enderecoIp) {
        super(nome, enderecoIp);
    }

    @Override
    public String tipoDispositivo() {
        return "Firewall";
    }

    @Override
    public String diagnosticoEspecifico(MetricaRede metrica) {
        if (metrica == null) {
            return "Sem dados coletados ainda.";
        }
        if (!metrica.isAlcancavel()) {
            // Muitos firewalls bloqueiam ICMP por padrão; o operador
            // precisa interpretar esse resultado com cuidado.
            return "Firewall não respondeu ao ICMP — pode estar offline OU "
                    + "configurado para descartar ping (verifique regras).";
        }
        if (metrica.getPerdaPacotesPercentual() > 25) {
            return "Firewall respondendo de forma intermitente (perda "
                    + metrica.getPerdaFormatada() + ").";
        }
        return "Firewall ativo e respondendo aos pacotes ICMP.";
    }
}
