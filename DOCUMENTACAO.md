# Gerenciador de Dispositivos de Rede — Documentação

Trabalho Final de POO — aplicação Java Swing para cadastro, monitoramento
e diagnóstico de dispositivos de rede (roteadores, switches, firewalls e
servidores).

> Status atual: versão **básica/enxuta**, alinhada ao escopo mínimo do
> enunciado. Ferramentas extras (DNS, TCP, HTTP, aba de rotas) foram
> removidas em uma etapa de simplificação.

Este documento tem duas partes:

1. **Mapa enunciado ↔ código** — onde cada requisito está implementado.
2. **Arquitetura e módulos** — explicação detalhada do funcionamento.

Documentos relacionados:

- [APRESENTACAO.md](APRESENTACAO.md) — roteiro de apresentação ao professor.
- [PADROES_DE_PROJETO.md](PADROES_DE_PROJETO.md) — teoria e aplicação dos padrões.
- [DIAGRAMA_UML.md](DIAGRAMA_UML.md) — diagramas UML (classes, sequência, componentes).

---

## 1. Mapa: enunciado ↔ código

### 1.1 Itens avaliativos (Entrega 2)

| Item | Onde está no código |
|---|---|
| **a) Polimorfismo** (interface ou classe abstrata) | Classe abstrata [DispositivoRede](src/model/DispositivoRede.java) e as subclasses concretas [Roteador](src/model/Roteador.java), [SwitchRede](src/model/SwitchRede.java), [Firewall](src/model/Firewall.java) e [Servidor](src/model/Servidor.java). Cada subclasse sobrescreve `tipoDispositivo()` e `diagnosticoEspecifico(MetricaRede)`. O monitor trabalha só com a referência abstrata; o método correto é resolvido em tempo de execução. |
| **b) Threads** | [MonitorDispositivos](src/monitor/MonitorDispositivos.java) usa **uma thread por dispositivo** (classe interna `ThreadColetaDispositivo extends Thread`) com loop em `run()`, `Thread.sleep(15s)` entre coletas e `interrupt()` para encerramento — no estilo visto em aula. |
| **c) Interface gráfica** | Pacote [gui/](src/gui) em Java Swing: [JanelaPrincipal](src/gui/JanelaPrincipal.java) (JFrame com JToolBar, JTable, JTabbedPane, JSplitPane), [DialogoDispositivo](src/gui/DialogoDispositivo.java) (cadastro/edição), [PainelDetalhes](src/gui/PainelDetalhes.java) e [PainelInterfaces](src/gui/PainelInterfaces.java). |
| **d) Padrão de projeto** | Dois padrões: **Factory Method** em [DispositivoFactory](src/service/DispositivoFactory.java) e **Observer** em [DispositivoObserver](src/monitor/DispositivoObserver.java) + [MonitorDispositivos](src/monitor/MonitorDispositivos.java) ↔ [JanelaPrincipal](src/gui/JanelaPrincipal.java). Detalhes em [PADROES_DE_PROJETO.md](PADROES_DE_PROJETO.md). |

### 1.2 Funcionalidades pedidas no enunciado

| Requisito | Onde está |
|---|---|
| Cadastrar dispositivos de rede | [DialogoDispositivo](src/gui/DialogoDispositivo.java) (tipo + nome + IP/host) + `DispositivoFactory.criar(...)`. |
| Diferentes tipos (roteador, switch, firewall, servidor) | Enum `TipoDispositivo` em [DispositivoFactory](src/service/DispositivoFactory.java) e as 4 subclasses concretas em [model/](src/model). |
| Editar dispositivos | Diálogo reabre com o dispositivo selecionado e altera-o in-place. |
| Remover dispositivos | `JanelaPrincipal.removerSelecionado()` chama `monitor.removerDispositivo(d)`, que cancela a thread do dispositivo. |
| **Ping** (conectividade + latência) | `FerramentaRede.ping(host, qtd)` em [FerramentaRede](src/service/FerramentaRede.java) — executa o `ping` do SO e faz parsing por regex. |
| **Traceroute** (caminho até o destino) | `FerramentaRede.traceroute(host)` — `tracert` no Windows e `traceroute` no Linux. |
| **MTR** (latência + perda) | `MonitorDispositivos` chama `ping(host, 8)`: 8 pacotes entregam latência média + percentual de perda — exatamente o resumo do MTR. |
| Consulta às interfaces de rede locais | `InterfaceRedeInfo.listarLocais()` em [InterfaceRedeInfo](src/service/InterfaceRedeInfo.java) (usa `java.net.NetworkInterface`); aba "Interfaces de Rede" em [PainelInterfaces](src/gui/PainelInterfaces.java). |
| Coleta periódica sem travar a GUI | Threads dedicadas no monitor + `SwingUtilities.invokeLater` em `JanelaPrincipal.aoAtualizarDispositivo`. |
| Múltiplos testes simultâneos | Uma thread por dispositivo, todas ativas em paralelo. |
| Status em tempo real | `JanelaPrincipal` implementa `DispositivoObserver`; ao ser notificada, atualiza tabela e painel de detalhes. |
| Indicação visual por cores (verde/amarelo/vermelho) | Enum [StatusDispositivo](src/model/StatusDispositivo.java) (OK, ATENCAO, FALHA, DESCONHECIDO). `RendererStatus` em `JanelaPrincipal` pinta o fundo da célula da coluna Status. |
| Latência média e rotas | [PainelDetalhes](src/gui/PainelDetalhes.java) mostra latência, perda, hora, diagnóstico e o traceroute completo em fonte monoespaçada. |
| Diagnóstico breve sobre falhas | `DispositivoRede.diagnosticoEspecifico(MetricaRede)` — método polimórfico sobrescrito em cada subclasse. |
| Estrutura modular | Pacotes `model/`, `service/`, `monitor/`, `gui/`. |

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
+--------------+      processos do SO (ping, tracert)
| Ferramenta-  |----+ APIs Java (NetworkInterface, ProcessBuilder)
| Rede         |    |
+--------------+    v
                +-------+
                | model |  DispositivoRede (abstrato) + subclasses
                +-------+
```

Três caminhos de execução coexistem:

1. **EDT (Event Dispatch Thread)** do Swing — toda a interface gráfica.
2. **Threads do monitor** — uma por dispositivo cadastrado, em loop de
   coleta a cada 15 segundos.
3. **Threads do SO** geradas por `ProcessBuilder` — uma por chamada
   externa (`ping`, `tracert`); curtas, terminam sozinhas (ou são
   mortas pelo timeout).

A comunicação monitor → GUI segue o **Observer**: o monitor não conhece
Swing; apenas chama `observer.aoAtualizarDispositivo(d)`. A
`JanelaPrincipal` recebe a notificação numa thread de monitoramento e
salta para a EDT via `SwingUtilities.invokeLater` — regra fundamental
do Swing.

### 2.2 Estrutura de pastas

```
src/
├── App.java                       ← ponto de entrada
├── model/                         ← entidades de domínio
│   ├── DispositivoRede.java       ← classe abstrata
│   ├── Roteador.java              ← subclasses concretas
│   ├── SwitchRede.java
│   ├── Firewall.java
│   ├── Servidor.java
│   ├── MetricaRede.java           ← value object com o resultado da coleta
│   └── StatusDispositivo.java     ← enum (OK / ATENCAO / FALHA / DESCONHECIDO)
├── service/                       ← lógica de I/O e fábrica
│   ├── FerramentaRede.java        ← ping e traceroute
│   ├── DispositivoFactory.java    ← Factory Method
│   ├── InterfaceRedeInfo.java     ← consulta NetworkInterface
│   └── ResultadoPing.java         ← value object retornado pelo ping
├── monitor/                       ← núcleo de threads
│   ├── DispositivoObserver.java   ← contrato do Observer
│   └── MonitorDispositivos.java
└── gui/                           ← interface Swing
    ├── JanelaPrincipal.java       ← janela principal (JFrame)
    ├── DialogoDispositivo.java    ← modal de cadastro/edição
    ├── PainelDetalhes.java        ← lado direito da aba "Dispositivos"
    ├── PainelInterfaces.java      ← aba "Interfaces de Rede"
    └── ModeloDispositivos.java    ← TableModel da tabela principal
```

### 2.3 Módulos em detalhe

#### `App.java` — inicialização

- Aplica o look-and-feel do sistema.
- Instancia o `MonitorDispositivos`.
- Pré-cadastra dois dispositivos (Cloudflare `1.1.1.1` e Google DNS
  `8.8.8.8`) só para a demonstração.
- Cria a `JanelaPrincipal` **dentro de** `SwingUtilities.invokeLater`,
  garantindo que a UI seja construída na EDT.

#### Pacote `model/`

POJOs de domínio (não conhecem Swing, sockets, threads).

- **`StatusDispositivo`** (enum): OK (verde), ATENCAO (amarelo),
  FALHA (vermelho), DESCONHECIDO (cinza). Cada valor tem descrição
  amigável e cor.
- **`MetricaRede`** (imutável): alcançável, latência média, perda,
  lista de saltos do traceroute, status e diagnóstico. Imutabilidade
  torna seguro compartilhar entre threads.
- **`DispositivoRede`** (abstrata): `id` (via `AtomicInteger`), `nome`,
  `enderecoIp`, `ultimaMetrica` marcada como `volatile`. Dois métodos
  abstratos: `tipoDispositivo()` e `diagnosticoEspecifico(MetricaRede)`.
- **`Roteador`, `SwitchRede`, `Firewall`, `Servidor`**: cada subclasse
  aplica regras próprias no diagnóstico (rota longa, latência de LAN,
  ICMP bloqueado, latência alta em servidor, etc.).

#### Pacote `service/`

- **`FerramentaRede`** (utilitária, sem estado): invoca processos do
  SO via `ProcessBuilder`, faz parsing por regex e detecta Windows vs.
  Linux. Métodos: `ping(host, qtd)` e `traceroute(host)`.
- **`DispositivoFactory`** (Factory Method): expõe `criar(tipo, nome, ip)`
  e o enum `TipoDispositivo`. Concentra validação e esconde as classes
  concretas do resto do sistema.
- **`InterfaceRedeInfo`**: DTO + `listarLocais()` estático que consulta
  `java.net.NetworkInterface`.
- **`ResultadoPing`**: DTO imutável (alcançável, latência média, perda).

#### Pacote `monitor/`

- **`DispositivoObserver`** (interface): contrato do Observer.
- **`MonitorDispositivos`**: gerencia o ciclo de vida das threads.
  - Ao adicionar um dispositivo, cria uma `ThreadColetaDispositivo`
    (classe interna que `extends Thread`) e chama `start()`.
  - Cada thread executa `run()` em loop:
    1. `executarColeta(d)` (ping → traceroute → status → diagnóstico → salvar);
    2. `Thread.sleep(INTERVALO_MS)` (15 000 ms);
    3. Repete até `interrupt()`.
  - Notifica todos os observadores ao fim de cada coleta.
  - Mantém `ConcurrentHashMap<Integer, ThreadColetaDispositivo>` para
    localizar a thread na remoção.
  - `encerrar()` interrompe todas as threads (chamado no fechamento
    da janela).

#### Pacote `gui/`

- **`JanelaPrincipal`** (`JFrame` + `DispositivoObserver`):
  - `JToolBar` com Adicionar / Editar / Remover.
  - `JTabbedPane` com Dispositivos e Interfaces de Rede.
  - `JSplitPane` na aba Dispositivos: JTable (esquerda) + `PainelDetalhes` (direita).
  - `RendererStatus` custom pinta o fundo da coluna Status com a cor
    do enum.
  - `aoAtualizarDispositivo(d)` é chamado da thread do monitor →
    pula para a EDT via `SwingUtilities.invokeLater`.
  - `WindowAdapter` chama `monitor.encerrar()` no fechamento.
- **`DialogoDispositivo`** (`JDialog` modal): tipo (combo), nome, IP.
  Em edição, o combo de tipo é desabilitado.
- **`PainelDetalhes`**: título, tipo, status colorido, latência, perda,
  hora da última coleta, diagnóstico e traceroute completo em fonte
  monoespaçada.
- **`PainelInterfaces`**: lista placas locais com botão "Atualizar".
- **`ModeloDispositivos`** (`AbstractTableModel`): backend da JTable
  com `adicionar`, `remover`, `linhaDe(d)` (busca por id) e
  `atualizarLinha(i)`.

### 2.4 Fluxo completo de uma coleta

1. Usuário clica em **Adicionar**, preenche o formulário e confirma.
2. `DialogoDispositivo` chama `DispositivoFactory.criar(...)` → obtém,
   por exemplo, um `Servidor`.
3. `JanelaPrincipal` adiciona o dispositivo ao `MonitorDispositivos`
   e ao `ModeloDispositivos`.
4. `adicionarDispositivo(d)` cria uma `ThreadColetaDispositivo` e chama
   `start()`.
5. Primeira iteração do `run()`:
   - `FerramentaRede.ping(ip, 8)` → latência e perda;
   - se alcançável, `traceroute` → lista de saltos;
   - `interpretarStatus(ping)` → OK/ATENCAO/FALHA;
   - polimorfismo: `d.diagnosticoEspecifico(previa)`;
   - `MetricaRede` final salva no dispositivo (`setUltimaMetrica`).
6. A thread invoca `obs.aoAtualizarDispositivo(d)`.
7. `JanelaPrincipal.aoAtualizarDispositivo` recebe na thread do monitor
   e faz `SwingUtilities.invokeLater(...)`.
8. Na EDT, `modelo.linhaDe(d)` + `atualizarLinha(row)` redesenha a
   linha. Se o dispositivo está selecionado, `PainelDetalhes.exibir(d)`
   também é chamado.
9. A thread dorme 15 s e repete o ciclo.
10. Ao fechar a janela, `windowClosing` chama `monitor.encerrar()`,
    que percorre as threads e as interrompe.

### 2.5 Como executar

```powershell
# Compilar (todos os .java de src/)
if (Test-Path bin) { Remove-Item bin -Recurse -Force }
New-Item -ItemType Directory -Force -Path bin | Out-Null
$files = Get-ChildItem -Path src -Recurse -Filter *.java | ForEach-Object { $_.FullName }
javac -d bin -encoding UTF-8 $files

# Rodar
java -cp bin App
```

Ou usar o botão **Run** do VS Code com o arquivo [App.java](src/App.java) aberto.
