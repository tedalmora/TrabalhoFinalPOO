import java.awt.EventQueue;

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

        // crio a GUI e passo o monitor para ela, para que a GUI possa exibir os dispositivos e suas métricas
        // eu mando minha UI pra thread de eventos do Swing, que é a thread que cuida da interface gráfica
        // toda mudança na interface gráfica deve ser feita nessa thread, senão dá erro
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                new JanelaPrincipal(monitor).setVisible(true);
            }
        });
    }
}
