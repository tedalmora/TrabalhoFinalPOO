package monitor;

import model.DispositivoRede;

/*
Padrão Observer: contrato implementado por quem quiser ser avisado
quando o monitor termina uma coleta. Na prática, o único observador
é a JanelaPrincipal, que atualiza a tabela e o painel de detalhes.
*/
public interface DispositivoObserver {

    // Chamado pelo monitor após cada coleta.
    void aoAtualizarDispositivo(DispositivoRede dispositivo);
}
