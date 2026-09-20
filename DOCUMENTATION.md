# Documentação do Sistema SIC/XE
**Módulo:** Simulador da Máquina Virtual (Core)

## Visão Geral da Arquitetura (Padrão MVC)
Para garantir que a lógica da máquina (Modelo) seja estritamente independente da interface gráfica (Visão) exigida pelo projeto, o sistema foi estruturado seguindo o padrão MVC. A Máquina Virtual expõe contratos públicos puros (dados brutos e métodos de controle como `step()`), permitindo que um Controlador orquestre a execução sem congelar a Thread da interface gráfica (JavaFX/Swing). Esta separação também prepara o terreno para que futuros módulos, como o Montador e o Ligador, consigam interagir com a memória e os registradores de forma modular.

---

## Sumário
* **Visão Geral da Arquitetura**
* **1. Pacote `types`: Tipos Fundamentais e Representação de Dados**
  * 1.1. Classe `Word24`
  * 1.2. Enum `Register`
  * 1.3. Enum `ConditionCode`
* **2. Pacote `hardware`: Componentes de Hardware**
  * 2.1. Classe `Memory`
  * 2.2. Classe `RegisterBank`

---

## 1. Pacote `types`: Tipos Fundamentais

O alicerce do simulador resolve o principal atrito estrutural entre a especificação de hardware do SIC/XE e a linguagem Java: a incompatibilidade de tamanhos de palavra e a ausência de tipos primitivos sem sinal (*unsigned*).

### 1.1. Classe `Word24`
A arquitetura SIC/XE define a palavra de memória e os registradores padrão com o tamanho exato de 24 bits (3 bytes). A linguagem Java, por padrão, trata operandos inteiros como 32 bits com sinal. A classe `Word24` encapsula um inteiro nativo do Java e aplica máscaras bit a bit (`& 0xFFFFFF`) no construtor para garantir que qualquer valor seja truncado ao limite físico da arquitetura hipotética.

*   **Prevenção de Extensão de Sinal:** Como o Java não tem bytes *unsigned*, ele propaga o sinal de bytes com o bit mais significativo ativo (tratando-os como números negativos). A constante `UNSIGNED_BYTE_MASK` (0xFF) anula esse efeito durante as leituras da memória.
*   **Tratamento de Complemento de 2 (Endereçamento Relativo):** O método `fromSigned12Bit()` isola os 12 bits de deslocamento (*displacement*) das instruções de Formato 3. Se o valor for negativo (indicando um salto para trás), o método propaga os bits "1" para os espaços superiores de 24 bits, permitindo que a soma com o PC resulte na matemática correta pela CPU. O método `toIntSigned()` aplica a mesma lógica para ler a palavra inteira de 24 bits como um inteiro Java válido (com sinal estendido para 32 bits).
*   **Aritmética e Legibilidade:** A classe centraliza métodos limpos como `add()`, `subtract()` e abstrações de extração de bytes (`getHighByte`, `getMiddleByte`, `getLowByte`). Isso evita a poluição visual de operadores bit a bit (`>>`, `<<`, `&`) espalhados pelo decodificador da CPU, mantendo a Unidade de Execução altamente legível e didática.

### 1.2. Enum `Register`
O SIC/XE possui registradores mapeados para códigos numéricos de 0 a 9 (sem o uso do 7). Este Enum vincula a representação semântica (A, B, X, PC) ao seu respectivo ID de hardware.
*   **Controle de Escopo:** O uso de um Enum, em vez de variáveis inteiras soltas, previne erros de compilação. Quando o decodificador avalia uma instrução do Formato 2 (ex: `ADDR r1, r2`), a extração dos códigos de 4 bits é rapidamente convertida usando algo como `Register.fromCode()`. 
*   **Registrador F:** O registrador `F` (Ponto Flutuante), de 48 bits, foi mapeado (código 6) para refletir a especificação de hardware. No entanto, não será utilizado.

### 1.3. Enum `ConditionCode`
Responsável por mapear os três estados lógicos que a Palavra de Status (SW) pode assumir após instruções de comparação, como `COMP` ou `COMPR`.
*   **Estados:** `LESS_THAN` (<), `EQUAL` (=) e `GREATER_THAN` (>).
*   **Justificativa Técnica:** No hardware original, a *Condition Code* (CC) altera bits específicos da Palavra de Status (SW). Para fins de clareza do simulador, essas escovações de bits foram abstraídas através deste Enum. Isso torna a semântica das instruções de salto condicional (`JEQ`, `JLT`, `JGT`) muito mais intuitiva no loop principal da Unidade de Controle.

---

## 2. Pacote `hardware`: Componentes de Hardware

Este pacote implementa os recursos físicos básicos da máquina virtual.

### 2.1. Classe `Memory`
A memória principal do simulador é implementada de forma contígua através de um vetor de bytes (`byte[] data`).

*   **Capacidade Máxima (Suporte ao Formato 4):** A especificação básica exige um mínimo de 1 KB. No entanto, as instruções de Formato 4 exigem a manipulação de um endereço direto de 20 bits. Para suportar esse endereçamento nativamente (sem gerar exceções de *Out Of Bounds* falsas), a memória aloca estritamente o teto da arquitetura: 1 MB (1.048.576 bytes).
*   **Acesso Híbrido Flexível:** A classe expõe métodos para manipular dados em dois níveis, dependendo da instrução:
    *   Nível de Byte (`readByte` / `writeByte`): Utilizado por instruções de manipulação de caracteres únicas, como `LDCH` e `STCH`.
    *   Nível de Palavra (`readWord` / `writeWord`): Utilizado por manipuladores de registradores gerais (`LDA`, `STA`). A memória resolve a leitura Big-Endian automaticamente instanciando um `Word24`.
*   **Mecanismo de Fail-Safe:** Todo e qualquer acesso físico à memória (seja de byte ou palavra) passa primeiramente pela validação do método `validateAddress()`. Cálculos incorretos de Endereço Efetivo (TA) resultarão instantaneamente no lançamento de uma `IllegalArgumentException`.

### 2.2. Classe `RegisterBank`
O Banco de Registradores agrupa o estado de todos os registradores da CPU. Ele utiliza internamente uma estrutura `EnumMap<Register, Word24>`.

*   **Bloqueio de Operações Não Suportadas (Reg F):** O simulador não executa instruções de ponto flutuante (`ADDF`, `COMPF`, etc.). O registrador `F` é inicializado como nulo e isolado pelo método interno `validateRegister()`. Qualquer tentativa de ler ou escrever em `F` é tratada como uso indevido e lança uma `UnsupportedOperationException`, garantindo que o programa aborte preventivamente.
*   **Acesso Rápido ao PC:** O *Program Counter* é o registrador mais acessado da máquina (lido a cada ciclo de busca/fetch). Por isso, foram criados métodos utilitários diretos (`getPC()` e `setPC()`) que interagem com ele usando tipos inteiros nativos do Java, simplificando drasticamente a lógica da Unidade de Execução, que não precisa ficar lidando com desempacotamento de objetos a cada avanço.
*   **Integração do Código de Condição (CC):** Aproveitando a abstração criada no Enum `ConditionCode`, o Banco de Registradores possui um estado nativo e métodos facilitadores (`getConditionCode` e `setConditionCode`) para processar e responder a saltos condicionais (`J`, `JEQ`, etc.) sem que o executor precise escovar bits na palavra SW.