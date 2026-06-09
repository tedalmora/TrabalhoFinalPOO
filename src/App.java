import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import gui.JanelaPrincipal;
import monitor.MonitorDispositivos;
import service.DispositivoFactory;
import service.DispositivoFactory.TipoDispositivo;

/**
 * Ponto de entrada da aplicação "Gerenciador de Dispositivos de Rede".
 *
 * Responsabilidades:
 *  1. Aplicar o look-and-feel do sistema (UX nativa);
 *  2. Instanciar o {@link MonitorDispositivos} (núcleo de threads);
 *  3. Pré-cadastrar alguns dispositivos de exemplo (para demonstração);
 *  4. Criar e exibir a janela principal NA EVENT DISPATCH THREAD.
 *
 * Regra de ouro do Swing: TODA criação/manipulação de componentes deve
 * acontecer na EDT. Por isso usamos SwingUtilities.invokeLater.
 *
 * Visão geral da arquitetura:
 *
 *  +--------------+   notifica   +--------------------+   atualiza   +-----+
 *  |  Monitor     |------------->|  JanelaPrincipal   |------------->| GUI |
 *  |  (threads)   |   (Observer) |  (DispositivoObs.) |              +-----+
 *  +--------------+              +--------------------+
 *        |
 *        | usa
 *        v
 *  +--------------+
 *  | Ferramenta-  |  (ping / traceroute / MTR — processos do SO)
 *  | Rede         |
 *  +--------------+
 *
 *  Padrões de projeto utilizados:
 *    - FACTORY METHOD: {@link DispositivoFactory} centraliza a criação
 *      de subclasses concretas de DispositivoRede;
 *    - OBSERVER: {@link observer.DispositivoObserver} permite que a GUI
 *      reaja a mudanças de métrica sem o monitor conhecer Swing.
 *
 *  Itens avaliativos cobertos:
 *    a) Polimorfismo com classe abstrata (DispositivoRede + 4 subclasses);
 *    b) Threads (ScheduledExecutorService no MonitorDispositivos);
 *    c) Interface gráfica (Swing — JanelaPrincipal, diálogos, painéis);
 *    d) Padrão de projeto (Factory + Observer).
 */
public class App {

    public static void main(String[] args) {
        // Aplica o look-and-feel do sistema operacional. Se falhar,
        // o Swing simplesmente cai no L&F padrão (Metal) — sem problema.
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) { }

        // Cria o monitor que cuidará das threads de coleta.
        final MonitorDispositivos monitor = new MonitorDispositivos();

        // Pré-cadastra alguns dispositivos de exemplo para que o usuário
        // veja a aplicação em funcionamento imediatamente após abrir.
        // Comente estas linhas se preferir abrir a aplicação vazia.
        monitor.adicionarDispositivo(DispositivoFactory.criar(
                TipoDispositivo.SERVIDOR, "DNS Google", "8.8.8.8",
                53, false));
        monitor.adicionarDispositivo(DispositivoFactory.criar(
                TipoDispositivo.SERVIDOR, "Cloudflare DNS-over-HTTPS",
                "1.1.1.1", 443, true));
        monitor.adicionarDispositivo(DispositivoFactory.criar(
                TipoDispositivo.ROTEADOR, "Gateway local", "127.0.0.1"));

        // Cria e exibe a janela principal na EDT.
        SwingUtilities.invokeLater(() -> {
            JanelaPrincipal janela = new JanelaPrincipal(monitor);
            janela.setVisible(true);
        });
    }
}
