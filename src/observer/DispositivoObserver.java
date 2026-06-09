package observer;

import model.DispositivoRede;

/**
 * Implementação do padrão de projeto OBSERVER.
 *
 * Esta interface é o contrato que qualquer parte interessada deve
 * implementar para ser notificada quando o monitor terminar uma coleta
 * (e portanto a métrica do dispositivo mudou).
 *
 * No nosso sistema o principal observador é a janela principal da GUI:
 * quando o monitor (em uma thread de background) atualiza a métrica de um
 * dispositivo, a janela é avisada e refaz a linha correspondente na
 * JTable (sempre na EDT — Event Dispatch Thread do Swing).
 *
 * Vantagens deste desenho:
 *  - desacopla o monitor da GUI (o monitor não conhece Swing);
 *  - permite múltiplos observadores no futuro (ex.: logger, exportador).
 */
public interface DispositivoObserver {

    /**
     * Chamado pelo monitor após cada coleta.
     *
     * @param dispositivo o dispositivo cuja métrica foi atualizada.
     *                    A nova métrica está acessível via
     *                    {@link DispositivoRede#getUltimaMetrica()}.
     */
    void aoAtualizarDispositivo(DispositivoRede dispositivo);
}
