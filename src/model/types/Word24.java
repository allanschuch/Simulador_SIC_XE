package src.model.types;

/**
 * Encapsula uma palavra de 24 bits da arquitetura SIC/XE.
 * Esta classe blinda a simulação contra os detalhes de tipagem do Java (onde inteiros possuem 32 bits),
 * garantindo que todas as operações respeitem o limite exato de 24 bits (3 bytes) do hardware,
 * gerenciando automaticamente truncamentos e complemento de 2.
 */
public class Word24 {
    /** Máscara constante para garantir que apenas os 24 bits menos significativos sejam mantidos. */
    public static final int MAX_MASK = 0xFFFFFF; 

    /**
     * Máscara utilizada para anular a extensão de sinal automática do Java em operações com bytes.
     * Como o Java não possui tipos primitivos sem sinal (unsigned), um byte com o bit mais significativo
     * ativo é interpretado como um número negativo. Ao aplicarmos esta máscara (& 0xFF), 
     * forçamos o Java a tratar o valor estritamente como um inteiro sem sinal, variando de 0 a 255.
     */
    public static final int UNSIGNED_BYTE_MASK = 0xFF;

    // O valor interno é sempre mantido mascarado aos limites de 24 bits.
    private final int value;

    /**
     * Construtor padrão. Inicializa a palavra com o valor fornecido, 
     * truncando qualquer bit que exceda a 24ª posição.
     *
     * @param value O valor inteiro a ser encapsulado.
     */
    public Word24(int value) {
        this.value = value & MAX_MASK;
    }

    /**
     * Construtor que monta uma palavra de 24 bits a partir de 3 bytes independentes.
     * Assume a arquitetura Big-Endian, onde o primeiro byte é o mais significativo.
     * A constante UNSIGNED_BYTE_MASK garante que o Java trate os bytes como inteiros sem sinal,
     * impedindo a propagação de bits negativos durante o deslocamento (shift).
     *
     * @param highByte   O byte mais significativo (bits 16 a 23).
     * @param middleByte O byte intermediário (bits 8 a 15).
     * @param lowByte    O byte menos significativo (bits 0 a 7).
     */
    public Word24(int highByte, int middleByte, int lowByte) {
        this.value = ((highByte & UNSIGNED_BYTE_MASK) << 16) | 
                     ((middleByte & UNSIGNED_BYTE_MASK) << 8) | 
                     (lowByte & UNSIGNED_BYTE_MASK);
    }

    /**
     * Retorna o valor encapsulado como um inteiro simples (apenas os bits lógicos, sem tratar sinal).
     * Utilizado primordialmente para endereçamento lógico na memória.
     * 
     * @return O valor absoluto da palavra de 24 bits.
     */
    public int toIntUnsigned() {
        return value;
    }

    /**
     * Interpreta os 24 bits em complemento de 2 estendendo o sinal para 32 bits (int do Java).
     * <p>
     * <b>Por que é necessário:</b> No SIC/XE, o valor {@code 0xFFFFFF} representa o número <b>-1</b>.
     * Como o Java utiliza inteiros de 32 bits, se não estendermos o sinal, {@code 0x00FFFFFF}
     * seria avaliado erroneamente como <b>+16.777.215</b>. A máscara {@code | 0xFF000000} replica
     * o bit de sinal negativo nos bits 24..31, garantindo que o Java interprete o valor real como -1.
     * </p>
     *
     * @return O valor inteiro de 32 bits com sinal preservado [-8.388.608 a +8.388.607].
     */
    public int toIntSigned() {
        if ((value & 0x800000) != 0) { // Se o bit 23 estiver ativo (valor negativo em 24 bits)
            return value | 0xFF000000; // Estende o sinal preenchendo os 8 bits mais altos do int Java com 1
        }
        return value;
    }

    /**
     * Extrai o byte mais significativo (High Byte) desta palavra de 24 bits.
     * 
     * @return Um inteiro sem sinal (0 a 255) correspondente aos 8 bits mais altos.
     */
    public int getHighByte() {
        return (value >> 16) & UNSIGNED_BYTE_MASK;
    }

    /**
     * Extrai o byte intermediário (Middle Byte) desta palavra de 24 bits.
     * 
     * @return Um inteiro sem sinal (0 a 255) correspondente aos 8 bits centrais.
     */
    public int getMiddleByte() {
        return (value >> 8) & UNSIGNED_BYTE_MASK;
    }

    /**
     * Extrai o byte menos significativo (Low Byte) desta palavra de 24 bits.
     * 
     * @return Um inteiro sem sinal (0 a 255) correspondente aos 8 bits mais baixos.
     */
    public int getLowByte() {
        return value & UNSIGNED_BYTE_MASK;
    }

    /**
     * Cria uma nova Word24 realizando a adição aritmética com outra Word24.
     * O "overflow" para além dos 24 bits é natural e truncado de forma segura pelo construtor.
     *
     * @param other A palavra a ser somada.
     * @return Uma nova Word24 contendo o resultado truncado.
     */
    public Word24 add(Word24 other) {
        return new Word24(this.value + other.value);
    }

    /**
     * Cria uma nova Word24 realizando a subtração aritmética com outra Word24.
     *
     * @param other A palavra a ser subtraída.
     * @return Uma nova Word24 contendo o resultado da subtração truncado.
     */
    public Word24 subtract(Word24 other) {
        return new Word24(this.value - other.value);
    }

    /**
     * Cria uma nova Word24 mantendo os bytes superiores intactos, mas substituindo o byte menos significativo.
     * Essencial para a instrução LDCH (Load Character), que altera apenas o byte mais à direita.
     * 
     * @param lowByte O novo byte a ser inserido (será truncado para 8 bits).
     * @return Uma nova Word24 atualizada.
     */
    public Word24 withLowByte(int lowByte) {
        return new Word24(getHighByte(), getMiddleByte(), lowByte);
    }

    /**
     * Realiza a operação lógica AND bit a bit com outra palavra de 24 bits.
     * Utilizado pela instrução AND.
     * 
     * @param other O operando da memória.
     * @return O resultado do AND lógico.
     */
    public Word24 bitwiseAnd(Word24 other) {
        return new Word24(this.value & other.value);
    }

    /**
     * Realiza a operação lógica OR bit a bit com outra palavra de 24 bits.
     * Utilizado pela instrução OR.
     * 
     * @param other O operando da memória.
     * @return O resultado do OR lógico.
     */
    public Word24 bitwiseOr(Word24 other) {
        return new Word24(this.value | other.value);
    }

    /**
     * Realiza um deslocamento circular à esquerda (Left Circular Shift) em um espaço estrito de 24 bits.
     * Utilizado pela instrução SHIFTL.
     * 
     * @param n O número de bits a deslocar.
     * @return O valor deslocado circularmente.
     */
    public Word24 shiftLeftCircular(int n) {
        int nMod = n % 24; // Previne deslocamentos maiores que a própria palavra
        // Desloca para a esquerda e reinsere os bits que "caíram" pela esquerda na base à direita
        int shifted = ((this.value << nMod) | (this.value >>> (24 - nMod))) & MAX_MASK;
        return new Word24(shifted);
    }

    /**
     * Realiza um deslocamento aritmético à direita (Right Shift).
     * As posições vazias à esquerda são preenchidas com o bit de sinal original.
     * Utilizado pela instrução SHIFTR.
     * 
     * @param n O número de bits a deslocar.
     * @return O valor deslocado com sinal estendido.
     */
    public Word24 shiftRightArithmetic(int n) {
        // Ao converter para signed, o Java (que usa 32 bits) preenche o topo corretamente.
        // O operador aritmético '>>' mantém o sinal durante o deslocamento.
        int shifted = toIntSigned() >> n;
        return new Word24(shifted); // O construtor trunca de volta para 24 bits de forma segura
    }

    /**
     * Retorna a representação da palavra em Hexadecimal, padronizada rigorosamente com 6 dígitos.
     * Fundamental para a Interface Gráfica, logs e depuração visual.
     *
     * @return A string hexadecimal (ex: "001A3F").
     */
    public String toHexString() {
        return String.format("%06X", value);
    }

    /**
     * Constrói uma Word24 a partir de um valor de 12 bits (deslocamento/displacement) em complemento de 2.
     * Utilizado para o cálculo de Endereços Efetivos (TA) nas instruções de Formato 3.
     * <p>
     * <b>Por que é necessário:</b> No endereçamento relativo ao PC ({@code p=1}), o deslocamento é em complemento de 2 (-2048 a +2047).
     * Um salto para trás usa valor negativo (ex.: {@code 0xFFF} para -1). Se não estendermos o sinal,
     * o Java interpretará {@code 0x00000FFF} como <b>+4095</b>, fazendo a CPU saltar milhares de bytes
     * para a frente ao somar {@code PC + disp}, em vez de recuar. A máscara {@code | 0xFFFFF000}
     * propaga os 1s superiores e garante a aritmética correta.
     * </p>
     *
     * @param disp12 Valor numérico de 12 bits lido do campo displacement da instrução.
     * @return Uma {@link Word24} representando o valor com sinal estendido em 24 bits.
     */

    public static Word24 fromSigned12Bit(int disp12) {
        int maskedDisp = disp12 & 0xFFF; // Isola estritamente os 12 bits
        if ((maskedDisp & 0x800) != 0) { // Se o bit 11 (bit de sinal de 12 bits) for 1
            return new Word24(maskedDisp | 0xFFFFF000); // Estende o sinal negativo para todo o espaço superior
        }
        return new Word24(maskedDisp); // Retorna como positivo se o bit 11 for 0
    }
}
