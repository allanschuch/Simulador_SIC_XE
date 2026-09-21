package src.model.hardware;

import src.model.types.Word24;

/**
 * Representa a Memória Principal da máquina hipotética SIC/XE.
 * A memória é estruturada como um array contínuo de bytes (8 bits).
 * 
 * A instrução de Formato 4 suporta um endereço de até 20 bits.
 * Portanto, esta simulação aloca 2^20 bytes (1 Megabyte) de memória, 
 * superando o requisito mínimo de 1 KB.
 */
public class Memory {
    
    /** 
     * Tamanho total da memória: 1 MB (1.048.576 bytes).
     * Cobre totalmente o espaço de endereçamento de 20 bits do Formato 4.
     */
    public static final int MAX_MEMORY_SIZE = 1048576; 
    
    private final byte[] data;

    /**
     * Construtor padrão que inicializa a memória com o tamanho máximo (1 MB)
     * e zera todos os seus endereços.
     */
    public Memory() {
        this.data = new byte[MAX_MEMORY_SIZE];
    }

    /**
     * Lê um único byte (8 bits) do endereço especificado.
     * 
     * @param address O endereço de memória (0 a 1.048.575).
     * @return O valor do byte lido tratado como um inteiro sem sinal (0 a 255).
     * @throws IllegalArgumentException se o endereço estiver fora dos limites da memória.
     */
    public int readByte(int address) {
        validateAddress(address);
        // Utiliza a máscara para que o Java trate o byte lido como um inteiro sem sinal,
        // evitando problemas de extensão de sinal negativo.
        return data[address] & Word24.UNSIGNED_BYTE_MASK; 
    }

    /**
     * Escreve um único byte (8 bits) no endereço especificado.
     * 
     * @param address O endereço de memória (0 a 1.048.575).
     * @param value O valor a ser escrito.
     * @throws IllegalArgumentException se o endereço estiver fora dos limites da memória.
     */
    public void writeByte(int address, int value) {
        validateAddress(address);
        // A máscara garante que apenas os 8 bits menos significativos sejam gravados.
        data[address] = (byte) (value & Word24.UNSIGNED_BYTE_MASK);
    }

    /**
     * Lê uma palavra de 24 bits (3 bytes) a partir do endereço especificado.
     * O SIC/XE define a palavra de memória como 24 bits.
     * 
     * @param address O endereço inicial da memória.
     * @return Uma instância de Word24 contendo o valor lido.
     */
    public Word24 readWord(int address) {
        return new Word24(
            readByte(address),      // Byte mais significativo (High)
            readByte(address + 1),  // Byte intermediário (Middle)
            readByte(address + 2)   // Byte menos significativo (Low)
        );
    }

    /**
     * Escreve uma palavra de 24 bits (3 bytes) a partir do endereço especificado.
     * Desmembra a palavra utilizando as funções de abstração da Word24.
     * 
     * @param address O endereço inicial da memória.
     * @param word A instância de Word24 a ser armazenada.
     */
    public void writeWord(int address, Word24 word) {
        writeByte(address, word.getHighByte());
        writeByte(address + 1, word.getMiddleByte());
        writeByte(address + 2, word.getLowByte());
    }

    /**
     * Valida se o endereço solicitado está dentro do espaço físico da memória.
     * 
     * @param address O endereço a ser validado.
     * @throws IllegalArgumentException se ocorrer Violação de Acesso (Access Violation).
     */
    private void validateAddress(int address) {
        if (address < 0 || address >= MAX_MEMORY_SIZE) {
            throw new IllegalArgumentException(
                String.format("Violação de Acesso à Memória. Endereço fora dos limites: %06X", address)
            );
        }
    }
}
