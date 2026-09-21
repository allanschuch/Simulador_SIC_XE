import model.machine.SICXEMachine;
import model.hardware.Memory;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Teste automatizado do núcleo de execução do SIC/XE.
 * Valida o comportamento de laços, saltos condicionais (JLT) e indexação (TIXR).
 */
public class VMTest {

    @Test
    public void testLoopAndBranching() {
        SICXEMachine cpu = new SICXEMachine();
        Memory mem = cpu.getMemory();

        // 1. CLEAR X (Opcode B4, r1=X(1), r2=0) -> B4 10
        mem.writeByte(0x0000, 0xB4); mem.writeByte(0x0001, 0x10);

        // 2. LDA #0 (Opcode 00, Imediato -> 01, Disp -> 000) -> 01 00 00
        mem.writeByte(0x0002, 0x01); mem.writeByte(0x0003, 0x00); mem.writeByte(0x0004, 0x00);

        // 3. LDT #5 (Opcode 74, Imediato -> 75, Disp -> 005) -> 75 00 05
        mem.writeByte(0x0005, 0x75); mem.writeByte(0x0006, 0x00); mem.writeByte(0x0007, 0x05);

        // --- INÍCIO DO LOOP (Endereço 0x0008) ---

        // 4. ADD #2 (Opcode 18, Imediato -> 19, Disp -> 002) -> 19 00 02
        mem.writeByte(0x0008, 0x19); mem.writeByte(0x0009, 0x00); mem.writeByte(0x000A, 0x02);

        // 5. TIXR T (Opcode B8, r1=T(5), r2=0) -> B8 50
        mem.writeByte(0x000B, 0xB8); mem.writeByte(0x000C, 0x50);

        // 6. JLT 0x0008 (Opcode 38, Simples -> 3B, Disp/Addr -> 008) -> 3B 00 08
        mem.writeByte(0x000D, 0x3B); mem.writeByte(0x000E, 0x00); mem.writeByte(0x000F, 0x08);

        // 7. STA 0x0100 (Opcode 0C, Simples -> 0F, Disp/Addr -> 100) -> 0F 01 00
        mem.writeByte(0x0010, 0x0F); mem.writeByte(0x0011, 0x01); mem.writeByte(0x0012, 0x00);

        // Executa até que o PC passe do endereço da instrução STA (0x0012)
        int step = 1;
        while (cpu.getRegisters().getPC() < 0x0013) {
            cpu.step();
            step++;
            if (step > 30) {
                fail("Loop infinito detectado. A instrução JLT falhou ao avaliar o Código de Condição.");
            }
        }

        // Asserção: Verifica se o valor armazenado na memória corresponde ao cálculo (2 * 5 = 10 -> 0x0A)
        String valorSalvo = mem.readWord(0x0100).toHexString();
        assertEquals("00000A", valorSalvo, "Falha na execução do laço: o valor final em memória divergiu.");
    }
}