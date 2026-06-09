package gui;

import java.util.ArrayList;
import java.util.List;
import javax.swing.table.AbstractTableModel;

import model.DispositivoRede;
import model.MetricaRede;

/**
 * TableModel customizado que alimenta a JTable principal de dispositivos.
 *
 * Importante: este modelo é manipulado SEMPRE pela Event Dispatch Thread
 * (EDT) do Swing. A janela principal cuida de empurrar para a EDT as
 * atualizações recebidas das threads de monitoramento.
 *
 * Colunas:
 *  0) Status (renderizado como bola colorida pelo renderer da janela);
 *  1) Tipo (Roteador, Switch, Firewall, Servidor);
 *  2) Nome;
 *  3) IP/Host;
 *  4) Latência média;
 *  5) Perda de pacotes;
 *  6) Última coleta (hora).
 */
public class ModeloDispositivos extends AbstractTableModel {

    private static final String[] COLUNAS = {
            "Status", "Tipo", "Nome", "IP/Host", "Latência", "Perda", "Atualizado"
    };

    private final List<DispositivoRede> linhas = new ArrayList<>();

    public void adicionar(DispositivoRede d) {
        linhas.add(d);
        int idx = linhas.size() - 1;
        fireTableRowsInserted(idx, idx);
    }

    public void remover(int linha) {
        if (linha >= 0 && linha < linhas.size()) {
            linhas.remove(linha);
            fireTableRowsDeleted(linha, linha);
        }
    }

    public DispositivoRede getDispositivo(int linha) {
        if (linha < 0 || linha >= linhas.size()) return null;
        return linhas.get(linha);
    }

    /** Localiza a linha de um dispositivo específico. Retorna -1 se não achar. */
    public int linhaDe(DispositivoRede d) {
        for (int i = 0; i < linhas.size(); i++) {
            if (linhas.get(i).getId() == d.getId()) return i;
        }
        return -1;
    }

    /** Notifica a JTable que uma linha mudou (após atualização de métrica). */
    public void atualizarLinha(int linha) {
        if (linha >= 0 && linha < linhas.size()) {
            fireTableRowsUpdated(linha, linha);
        }
    }

    @Override public int getRowCount() { return linhas.size(); }
    @Override public int getColumnCount() { return COLUNAS.length; }
    @Override public String getColumnName(int c) { return COLUNAS[c]; }

    @Override
    public Object getValueAt(int r, int c) {
        DispositivoRede d = linhas.get(r);
        MetricaRede m = d.getUltimaMetrica();
        switch (c) {
            case 0: return d.getStatusAtual(); // renderer transforma em bolinha colorida
            case 1: return d.tipoDispositivo();
            case 2: return d.getNome();
            case 3: return d.getEnderecoIp();
            case 4: return m == null ? "-" : m.getLatenciaFormatada();
            case 5: return m == null ? "-" : m.getPerdaFormatada();
            case 6: return m == null ? "-" : m.getColetadaEmFormatada();
            default: return "";
        }
    }
}
