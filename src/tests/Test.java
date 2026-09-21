package src.tests;

import src.model.machine.SICXEMachine;
import src.model.hardware.Memory;
import src.model.types.Register;
import src.model.types.ConditionCode;

/**
 * Teste avançado do simulador SIC/XE.
 * Executa um laço de repetição que soma o valor 2 ao Acumulador 5 vezes,
 * utilizando registradores de índice (X), limites (T) e saltos condicionais (JLT).
 */
public class Test {

    public static void main(String[] args) {
        System.out.println("=== INICIANDO TESTE COMPLEXO (LOOP E BRANCHING) ===");
        
        SICXEMachine cpu = new SICXEMachine();
        Memory mem = cpu.getMemory();

        // 1. CLEAR X (Opcode B4, r1=X(1), r2=0) -> B4 10
        mem.writeByte(0x0000, 0xB4);
        mem.writeByte(0x0001, 0x10);

        // 2. LDA #0 (Opcode 00, Imediato -> 01, Disp -> 000) -> 01 00 00
        mem.writeByte(0x0002, 0x01);
        mem.writeByte(0x0003, 0x00);
        mem.writeByte(0x0004, 0x00);

        // 3. LDT #5 (Opcode 74, Imediato -> 75, Disp -> 005) -> 75 00 05
        mem.writeByte(0x0005, 0x75);
        mem.writeByte(0x0006, 0x00);
        mem.writeByte(0x0007, 0x05);

        // --- INÍCIO DO LOOP (Endereço 0x0008) ---

        // 4. ADD #2 (Opcode 18, Imediato -> 19, Disp -> 002) -> 19 00 02
        mem.writeByte(0x0008, 0x19);
        mem.writeByte(0x0009, 0x00);
        mem.writeByte(0x000A, 0x02);

        // 5. TIXR T (Opcode B8, r1=T(5), r2=0) -> B8 50
        mem.writeByte(0x000B, 0xB8);
        mem.writeByte(0x000C, 0x50);

        // 6. JLT 0x0008 (Opcode 38, Simples -> 3B, Disp/Addr -> 008) -> 3B 00 08
        mem.writeByte(0x000D, 0x3B);
        mem.writeByte(0x000E, 0x00);
        mem.writeByte(0x000F, 0x08);

        // --- FIM DO LOOP ---

        // 7. STA 0x0100 (Opcode 0C, Simples -> 0F, Disp/Addr -> 100) -> 0F 01 00
        mem.writeByte(0x0010, 0x0F);
        mem.writeByte(0x0011, 0x01);
        mem.writeByte(0x0012, 0x00);

        System.out.println("Programa injetado. Executando o laço...");

        int step = 1;
        while (cpu.getRegisters().getPC() < 0x0013) {
            int currentPC = cpu.getRegisters().getPC();
            cpu.step();
            
            String valA = cpu.getRegisters().get(Register.A).toHexString();
            String valX = cpu.getRegisters().get(Register.X).toHexString();
            ConditionCode cc = cpu.getRegisters().getConditionCode();
            
            System.out.printf("Passo %02d | PC Executado: %06X -> Novo PC: %06X | Reg A: %s | Reg X: %s | CC: %s\n", 
                              step, currentPC, cpu.getRegisters().getPC(), valA, valX, cc);
            step++;
            
            if (step > 30) {
                System.out.println("ERRO: Loop infinito detectado. O JLT não escapou corretamente.");
                break;
            }
        }

        // Validação Final
        String valorSalvo = mem.readWord(0x0100).toHexString();
        System.out.println("\n=== RESULTADO FINAL ===");
        System.out.println("Valor armazenado na memória no endereço 0x0100: " + valorSalvo);
        
        if (valorSalvo.equals("00000A")) {
            System.out.println("STATUS: SUCESSO ABSOLUTO! Saltos condicionais e iteradores funcionando!");
        } else {
            System.out.println("STATUS: FALHA! O valor final divergiu do esperado.");
        }
    }
}