package model;

/**
 * Subclasse concreta para Servidores. Foco em latência: servidores que
 * hospedam serviços críticos precisam responder rapidamente.
 */
public class Servidor extends DispositivoRede {

    public Servidor(String nome, String enderecoIp) {
        super(nome, enderecoIp);
    }

    @Override
    public String tipoDispositivo() {
        return "Servidor";
    }

    @Override
    public String diagnosticoEspecifico(MetricaRede metrica) {
        if (metrica == null) {
            return "Sem dados coletados ainda.";
        }
        if (!metrica.isAlcancavel()) {
            return "Servidor offline — verifique serviços e conectividade.";
        }
        if (metrica.getPerdaPacotesPercentual() > 0) {
            return "Servidor respondendo, mas com perda de pacotes ("
                    + metrica.getPerdaFormatada() + ") — investigar rede.";
        }
        if (metrica.getLatenciaMediaMs() > 80) {
            return "Servidor com latência elevada ("
                    + metrica.getLatenciaFormatada()
                    + ") — pode impactar usuários.";
        }
        return "Servidor saudável.";
    }
}
