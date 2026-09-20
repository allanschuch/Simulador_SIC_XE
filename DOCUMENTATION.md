# Documentação do Sistema SIC/XE
**Módulo:** Simulador da Máquina Virtual (Core)

## Visão Geral da Arquitetura (Padrão MVC)
O sistema implementa o padrão MVC. A Máquina Virtual (Modelo) expõe o estado da memória, dos registradores e os métodos de controle de ciclo (como `step()`), sem possuir acoplamento com bibliotecas gráficas. O Controlador orquestra a execução e interage com a Interface Gráfica (Visão) sem bloquear a Thread da interface. Esta estrutura arquitetural viabiliza a integração modular de futuros componentes, como o Montador e o Ligador.

---

## Sumário
* **Visão Geral da Arquitetura**
* **1. Pacote `types`: Tipos Fundamentais e Representação de Dados**
  * 1.1. Classe `Word24`
  * 1.2. Enum `Register`
  * 1.3. Enum `ConditionCode`
  * 1.4. Enum `Opcode`
  * 1.5. Enum `AddressingMode`
* **2. Pacote `hardware`: Componentes de Hardware**
  * 2.1. Classe `Memory`
  * 2.2. Classe `RegisterBank`
* **3. Pacote `decoder`: Decodificação de Instruções**
  * 3.1. Classe `DecodedInstruction`
  * 3.2. Classe `InstructionDecoder`

---

## 1. Pacote `types`: Tipos Fundamentais

Este pacote resolve a incompatibilidade estrutural entre a arquitetura SIC/XE e os tipos primitivos da linguagem Java, além de padronizar a identificação de instruções e modos de operação.

### 1.1. Classe `Word24`
A arquitetura SIC/XE utiliza operandos de 24 bits (3 bytes), enquanto a linguagem Java utiliza inteiros de 32 bits com sinal. A classe `Word24` encapsula um inteiro Java e aplica máscaras bit a bit (`& 0xFFFFFF`) no construtor, garantindo o truncamento dos valores para o teto de 24 bits.

*   **Prevenção de Extensão de Sinal:** O Java propaga o sinal negativo de bytes quando o bit mais significativo é 1. A constante `UNSIGNED_BYTE_MASK` (0xFF) aplica uma operação bit a bit para anular essa extensão e forçar a leitura do byte como um inteiro sem sinal (0 a 255).
*   **Tratamento de Complemento de 2 (Endereçamento Relativo):** O método `fromSigned12Bit()` isola os 12 bits do campo de deslocamento (*displacement*) utilizado nas instruções de Formato 3. Se o bit de sinal indicar valor negativo (salto para trás), a máscara `| 0xFFFFF000` estende os bits "1" para o espaço superior, garantindo a corretude da adição aritmética com o registrador PC. O método `toIntSigned()` aplica a extensão correspondente para 24 bits, retornando o valor com sinal estendido para 32 bits.
*   **Aritmética e Manipulação de Bytes:** Fornece métodos unificados para operações aritméticas (`add()`, `subtract()`) e leitura posicional (`getHighByte()`, `getMiddleByte()`, `getLowByte()`). O método `withLowByte()` viabiliza a substituição isolada do byte menos significativo, mantendo os bytes superiores intactos, fornecendo a base operacional para a instrução `LDCH`.
*   **Operações Lógicas e de Deslocamento:** Disponibiliza os métodos `bitwiseAnd()` e `bitwiseOr()` para efetuar as operações lógicas equivalentes exigidas pelas instruções `AND` e `OR`. Para manipulação em nível de bit, implementa `shiftLeftCircular()`, que executa a rotação circular truncando e realocando os bits dentro do espaço estrito de 24 bits (`SHIFTL`). O método `shiftRightArithmetic()` utiliza o operador aritmético de deslocamento nativo do Java (`>>`) sobre a representação com sinal estendido, garantindo que as posições vacantes à esquerda sejam preenchidas com o bit de sinal original, atendendo ao comportamento da instrução `SHIFTR`.

### 1.2. Enum `Register`
Mapeia os registradores definidos na especificação (A, X, L, B, S, T, F, PC e SW) para os seus respectivos códigos numéricos de hardware (0 a 9).
*   **Controle de Identificadores:** A substituição de variáveis inteiras por um Enum provê segurança de tipagem (type-safety). Opcodes lidos de instruções do Formato 2 são convertidos instanciando objetos tipados através de mapeamento (ex: `Register.fromCode()`).
*   **Registrador F:** O registrador de Ponto Flutuante (código 6, 48 bits) está mapeado para refletir a especificação original da arquitetura. No entanto, não é utilizado no processamento.

### 1.3. Enum `ConditionCode`
Enumera os três estados lógicos assumidos pela Palavra de Status (SW) após instruções de comparação (`COMP`, `COMPR`).
*   **Estados Definidos:** `LESS_THAN` (<), `EQUAL` (=) e `GREATER_THAN` (>).
*   **Abstração:** Substitui a manipulação de bits na Palavra de Status (SW) e expõe o resultado das comparações em alto nível, direcionando a lógica de controle de fluxo para as instruções de salto condicional (`JEQ`, `JLT`, `JGT`).

### 1.4. Enum `Opcode`
Mapeia as instruções suportadas pelo simulador para seus respectivos códigos hexadecimais e formatos originais (2 ou 3/4).
*   **Identificação com Máscara:** O método estático `fromMachineByte()` recebe o primeiro byte da instrução lida na memória. Para instruções dos Formatos 3 e 4, os dois bits menos significativos desse byte contêm as flags `n` e `i`. O método aplica a constante `FORMAT_3_4_OPCODE_MASK` (`0xFC`) para zerar esses bits, permitindo o casamento exato do valor lido com o opcode base. Se o byte lido não pertencer a nenhuma instrução mapeada, lança `IllegalArgumentException`.

### 1.5. Enum `AddressingMode`
Representa os modos de endereçamento da arquitetura SIC/XE aplicáveis aos Formatos 3 e 4.
*   **Mapeamento Semântico:** Traduz a combinação das flags `n` e `i` para os estados lógicos correspondentes: `IMMEDIATE` (n=0, i=1), `INDIRECT` (n=1, i=0), `SIMPLE` (n=1, i=1) e `SIC_STANDARD` (n=0, i=0).

---

## 2. Pacote `hardware`: Componentes de Hardware

O pacote estrutura os recursos físicos e a persistência de estado da máquina virtual.

### 2.1. Classe `Memory`
Representa a memória principal como um vetor unidimensional de bytes (`byte[] data`).

*   **Capacidade Máxima:** A estrutura aloca estritamente 1 MB (1.048.576 bytes). Este tamanho engloba o limite máximo de endereçamento da arquitetura definido pelas instruções de Formato 4, que suportam um endereço de destino de até 20 bits.
*   **Acesso Híbrido:** A classe disponibiliza métodos de acesso granular em dois níveis:
    *   **Nível de Byte:** `readByte()` e `writeByte()`, utilizados pelas instruções `LDCH` e `STCH`.
    *   **Nível de Palavra:** `readWord()` e `writeWord()`, que interagem nativamente instanciando uma `Word24`. A leitura resolve internamente a indexação de 3 bytes consecutivos.
*   **Mecanismo de Proteção:** Todo acesso físico de leitura ou escrita executa previamente o método `validateAddress()`. O cálculo de um Endereço Efetivo (TA) fora do espaço de 1 MB ou negativo acarreta o lançamento imediato de `IllegalArgumentException`.

### 2.2. Classe `RegisterBank`
O Banco de Registradores unifica o estado dos registradores primários da CPU em uma estrutura baseada em `EnumMap<Register, Word24>`.

*   **Bloqueio de Operações Restritas:** O registrador de Ponto Flutuante (`F`) é inicializado como nulo e isolado pelo método `validateRegister()`. A tentativa de leitura ou escrita no registrador `F` lança uma `UnsupportedOperationException`, abortando a execução para sinalizar operação inválida.
*   **Manipulação do PC:** Para otimizar as operações contínuas no ciclo de busca (*fetch*), a classe fornece os atalhos `getPC()` e `setPC()`. Estes métodos retornam e recebem inteiros sem sinal nativos, dispensando a conversão para `Word24` a cada incremento.
*   **Integração do Código de Condição (CC):** Utiliza as abstrações definidas no Enum `ConditionCode` por meio dos métodos `getConditionCode()` e `setConditionCode()`, armazenando o estado condicional sem alterar fisicamente bits operacionais no registrador SW.

---

## 3. Pacote `decoder`: Decodificação de Instruções

Isola a lógica responsável por interpretar os bytes lidos da memória, transformar bits isolados em estruturas tipadas e alimentar a unidade de execução.

### 3.1. Classe `DecodedInstruction`
Atua como um contêiner de dados (DTO) imutável que centraliza as propriedades decodificadas de uma instrução lida.

*   **Separação de Formatos:** Possui construtores distintos para acomodar a discrepância estrutural entre instruções. O construtor de Formato 2 inicializa exclusivamente os parâmetros de registradores (`r1` e `r2`). O construtor de Formatos 3 e 4 inicializa o modo de endereçamento, as flags de cálculo de endereço (`x`, `b`, `p`, `e`) e o valor de deslocamento/endereço, definindo os parâmetros do Formato 2 como nulos.
*   **Facilitador de Álgebra Relativa:** O método `getSignedDisplacement()` acessa o valor cru de deslocamento extraído (12 bits) e o retorna encapsulado através de `Word24.fromSigned12Bit()`. Isso entrega para a unidade de controle o valor pronto com o sinal estendido, essencial para a adição aritmética correta no endereçamento relativo ao PC.

### 3.2. Classe `InstructionDecoder`
Responsável pela leitura crua da memória e extração estruturada das flags lógicas e operandos, delegando o resultado para a instanciação de um objeto `DecodedInstruction`.

*   **Decodificação de Formato 2:** Para instruções de 2 bytes, a classe lê o segundo byte da memória e aplica operações de deslocamento de bits (`>>`) aliadas à constante `DISPLACEMENT_HIGH_NIBBLE_MASK` (`0x0F`) para extrair e mapear isoladamente os nibbles (4 bits) correspondentes aos registradores `r1` e `r2`.
*   **Extração de Flags (Formatos 3 e 4):** A identificação das diretrizes de endereçamento é feita via máscaras bit a bit (`&`). A classe extrai as flags `n` e `i` a partir dos dois bits menos significativos do primeiro byte (`FLAG_N_MASK`, `FLAG_I_MASK`), e as flags `x`, `b`, `p` e `e` a partir dos quatro bits mais significativos do segundo byte lido.
*   **Concatenação de Endereços/Deslocamentos:**
    *   **Formato 4:** Ativado quando a flag `e` é verdadeira. O decodificador consome 4 bytes da memória e concatena os 4 bits inferiores do segundo byte com os 16 bits dos bytes três e quatro. Este processo utiliza deslocamentos lógicos (`<<`) e operadores OR (`|`), resultando no endereço absoluto de 20 bits.
    *   **Formato 3:** Ativado quando a flag `e` é falsa. O decodificador consome 3 bytes e forma o deslocamento relativo de 12 bits combinando o nibble inferior do byte dois e a totalidade do byte três.