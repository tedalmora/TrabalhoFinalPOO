# Gerenciador de Dispositivos de Rede — Documentação

Trabalho Final de POO — aplicação Java Swing para cadastro, monitoramento
e diagnóstico de dispositivos de rede (roteadores, switches, firewalls e
servidores).

Este documento tem duas partes:

1. **Mapa enunciado ↔ código** — onde cada requisito está implementado.
2. **Arquitetura e módulos** — explicação detalhada do funcionamento.

---

## 1. Mapa: enunciado ↔ código

### 1.1 Itens avaliativos (Entrega 2)

| Item do enunciado | Onde está no código |
|---|---|
| **a) Polimorfismo** (interface ou classe abstrata) | Classe abstrata `model.DispositivoRede` (`src/model/DispositivoRede.java`) e quatro subclasses concretas: `Roteador`, `SwitchRede`, `Firewall`, `Servidor`. Cada subclasse sobrescreve `tipoDispositivo()` e `diagnosticoEspecifico(MetricaRede)`. O monitor trabalha apenas com a referência abstrata e o método correto é resolvido em tempo de execução. |
| **b) Threads** | `monitor.MonitorDispositivos` (`src/monitor/MonitorDispositivos.java`) cria um `ScheduledExecutorService` com 4 threads daemon. Cada dispositivo é monitorado por uma tarefa periódica (`scheduleWithFixedDelay`) que executa ping, traceroute e os demais testes sem bloquear a interface gráfica. |
| **c) Interface gráfica** | Pacote `gui/` em Java Swing: `JanelaPrincipal` (janela principal com JTable, JTabbedPane, JSplitPane, JToolBar), `DialogoDispositivo` (cadastro/edição), `PainelDetalhes` (resultado por dispositivo), `PainelInterfaces` (placas locais) e `PainelRotas` (tabela de rotas). |
| **d) Padrão de projeto** | Dois padrões foram aplicados: **Factory Method** em `service.DispositivoFactory` (centraliza a criação das subclasses concretas) e **Observer** em `observer.DispositivoObserver` + `MonitorDispositivos` ↔ `JanelaPrincipal` (a GUI é notificada quando uma métrica é atualizada). |

### 1.2 Funcionalidades pedidas no enunciado

| Requisito | Onde está |
|---|---|
| Cadastrar dispositivos de rede | `gui.DialogoDispositivo` (formulário com nome, IP, tipo, porta TCP, HTTP) + `service.DispositivoFactory.criar(...)`. |
| Diferentes tipos de equipamentos (roteador, switch, firewall, servidor) | `service.DispositivoFactory.TipoDispositivo` (enum) e as 4 subclasses concretas em `model/`. |
| Editar dispositivos | `gui.DialogoDispositivo` reabre com o dispositivo selecionado e altera-o in-place (`emEdicao.setNome(...)`, etc.). |
| Remover dispositivos | `gui.JanelaPrincipal.removerSelecionado()` chama `monitor.removerDispositivo(d)`. |
| **Ping** (conectividade + latência) | `service.FerramentaRede.ping(host, qtdPacotes)` — executa o `ping` do SO, faz o parsing da saída e devolve `ResultadoPing`. |
| **Traceroute** (caminho até o destino) | `service.FerramentaRede.traceroute(host)` — usa `tracert` no Windows e `traceroute` no Linux. |
| **MTR** (latência + perda) | `service.FerramentaRede.mtrSimplificado(host)` — ping com 8 pacotes; o `ResultadoPing` já entrega latência média + percentual de perda, exatamente o que o MTR resume. |
| **DNS direto** (hostname → IP) | `service.FerramentaRede.resolverDnsDireto(host)` — usa `InetAddress.getAllByName`. |
| **DNS reverso** (IP → hostname) | `service.FerramentaRede.resolverDnsReverso(host)` — usa `InetAddress.getByAddress` + `getCanonicalHostName` (forçando o lookup PTR real). |
| **Teste de porta TCP** | `service.FerramentaRede.testarPortaTcp(host, porta, timeout)` — `Socket.connect` com timeout. Cadastrado por dispositivo (campo opcional no diálogo). |
| **HTTP HEAD/GET** | `service.FerramentaRede.testarHttp(host, porta)` — tenta HEAD; cai para GET em caso de 405. Habilitado por dispositivo (checkbox no diálogo). |
| **Consulta às interfaces de rede locais** | `service.InterfaceRedeInfo.listarLocais()` (usa `java.net.NetworkInterface`) e aba "Interfaces de Rede" (`gui.PainelInterfaces`). |
| **Tabela de rotas locais** | `service.RotaInfo.listarLocais()` (`route print -4` no Windows, `netstat -rn` no Linux) e aba "Tabela de Rotas" (`gui.PainelRotas`). |
| Coleta periódica em background sem travar a GUI | Pool de threads em `monitor.MonitorDispositivos` + comunicação com a GUI via `SwingUtilities.invokeLater` em `JanelaPrincipal.aoAtualizarDispositivo`. |
| Múltiplos testes simultâneos | Pool com 4 threads (`Executors.newScheduledThreadPool(4, ...)`); cada dispositivo tem sua própria tarefa agendada. |
| Status em tempo real | `JanelaPrincipal` implementa `DispositivoObserver`; ao receber notificação atualiza a linha da JTable e o painel de detalhes. |
| **Indicação visual por cores** (verde, amarelo, vermelho) | `model.StatusDispositivo` define `OK` (verde), `ATENCAO` (amarelo), `FALHA` (vermelho), `DESCONHECIDO` (cinza). O renderer `RendererStatus` em `JanelaPrincipal` desenha bolinha colorida + tom de fundo. |
| Apresentação de latência média e rotas | `gui.PainelDetalhes` mostra latência formatada, perda, hora da última coleta e o traceroute completo em fonte monoespaçada. |
| Diagnóstico breve sobre falhas | `model.DispositivoRede.diagnosticoEspecifico(MetricaRede)` — método polimórfico sobrescrito em cada subclasse. |
| Estrutura modular separando responsabilidades | Pacotes `model/`, `service/`, `monitor/`, `observer/`, `gui/` (detalhes na seção 2). |

---

## 2. Arquitetura e módulos

### 2.1 Visão geral

```
+---------+   notifica   +-----------------+   atualiza   +-----+
| Monitor |------------->| JanelaPrincipal |------------->| GUI |
| threads |  (Observer)  | (Observer impl) |              +-----+
+---------+              +-----------------+
     |
     | usa
     v
+--------------+      processos do SO (ping, tracert, ...)
| Ferramenta-  |----+ APIs Java (Socket, InetAddress, HttpURLConnection)
| Rede         |    |
+--------------+    v
                +-------+
                | model |  DispositivoRede (abstrato) + subclasses
                +-------+
```

A aplicação tem **três caminhos** de execução:

1. **EDT (Event Dispatch Thread)** do Swing — toda a interface gráfica.
2. **Pool de threads de monitoramento** — executa as ferramentas de
   diagnóstico em paralelo, sem bloquear a EDT.
3. **Threads do SO** geradas por `ProcessBuilder` — uma para cada
   chamada externa (`ping`, `tracert`, etc.); são curtas e finalizam
   sozinhas (ou são mortas pelo timeout).

A comunicação entre o monitor e a GUI segue o padrão **Observer**: o
monitor não conhece Swing; ele apenas chama `observer.aoAtualizarDispositivo(d)`.
A `JanelaPrincipal` recebe a notificação numa thread de monitoramento e
pula para a EDT via `SwingUtilities.invokeLater` antes de mexer em
qualquer componente Swing — regra fundamental do framework.

### 2.2 Estrutura de pastas

```
src/
├── App.java                       ← ponto de entrada
├── model/                         ← entidades de domínio (POJOs)
│   ├── DispositivoRede.java       ← classe abstrata
│   ├── Roteador.java              ← subclasses concretas
│   ├── SwitchRede.java
│   ├── Firewall.java
│   ├── Servidor.java
│   ├── MetricaRede.java           ← value object com o resultado da coleta
│   └── StatusDispositivo.java     ← enum (OK / ATENCAO / FALHA / DESCONHECIDO)
├── service/                       ← lógica de I/O e fábricas
│   ├── FerramentaRede.java        ← ping, traceroute, DNS, TCP, HTTP
│   ├── DispositivoFactory.java    ← Factory Method
│   ├── InterfaceRedeInfo.java     ← consulta NetworkInterface
│   ├── RotaInfo.java              ← consulta route print / netstat -rn
│   ├── ResultadoPing.java         ← value objects de retorno
│   ├── ResultadoDns.java
│   ├── ResultadoTcp.java
│   └── ResultadoHttp.java
├── monitor/                       ← núcleo de threads
│   └── MonitorDispositivos.java
├── observer/                      ← contrato do Observer
│   └── DispositivoObserver.java
└── gui/                           ← interface Swing
    ├── JanelaPrincipal.java       ← janela principal (JFrame)
    ├── DialogoDispositivo.java    ← modal de cadastro/edição
    ├── PainelDetalhes.java        ← lado direito da aba "Dispositivos"
    ├── PainelInterfaces.java      ← aba "Interfaces de Rede"
    ├── PainelRotas.java           ← aba "Tabela de Rotas"
    └── ModeloDispositivos.java    ← TableModel da tabela principal
```

### 2.3 Módulos em detalhe

#### `App.java` — Inicialização

- Aplica o look-and-feel do sistema operacional (UX nativa).
- Instancia o `MonitorDispositivos` (que já cria o pool de threads).
- Pré-cadastra três dispositivos de exemplo para demonstração.
- Cria a `JanelaPrincipal` **dentro de** `SwingUtilities.invokeLater`,
  garantindo que toda a construção da UI rode na EDT.

#### Pacote `model/` — entidades de domínio

São **POJOs** (Plain Old Java Objects) — não conhecem Swing, threads,
nem sockets. Servem apenas para representar conceitos do domínio.

- **`StatusDispositivo`** (enum): traduz o resultado da coleta em uma
  semântica visual (`OK` verde, `ATENCAO` amarelo, `FALHA` vermelho,
  `DESCONHECIDO` cinza). Cada valor carrega sua descrição amigável e a
  cor — usada tanto pelo renderer da JTable quanto pelo painel de detalhes.

- **`MetricaRede`** (value object imutável): agrupa o resultado de uma
  coleta inteira: ping (alcançável, latência, perda), traceroute (lista
  de saltos), DNS direto, DNS reverso, TCP, HTTP, status e diagnóstico
  textual. Os campos opcionais (DNS, TCP, HTTP) podem ser `null` quando
  o teste não foi executado. A imutabilidade é importante porque a
  métrica é compartilhada entre a thread do monitor (que escreve) e a
  EDT (que lê) — não precisa de sincronização extra.

- **`DispositivoRede`** (abstrata): núcleo do polimorfismo. Tem
  `id` (gerado via `AtomicInteger`), `nome`, `enderecoIp`, `portaTcp`
  (opcional), `verificarHttp` (boolean) e `ultimaMetrica` marcada como
  `volatile` (garante visibilidade entre threads para a referência).
  Define dois métodos abstratos: `tipoDispositivo()` (rótulo para a GUI)
  e `diagnosticoEspecifico(MetricaRede)` (texto interpretativo,
  diferente em cada tipo).

- **`Roteador`, `SwitchRede`, `Firewall`, `Servidor`**: implementações
  concretas. Cada uma tem regras próprias no `diagnosticoEspecifico`:
  - **Roteador** — alerta se a rota tem mais de 15 saltos (possível loop);
  - **SwitchRede** — espera latência baixíssima por estar em LAN (>20ms é suspeito);
  - **Firewall** — interpreta ICMP bloqueado como ambíguo (pode estar OK);
  - **Servidor** — qualquer perda ou latência > 80 ms já é alerta.

#### Pacote `service/` — lógica de I/O

Camada responsável por **toda** interação com o "mundo externo" (sistema
operacional, rede, processos). Esconde os detalhes de parsing e
multiplataforma.

- **`FerramentaRede`** (classe utilitária): coração das medições.
  - `ping(host, qtd)` — chama `ping -n` (Windows) ou `ping -c` (Linux),
    parseia a saída procurando `time=` e percentual de perda usando
    regex (suporta saídas em inglês e português).
  - `traceroute(host)` — usa `tracert -d -h 20` ou `traceroute -n -m 20`;
    filtra apenas linhas que parecem hop válido.
  - `mtrSimplificado(host)` — ping com 8 pacotes (latência + perda).
  - `resolverDnsDireto(host)` — `InetAddress.getAllByName` com cronômetro.
  - `resolverDnsReverso(host)` — truque com `InetAddress.getByAddress`
    para **forçar** o lookup PTR de verdade (se chamássemos
    `getCanonicalHostName` direto no resultado de `getByName`, a JVM
    cacheia o hostname e nunca consulta o DNS).
  - `testarPortaTcp(host, porta, timeoutMs)` — `Socket.connect` com
    timeout. Mede o tempo até o handshake; complementa o ping porque
    atravessa firewalls que bloqueiam ICMP mas liberam a porta.
  - `testarHttp(host, portaTcp)` — tenta HEAD; cai para GET em caso de
    405; escolhe `http://` ou `https://` com base na porta cadastrada;
    se a primeira tentativa falhar, tenta o esquema alternativo. Define
    timeouts de 4 s e User-Agent neutro (servidores recusam UA vazio).
  - Método interno `executarComando(...)` centraliza o `ProcessBuilder`,
    com `redirectErrorStream(true)` e timeout para nunca travar a thread.

- **`DispositivoFactory`** (Factory Method): expõe duas sobrecargas de
  `criar(...)` e um enum `TipoDispositivo`. Centraliza a validação
  (nome, IP, range de porta) e esconde quais classes concretas existem.
  Adicionar um novo tipo (ex.: `AccessPoint`) exige mudar apenas este
  arquivo e o enum — a GUI continua igual. Também tem `tipoDe(d)`, o
  caminho inverso, usado para pré-selecionar o combo na edição.

- **`InterfaceRedeInfo`** + **`RotaInfo`** (DTOs com lógica estática
  de coleta): cada uma sabe consultar o SO e devolver uma lista pronta
  para o painel correspondente.

- **`ResultadoPing`, `ResultadoDns`, `ResultadoTcp`, `ResultadoHttp`**
  (DTOs imutáveis): contratos de retorno da `FerramentaRede`. Cada um
  tem um método `resumo()` que devolve texto pronto para a GUI.

#### Pacote `monitor/` — threads

- **`MonitorDispositivos`**: mantém a lista de dispositivos
  (`CopyOnWriteArrayList` — leituras concorrentes baratas), o mapa
  `id → tarefa agendada` (`ConcurrentHashMap`) e a lista de observadores
  (`CopyOnWriteArrayList`).

  - Usa um **`ScheduledExecutorService`** com 4 threads **daemon**
    (`Executors.newScheduledThreadPool(4, threadFactoryDaemon)`).
    Threads daemon permitem o processo encerrar mesmo se o pool ainda
    estiver vivo.
  - Para cada dispositivo, agenda uma tarefa via
    **`scheduleWithFixedDelay`** — assim a próxima coleta só começa
    depois da anterior terminar, evitando sobreposição em dispositivos
    lentos.
  - `executarColeta(d)` roda na thread do pool:
    1. `mtrSimplificado` (ping com 8 pacotes);
    2. `traceroute` **só** se o ping respondeu (economia de tempo);
    3. DNS direto + reverso (rápidos, sempre);
    4. TCP se o dispositivo tem porta cadastrada;
    5. HTTP se o dispositivo tem o checkbox marcado;
    6. Calcula o status combinando todos os sinais;
    7. Pede o diagnóstico ao próprio dispositivo (polimorfismo);
    8. Cria a `MetricaRede` final e salva (`setUltimaMetrica`);
    9. Notifica todos os observadores.
  - `coletarAgora(d)` permite que a GUI dispare uma coleta imediata
    (botão "Coletar agora"), sem esperar o próximo ciclo.
  - `setIntervaloSegundos(n)` reagenda todas as tarefas com o novo período.
  - `encerrar()` chama `shutdownNow()` no pool — é executado pelo
    window listener da janela principal ao fechar.

#### Pacote `observer/` — Observer

- **`DispositivoObserver`** (interface funcional com um método):
  `aoAtualizarDispositivo(DispositivoRede d)`. É o contrato implementado
  pela `JanelaPrincipal` para receber as notificações do monitor.
  Manter essa interface separada permite ter outros observadores no
  futuro (ex.: um logger, um exportador CSV) sem mexer no monitor.

#### Pacote `gui/` — Swing

- **`JanelaPrincipal`** (`JFrame` + `DispositivoObserver`): junta tudo.
  - **`JToolBar`** com Adicionar / Editar / Remover / Coletar agora.
  - **`JTabbedPane`** com três abas: Dispositivos, Interfaces de Rede,
    Tabela de Rotas.
  - **`JSplitPane`** divide a aba Dispositivos entre a JTable (esquerda)
    e o `PainelDetalhes` (direita).
  - **`JTable`** alimentada por `ModeloDispositivos`. A coluna "Status"
    tem um renderer customizado (`RendererStatus`) que desenha uma
    bolinha colorida (classe interna `BolinhaIcon` com antialiasing) e
    pinta o fundo com um tom suave da cor do status.
  - **`Timer`** (Swing) atualiza o rodapé a cada segundo (estético).
  - **Implementação do Observer**: `aoAtualizarDispositivo` é chamado
    de uma thread do monitor; o método usa **`SwingUtilities.invokeLater`**
    para descobrir a linha do dispositivo na tabela, marcar como
    atualizada e (se for o selecionado) atualizar o painel de detalhes.
  - **`WindowAdapter`** no `windowClosing` chama `monitor.encerrar()`
    antes de finalizar o processo — encerramento limpo.

- **`DialogoDispositivo`** (`JDialog` modal): formulário com tipo,
  nome, IP, porta TCP (opcional) e checkbox HTTP. Em modo edição,
  desabilita o combo de tipo (mudar o tipo destruiria o ID e a métrica
  acumulada). Validação delegada à Factory; mensagens de erro via
  `JOptionPane`.

- **`PainelDetalhes`** (`JPanel`): mostra título, tipo, status colorido,
  latência, perda, hora da última coleta, resultados de DNS direto e
  reverso, porta TCP, HTTP, o diagnóstico textual e a saída completa do
  traceroute em uma `JTextArea` monoespaçada com scroll. Tem método
  `exibir(d)` que aceita `null` (limpa tudo).

- **`PainelInterfaces`** (`JPanel`): lista as placas locais via
  `InterfaceRedeInfo.listarLocais()`. As colunas "Ativa" e "Loopback"
  usam `Boolean.class`, e a "Ativa" tem renderer próprio que pinta a
  célula de verde claro ou vermelho claro.

- **`PainelRotas`** (`JPanel`): lista a tabela de rotas IPv4 do
  sistema. Usa fonte monoespaçada para alinhar IPs. Botão "Atualizar"
  recarrega executando o comando do SO novamente.

- **`ModeloDispositivos`** (`AbstractTableModel`): backend da tabela
  principal. Métodos `adicionar`, `remover`, `linhaDe(d)` (busca por
  ID) e `atualizarLinha(i)` (dispara `fireTableRowsUpdated`).

### 2.4 Fluxo completo de uma coleta

1. Usuário clica em "Adicionar", preenche o formulário e confirma.
2. `DialogoDispositivo` chama `DispositivoFactory.criar(...)` que
   devolve, por exemplo, um `Servidor`.
3. `JanelaPrincipal` adiciona o dispositivo ao `MonitorDispositivos`
   e ao `ModeloDispositivos` (linha nova aparece na JTable).
4. O monitor agenda uma `Runnable` no pool de threads
   (`scheduleWithFixedDelay`, intervalo de 15 s).
5. Na primeira execução, a tarefa (thread `monitor-rede-1`) chama
   `FerramentaRede.mtrSimplificado` → spawna o processo `ping` do SO →
   coleta latência e perda.
6. Como o ping teve sucesso, chama `traceroute` (spawna `tracert`).
7. Faz DNS direto, DNS reverso, TCP (se houver porta cadastrada) e
   HTTP (se marcado).
8. Combina tudo em `StatusDispositivo` (OK / ATENCAO / FALHA).
9. Chama `dispositivo.diagnosticoEspecifico(metrica)` — polimorfismo:
   por ser um `Servidor`, executa as regras desta classe.
10. Salva a métrica no dispositivo e chama
    `observador.aoAtualizarDispositivo(d)` para cada observador.
11. `JanelaPrincipal.aoAtualizarDispositivo` recebe a chamada na
    thread do monitor; faz `SwingUtilities.invokeLater(...)` para
    saltar para a EDT.
12. Na EDT, encontra a linha do dispositivo
    (`modelo.linhaDe(d)`) e chama `modelo.atualizarLinha(row)` →
    `fireTableRowsUpdated` redesenha a célula com o novo status
    colorido. Se o dispositivo está selecionado, o
    `PainelDetalhes` também é refrescado.
13. 15 segundos depois, o ciclo se repete automaticamente.

Durante todo esse processo a janela continua respondendo a cliques —
nenhum I/O bloqueia a EDT.

### 2.5 Por que essas escolhas

- **Classe abstrata em vez de interface**: porque os tipos compartilham
  estado real (id, nome, IP, métrica, porta, flag HTTP). Interfaces
  serviriam, mas duplicariam código nas subclasses.
- **`ScheduledExecutorService` em vez de `Thread` cru**: reaproveita
  threads, encerra limpo, tem timeout nativo e oferece
  `scheduleWithFixedDelay` que é exatamente o comportamento desejado.
- **Observer em vez de a GUI consultar em loop**: empurrar
  atualizações é mais eficiente e mantém a separação entre monitor
  (sem Swing) e GUI.
- **Factory para dispositivos**: o requisito ("diferentes tipos de
  dispositivos") combina perfeitamente com Factory; centraliza a
  validação e isola a GUI das classes concretas.
- **`CopyOnWriteArrayList`**: a lista de dispositivos é lida muito
  mais do que escrita (toda atualização de UI percorre, mas só
  cadastros/remoções modificam).
- **Imutabilidade do `MetricaRede`**: permite passar a métrica entre
  threads sem sincronização.

### 2.6 Como executar

```powershell
# Compilar
javac -d bin -encoding UTF-8 (Get-ChildItem -Path src -Recurse -Filter *.java).FullName

# Rodar
java -cp bin App
```

(ou usar o botão **Run** do VS Code com o arquivo `App.java` aberto.)
