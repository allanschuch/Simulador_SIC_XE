package controller;

/**
 * Interface de escuta (Observer) para os eventos de estado da Máquina Virtual.
 * Permite que a Interface Gráfica (View) seja notificada das mudanças ocorridas
 * no hardware (registradores, memória) sem que haja acoplamento direto com a lógica da CPU.
 */
public interface MachineStateListener {
    
    /**
     * Invocado pelo Controller sempre que a CPU completa um ciclo de instrução 
     * com sucesso (seja passo a passo ou em execução contínua).
     */
    void onMachineStateChanged();

    /**
     * Invocado pelo Controller quando a máquina encontra uma instrução inválida,
     * violação de memória ou qualquer erro fatal de execução.
     * 
     * @param errorMessage A descrição do erro ocorrido.
     */
    void onMachineError(String errorMessage);
}