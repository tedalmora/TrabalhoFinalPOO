import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import gui.JanelaPrincipal;
import monitor.MonitorDispositivos;
import service.DispositivoFactory;
import service.DispositivoFactory.TipoDispositivo;

/**
 * Ponto de entrada do "Gerenciador de Dispositivos de Rede".
 *
 * Padrões: Factory Method (DispositivoFactory) e Observer (DispositivoObserver).
 * Itens avaliativos: polimorfismo (model.DispositivoRede + subclasses),
 * threads (MonitorDispositivos), GUI (pacote gui), padrão de projeto.
 */
public class App {

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) { }

        MonitorDispositivos monitor = new MonitorDispositivos();

        // Dispositivos de exemplo para abrir a app já com algo monitorado.
        monitor.adicionarDispositivo(DispositivoFactory.criar(
                TipoDispositivo.SERVIDOR, "Cloudflare", "1.1.1.1", 443, true));
        monitor.adicionarDispositivo(DispositivoFactory.criar(
                TipoDispositivo.SERVIDOR, "Google DNS", "8.8.8.8"));

        // Swing exige que a UI seja criada na EDT.
        SwingUtilities.invokeLater(() -> new JanelaPrincipal(monitor).setVisible(true));
    }
}
