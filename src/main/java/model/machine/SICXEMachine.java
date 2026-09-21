package model.machine;

import model.decoder.DecodedInstruction;
import model.decoder.InstructionDecoder;
import model.execution.Executor;
import model.execution.TargetAddressCalculator;
import model.hardware.Memory;
import model.hardware.RegisterBank;
import model.types.Register;
import model.types.Word24;

/**
 * Fachada principal do simulador SIC/XE (O "Model" do padrão MVC).
 * Esta classe encapsula todos os componentes de hardware e as unidades lógicas,
 * fornecendo uma API limpa para o Controlador coordenar a execução sem acoplamento.
 */
public class SICXEMachine {

    private final Memory memory;
    private final RegisterBank registers;
    
    private final InstructionDecoder decoder;
    private final TargetAddressCalculator taCalculator;
    private final Executor executor;

    /**
     * Construtor padrão da máquina.
     * Inicializa o hardware (Memória e Banco de Registradores) e as unidades lógicas.
     */
    public SICXEMachine() {
        this.memory = new Memory();
        this.registers = new RegisterBank();
        
        this.decoder = new InstructionDecoder();
        this.taCalculator = new TargetAddressCalculator();
        this.executor = new Executor(this.memory, this.registers);
    }

    /**
     * Executa exatamente um ciclo de máquina (Busca, Decodificação e Execução).
     */
    public void step() {
        // 1. FETCH (Busca e Decodificação preliminar)
        int currentPC = registers.getPC();
        DecodedInstruction inst = decoder.decode(memory, currentPC);
        
        // 2. AVANÇO DO PC
        // O SIC/XE avança o PC logo após a busca. Isso é importante por dois motivos:
        //
        // a) SUB-ROTINAS (JSUB / RSUB):
        //    O JSUB salva (PC) no registrador L como endereço de retorno (L <- PC; PC <- m).
        //    Se o PC não estivesse apontando para a instrução SEGUINTE (currentPC + tamanho),
        //    o RSUB (PC <- L) retornaria para a própria chamada JSUB, gerando um loop infinito.
        //
        // b) ENDEREÇAMENTO PC-RELATIVO:
        //    A fórmula do hardware é TA = (PC) + disp. O montador calcula o campo 'disp'
        //    tomando como base a próxima instrução, exigindo o PC já incrementado.
        int nextPC = currentPC + inst.getFormat();
        registers.setPC(nextPC);
        
        // 3. CÁLCULO DE ENDEREÇO EFETIVO (TA)
        // O decodificador preencheu as flags, e o TA Calculator resolve o endereçamento.
        int targetAddress = taCalculator.calculateTargetAddress(inst, registers, memory);
        
        // 4. EXECUÇÃO
        // Entrega a instrução e o endereço final pronto para a Unidade de Execução.
        executor.execute(inst, targetAddress);
    }

    /**
     * Reinicia a máquina para o seu estado original.
     * Zera a memória e os registradores, preparando a CPU para a carga de um novo programa.
     */
    public void reset() {
        // Zera os registradores
        for (Register reg : Register.values()) {
            if (reg != Register.F) {
                registers.set(reg, new Word24(0));
            }
        }
        registers.setConditionCode(model.ConditionCode.NONE);
        
        // Zera a memória
        for (int i = 0; i < Memory.MAX_MEMORY_SIZE; i++) {
            memory.writeByte(i, 0);
        }
    }

    /**
     * Expõe a memória para leitura por módulos externos.
     * Para o Controller atualizar a Interface Gráfica e para o Carregador.
     * 
     * @return A instância de Memory associada a esta máquina.
     */
    public Memory getMemory() {
        return memory;
    }

    /**
     * Expõe o banco de registradores para leitura por módulos externos.
     * Para a Interface Gráfica inspecionar o estado da CPU após cada passo.
     * 
     * @return A instância de RegisterBank associada a esta máquina.
     */
    public RegisterBank getRegisters() {
        return registers;
    }
}