package model.execution;

import model.decoder.DecodedInstruction;
import model.hardware.Memory;
import model.hardware.RegisterBank;
import model.types.AddressingMode;
import model.types.Register;
import model.types.Word24;

/**
 * Utilitário responsável por calcular o Endereço Efetivo (Target Address - TA)
 * de uma instrução dos Formatos 3 e 4, resolvendo deslocamentos, indexação e indireção.
 */
public class TargetAddressCalculator {

    /**
     * Calcula o endereço final na memória (TA) alvo da instrução atual,
     * aplicando as regras de endereçamento do SIC/XE baseadas nas flags (b, p, x, n, i).
     *
     * @param inst A instrução previamente decodificada contendo as flags.
     * @param registers O banco de registradores para leitura do PC, Base (B) ou Índice (X).
     * @param memory A memória principal, necessária para resolver ponteiros no modo indireto.
     * @return O inteiro representando o endereço efetivo (absoluto) final.
     * @throws IllegalArgumentException se as flags p e b estiverem ativas simultaneamente.
     */
    public int calculateTargetAddress(DecodedInstruction inst, RegisterBank registers, Memory memory) {
        // Instruções de Formato 2 operam apenas em registradores, não possuem TA
        if (inst.getFormat() == 2) {
            return 0; 
        }

        int targetAddress = 0;

        // 1. RESOLUÇÃO DO DESLOCAMENTO BASE (Flags P e B)
        if (inst.isPcRelative() && !inst.isBaseRelative()) {
            // p=1, b=0: PC-relativo. O deslocamento de 12 bits é tratado como um número COM SINAL (complemento de 2).
            // O valor estendido para 32 bits pela Word24 é somado ao PC atual.
            targetAddress = registers.getPC() + inst.getSignedDisplacement().toIntSigned();
            
        } else if (!inst.isPcRelative() && inst.isBaseRelative()) {
            // p=0, b=1: Base-relativo. O deslocamento é tratado como um número SEM SINAL (0 a 4095).
            targetAddress = registers.get(Register.B).toIntUnsigned() + inst.getDisplacementOrAddress();
            
        } else if (!inst.isPcRelative() && !inst.isBaseRelative()) {
            // p=0, b=0: Endereçamento direto/absoluto. Utilizado no Formato 4 (20 bits) ou no SIC Padrão.
            targetAddress = inst.getDisplacementOrAddress();
            
        } else {
            // p=1, b=1: O hardware SIC/XE trata essa combinação como erro, pois não há como ser relativo a ambos.
            throw new IllegalArgumentException("Erro de endereçamento: Flags PC-relativo (p) e Base-relativo (b) não podem estar ativas simultaneamente.");
        }

        // 2. INDEXAÇÃO (Flag X)
        if (inst.isIndexed()) {
            // Adiciona o valor contido no registrador X ao endereço calculado.
            targetAddress += registers.get(Register.X).toIntUnsigned();
        }

        // 3. SEGURANÇA FÍSICA: Trunca o endereço para o limite de endereçamento real
        // Garante que cálculos de retrocesso de PC não gerem endereços negativos vazando para o int do Java.
        targetAddress = targetAddress & Word24.MAX_MASK;

        // 4. RESOLUÇÃO DE INDIREÇÃO (Modo Indireto: n=1, i=0)
        if (inst.getAddressingMode() == AddressingMode.INDIRECT) {
            // O endereço que calculamos até aqui é apenas um ponteiro.
            // Precisamos ir à memória, ler a palavra de 24 bits nesse ponteiro,
            // e esse valor será o nosso verdadeiro Endereço Efetivo.
            targetAddress = memory.readWord(targetAddress).toIntUnsigned();
        }

        // Retorna o TA.
        // Nota: Para o modo Imediato (n=0, i=1), o próprio TA é o operando. O Executor saberá não ler a memória.
        return targetAddress;
    }
}