package view;

import controller.MachineController;
import controller.MachineStateListener;
import model.hardware.Memory;
import model.machine.SICXEMachine;
import model.types.Register;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;

/**
 * Interface Gráfica (View) do Simulador SIC/XE.
 * Construída em Java Swing. Implementa o padrão Observer
 * (MachineStateListener) para reagir assincronamente às mudanças da CPU
 * comandadas pelo Controller.
 */
public class SICXEView extends JFrame implements MachineStateListener {

    private final SICXEMachine machine;
    private final MachineController controller;

    private JTable memoryTable;
    private JTable registerTable;
    private MemoryTableModel memoryModel;
    private RegisterTableModel registerModel;
    private JSlider speedSlider;

    /**
     * Constrói e inicializa os componentes visuais do simulador.
     * 
     * @param machine A instância do Model (para leitura de dados).
     * @param controller A instância do Controller (para envio de comandos).
     */
    public SICXEView(SICXEMachine machine, MachineController controller) {
        this.machine = machine;
        this.controller = controller;

        // Assina esta View para receber eventos do Controller
        this.controller.setMachineStateListener(this);

        setTitle("Simulador SIC/XE - Grupo 6");
        setSize(900, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        initComponents();
    }

    /**
     * Inicializa e posiciona os painéis (Memória, Registradores e Controles).
     */
    private void initComponents() {
        // --- 1. PAINEL DE REGISTRADORES (Esquerda) ---
        registerModel = new RegisterTableModel(machine);
        registerTable = new JTable(registerModel);
        registerTable.setRowHeight(30);
        registerTable.setFont(new Font("Monospaced", Font.BOLD, 14));
        
        JScrollPane registerScroll = new JScrollPane(registerTable);
        registerScroll.setPreferredSize(new Dimension(300, 0));
        registerScroll.setBorder(BorderFactory.createTitledBorder("Banco de Registradores"));
        add(registerScroll, BorderLayout.WEST);

        // --- 2. PAINEL DE MEMÓRIA (Centro) ---
        memoryModel = new MemoryTableModel(machine.getMemory());
        memoryTable = new JTable(memoryModel);
        memoryTable.setFont(new Font("Monospaced", Font.PLAIN, 14));
        
        JScrollPane memoryScroll = new JScrollPane(memoryTable);
        memoryScroll.setBorder(BorderFactory.createTitledBorder("Memória Principal (1MB)"));
        add(memoryScroll, BorderLayout.CENTER);

        // --- 3. PAINEL DE CONTROLES (Inferior) ---
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        
        JButton btnLoadHex = new JButton("Carregar Hex(Teste)");
        JButton btnStep = new JButton("Step");
        JButton btnRun = new JButton("Run");
        JButton btnPause = new JButton("Pause");
        JButton btnReset = new JButton("Reset");

        // Slider para controlar a velocidade da simulação (Escala de 0 a 100)
        // Valor inicial: 50 (Metade da barra)
        speedSlider = new JSlider(0, 100, 50);
        speedSlider.setToolTipText("Velocidade de Execução");
        
        // Listener que converte o valor visual (0 a 100) para Delay em milissegundos
        speedSlider.addChangeListener(e -> {
            int speedValue = speedSlider.getValue();
            int delayMs = 20 + (100 - speedValue) * 10;
            controller.setDelayMs(delayMs);
        });
        
        // Sincroniza o Controller com o valor inicial do Slider (50 -> 520ms) no momento da criação
        controller.setDelayMs(20 + (100 - speedSlider.getValue()) * 10);

        // Vinculando os botões aos métodos do Controller
        btnLoadHex.addActionListener(e -> openPseudoLoaderDialog());
        btnStep.addActionListener(e -> controller.step());
        btnRun.addActionListener(e -> controller.run());
        btnPause.addActionListener(e -> controller.pause());
        btnReset.addActionListener(e -> controller.reset());

        controlPanel.add(btnLoadHex);
        controlPanel.add(btnStep);
        controlPanel.add(btnRun);
        controlPanel.add(btnPause);
        controlPanel.add(btnReset);
        controlPanel.add(new JLabel("Velocidade:"));
        controlPanel.add(speedSlider);

        add(controlPanel, BorderLayout.SOUTH);
    }

    /**
     * Abre uma janela de diálogo para o usuário colar o código de máquina
     * em formato hexadecimal. O input é repassado ao Controller.
     */
    private void openPseudoLoaderDialog() {
        JTextArea textArea = new JTextArea(10, 30);
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        textArea.setLineWrap(true);
        JScrollPane scrollPane = new JScrollPane(textArea);

        int result = JOptionPane.showConfirmDialog(
                this, scrollPane, "Cole o código de máquina (Hexadecimal)",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE
        );

        if (result == JOptionPane.OK_OPTION) {
            String hexCode = textArea.getText();
            controller.loadHexCode(hexCode);
            // Faz a tabela de memória voltar para o endereço 0x0000
            memoryTable.scrollRectToVisible(memoryTable.getCellRect(0, 0, true));
        }
    }

    // ==========================================
    // MÉTODOS DO MACHINE_STATE_LISTENER
    // ==========================================

    @Override
    public void onMachineStateChanged() {
        // As Threads em background não podem alterar componentes visuais no Swing.
        // O invokeLater injeta a atualização na Thread correta de Eventos da Interface Gráfica.
        SwingUtilities.invokeLater(() -> {
            registerModel.fireTableDataChanged();
            memoryModel.fireTableDataChanged();
            
            // Foco automático na tabela de memória seguindo o Program Counter (PC)
            int currentPC = machine.getRegisters().getPC();
            if (currentPC >= 0 && currentPC < Memory.MAX_MEMORY_SIZE) {
                memoryTable.setRowSelectionInterval(currentPC, currentPC);
                memoryTable.scrollRectToVisible(memoryTable.getCellRect(currentPC, 0, true));
            }
        });
    }

    @Override
    public void onMachineError(String errorMessage) {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(this, errorMessage, "Erro de Execução", JOptionPane.ERROR_MESSAGE);
        });
    }

    // ==========================================
    // MODELOS DE DADOS OTIMIZADOS
    // ==========================================

    /**
     * Modelo de tabela para ler a memória sem alocar componentes visuais
     * para 1 Milhão de endereços, evitando estouro de memória (OutOfMemory).
     */
    private static class MemoryTableModel extends AbstractTableModel {
        private final Memory memory;
        private final String[] columns = {"Endereço (Hex)", "Byte (Hex)", "Byte (Dec)"};

        public MemoryTableModel(Memory memory) {
            this.memory = memory;
        }

        @Override
        public int getRowCount() { return Memory.MAX_MEMORY_SIZE; }

        @Override
        public int getColumnCount() { return columns.length; }

        @Override
        public String getColumnName(int column) { return columns[column]; }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            int byteValue = memory.readByte(rowIndex);
            return switch (columnIndex) {
                case 0 -> String.format("0x%05X", rowIndex);
                case 1 -> String.format("%02X", byteValue);
                case 2 -> String.valueOf(byteValue);
                default -> "";
            };
        }
    }

    /**
     * Modelo de tabela para listar os registradores atuais da CPU.
     */
    private static class RegisterTableModel extends AbstractTableModel {
        private final SICXEMachine machine;
        private final Register[] registers = {
            Register.A, Register.X, Register.L, Register.B, Register.S, Register.T, Register.PC
        };
        private final String[] columns = {"Registrador", "Valor (Hex)", "Valor (Dec)"};

        public RegisterTableModel(SICXEMachine machine) {
            this.machine = machine;
        }

        @Override
        public int getRowCount() { return registers.length + 1; } // +1 para a Status Word (Condições)

        @Override
        public int getColumnCount() { return columns.length; }

        @Override
        public String getColumnName(int column) { return columns[column]; }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if (rowIndex == registers.length) {
                return switch (columnIndex) {
                    case 0 -> "SW (CC)";
                    case 1, 2 -> machine.getRegisters().getConditionCode().name();
                    default -> "";
                };
            }

            Register reg = registers[rowIndex];
            int valueUnsigned = machine.getRegisters().get(reg).toIntUnsigned();
            int valueSigned = machine.getRegisters().get(reg).toIntSigned();

            return switch (columnIndex) {
                case 0 -> reg.name();
                case 1 -> String.format("%06X", valueUnsigned);
                case 2 -> String.valueOf(valueSigned);
                default -> "";
            };
        }
    }
}