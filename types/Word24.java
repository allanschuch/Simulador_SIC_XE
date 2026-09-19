package types;

/**
 * Encapsula uma palavra de 24 bits da arquitetura SIC/XE.
 * Esta classe blinda a simulação contra os detalhes de tipagem do Java (onde inteiros possuem 32 bits),
 * garantindo que todas as operações respeitem o limite exato de 24 bits (3 bytes) do hardware,
 * gerenciando automaticamente truncamentos e complemento de 2.
 */
public class Word24 {
    /** Máscara constante para garantir que apenas os 24 bits menos significativos sejam mantidos. */
    public static final int MAX_MASK = 0xFFFFFF; 

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
