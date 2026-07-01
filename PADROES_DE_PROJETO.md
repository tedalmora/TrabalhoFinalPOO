# Padrões de Projeto — Teoria e Aplicação

Este documento explica os dois padrões de projeto usados no
"Gerenciador de Dispositivos de Rede":

1. **Factory Method** — criação centralizada de objetos.
2. **Observer** — notificação de mudanças de estado.

Cada seção tem três partes:

- **Teoria**: o que é o padrão, quando e por que usar.
- **Estrutura genérica**: participantes e diagrama.
- **Aplicação no projeto**: onde está no código e como funciona.

Complemento: [DIAGRAMA_UML.md](DIAGRAMA_UML.md) mostra os diagramas
das classes envolvidas.

---

## 1. Factory Method

### 1.1 Teoria

O **Factory Method** é um dos 23 padrões clássicos do livro "Design
Patterns" do Gang of Four (GoF). Está na categoria dos **padrões
criacionais** — padrões que tratam de como objetos são instanciados.

**Definição do GoF (adaptada):**

> "Definir uma interface para criar um objeto, mas deixar as subclasses
> (ou uma classe fábrica dedicada) decidirem qual classe instanciar.
> O Factory Method permite que uma classe delegue a instanciação para
> outra."

**Problema que o padrão resolve:**

Quando o código cliente precisa criar objetos, mas:

- Existem várias classes concretas possíveis (uma família de tipos).
- O cliente não deveria conhecer cada uma delas.
- Adicionar um novo tipo não deveria obrigar mudanças no cliente.

Sem o padrão, o código cliente ficaria cheio de `if`/`switch`
espalhados:

```java
// Sem factory — cliente acoplado às classes concretas
DispositivoRede d;
if (tipo.equals("roteador")) d = new Roteador(...);
else if (tipo.equals("switch")) d = new SwitchRede(...);
// ... isso repetido em vários lugares
```

Isso viola dois princípios importantes:

- **Open/Closed Principle**: o cliente deveria estar aberto para
  extensão (novos tipos) mas fechado para modificação.
- **Dependency Inversion**: o cliente depende de classes concretas em
  vez de depender só de abstrações.

**Solução do padrão:**

Concentrar toda a lógica de criação em um único ponto (a fábrica).
O cliente passa um parâmetro simples (enum, string, tipo) e recebe
de volta a abstração pronta.

**Quando usar:**

- Você tem uma hierarquia de classes com uma superclasse comum.
- O tipo concreto é escolhido em tempo de execução (ex.: entrada do
  usuário, configuração).
- A criação envolve regras de validação ou parametrização repetitivas
  que você não quer duplicar.

**Variantes do padrão:**

- **Factory Method clássico**: um método `factory` em uma superclasse
  que subclasses sobrescrevem.
- **Static Factory / Simple Factory**: uma classe utilitária estática
  com métodos de criação. Não é padrão GoF puro, mas é a forma mais
  usada na prática — é o que usamos aqui.
- **Abstract Factory**: uma família de fábricas relacionadas.

### 1.2 Estrutura genérica

```
+---------+          +--------------+
| Cliente |--------->|   Factory    |
+---------+  usa     +--------------+
                     | + criar(tipo)|
                     +--------------+
                            |
                            | cria
                            v
                     +--------------+
                     | ProdutoBase  | (abstrata)
                     +--------------+
                            ^
              +-------------+-------------+
              |             |             |
        +---------+   +---------+   +---------+
        | ProdA   |   | ProdB   |   | ProdC   |
        +---------+   +---------+   +---------+
```

Participantes:

- **Cliente**: quem precisa dos objetos, mas não sabe (nem quer saber)
  criá-los.
- **Factory**: concentra a lógica de instanciação.
- **Produto base**: a abstração comum (classe abstrata ou interface).
- **Produtos concretos**: as implementações reais.

### 1.3 Aplicação no projeto

**Arquivo:** [DispositivoFactory.java](src/service/DispositivoFactory.java)

Estrutura no nosso código:

```
+-----------------------+          +----------------------+
| gui.DialogoDispositivo|--------->| DispositivoFactory   |
+-----------------------+  usa     +----------------------+
                                   | + criar(tipo,nome,ip)|
                                   +----------------------+
                                             |
                                             | cria
                                             v
                                   +----------------------+
                                   | model.DispositivoRede| (abstrata)
                                   +----------------------+
                                             ^
                        +------------+-------+-------+------------+
                        |            |               |            |
                    +---------+  +---------+     +---------+  +---------+
                    | Roteador|  |SwitchRede|    |Firewall |  |Servidor |
                    +---------+  +---------+     +---------+  +---------+
```

Código da fábrica:

```java
public final class DispositivoFactory {

    public enum TipoDispositivo {
        ROTEADOR("Roteador"),
        SWITCH("Switch"),
        FIREWALL("Firewall"),
        SERVIDOR("Servidor");
        // ...
    }

    public static DispositivoRede criar(TipoDispositivo tipo,
                                        String nome, String ip) {
        // Validações centralizadas: nome não vazio, IP não vazio, etc.
        if (tipo == null)
            throw new IllegalArgumentException("Tipo é obrigatório.");
        if (nome == null || nome.trim().isEmpty())
            throw new IllegalArgumentException("Nome é obrigatório.");
        if (ip == null || ip.trim().isEmpty())
            throw new IllegalArgumentException("IP/host é obrigatório.");

        String n = nome.trim();
        String i = ip.trim();

        // Decide a classe concreta a partir do enum:
        DispositivoRede d;
        switch (tipo) {
            case ROTEADOR: d = new Roteador(n, i); break;
            case SWITCH:   d = new SwitchRede(n, i); break;
            case FIREWALL: d = new Firewall(n, i); break;
            case SERVIDOR: d = new Servidor(n, i); break;
            default: throw new IllegalArgumentException("Tipo inválido.");
        }
        return d;
    }
}
```

**Como é usada:**

Na GUI, o `DialogoDispositivo` só sabe do enum e da abstração:

```java
TipoDispositivo tipo = (TipoDispositivo) comboTipo.getSelectedItem();
DispositivoRede novo = DispositivoFactory.criar(tipo, nome, ip);
```

Também no `App.java` de inicialização:

```java
monitor.adicionarDispositivo(DispositivoFactory.criar(
    TipoDispositivo.SERVIDOR, "Cloudflare", "1.1.1.1"));
```

Nenhum lugar da aplicação faz `new Roteador(...)` diretamente — só a
`DispositivoFactory`.

**Benefícios concretos que ganhamos:**

1. **Isolamento**: A GUI não importa as classes `Roteador`,
   `SwitchRede`, `Firewall`, `Servidor`. Só importa a abstrata
   `DispositivoRede` e o enum `TipoDispositivo`.
2. **Validação em um lugar só**: nome/IP em branco falham cedo, e
   sempre da mesma forma. Não precisa duplicar `if (nome == null)`
   em vários pontos.
3. **Extensibilidade**: adicionar um novo tipo (ex.: `AccessPoint`)
   exige apenas:
   - Criar a subclasse `AccessPoint extends DispositivoRede`.
   - Adicionar `ACCESS_POINT` no enum `TipoDispositivo`.
   - Adicionar o `case` no switch.
   - Nada muda na GUI, no monitor, no observer.

### 1.4 Método auxiliar `tipoDe(...)`

A fábrica também tem um método reverso:

```java
public static TipoDispositivo tipoDe(DispositivoRede d) {
    for (TipoDispositivo t : TipoDispositivo.values()) {
        if (t.getRotulo().equalsIgnoreCase(d.tipoDispositivo())) return t;
    }
    return TipoDispositivo.SERVIDOR;
}
```

Usado pelo `DialogoDispositivo` no modo edição: dado o dispositivo
selecionado, descobre qual enum pré-selecionar no combo.

---

## 2. Observer

### 2.1 Teoria

O **Observer** é um dos padrões **comportamentais** do GoF. Trata da
comunicação entre objetos quando um deles muda de estado.

**Definição do GoF (adaptada):**

> "Definir uma dependência um-para-muitos entre objetos, de modo que
> quando um objeto muda de estado, todos os seus dependentes são
> notificados e atualizados automaticamente."

**Problema que o padrão resolve:**

Você tem um objeto (o **sujeito**, ou **subject**) cujo estado muda
com o tempo. Outros objetos (os **observadores**) precisam reagir a
essas mudanças.

Sem o padrão, você teria duas alternativas ruins:

1. **Polling**: cada observador ficaria em loop perguntando "mudou?
   mudou? mudou?". Desperdiça CPU e complica o código.
2. **Acoplamento direto**: o sujeito conheceria cada observador
   explicitamente e chamaria cada um. Se surge um novo observador,
   precisa mexer no sujeito.

**Solução do padrão:**

- O sujeito mantém uma **lista de observadores** (via uma interface
  comum).
- Quando o estado muda, o sujeito percorre a lista e chama um
  método padronizado em cada observador.
- Observadores se **registram** (subscribe) e podem se
  **desregistrar** (unsubscribe) dinamicamente.

**Quando usar:**

- Um objeto precisa notificar outros sem saber quem eles são.
- O conjunto de observadores muda ao longo do tempo.
- Você quer manter partes do sistema **desacopladas** — por exemplo,
  a lógica de negócio (o monitor) não deve depender da GUI (Swing).

**Exemplos famosos na JDK:**

- `java.util.Observer` / `java.util.Observable` (deprecated no Java 9).
- `PropertyChangeListener` / `PropertyChangeSupport` (JavaBeans).
- Todos os listeners do Swing (`ActionListener`, `MouseListener`, etc.)
  são aplicações de Observer.

### 2.2 Estrutura genérica

```
+------------------+                +-------------------+
|     Subject      |<>------------->|     Observer      | (interface)
+------------------+     lista de   +-------------------+
| + adicionar(obs) |                | + aoAtualizar(...)|
| + remover(obs)   |                +-------------------+
| + notificar()    |                          ^
+------------------+                          |
        |                          +----------+----------+
        | quando muda              |                     |
        v                    +----------+          +----------+
   notificar todos           | ObsConcA |          | ObsConcB |
                             +----------+          +----------+
```

Participantes:

- **Subject (Sujeito)**: o objeto observável. Mantém a lista de
  observadores e notifica-os.
- **Observer (Observador)**: a interface comum que os observadores
  implementam.
- **Observadores concretos**: reagem à notificação de forma específica.

### 2.3 Aplicação no projeto

**Arquivos envolvidos:**

- [DispositivoObserver.java](src/monitor/DispositivoObserver.java) — interface
- [MonitorDispositivos.java](src/monitor/MonitorDispositivos.java) — Subject
- [JanelaPrincipal.java](src/gui/JanelaPrincipal.java) — Observer concreto

Estrutura no nosso código:

```
+---------------------+     +---------------------+
| MonitorDispositivos |<>---| DispositivoObserver | (interface)
+---------------------+     +---------------------+
| observadores : List |     | + aoAtualizar-      |
| + adicionarObs()    |     |   Dispositivo(d)    |
| + notificar (interno)|    +---------------------+
+---------------------+               ^
        |                             |
        | após cada coleta,           |
        | percorre observadores        +---> implementa
        v                              |
+---------------------+                |
| ThreadColeta        |         +--------------------+
| (uma por dispositivo)|         | JanelaPrincipal   |
+---------------------+          +--------------------+
                                 | atualiza JTable    |
                                 | e PainelDetalhes  |
                                 | (na EDT)          |
                                 +-------------------+
```

**A interface (contrato):**

```java
public interface DispositivoObserver {
    void aoAtualizarDispositivo(DispositivoRede dispositivo);
}
```

Uma linha só — o mínimo necessário. Quem implementar precisa fazer
algo com o dispositivo atualizado.

**O Subject (`MonitorDispositivos`):**

```java
public class MonitorDispositivos {

    private final List<DispositivoObserver> observadores =
            new CopyOnWriteArrayList<>();

    public void adicionarObservador(DispositivoObserver obs) {
        observadores.add(obs);
    }

    // Chamado pela ThreadColetaDispositivo após cada ciclo:
    private void executarColeta(DispositivoRede d) {
        // ... executa ping, traceroute, salva métrica ...

        // Notifica todos os observadores:
        for (DispositivoObserver obs : observadores) {
            obs.aoAtualizarDispositivo(d);
        }
    }
}
```

Detalhes importantes:

- Usamos `CopyOnWriteArrayList` para permitir iteração segura mesmo se
  alguém se registrar/desregistrar durante a notificação.
- O monitor **não conhece Swing** — ele só depende da interface
  `DispositivoObserver`.

**O Observer concreto (`JanelaPrincipal`):**

```java
public class JanelaPrincipal extends JFrame implements DispositivoObserver {

    public JanelaPrincipal(MonitorDispositivos monitor) {
        // ...
        monitor.adicionarObservador(this);   // <-- registro
        // ...
    }

    @Override
    public void aoAtualizarDispositivo(DispositivoRede d) {
        // Este método executa na thread do monitor.
        // Salta para a EDT antes de mexer em componentes Swing:
        SwingUtilities.invokeLater(() -> {
            int row = modelo.linhaDe(d);
            if (row >= 0) modelo.atualizarLinha(row);
            DispositivoRede sel = selecionado();
            if (sel != null && sel == d) painelDetalhes.exibir(d);
        });
    }
}
```

**Sequência da notificação:**

1. A thread do dispositivo termina uma coleta e chama
   `executarColeta(d)`.
2. O método percorre `observadores` e chama
   `obs.aoAtualizarDispositivo(d)` em cada um.
3. O único observador registrado é a `JanelaPrincipal`.
4. Como estamos numa thread de fundo (não na EDT), a implementação
   usa `SwingUtilities.invokeLater` para agendar a atualização visual.
5. Na EDT, a linha da tabela é redesenhada (novo status colorido) e,
   se o dispositivo está selecionado, o painel de detalhes também.

**Benefícios concretos:**

1. **Desacoplamento total**: o monitor pode ser testado sem GUI. Basta
   registrar um observador de mentira (mock) que só imprime no console.
2. **Extensibilidade**: dá pra adicionar um segundo observador (por
   exemplo, um logger de arquivo) sem tocar em `MonitorDispositivos`
   nem em `JanelaPrincipal`.
3. **Fluxo natural entre threads**: o monitor entrega o resultado
   quando fica pronto; a GUI reage assim que recebe. Sem polling,
   sem `Thread.sleep`, sem locks explícitos.

---

## 3. Como os dois padrões se combinam

Um resumo do papel de cada um no ciclo de vida do sistema:

**Ao cadastrar um dispositivo** (fluxo do Factory):

```
DialogoDispositivo
     │  chama
     ▼
DispositivoFactory.criar(tipo, nome, ip)
     │  devolve
     ▼
DispositivoRede (abstrato) — na prática Roteador/Switch/...
     │
     ▼
monitor.adicionarDispositivo(d)  ──►  cria ThreadColeta e start()
```

**A cada coleta** (fluxo do Observer):

```
ThreadColeta.run()
     │  chama
     ▼
executarColeta(d)  ──►  ping + traceroute + salva métrica
     │  ao fim,
     ▼
observadores.forEach(obs -> obs.aoAtualizarDispositivo(d))
     │
     ▼
JanelaPrincipal.aoAtualizarDispositivo(d)
     │  invokeLater(...)
     ▼
EDT: atualiza JTable e PainelDetalhes
```

O Factory resolve **como criar** os objetos.
O Observer resolve **como reagir** às mudanças.
Juntos, mantêm o sistema **desacoplado, extensível e testável** —
que é exatamente o objetivo dos padrões de projeto.

---

## 4. Referências

- Gamma, Helm, Johnson, Vlissides. *Design Patterns: Elements of
  Reusable Object-Oriented Software.* Addison-Wesley, 1994. (Livro
  clássico do "Gang of Four".)
- Freeman, Robson, Sierra, Bates. *Head First Design Patterns.*
  O'Reilly. Versão mais didática, com muitos exemplos em Java.
- Documentação oficial da JDK sobre a interface antiga
  `java.util.Observer` (deprecated no Java 9) — bom para entender
  a estrutura clássica do padrão.
