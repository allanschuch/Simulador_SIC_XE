package hardware;

import types.Word24;

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
     * @return O valor do byte lido como um inteiro sem sinal (0 a 255).
     * @throws IllegalArgumentException se o endereço estiver fora dos limites da memória.
     */
    public int readByte(int address) {
        validateAddress(address);
        // Aplica a máscara 0xFF para garantir que o byte retornado seja tratado 
        // como valor sem sinal (unsigned) no Java.
        return data[address] & 0xFF; 
    }

    /**
     * Escreve um único byte (8 bits) no endereço especificado.
     * 
     * @param address O endereço de memória (0 a 1.048.575).
     * @param value O valor a ser escrito (será truncado para 8 bits).
     * @throws IllegalArgumentException se o endereço estiver fora dos limites da memória.
     */
    public void writeByte(int address, int value) {
        validateAddress(address);
        data[address] = (byte) (value & 0xFF);
    }

    /**
     * Lê uma palavra de 24 bits (3 bytes) a partir do endereço especificado.
     * O SIC/XE define a palavra de memória como 24 bits.
     * 
     * @param address O endereço inicial da memória.
     * @return Uma instância de Word24 contendo o valor lido.
     */
    public Word24 readWord(int address) {
        int b1 = readByte(address);       // Byte mais significativo (Big-Endian)
        int b2 = readByte(address + 1);
        int b3 = readByte(address + 2);   // Byte menos significativo

        int combined = (b1 << 16) | (b2 << 8) | b3;
        return new Word24(combined);
    }

    /**
     * Escreve uma palavra de 24 bits (3 bytes) a partir do endereço especificado.
     * 
     * @param address O endereço inicial da memória.
     * @param word A instância de Word24 a ser armazenada.
     */
    public void writeWord(int address, Word24 word) {
        int value = word.toIntUnsigned();
        
        writeByte(address, (value >> 16) & 0xFF);     // Isola e escreve os 8 bits superiores
        writeByte(address + 1, (value >> 8) & 0xFF);  // Isola e escreve os 8 bits do meio
        writeByte(address + 2, value & 0xFF);         // Isola e escreve os 8 bits inferiores
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
