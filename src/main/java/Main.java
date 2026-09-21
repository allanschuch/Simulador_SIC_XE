import controller.MachineController;
import model.machine.SICXEMachine;
import view.SICXEView;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Ponto de entrada (Entry Point) do Simulador SIC/XE.
 * Responsável por orquestrar a inicialização das camadas da arquitetura MVC (Model, Controller, View)
 * e garantir que a interface gráfica seja construída na Thread correta (Event Dispatch Thread).
 */
public class Main {

    /**
     * Método principal que inicia a execução da Máquina Virtual.
     * 
     * @param args Argumentos de linha de comando (não são necessários para este simulador).
     */
    public static void main(String[] args) {
        // Tenta forçar o Swing a usar o design nativo do sistema operacional do usuário (Windows/Linux)
        // em vez do visual padrão antiquado do Java (Metal).
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.err.println("Aviso: Não foi possível aplicar o tema nativo do sistema. Usando tema padrão.");
        }

        // As regras do Java Swing exigem que a interface gráfica seja criada e manipulada
        // exclusivamente na Event Dispatch Thread (EDT) para evitar problemas de concorrência.
        SwingUtilities.invokeLater(() -> {
            try {
                // 1. Model: Inicializa o núcleo da CPU e a memória de 1MB.
                SICXEMachine machine = new SICXEMachine();

                // 2. Controller: Inicializa a lógica de controle repassando a referência do Model.
                MachineController controller = new MachineController(machine);

                // 3. View: Constrói a janela gráfica injetando o Model (para leitura) e o Controller (para ações).
                SICXEView view = new SICXEView(machine, controller);

                // Exibe a interface gráfica na tela.
                view.setVisible(true);
                
            } catch (Exception e) {
                System.err.println("Falha crítica ao inicializar o Simulador SIC/XE: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
}