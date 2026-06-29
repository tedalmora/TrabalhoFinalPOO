package model;

// Servidor — foco em latência e perda zero.
public class Servidor extends DispositivoRede {

    public Servidor(String nome, String enderecoIp) {
        super(nome, enderecoIp);
    }

    @Override
    public String tipoDispositivo() { 
        return "Servidor"; 
    }

    @Override
    public String diagnosticoEspecifico(MetricaRede m) {
        if (m == null) return "Sem dados coletados ainda.";
        if (!m.isAlcancavel())
            return "Servidor offline — verifique serviços e conectividade.";
        if (m.getPerdaPacotesPercentual() > 0)
            return "Servidor respondendo, mas com perda de pacotes (" + m.getPerdaFormatada() + ").";
        if (m.getLatenciaMediaMs() > 80)
            return "Servidor com latência elevada (" + m.getLatenciaFormatada() + ").";
        return "Servidor saudável.";
    }
}
