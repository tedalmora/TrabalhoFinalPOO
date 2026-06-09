package model;

/**
 * Subclasse concreta que representa um Roteador. Sobrescreve
 * {@link #diagnosticoEspecifico(MetricaRede)} para destacar problemas
 * comuns em roteadores, como rotas excessivamente longas (muitos saltos).
 */
public class Roteador extends DispositivoRede {

    public Roteador(String nome, String enderecoIp) {
        super(nome, enderecoIp);
    }

    @Override
    public String tipoDispositivo() {
        return "Roteador";
    }

    @Override
    public String diagnosticoEspecifico(MetricaRede metrica) {
        if (metrica == null) {
            return "Sem dados coletados ainda.";
        }
        if (!metrica.isAlcancavel()) {
            return "Roteador não respondeu — verifique alimentação e cabos.";
        }
        // Heurística específica de roteador: rota muito longa pode indicar
        // problema de roteamento (loop, redirecionamentos inesperados).
        int saltos = metrica.getRotaTraceroute().size();
        if (saltos > 15) {
            return "Roteador alcançável, mas a rota possui " + saltos
                    + " saltos (verifique a tabela de roteamento).";
        }
        if (metrica.getLatenciaMediaMs() > 100) {
            return "Roteador respondendo com latência alta ("
                    + metrica.getLatenciaFormatada() + ").";
        }
        return "Roteador funcionando normalmente.";
    }
}
