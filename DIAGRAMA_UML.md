# Diagramas UML — Gerenciador de Dispositivos de Rede

Diagramas em [Mermaid](https://mermaid.js.org/) (renderizam
automaticamente no GitHub, GitLab e no preview do VS Code).

Índice:

1. Diagrama de classes completo
2. Diagrama de classes por pacote (visões parciais)
3. Diagrama de sequência — cadastro + coleta periódica
4. Diagrama de sequência — encerramento
5. Diagrama de componentes / camadas

---

## 1. Diagrama de classes completo

```mermaid
classDiagram
    direction LR

    %% ========== MODEL ==========
    class DispositivoRede {
        <<abstract>>
        -id : int
        -nome : String
        -enderecoIp : String
        -ultimaMetrica : MetricaRede
        +getId() int
        +getNome() String
        +setNome(String)
        +getEnderecoIp() String
        +setEnderecoIp(String)
        +getUltimaMetrica() MetricaRede
        +setUltimaMetrica(MetricaRede)
        +getStatusAtual() StatusDispositivo
        +tipoDispositivo()* String
        +diagnosticoEspecifico(MetricaRede)* String
    }

    class Roteador {
        +tipoDispositivo() String
        +diagnosticoEspecifico(MetricaRede) String
    }

    class SwitchRede {
        +tipoDispositivo() String
        +diagnosticoEspecifico(MetricaRede) String
    }

    class Firewall {
        +tipoDispositivo() String
        +diagnosticoEspecifico(MetricaRede) String
    }

    class Servidor {
        +tipoDispositivo() String
        +diagnosticoEspecifico(MetricaRede) String
    }

    class MetricaRede {
        <<immutable>>
        -alcancavel : boolean
        -latenciaMediaMs : double
        -perdaPacotesPercentual : double
        -rotaTraceroute : List~String~
        -status : StatusDispositivo
        -diagnostico : String
        -coletadaEm : LocalDateTime
        +isAlcancavel() boolean
        +getLatenciaMediaMs() double
        +getPerdaPacotesPercentual() double
        +getRotaTraceroute() List~String~
        +getStatus() StatusDispositivo
        +getDiagnostico() String
        +getLatenciaFormatada() String
        +getPerdaFormatada() String
        +getColetadaEmFormatada() String
    }

    class StatusDispositivo {
        <<enum>>
        OK
        ATENCAO
        FALHA
        DESCONHECIDO
        +getDescricao() String
        +getCor() Color
    }

    DispositivoRede <|-- Roteador
    DispositivoRede <|-- SwitchRede
    DispositivoRede <|-- Firewall
    DispositivoRede <|-- Servidor
    DispositivoRede o-- MetricaRede : ultimaMetrica
    MetricaRede --> StatusDispositivo : status

    %% ========== SERVICE ==========
    class DispositivoFactory {
        <<utility>>
        +criar(tipo, nome, ip)$ DispositivoRede
        +tipoDe(DispositivoRede)$ TipoDispositivo
    }

    class TipoDispositivo {
        <<enum>>
        ROTEADOR
        SWITCH
        FIREWALL
        SERVIDOR
    }

    class FerramentaRede {
        <<utility>>
        +ping(host, qtd)$ ResultadoPing
        +traceroute(host)$ List~String~
    }

    class ResultadoPing {
        <<immutable>>
        -alcancavel : boolean
        -latenciaMediaMs : double
        -perdaPercentual : double
        +isAlcancavel() boolean
        +getLatenciaMediaMs() double
        +getPerdaPercentual() double
    }

    class InterfaceRedeInfo {
        -nome : String
        -descricao : String
        -ativa : boolean
        -loopback : boolean
        -enderecos : List~String~
        +listarLocais()$ List~InterfaceRedeInfo~
    }

    DispositivoFactory ..> DispositivoRede : cria
    DispositivoFactory ..> Roteador : cria
    DispositivoFactory ..> SwitchRede : cria
    DispositivoFactory ..> Firewall : cria
    DispositivoFactory ..> Servidor : cria
    DispositivoFactory --> TipoDispositivo
    FerramentaRede ..> ResultadoPing : cria

    %% ========== MONITOR ==========
    class DispositivoObserver {
        <<interface>>
        +aoAtualizarDispositivo(DispositivoRede)
    }

    class MonitorDispositivos {
        -dispositivos : List~DispositivoRede~
        -observadores : List~DispositivoObserver~
        -threadsPorDispositivo : Map
        -INTERVALO_MS = 15000
        +adicionarDispositivo(DispositivoRede)
        +removerDispositivo(DispositivoRede)
        +adicionarObservador(DispositivoObserver)
        +encerrar()
        -executarColeta(DispositivoRede)
        -interpretarStatus(ResultadoPing) StatusDispositivo
    }

    class ThreadColetaDispositivo {
        <<inner class, extends Thread>>
        -dispositivo : DispositivoRede
        -ativo : boolean
        +run()
        +encerrar()
    }

    MonitorDispositivos "1" *-- "*" ThreadColetaDispositivo : cria
    MonitorDispositivos --> DispositivoObserver : notifica
    MonitorDispositivos --> DispositivoRede : monitora
    MonitorDispositivos ..> FerramentaRede : usa
    ThreadColetaDispositivo --> DispositivoRede
    MonitorDispositivos ..> MetricaRede : cria

    %% ========== GUI ==========
    class JanelaPrincipal {
        <<JFrame, DispositivoObserver>>
        -monitor : MonitorDispositivos
        -modelo : ModeloDispositivos
        -tabela : JTable
        -painelDetalhes : PainelDetalhes
        +aoAtualizarDispositivo(DispositivoRede)
        -abrirDialogoNovo()
        -abrirDialogoEditar()
        -removerSelecionado()
    }

    class DialogoDispositivo {
        <<JDialog>>
        -campoNome : JTextField
        -campoIp : JTextField
        -comboTipo : JComboBox
        -emEdicao : DispositivoRede
        +foiConfirmado() boolean
        +getResultado() DispositivoRede
    }

    class PainelDetalhes {
        <<JPanel>>
        +exibir(DispositivoRede)
    }

    class PainelInterfaces {
        <<JPanel>>
        +atualizar()
    }

    class ModeloDispositivos {
        <<AbstractTableModel>>
        +adicionar(DispositivoRede)
        +remover(int)
        +getDispositivo(int) DispositivoRede
        +linhaDe(DispositivoRede) int
        +atualizarLinha(int)
    }

    JanelaPrincipal ..|> DispositivoObserver
    JanelaPrincipal --> MonitorDispositivos : usa
    JanelaPrincipal --> ModeloDispositivos
    JanelaPrincipal --> PainelDetalhes
    JanelaPrincipal --> PainelInterfaces
    JanelaPrincipal ..> DialogoDispositivo : cria
    DialogoDispositivo ..> DispositivoFactory : usa
    ModeloDispositivos --> DispositivoRede
    PainelDetalhes --> DispositivoRede
    PainelDetalhes --> MetricaRede
    PainelInterfaces ..> InterfaceRedeInfo

    %% ========== APP ==========
    class App {
        +main(String[])$
    }

    App ..> MonitorDispositivos : cria
    App ..> JanelaPrincipal : cria
    App ..> DispositivoFactory : usa
```

---

## 2. Diagramas por pacote (visões parciais)

### 2.1 Pacote `model` — polimorfismo em foco

```mermaid
classDiagram
    direction TB

    class DispositivoRede {
        <<abstract>>
        #id : int
        #nome : String
        #enderecoIp : String
        #ultimaMetrica : MetricaRede
        +tipoDispositivo()* String
        +diagnosticoEspecifico(MetricaRede)* String
    }

    class Roteador
    class SwitchRede
    class Firewall
    class Servidor

    DispositivoRede <|-- Roteador
    DispositivoRede <|-- SwitchRede
    DispositivoRede <|-- Firewall
    DispositivoRede <|-- Servidor

    class MetricaRede {
        <<immutable>>
    }
    class StatusDispositivo {
        <<enum>>
        OK
        ATENCAO
        FALHA
        DESCONHECIDO
    }

    DispositivoRede o-- MetricaRede
    MetricaRede --> StatusDispositivo
```

### 2.2 Padrão Factory Method — visão focada

```mermaid
classDiagram
    direction LR

    class DialogoDispositivo {
        <<Cliente>>
    }

    class DispositivoFactory {
        <<Factory>>
        +criar(tipo, nome, ip)$ DispositivoRede
    }

    class DispositivoRede {
        <<Produto abstrato>>
    }

    class Roteador
    class SwitchRede
    class Firewall
    class Servidor

    DialogoDispositivo ..> DispositivoFactory : usa
    DispositivoFactory ..> Roteador : cria
    DispositivoFactory ..> SwitchRede : cria
    DispositivoFactory ..> Firewall : cria
    DispositivoFactory ..> Servidor : cria
    DispositivoRede <|-- Roteador
    DispositivoRede <|-- SwitchRede
    DispositivoRede <|-- Firewall
    DispositivoRede <|-- Servidor
```

### 2.3 Padrão Observer — visão focada

```mermaid
classDiagram
    direction LR

    class DispositivoObserver {
        <<interface>>
        +aoAtualizarDispositivo(DispositivoRede)
    }

    class MonitorDispositivos {
        <<Subject>>
        -observadores : List~DispositivoObserver~
        +adicionarObservador(DispositivoObserver)
        -notificar (interno)
    }

    class JanelaPrincipal {
        <<Observer concreto>>
        +aoAtualizarDispositivo(DispositivoRede)
    }

    MonitorDispositivos o-- DispositivoObserver : observadores
    JanelaPrincipal ..|> DispositivoObserver
    JanelaPrincipal --> MonitorDispositivos : registra-se
```

### 2.4 Pacote `monitor` — threads

```mermaid
classDiagram
    direction TB

    class MonitorDispositivos {
        -threadsPorDispositivo : Map~Integer, ThreadColetaDispositivo~
        +adicionarDispositivo(DispositivoRede)
        +removerDispositivo(DispositivoRede)
        +encerrar()
        -executarColeta(DispositivoRede)
    }

    class ThreadColetaDispositivo {
        <<inner, extends Thread>>
        -dispositivo : DispositivoRede
        -ativo : boolean
        +run()
        +encerrar()
    }

    class DispositivoRede
    class FerramentaRede {
        <<utility>>
        +ping(host, qtd)$
        +traceroute(host)$
    }

    MonitorDispositivos "1" *-- "*" ThreadColetaDispositivo
    ThreadColetaDispositivo --> DispositivoRede : monitora
    MonitorDispositivos ..> FerramentaRede : usa
    ThreadColetaDispositivo ..> MonitorDispositivos : chama executarColeta
```

---

## 3. Diagrama de sequência — cadastro + coleta periódica

```mermaid
sequenceDiagram
    autonumber
    actor Usuario
    participant Janela as JanelaPrincipal
    participant Dialogo as DialogoDispositivo
    participant Factory as DispositivoFactory
    participant Monitor as MonitorDispositivos
    participant Modelo as ModeloDispositivos
    participant Thread as ThreadColeta
    participant Ferr as FerramentaRede
    participant SO as Processo ping/tracert
    participant Disp as DispositivoRede

    Usuario->>Janela: clica "Adicionar"
    Janela->>Dialogo: abre modal
    Usuario->>Dialogo: preenche tipo/nome/ip e confirma
    Dialogo->>Factory: criar(tipo, nome, ip)
    Factory-->>Dialogo: DispositivoRede (ex.: Servidor)
    Dialogo-->>Janela: getResultado()
    Janela->>Monitor: adicionarDispositivo(d)
    Monitor->>Thread: new ThreadColetaDispositivo(d).start()
    Janela->>Modelo: adicionar(d)

    loop A cada 15 s
        Thread->>Monitor: executarColeta(d)
        Monitor->>Ferr: ping(ip, 8)
        Ferr->>SO: ProcessBuilder ping
        SO-->>Ferr: saída de texto
        Ferr-->>Monitor: ResultadoPing
        Monitor->>Ferr: traceroute(ip)  (se alcançável)
        Ferr->>SO: ProcessBuilder tracert
        SO-->>Ferr: saída de texto
        Ferr-->>Monitor: List~String~ hops
        Monitor->>Disp: diagnosticoEspecifico(previa)  [polimorfismo]
        Disp-->>Monitor: texto
        Monitor->>Disp: setUltimaMetrica(metrica)
        Monitor->>Janela: aoAtualizarDispositivo(d)  [Observer]
        Janela-->>Janela: SwingUtilities.invokeLater
        Janela->>Modelo: atualizarLinha(row)
        Note over Janela,Modelo: EDT redesenha a célula com a nova cor
        Thread->>Thread: Thread.sleep(15000)
    end
```

---

## 4. Diagrama de sequência — encerramento

```mermaid
sequenceDiagram
    autonumber
    actor Usuario
    participant Janela as JanelaPrincipal
    participant Monitor as MonitorDispositivos
    participant T1 as ThreadColeta(d1)
    participant T2 as ThreadColeta(d2)

    Usuario->>Janela: fecha a janela (X)
    Janela->>Monitor: encerrar()
    Monitor->>T1: encerrar() → interrupt()
    Monitor->>T2: encerrar() → interrupt()
    T1-->>Monitor: sai do while (ativo=false)
    T2-->>Monitor: sai do while (ativo=false)
    Janela->>Janela: dispose()
    Janela->>Janela: System.exit(0)
```

---

## 5. Diagrama de componentes / camadas

```mermaid
flowchart TB
    subgraph GUI["gui/ — camada de apresentação (Swing)"]
        JP[JanelaPrincipal<br/>JFrame + Observer]
        DD[DialogoDispositivo<br/>JDialog]
        PD[PainelDetalhes<br/>JPanel]
        PI[PainelInterfaces<br/>JPanel]
        MD[ModeloDispositivos<br/>AbstractTableModel]
    end

    subgraph MON["monitor/ — camada de threads"]
        MDS[MonitorDispositivos]
        OBS[DispositivoObserver<br/>interface]
        TC[ThreadColetaDispositivo<br/>extends Thread]
    end

    subgraph SRV["service/ — I/O e criação"]
        FR[FerramentaRede<br/>ping + traceroute]
        DF[DispositivoFactory<br/>Factory Method]
        IR[InterfaceRedeInfo]
        RP[ResultadoPing]
    end

    subgraph MOD["model/ — domínio"]
        DR[DispositivoRede<br/>abstract]
        SUB[Roteador · SwitchRede<br/>Firewall · Servidor]
        MR[MetricaRede]
        SD[StatusDispositivo<br/>enum]
    end

    subgraph SO["Sistema Operacional"]
        PING[ping]
        TRACERT[tracert / traceroute]
        NIC[NetworkInterface API]
    end

    APP((App.java))

    APP --> MDS
    APP --> JP
    APP --> DF

    JP -- registra --> MDS
    JP -- implementa --> OBS
    JP --> DD
    JP --> PD
    JP --> PI
    JP --> MD

    DD --> DF
    DF --> DR
    DR <|-- SUB
    DR --> MR
    MR --> SD

    MDS --> OBS
    MDS --> TC
    MDS --> FR
    TC --> DR
    MDS -.->|"chama diagnosticoEspecifico"| DR

    FR --> PING
    FR --> TRACERT
    IR --> NIC
    PI --> IR
    PD --> DR
    PD --> MR
    MD --> DR
```

---

## Observação sobre o arquivo `UMLProject.png`

Existe também um `UMLProject.png` na raiz do projeto (imagem gerada por
ferramenta externa). Este arquivo `.md` complementa esse PNG com
diagramas em texto (versionáveis no Git, editáveis, sempre em dia com
o código).
