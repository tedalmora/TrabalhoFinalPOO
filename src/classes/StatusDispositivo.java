package classes;

import java.awt.Color;

/*
Enumeração que representa o estado operacional de um dispositivo de rede.
Esses status são usados pela GUI para colorir as linhas da tabela
(verde = OK, amarelo = ATENCAO, vermelho = FALHA, cinza = DESCONHECIDO),
eu uso um enum assim:
StatusDispositivo status = StatusDispositivo.OK;
ou Dispositivo.getUltimaMetrica().getStatus() == StatusDispositivo.FALHA
*/

// cada status (OK, ATENCAO, etc) é um valor do enum, e cada valor tem uma descrição e uma cor associada. A descrição é usada na interface gráfica, e a cor é usada para colorir a linha da tabela de dispositivos.
public enum StatusDispositivo {

    // Dispositivo respondeu ao ping com latência baixa e sem perda.
    OK("Operacional", new Color(76, 175, 80)),

    // Dispositivo respondeu, porém com latência alta ou perda parcial de pacotes.
    ATENCAO("Instável", new Color(255, 193, 7)),

    // Dispositivo não respondeu a nenhum pacote (provavelmente offline).
    FALHA("Falha / Offline", new Color(244, 67, 54)),

    // Ainda não foi feita nenhuma coleta para este dispositivo.
    DESCONHECIDO("Aguardando coleta", new Color(189, 189, 189));

    // Texto amigável para exibir na interface gráfica.
    private String descricao;

    // Cor associada ao status (usada pelo renderer da JTable).
    private Color cor;

    StatusDispositivo(String descricao, Color cor) {
        this.descricao = descricao;
        this.cor = cor;
    }

    public String getDescricao() {
        return descricao;
    }

    public Color getCor() {
        return cor;
    }
}
