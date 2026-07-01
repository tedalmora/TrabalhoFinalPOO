package classes;

// Roteador. Alerta se a rota tem muitos saltos (possível loop, quando o roteador está mal configurado) ou se a latência é alta.
public class Roteador extends DispositivoRede {

    public Roteador(String nome, String enderecoIp) {
        super(nome, enderecoIp);
    }

    @Override
    public String tipoDispositivo() { 
        return "Roteador"; 
    }

    @Override
    public String diagnosticoEspecifico(MetricaRede m) {
        if (m == null) return "Sem dados coletados ainda.";
        if (!m.isAlcancavel())
            return "Roteador não respondeu — verifique alimentação e cabos.";
        int saltos = m.getRotaTraceroute().size();
        if (saltos > 15)
            return "Roteador alcançável, mas rota com " + saltos + " saltos.";
        if (m.getLatenciaMediaMs() > 100)
            return "Roteador respondendo com latência alta (" + m.getLatenciaFormatada() + ").";
        return "Roteador funcionando normalmente.";
    }
}
