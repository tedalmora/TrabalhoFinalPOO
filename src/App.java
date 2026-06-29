import javax.swing.SwingUtilities;

import gui.JanelaPrincipal;
import monitor.MonitorDispositivos;

/**
 * Ponto de entrada do "Gerenciador de Dispositivos de Rede".
 *
 * Padrões: Factory Method (DispositivoFactory) e Observer (DispositivoObserver).
 * Itens avaliativos: polimorfismo (model.DispositivoRede + subclasses),
 * threads (MonitorDispositivos), GUI (pacote gui), padrão de projeto.
 */
public class App {

    public static void main(String[] args) {

        // crio um monitor de dispositivos que gerencia as threads dos meus devices
        MonitorDispositivos monitor = new MonitorDispositivos();

        // nao uso eventqueue.invokeLater() aqui porque a janela principal ja faz isso internamente. so crio a janela e deixo ela visivel, o resto é com ela.
        SwingUtilities.invokeLater(() -> new JanelaPrincipal(monitor).setVisible(true));
    }
}
