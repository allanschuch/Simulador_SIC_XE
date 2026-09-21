# Documentação do Sistema SIC/XE
**Módulo:** Simulador da Máquina Virtual (Core)

## Visão Geral da Arquitetura (Padrão MVC)
O sistema implementa o padrão MVC. A Máquina Virtual (Modelo) expõe o estado da memória, dos registradores e os métodos de controle de ciclo (como `step()`), sem possuir acoplamento com bibliotecas gráficas. O Controller orquestra a execução e interage com a Interface Gráfica (View) sem bloquear a Thread da interface. Esta estrutura arquitetural viabiliza a integração modular de futuros componentes, como o Montador e o Ligador.

---

## Sumário
* **Visão Geral da Arquitetura**
* **MÓDULO 1: MODEL**
  * **1. Pacote `types`: Tipos Fundamentais e Representação de Dados**
    * 1.1.1. Classe `Word24`
    * 1.1.2. Enum `Register`
    * 1.1.3. Enum `ConditionCode`
    * 1.1.4. Enum `Opcode`
    * 1.1.5. Enum `AddressingMode`
  * **2. Pacote `hardware`: Componentes de Hardware**
    * 1.2.1. Classe `Memory`
    * 1.2.2. Classe `RegisterBank`
  * **3. Pacote `decoder`: Decodificação de Instruções**
    * 1.3.1. Classe `DecodedInstruction`
    * 1.3.2. Classe `InstructionDecoder`
  * **4. Pacote `execution`: Execução e Endereçamento**
    * 1.4.1. Classe `TargetAddressCalculator`
    * 1.4.2. Classe `Executor`
  * **5. Pacote `machine`: Fachada e Controle da Máquina**
    * 1.5.1. Classe `SICXEMachine`
* **MÓDULO 2: Controller**
  * **1. Pacote `controller`: Orquestração e Eventos**
    * 2.1.1. Interface `MachineStateListener`
    * 2.1.2. Classe `MachineController`

---

## MÓDULO 1: MODEL

---

## 1.1. Pacote `types`: Tipos Fundamentais

Este pacote resolve a incompatibilidade estrutural entre a arquitetura SIC/XE e os tipos primitivos da linguagem Java, além de padronizar a identificação de instruções e modos de operação.

### 1.1.1. Classe `Word24`
A arquitetura SIC/XE utiliza operandos de 24 bits (3 bytes), enquanto a linguagem Java utiliza inteiros de 32 bits com sinal. A classe `Word24` encapsula um inteiro Java e aplica máscaras bit a bit (`& 0xFFFFFF`) no construtor, garantindo o truncamento dos valores para o teto de 24 bits.

*   **Prevenção de Extensão de Sinal:** O Java propaga o sinal negativo de bytes quando o bit mais significativo é 1. A constante `UNSIGNED_BYTE_MASK` (0xFF) aplica uma operação bit a bit para anular essa extensão e forçar a leitura do byte como um inteiro sem sinal (0 a 255).
*   **Tratamento de Complemento de 2 (Endereçamento Relativo):** O método `fromSigned12Bit()` isola os 12 bits do campo de deslocamento (*displacement*) utilizado nas instruções de Formato 3. Se o bit de sinal indicar valor negativo (salto para trás), a máscara `| 0xFFFFF000` estende os bits "1" para o espaço superior, garantindo a corretude da adição aritmética com o registrador PC. O método `toIntSigned()` aplica a extensão correspondente para 24 bits, retornando o valor com sinal estendido para 32 bits.
*   **Aritmética e Manipulação de Bytes:** Fornece métodos unificados para operações aritméticas (`add()`, `subtract()`) e leitura posicional (`getHighByte()`, `getMiddleByte()`, `getLowByte()`). O método `withLowByte()` viabiliza a substituição isolada do byte menos significativo, mantendo os bytes superiores intactos, fornecendo a base operacional para a instrução `LDCH`.
*   **Operações Lógicas e de Deslocamento:** Disponibiliza os métodos `bitwiseAnd()` e `bitwiseOr()` para efetuar as operações lógicas equivalentes exigidas pelas instruções `AND` e `OR`. Para manipulação em nível de bit, implementa `shiftLeftCircular()`, que executa a rotação circular truncando e realocando os bits dentro do espaço estrito de 24 bits (`SHIFTL`). O método `shiftRightArithmetic()` utiliza o operador aritmético de deslocamento nativo do Java (`>>`) sobre a representação com sinal estendido, garantindo que as posições vacantes à esquerda sejam preenchidas com o bit de sinal original, atendendo ao comportamento da instrução `SHIFTR`.

### 1.1.2. Enum `Register`
Mapeia os registradores definidos na especificação (A, X, L, B, S, T, F, PC e SW) para os seus respectivos códigos numéricos de hardware (0 a 9).
*   **Controle de Identificadores:** A substituição de variáveis inteiras por um Enum provê segurança de tipagem (type-safety). Opcodes lidos de instruções do Formato 2 são convertidos instanciando objetos tipados através de mapeamento (ex: `Register.fromCode()`).
*   **Registrador F:** O registrador de Ponto Flutuante (código 6, 48 bits) está mapeado para refletir a especificação original da arquitetura. No entanto, não é utilizado no processamento.

### 1.1.3. Enum `ConditionCode`
Enumera os três estados lógicos assumidos pela Palavra de Status (SW) após instruções de comparação (`COMP`, `COMPR`).
*   **Estados Definidos:** `LESS_THAN` (<), `EQUAL` (=) e `GREATER_THAN` (>).
*   **Abstração:** Substitui a manipulação de bits na Palavra de Status (SW) e expõe o resultado das comparações em alto nível, direcionando a lógica de controle de fluxo para as instruções de salto condicional (`JEQ`, `JLT`, `JGT`).

### 1.1.4. Enum `Opcode`
Mapeia as instruções suportadas pelo simulador para seus respectivos códigos hexadecimais e formatos originais (2 ou 3/4).
*   **Identificação com Máscara:** O método estático `fromMachineByte()` recebe o primeiro byte da instrução lida na memória. Para instruções dos Formatos 3 e 4, os dois bits menos significativos desse byte contêm as flags `n` e `i`. O método aplica a constante `FORMAT_3_4_OPCODE_MASK` (`0xFC`) para zerar esses bits, permitindo o casamento exato do valor lido com o opcode base. Se o byte lido não pertencer a nenhuma instrução mapeada, lança `IllegalArgumentException`.

### 1.1.5. Enum `AddressingMode`
Representa os modos de endereçamento da arquitetura SIC/XE aplicáveis aos Formatos 3 e 4.
*   **Mapeamento Semântico:** Traduz a combinação das flags `n` e `i` para os estados lógicos correspondentes: `IMMEDIATE` (n=0, i=1), `INDIRECT` (n=1, i=0), `SIMPLE` (n=1, i=1) e `SIC_STANDARD` (n=0, i=0).

---

## 1.2. Pacote `hardware`: Componentes de Hardware

O pacote estrutura os recursos físicos e a persistência de estado da máquina virtual.

### 1.2.1. Classe `Memory`
Representa a memória principal como um vetor unidimensional de bytes (`byte[] data`).

*   **Capacidade Máxima:** A estrutura aloca estritamente 1 MB (1.048.576 bytes). Este tamanho engloba o limite máximo de endereçamento da arquitetura definido pelas instruções de Formato 4, que suportam um endereço de destino de até 20 bits.
*   **Acesso Híbrido:** A classe disponibiliza métodos de acesso granular em dois níveis:
    *   **Nível de Byte:** `readByte()` e `writeByte()`, utilizados pelas instruções `LDCH` e `STCH`.
    *   **Nível de Palavra:** `readWord()` e `writeWord()`, que interagem nativamente instanciando uma `Word24`. A leitura resolve internamente a indexação de 3 bytes consecutivos.
*   **Mecanismo de Proteção:** Todo acesso físico de leitura ou escrita executa previamente o método `validateAddress()`. O cálculo de um Endereço Efetivo (TA) fora do espaço de 1 MB ou negativo acarreta o lançamento imediato de `IllegalArgumentException`.

### 1.2.2. Classe `RegisterBank`
O Banco de Registradores unifica o estado dos registradores primários da CPU em uma estrutura baseada em `EnumMap<Register, Word24>`.

*   **Bloqueio de Operações Restritas:** O registrador de Ponto Flutuante (`F`) é inicializado como nulo e isolado pelo método `validateRegister()`. A tentativa de leitura ou escrita no registrador `F` lança uma `UnsupportedOperationException`, abortando a execução para sinalizar operação inválida.
*   **Manipulação do PC:** Para otimizar as operações contínuas no ciclo de busca (*fetch*), a classe fornece os atalhos `getPC()` e `setPC()`. Estes métodos retornam e recebem inteiros sem sinal nativos, dispensando a conversão para `Word24` a cada incremento.
*   **Integração do Código de Condição (CC):** Utiliza as abstrações definidas no Enum `ConditionCode` por meio dos métodos `getConditionCode()` e `setConditionCode()`, armazenando o estado condicional sem alterar fisicamente bits operacionais no registrador SW.

---

## 1.3. Pacote `decoder`: Decodificação de Instruções

Isola a lógica responsável por interpretar os bytes lidos da memória, transformar bits isolados em estruturas tipadas e alimentar a unidade de execução.

### 1.3.1. Classe `DecodedInstruction`
Atua como um contêiner de dados (DTO) imutável que centraliza as propriedades decodificadas de uma instrução lida.

*   **Separação de Formatos:** Possui construtores distintos para acomodar a discrepância estrutural entre instruções. O construtor de Formato 2 inicializa exclusivamente os parâmetros de registradores (`r1` e `r2`). O construtor de Formatos 3 e 4 inicializa o modo de endereçamento, as flags de cálculo de endereço (`x`, `b`, `p`, `e`) e o valor de deslocamento/endereço, definindo os parâmetros do Formato 2 como nulos.
*   **Facilitador de Álgebra Relativa:** O método `getSignedDisplacement()` acessa o valor cru de deslocamento extraído (12 bits) e o retorna encapsulado através de `Word24.fromSigned12Bit()`. Isso entrega para a unidade de controle o valor pronto com o sinal estendido, essencial para a adição aritmética correta no endereçamento relativo ao PC.

### 1.3.2. Classe `InstructionDecoder`
Responsável pela leitura crua da memória e extração estruturada das flags lógicas e operandos, delegando o resultado para a instanciação de um objeto `DecodedInstruction`.

*   **Decodificação de Formato 2:** Para instruções de 2 bytes, a classe lê o segundo byte da memória e aplica operações de deslocamento de bits (`>>`) aliadas à constante `DISPLACEMENT_HIGH_NIBBLE_MASK` (`0x0F`) para extrair e mapear isoladamente os nibbles (4 bits) correspondentes aos registradores `r1` e `r2`.
*   **Extração de Flags (Formatos 3 e 4):** A identificação das diretrizes de endereçamento é feita via máscaras bit a bit (`&`). A classe extrai as flags `n` e `i` a partir dos dois bits menos significativos do primeiro byte (`FLAG_N_MASK`, `FLAG_I_MASK`), e as flags `x`, `b`, `p` e `e` a partir dos quatro bits mais significativos do segundo byte lido.
*   **Concatenação de Endereços/Deslocamentos:**
    *   **Formato 4:** Ativado quando a flag `e` é verdadeira. O decodificador consome 4 bytes da memória e concatena os 4 bits inferiores do segundo byte com os 16 bits dos bytes três e quatro. Este processo utiliza deslocamentos lógicos (`<<`) e operadores OR (`|`), resultando no endereço absoluto de 20 bits.
    *   **Formato 3:** Ativado quando a flag `e` é falsa. O decodificador consome 3 bytes e forma o deslocamento relativo de 12 bits combinando o nibble inferior do byte dois e a totalidade do byte três.

## 1.4. Pacote `execution`: Execução e Endereçamento

Este pacote é responsável pela resolução final de ponteiros em memória e pela aplicação das mutações de estado na CPU (registradores e memória principal) de acordo com a semântica de cada instrução.

### 1.4.1. Classe `TargetAddressCalculator`
Isola a lógica de cálculo do Endereço Efetivo (Target Address - TA) exclusivo para as instruções dos Formatos 3 e 4. 

*   **Resolução de Deslocamento:** Avalia as flags de base e contador de programa. Se `p=1` (PC-relativo), soma o valor de deslocamento com sinal estendido ao registrador PC. Se `b=1` (Base-relativo), soma o deslocamento sem sinal ao registrador B. Se ambas forem nulas, assume o valor lido como endereço absoluto de 20 bits. A ativação simultânea das flags `p` e `b` lança uma `IllegalArgumentException`.
*   **Indexação:** Caso a flag `x` esteja ativa, adiciona o valor contido no registrador `X` ao endereço computado parcial.
*   **Truncamento Físico:** Aplica a máscara `& 0xFFFFFF` ao final do cálculo do TA para garantir que eventuais retrocessos relativos ao PC não gerem endereços negativos que vazem para a base de 32 bits do inteiro Java, confinando o ponteiro aos limites arquiteturais.
*   **Indireção:** Se o modo for indireto (`n=1, i=0`), o endereço computado é tratado como ponteiro primário. A classe acessa a memória, lê a palavra de 24 bits contida neste ponteiro e a define como o Endereço Efetivo final.

### 1.4.2. Classe `Executor`
Representa a Unidade Lógica, Aritmética e de Execução (ALU). Processa o DTO `DecodedInstruction` e executa as rotinas correspondentes, sem manipular operações de baixo nível de mascaramento de bits.

*   **Busca de Operandos Dinâmica:** O método interno `fetchOperand()` interroga o modo de endereçamento. Para o modo Imediato (`n=0, i=1`), encapsula diretamente o TA computado em uma `Word24`. Para os demais casos, efetua a leitura da palavra correspondente na memória principal.
*   **Movimentação e Manipulação de Bytes:** Executa transferências diretas de Load e Store em nível de palavra (`LDA`, `STA`). Nas instruções específicas de caractere (`LDCH`, `STCH`), utiliza os métodos de precisão isolada (`withLowByte()`, `getLowByte()`) para interagir restritamente com o último byte, preservando o restante da estrutura.
*   **Aritmética e Tratamento de Complemento de 2:** Delega as operações matemáticas para o controle interno da classe `Word24`. A conversão nativa para `toIntSigned()` blinda a aritmética de 24 bits, assegurando o comportamento correto perante valores negativos pela JVM. Operações de `DIV` ou `DIVR` por zero acionam uma `ArithmeticException`.
*   **Deslocamento em Registradores (Shift):** Para as instruções `SHIFTL` e `SHIFTR`, o executor converte o identificador de r2 (armazenado como `n-1`) de volta para o número real de saltos `n` antes de invocar a translação de bits.
*   **Controle de Fluxo e Subrotinas:** A classe altera o fluxo de execução manipulando diretamente o registrador PC. Comparações (`COMP`, `COMPR`) fixam o novo estado no `ConditionCode`. Saltos condicionais (`JEQ`, `JLT`, `JGT`) avaliam este código para deferir ou ignorar o salto. Subrotinas (`JSUB`) registram o estado atual do PC no registrador de ligação (`L`) antes de executar o salto, permitindo a retomada exata na instrução de retorno (`RSUB`). 

## 1.5. Pacote `machine`: Fachada e Controle da Máquina

Este pacote atua como o ponto central de integração do simulador, estruturando o Modelo (Model) no padrão arquitetural MVC e isolando os componentes internos das interações de interface.

### 1.5.1. Classe `SICXEMachine`
Encapsula as instâncias físicas e lógicas da arquitetura SIC/XE, fornecendo uma API de controle unificada para a coordenação do ciclo de instrução.

*   **Inicialização Integrada:** O construtor centraliza a instanciação da memória principal (`Memory`), do banco de registradores (`RegisterBank`), do decodificador (`InstructionDecoder`), do calculador de endereços (`TargetAddressCalculator`) e da unidade aritmética/lógica (`Executor`).
*   **Ciclo de Execução (`step()`):** Implementa o fluxo clássico de pipeline em um método sequencial:
    *   **Busca e Decodificação:** Lê o endereço armazenado no registrador PC e aciona o decodificador para extrair e tipar a instrução corrente.
    *   **Avanço Antecipado do PC:** O registrador PC é incrementado imediatamente após a decodificação, somando-se o formato da instrução (tamanho em bytes) ao valor atual. Esta atualização pré-execução é um requisito da arquitetura para viabilizar o cálculo do endereçamento PC-relativo (que usa o PC da próxima instrução como base) e para garantir o armazenamento correto do endereço de retorno em chamadas de subrotinas (`JSUB`).
    *   **Endereçamento e Execução:** Invoca o cálculo do Endereço Efetivo (TA) com as flags extraídas e despacha o pacote de dados para a unidade de execução aplicar a alteração de estado.
*   **Restabelecimento de Estado (`reset()`):** Zera toda a alocação da memória principal e redefine os registradores operacionais para zero, limpando também o Código de Condição (CC).
*   **Exposição de Hardware:** Os métodos `getMemory()` e `getRegisters()` fornecem acesso direto aos componentes. Essa abertura é necessária para que Controllers externos inspecionem o estado atual da máquina e atualizem a Interface Gráfica, mantendo a responsabilidade de mutação restrita à própria classe `SICXEMachine`.

---

## MÓDULO 2: Controller

---

## 6. Pacote `controller`: Orquestração e Eventos

Este pacote implementa a camada de controle do padrão MVC, atuando como intermediário entre a Máquina Virtual (Model) e a Interface Gráfica (View). O design garante que o processamento contínuo da CPU não bloqueie a Thread responsável pela interface gráfica.

### 6.1. Interface `MachineStateListener`
Contrato baseado no padrão *Observer* para comunicação assíncrona e desacoplada.

*   **`onMachineStateChanged`:** Invocado ao término de cada ciclo de instrução (passo a passo ou contínuo). Sinaliza à View o momento exato e seguro para consumir os dados atualizados da memória e dos registradores.
*   **`onMachineError`:** Disparado quando a execução é interrompida por uma exceção de hardware (ex: falha de endereçamento, opcode não suportado). Transfere o evento de falha para que a View o processe visualmente.

### 6.2. Classe `MachineController`
Encapsula as interações com a `SICXEMachine` e gerencia o controle de concorrência da simulação.

*   **Execução Contínua (`run` e `pause`):** O método `run()` aloca uma Thread *daemon* dedicada a um laço de repetição de ciclos de máquina. A variável de controle `running` é qualificada como `volatile`, o que obriga a leitura direta da memória principal; isso garante que a interrupção disparada pelo método `pause()` seja enxergada instantaneamente pela Thread em background, interrompendo a simulação de forma *thread-safe*.
*   **Avanço Único (`step`):** Executa o ciclo lógico da máquina protegido por um bloco `try-catch`. Em caso de exceção de hardware, o controlador executa um `pause()` preventivo e propaga o erro formatado — incluindo o estado atual do registrador PC — através do *listener*.
*   **Reset Lógico (`reset`):** Aplica a reinicialização restrita aos registradores operacionais (A, X, L, B, S, T) e ao PC. O `reset` do Controller não apaga a memória, preservando intencionalmente o último programa (código objeto) carregado para novas execuções.
*   **Carregador Temporário (`loadHexCode`):** Utilitário embutido para testes de execução. Recebe uma string contendo código hexadecimal, sanitiza a entrada removendo espaços e quebras de linha, valida a paridade de caracteres e escreve as instruções byte a byte na memória a partir do endereço `0x0000`.