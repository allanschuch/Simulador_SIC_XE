# Documentação do Sistema SIC/XE (UFPel)
**Módulo:** Simulador da Máquina Virtual (Core)
**Fase 1:** Tipos Fundamentais e Representação de Dados

## 1. Visão Geral da Arquitetura (Padrão MVC)
Para garantir que a lógica da máquina (Modelo) seja independente da interface gráfica (Visão) exigida pelo projeto, o sistema foi estruturado seguindo o padrão MVC. A Máquina Virtual expõe contratos públicos puros (dados brutos e métodos de controle como `step()`), permitindo que um Controlador orquestre a execução sem congelar a Thread de interface (JavaFX/Swing).

## 2. Tipos Fundamentais (Pacote `types`)

O alicerce do simulador resolve o principal atrito estrutural entre a especificação de hardware do SIC/XE e a linguagem Java: a incompatibilidade de tamanhos de palavra.

### 2.1. Classe `Word24`
A arquitetura SIC/XE define a palavra de memória e os registradores padrão (A, X, L, B, S, T, PC, SW) com o tamanho exato de 24 bits (3 bytes). A linguagem Java, no entanto, não possui tipos primitivos de 24 bits ou tipos *unsigned*, tratando operandos inteiros como 32 bits com sinal. 

A classe `Word24` encapsula um inteiro Java e aplica máscaras bit a bit (`& 0xFFFFFF`) no construtor para garantir que qualquer valor gerado ou armazenado seja truncado ao limite físico da arquitetura hipotética.
*   **Justificativa Técnica:** Ao invés de espalhar operações bit a bit por toda a CPU, a lógica foi centralizada na classe `Word24`. Isso resolve de forma elegante o problema da extensão de sinal. Por exemplo, instruções do Formato 3 utilizam um deslocamento (*displacement*) de 12 bits. Quando é necessário somar esse valor negativo ao `PC` ou `Base`, o método `Word24.fromSigned12Bit()` faz a extensão do sinal de 12 para 24 bits corretamente antes da matemática acontecer.
*   **Responsabilidades:** Somar, subtrair, converter para representação com sinal (complemento de 2), e formatar para Hexadecimal de 6 dígitos.

### 2.2. Enum `Register`
O SIC/XE possui 9 registradores mapeados para códigos numéricos de 0 a 9 (onde o código 7 não é utilizado). O Enum `Register` vincula a representação semântica (A, B, X, PC) ao seu respectivo ID de hardware.
*   **Completude:** O registrador `F` (Ponto Flutuante), que possui 48 bits, foi mapeado no Enum (código 6) para respeitar a estrutura de hardware. Contudo, conforme delimitação do projeto, instruções matemáticas de ponto flutuante não serão implementadas.
*   **Justificativa Técnica:** O uso de um Enum no lugar de inteiros soltos para identificar registradores previne erros em tempo de compilação. Quando o decodificador lê uma instrução do Formato 2, como `ADDR r1, r2`, ele extrai os códigos numéricos de 4 bits e os converte imediatamente para `Register.fromCode()`. O Banco de Registradores, portanto, não trabalha com índices de array confusos, mas com chaves Enum estritamente tipadas.

### 2.3. Enum `ConditionCode`
Responsável por mapear os três estados lógicos que a Palavra de Status (SW) pode assumir após instruções de comparação (`COMP`, `COMPR`).
*   **Estados:** `LESS_THAN` (<), `EQUAL` (=) e `GREATER_THAN` (>).
*   **Justificativa Técnica:** No hardware real, o Condition Code (CC) ocupa bits específicos do registrador SW. Para fins de clareza do simulador, a extração binária desses bits isolados foi abstraída através do `ConditionCode`. Isso torna a semântica das instruções de salto condicional (`JEQ`, `JLT`, `JGT`) diretamente integrável à lógica em alto nível.