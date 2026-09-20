package hardware;

import types.Register;
import types.ConditionCode;
import types.Word24;
import java.util.EnumMap;

/**
 * Representa o Banco de Registradores do processador SIC/XE.
 * São 7 registradores de 24 bits (A, X, L, B, S, T, SW) 
 * e o Contador de Instruções (PC) de 24 bits.
 * 
 * O registrador de Ponto Flutuante (F) possui 48 bits na especificação, 
 * mas, por delimitação do projeto, não será simulado.
 */
public class RegisterBank {
    
    private final EnumMap<Register, Word24> registers;
    
    /**
     * Representação de alto nível do Código Condicional (CC).
     * No hardware real, o CC ocupa bits específicos dentro da Palavra de Status (SW).
     * Aqui, utilizamos uma abstração.
     */
    private ConditionCode currentConditionCode;

    /**
     * Construtor padrão. Inicializa todos os registradores da arquitetura com o valor zero.
     */
    public RegisterBank() {
        this.registers = new EnumMap<>(Register.class);
        for (Register reg : Register.values()) {
            // Inicializa todos com 0, exceto F, que será mantido como null para garantir que 
            // operações indevidas não passem despercebidas.
            if (reg != Register.F) {
                registers.put(reg, new Word24(0));
            }
        }
        this.currentConditionCode = ConditionCode.NONE;
    }

    /**
     * Recupera o valor atual de um registrador específico.
     * 
     * @param reg O registrador desejado (Enum).
     * @return A instância de Word24 contida no registrador.
     * @throws UnsupportedOperationException se for solicitada a leitura do registrador F.
     */
    public Word24 get(Register reg) {
        validateRegister(reg);
        return registers.get(reg);
    }

    /**
     * Altera o valor de um registrador específico.
     * 
     * @param reg O registrador de destino (Enum).
     * @param value O novo valor a ser armazenado (Word24).
     * @throws UnsupportedOperationException se for solicitada a escrita no registrador F.
     */
    public void set(Register reg, Word24 value) {
        validateRegister(reg);
        registers.put(reg, value);
    }

    /**
     * Retorna o endereço armazenado no Contador de Instruções (PC).
     * 
     * @return O valor inteiro (sem sinal) do PC.
     */
    public int getPC() {
        return get(Register.PC).toIntUnsigned();
    }

    /**
     * Define o novo endereço do Contador de Instruções (PC).
     * 
     * @param address O endereço de memória para o qual o PC deve apontar.
     */
    public void setPC(int address) {
        set(Register.PC, new Word24(address));
    }

    /**
     * Atualiza o Código de Condição após operações de comparação (COMP, COMPR).
     * 
     * @param cc O novo estado condicional (LESS_THAN, EQUAL, GREATER_THAN).
     */
    public void setConditionCode(ConditionCode cc) {
        this.currentConditionCode = cc;
    }

    /**
     * Recupera o Código de Condição atual, utilizado pelas instruções de salto 
     * condicional (JEQ, JLT, JGT).
     * 
     * @return O estado condicional vigente.
     */
    public ConditionCode getConditionCode() {
        return currentConditionCode;
    }

    /**
     * Garante que o registrador de Ponto Flutuante (F) não seja utilizado acidentalmente.
     * 
     * @param reg O registrador a ser validado.
     * @throws UnsupportedOperationException se o registrador for F.
     */
    private void validateRegister(Register reg) {
        if (reg == Register.F) {
            throw new UnsupportedOperationException(
                "O registrador de Ponto Flutuante (F) não deve ser utilizado."
            );
        }
    }
}
