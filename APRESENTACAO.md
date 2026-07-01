# Roteiro de Apresentação — Gerenciador de Dispositivos de Rede

Documento de apoio para a apresentação do trabalho ao professor.
Duração estimada: **8 a 12 minutos**. Todo o texto é em linguagem
falada, para você ler/adaptar durante a defesa.

Ordem sugerida:

1. Contexto e objetivo (1 min)
2. Demonstração ao vivo (2 a 3 min)
3. Arquitetura em alto nível (2 min)
4. Itens avaliativos — código na mão (5 a 6 min)
   - Polimorfismo
   - Threads
   - Interface gráfica
   - Padrão de projeto
5. Encerramento (1 min)

---

## 1. Contexto e objetivo

> "O trabalho é um **Gerenciador de Dispositivos de Rede** feito em
> Java. A ideia é permitir que o usuário cadastre roteadores, switches,
> firewalls e servidores, e o próprio computador do usuário fique
> executando periodicamente **ping** e **traceroute** contra esses
> dispositivos, mostrando o status em uma interface gráfica com
> semáforo verde/amarelo/vermelho."

> "Também é possível listar as interfaces de rede locais do computador."

> "O projeto foi organizado em pacotes por responsabilidade: `model`
> (entidades), `service` (integração com o SO), `monitor` (threads) e
> `gui` (Swing)."

---

## 2. Demonstração ao vivo

Passos práticos:

1. Rodar `App.java`. A janela abre com dois dispositivos já cadastrados
   (Cloudflare e Google DNS).
2. Aguardar a primeira coleta (~poucos segundos): a linha da tabela
   passa de cinza (DESCONHECIDO) para verde (OK).
3. Selecionar uma das linhas → mostrar o **painel de detalhes** com
   latência, perda, diagnóstico e a saída completa do traceroute.
4. Clicar em **Adicionar** → cadastrar um dispositivo, por exemplo
   um "Roteador local" com IP `192.168.1.1` (ou um IP inexistente
   para mostrar o status VERMELHO).
5. Editar um dispositivo → mostrar que só nome/IP são editáveis; o
   tipo fica travado para não quebrar o histórico.
6. Trocar para a aba **Interfaces de Rede** → mostrar as placas locais.
7. Fechar a janela → apontar que as threads são encerradas antes
   do processo terminar (mostra o log limpo).

Fala sugerida durante a demo:

> "Cada dispositivo tem sua própria thread rodando em background. Ela
> faz o ping, o traceroute e devolve as métricas. A thread do Swing
> nunca fica travada porque a coleta é sempre feita fora dela — e a
> atualização da tela acontece via `SwingUtilities.invokeLater`."

---

## 3. Arquitetura em alto nível

> "A arquitetura tem três caminhos de execução coexistindo:"
>
> - "A **EDT** do Swing, que cuida da interface."
> - "Uma **thread por dispositivo**, que faz a coleta periódica."
> - "Threads de curta duração criadas pelo `ProcessBuilder` para
>   invocar `ping` e `tracert` no sistema operacional."

Desenhe (ou mostre em slide) esta figura:

```
+---------+   notifica   +-----------------+   atualiza   +-----+
| Monitor |------------->| JanelaPrincipal |------------->| GUI |
| threads |  (Observer)  | (Observer impl) |              +-----+
+---------+
     |
     v
+--------------+  processos do SO
| Ferramenta-  |  (ping, tracert)
| Rede         |
+--------------+
```

Fala:

> "O monitor não conhece Swing. Ele apenas notifica um observador
> quando a coleta termina. Isso é o padrão **Observer**. E quem cria
> as subclasses concretas de dispositivo é a `DispositivoFactory` — o
> padrão **Factory Method**. Assim a GUI só conhece a classe abstrata
> `DispositivoRede`."

---

## 4. Itens avaliativos — código na mão

### 4.1 Polimorfismo — classe abstrata com 4 subclasses

Arquivos: [DispositivoRede](src/model/DispositivoRede.java),
[Roteador](src/model/Roteador.java), [SwitchRede](src/model/SwitchRede.java),
[Firewall](src/model/Firewall.java), [Servidor](src/model/Servidor.java).

Abrir `DispositivoRede.java` e mostrar:

```java
public abstract class DispositivoRede {
    // ...
    public abstract String tipoDispositivo();
    public abstract String diagnosticoEspecifico(MetricaRede metrica);
}
```

Fala:

> "Aqui está a classe abstrata que representa qualquer dispositivo de
> rede. Ela tem dois métodos abstratos que cada subclasse é obrigada
> a implementar."

Depois abrir, por exemplo, `Roteador.java`:

```java
@Override
public String diagnosticoEspecifico(MetricaRede m) {
    if (!m.isAlcancavel())
        return "Roteador não respondeu — verifique alimentação e cabos.";
    int saltos = m.getRotaTraceroute().size();
    if (saltos > 15)
        return "Roteador alcançável, mas rota com " + saltos + " saltos.";
    if (m.getLatenciaMediaMs() > 100)
        return "Roteador respondendo com latência alta.";
    return "Roteador funcionando normalmente.";
}
```

E `Servidor.java`, mostrando regra diferente:

```java
if (m.getLatenciaMediaMs() > 80)
    return "Servidor com latência elevada...";
```

Fala:

> "Cada tipo aplica regras próprias. O monitor não precisa saber a
> classe concreta — ele chama `d.diagnosticoEspecifico(metrica)` e o
> Java resolve em tempo de execução qual implementação executar. Isso
> é polimorfismo dinâmico."

### 4.2 Threads — uma por dispositivo, no estilo da aula

Arquivo: [MonitorDispositivos](src/monitor/MonitorDispositivos.java).

Mostrar a classe interna:

```java
private class ThreadColetaDispositivo extends Thread {
    private final DispositivoRede dispositivo;
    private volatile boolean ativo = true;

    ThreadColetaDispositivo(DispositivoRede d) {
        super("monitor-" + d.getId());
        this.dispositivo = d;
        setDaemon(true);
    }

    @Override
    public void run() {
        while (ativo && !isInterrupted()) {
            executarColeta(dispositivo);
            try { Thread.sleep(INTERVALO_MS); }
            catch (InterruptedException e) { interrupt(); }
        }
    }

    void encerrar() { ativo = false; interrupt(); }
}
```

Fala:

> "Cada dispositivo cadastrado gera uma thread própria. Herdamos de
> `Thread`, sobrescrevemos o `run()`, entramos em loop, chamamos a
> coleta, dormimos 15 segundos e repetimos — exatamente o modelo
> visto em aula."
>
> "Quando o usuário remove um dispositivo, chamamos `interrupt()`
> naquela thread. Quando fecha a janela, `monitor.encerrar()`
> interrompe todas."
>
> "As threads são marcadas como **daemon**, então o processo consegue
> encerrar mesmo se alguma coleta ainda estiver rodando."

### 4.3 Interface gráfica — Swing

Arquivos: [JanelaPrincipal](src/gui/JanelaPrincipal.java),
[DialogoDispositivo](src/gui/DialogoDispositivo.java),
[PainelDetalhes](src/gui/PainelDetalhes.java),
[ModeloDispositivos](src/gui/ModeloDispositivos.java).

Mostrar em `JanelaPrincipal.java`:

```java
JToolBar barra = new JToolBar();
JButton btnAdd = new JButton("Adicionar");
JTabbedPane abas = new JTabbedPane();
abas.addTab("Dispositivos", split);
abas.addTab("Interfaces de Rede", new PainelInterfaces());
```

Depois o ponto crítico da concorrência:

```java
@Override
public void aoAtualizarDispositivo(DispositivoRede d) {
    // Roda na thread do monitor — salta para a EDT antes de mexer no Swing.
    SwingUtilities.invokeLater(() -> {
        int row = modelo.linhaDe(d);
        if (row >= 0) modelo.atualizarLinha(row);
        DispositivoRede sel = selecionado();
        if (sel != null && sel == d) painelDetalhes.exibir(d);
    });
}
```

Fala:

> "Regra de ouro do Swing: só se mexe em componentes na thread da
> interface. Como o monitor está em outra thread, usamos
> `SwingUtilities.invokeLater` para agendar a atualização na EDT."

Mostrar o renderer que pinta a coluna de status:

```java
Color c = s.getCor();
setBackground(new Color(c.getRed(), c.getGreen(), c.getBlue(), 80));
```

Fala:

> "A coluna Status usa um renderer custom que pinta o fundo da célula
> na cor do enum — verde, amarelo, vermelho ou cinza."

### 4.4 Padrão de projeto — Factory + Observer

Arquivos: [DispositivoFactory](src/service/DispositivoFactory.java) e
[DispositivoObserver](src/monitor/DispositivoObserver.java).

Mostrar a factory:

```java
public static DispositivoRede criar(TipoDispositivo tipo, String nome, String ip) {
    // validações...
    switch (tipo) {
        case ROTEADOR: d = new Roteador(n, i); break;
        case SWITCH:   d = new SwitchRede(n, i); break;
        case FIREWALL: d = new Firewall(n, i); break;
        case SERVIDOR: d = new Servidor(n, i); break;
    }
    return d;
}
```

Fala:

> "A GUI só conhece o enum `TipoDispositivo` e a classe abstrata
> `DispositivoRede`. Quem escolhe a classe concreta é a factory.
> Se amanhã adicionarmos um novo tipo, só mexemos aqui."

E o observer:

```java
public interface DispositivoObserver {
    void aoAtualizarDispositivo(DispositivoRede dispositivo);
}
```

Fala:

> "O monitor mantém uma lista de observadores. Quando a coleta termina,
> ele avisa todo mundo. A `JanelaPrincipal` implementa essa interface
> e é o único observador hoje — mas amanhã poderíamos ter um logger,
> um exportador CSV, sem mexer no monitor."

> Detalhes teóricos em [PADROES_DE_PROJETO.md](PADROES_DE_PROJETO.md).

---

## 5. Encerramento

> "Resumindo os itens avaliativos:"
>
> - "**Polimorfismo**: classe abstrata `DispositivoRede` + 4 subclasses
>   com `diagnosticoEspecifico` sobrescrito."
> - "**Threads**: `MonitorDispositivos` cria uma thread por dispositivo,
>   com loop e `sleep`, no mesmo estilo da aula."
> - "**Interface gráfica**: Swing com `JFrame`, `JTable`, `JTabbedPane`,
>   `JDialog` e cores dinâmicas."
> - "**Padrão de projeto**: `Factory Method` na criação de dispositivos
>   e `Observer` na comunicação monitor → GUI."
>
> "O projeto foi mantido enxuto de propósito: só o essencial do
> enunciado. O diagrama UML completo está no arquivo
> [DIAGRAMA_UML.md](DIAGRAMA_UML.md), a explicação teórica dos padrões
> em [PADROES_DE_PROJETO.md](PADROES_DE_PROJETO.md), e a documentação
> geral em [DOCUMENTACAO.md](DOCUMENTACAO.md)."

---

## Perguntas prováveis do professor

**"Por que classe abstrata e não interface?"**
> Os dispositivos compartilham estado real (id, nome, IP, métrica).
> Uma interface obrigaria a repetir esses campos em cada subclasse.
> A abstrata dá o esqueleto e obriga só o que muda: os dois métodos
> abstratos.

**"E se o ping travar?"**
> `ProcessBuilder` roda com timeout. Se estourar, `destroyForcibly`
> mata o processo. A thread do monitor sai do timeout e segue para
> o próximo ciclo.

**"E se duas threads mexerem na mesma métrica ao mesmo tempo?"**
> A `MetricaRede` é imutável e o campo `ultimaMetrica` em
> `DispositivoRede` é `volatile`. Reescrever uma referência atômica
> `volatile` é seguro entre threads.

**"Por que não usar `ScheduledExecutorService`?"**
> A primeira versão usava. Simplificamos para `Thread` puro para
> ficar mais próximo dos exemplos da disciplina.

**"O que acontece se o professor mudar o intervalo de 15 s?"**
> Basta alterar a constante `INTERVALO_MS` em
> [MonitorDispositivos](src/monitor/MonitorDispositivos.java).

**"Como funciona o padrão Factory aqui?"**
> A GUI chama `DispositivoFactory.criar(TipoDispositivo.ROTEADOR, ...)`.
> Ela nunca faz `new Roteador(...)` diretamente. Isso desacopla
> a interface das classes concretas.

**"E o Observer?"**
> A interface `DispositivoObserver` define um método:
> `aoAtualizarDispositivo`. A `JanelaPrincipal` implementa e se
> registra no monitor. Quando a métrica muda, o monitor notifica
> todos os inscritos.
