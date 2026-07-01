package monitor;

import classes.DispositivoRede;

/*
Padrão Observer: contrato implementado por quem quiser ser avisado
quando o monitor termina uma coleta. Na prática, o único observador
é a JanelaPrincipal, que atualiza a tabela e o painel de detalhes.
Na prática, o monitor chama o método aoAtualizarDispositivo() de cada observador quando um disp é atualizado
Nesse caso, so a JanelaPrincipal tem esse observer, entao é a unica avisada.
MOnitor (serie de threads verificando os devices) ->
Aacab de veririfcar o device e chama->
JanelaPrincipal.aoAtualizarDispositivo (tabela de dispositivos e painel de detalhes que implementa observer) ->
atualiza a tabela e o painel de detalhes com os dados do dispositivo atualizado, usando o swing thread
*/
public interface DispositivoObserver {

    // Chamado pelo monitor após cada coleta.
    void aoAtualizarDispositivo(DispositivoRede dispositivo);
}
