package src.model.types;

/**
 * Enumeração das instruções suportadas pelo simulador SIC/XE.
 */

public enum Opcode {
    // ---- INSTRUÇÕES DE FORMATO 2 ----
    ADDR(0x90, 2), SUBR(0x94, 2), MULR(0x98, 2), DIVR(0x9C, 2),
    COMPR(0xA0, 2), RMO(0xAC, 2), CLEAR(0xB4, 2), SHIFTL(0xA4, 2),
    SHIFTR(0xA8, 2), TIXR(0xB8, 2),

    // ---- INSTRUÇÕES DE FORMATOS 3 E 4 ----
    LDA(0x00, 3), LDX(0x04, 3), LDL(0x08, 3), LDB(0x68, 3),
    LDS(0x6C, 3), LDT(0x74, 3), LDCH(0x50, 3),
    
    STA(0x0C, 3), STX(0x10, 3), STL(0x14, 3), STB(0x78, 3),
    STS(0x7C, 3), STT(0x84, 3), STCH(0x54, 3),
    
    ADD(0x18, 3), SUB(0x1C, 3), MUL(0x20, 3), DIV(0x24, 3),
    AND(0x40, 3), OR(0x44, 3), COMP(0x28, 3),
    
    J(0x3C, 3), JEQ(0x30, 3), JGT(0x34, 3), JLT(0x38, 3),
    JSUB(0x48, 3), RSUB(0x4C, 3), TIX(0x2C, 3);

    /**
     * Máscara utilizada para isolar os 6 bits mais significativos do primeiro byte da instrução.
     * Nas instruções de Formato 3 e 4, os 2 bits menos significativos (bits 0 e 1) 
     * são ocupados pelas flags 'n' e 'i'. A operação bit a bit (byte & 0xFC) 
     * zera essas flags, permitindo comparar o valor limpo com o código hexadecimal original.
     */
    public static final int FORMAT_3_4_OPCODE_MASK = 0xFC;

    private final int code;
    private final int format;

    /**
     * Construtor do Opcode.
     * 
     * @param code Código hexadecimal da instrução.
     * @param format Formato base (2 para Formato 2; 3 para Formatos 3/4).
     */
    Opcode(int code, int format) {
        this.code = code;
        this.format = format;
    }

    public int getCode() { return code; }
    public int getFormat() { return format; }

    /**
     * Identifica a instrução a partir do primeiro byte lido da memória.
     * 
     * @param machineByte O primeiro byte da instrução.
     * @return O Opcode correspondente.
     * @throws IllegalArgumentException Se o byte não corresponder a nenhuma instrução suportada.
     */
    public static Opcode fromMachineByte(int machineByte) {
        // Tenta encontrar considerando Formato 2 (opcode utiliza os 8 bits completos)
        for (Opcode op : values()) {
            if (op.format == 2 && op.code == machineByte) {
                return op;
            }
        }
        
        // Tenta encontrar considerando Formato 3/4 (opcode utiliza apenas 6 bits)
        int maskedByte = machineByte & FORMAT_3_4_OPCODE_MASK;
        for (Opcode op : values()) {
            if (op.format == 3 && op.code == maskedByte) {
                return op;
            }
        }
        
        throw new IllegalArgumentException(String.format("Opcode desconhecido ou não suportado pelo simulador: %02X", machineByte));
    }
}