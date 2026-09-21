package controller;

import model.machine.SICXEMachine;
import model.types.Register;
import model.types.Word24;

/**
 * Controlador da Máquina Virtual SIC/XE.
 * Orquestra os comandos do usuário (Run, Step, Pause, Reset) e os repassa
 * para o modelo de hardware. Gerencia a execução assíncrona em uma Thread
 * separada para garantir que a Interface Gráfica permaneça responsiva.
 */
public class MachineController {
    
    private final SICXEMachine machine;
    private MachineStateListener listener;
    
    // Variável volátil garante que a Thread de execução leia as alterações de pausa imediatamente
    private volatile boolean running;
    private Thread executionThread;
    
    // Tempo de atraso entre as instruções durante o modo "Run" (em milissegundos).
    // Permite que o usuário veja a execução fluindo.
    private int delayMs = 100;

    /**
     * Constrói o controlador injetando a dependência do núcleo da máquina.
     * 
     * @param machine A instância da máquina SIC/XE que será controlada.
     */
    public MachineController(SICXEMachine machine) {
        this.machine = machine;
        this.running = false;
    }

    /**
     * Define o ouvinte (geralmente a View) que receberá as notificações de atualização.
     * 
     * @param listener A classe que implementa as rotinas de atualização visual.
     */
    public void setMachineStateListener(MachineStateListener listener) {
        this.listener = listener;
    }

    /**
     * Configura a velocidade da simulação visual no modo de execução contínua.
     * 
     * @param delayMs Tempo de espera em milissegundos entre cada ciclo de instrução.
     */
    public void setDelayMs(int delayMs) {
        this.delayMs = delayMs;
    }

    /**
     * Executa exatamente um ciclo de instrução (Busca, Decodificação e Execução).
     * Notifica a View ao final do processo ou em caso de falha.
     */
    public void step() {
        try {
            machine.step();
            notifyStateChanged();
        } catch (Exception e) {
            pause();
            notifyError("Erro de Execução (PC: " + String.format("%06X", machine.getRegisters().getPC()) + ")\n" + e.getMessage());
        }
    }

    /**
     * Inicia a execução contínua da máquina em uma Thread separada (Background).
     * A execução persistirá até que o método pause() seja invocado ou um erro ocorra.
     */
    public void run() {
        if (running) {
            return; // Evita a criação de múltiplas Threads simultâneas
        }
        
        running = true;
        executionThread = new Thread(() -> {
            while (running) {
                step();
                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    running = false;
                }
            }
        });
        
        // Define como Daemon para que a Thread morra automaticamente se o usuário fechar a janela
        executionThread.setDaemon(true);
        executionThread.start();
    }

    /**
     * Interrompe a execução contínua da máquina de forma segura.
     */
    public void pause() {
        running = false;
        if (executionThread != null) {
            executionThread.interrupt();
        }
    }

    /**
     * Zera todos os registradores básicos da máquina, reiniciando o estado lógico.
     * (A memória principal não é apagada neste processo, preservando o código carregado).
     */
    public void reset() {
        pause();
        machine.getRegisters().set(Register.A, new Word24(0));
        machine.getRegisters().set(Register.X, new Word24(0));
        machine.getRegisters().set(Register.L, new Word24(0));
        machine.getRegisters().set(Register.B, new Word24(0));
        machine.getRegisters().set(Register.S, new Word24(0));
        machine.getRegisters().set(Register.T, new Word24(0));
        machine.getRegisters().setPC(0);
        
        notifyStateChanged();
    }

    /**
     * Método auxiliar interno para disparar notificações de mudança de estado,
     * caso um ouvinte tenha sido registrado.
     */
    private void notifyStateChanged() {
        if (listener != null) {
            listener.onMachineStateChanged();
        }
    }

    /**
     * Método auxiliar interno para disparar notificações de erro.
     * 
     * @param message A mensagem de erro capturada.
     */
    private void notifyError(String message) {
        if (listener != null) {
            listener.onMachineError(message);
        }
    }
}