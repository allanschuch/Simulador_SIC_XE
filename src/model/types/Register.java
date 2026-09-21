package src.model.types;

/**
 * Enumeração dos registradores da arquitetura SIC/XE.
 * Mapeia cada registrador para o seu respectivo código numérico (0 a 9)
 * conforme a especificação do sistema para as instruções de Formato 2.
 */
public enum Register {
    /** Acumulador: usado para operações lógicas e aritméticas (24 bits). */
    A(0),
    /** Registrador de Índice: usado para endereçamento indexado (24 bits). */
    X(1),
    /** Registrador de Ligação: armazena endereço de retorno para subrotinas como JSUB (24 bits). */
    L(2),
    /** Registrador Base: usado para endereçamento base-relativo (24 bits). */
    B(3),
    /** Registrador de Uso Geral S (24 bits). */
    S(4),
    /** Registrador de Uso Geral T (24 bits). */
    T(5),
    /** Acumulador de Ponto Flutuante (48 bits - não é utilizado na simulação). */
    F(6),
    /** Contador de Instruções: contém o endereço da próxima instrução (24 bits). */
    PC(8),
    /** Palavra de Status: contém informações de estado, como o Condition Code (24 bits). */
    SW(9);

    private final int code;

    /**
     * @param code O código numérico em hardware do registrador.
     */
    Register(int code) {
        this.code = code;
    }

    /**
     * Retorna o código numérico embutido na instrução (Formato 2).
     * @return O valor inteiro de 0 a 9 correspondente ao registrador.
     */
    public int getCode() {
        return code;
    }

    /**
     * Recupera um registrador a partir do seu código numérico de máquina.
     * Útil no decodificador para as instruções de Formato 2.
     * 
     * @param code O código do registrador (ex: 0 retorna Register.A).
     * @return A constante Register correspondente.
     * @throws IllegalArgumentException se o código fornecido não existir na arquitetura.
     */
    public static Register fromCode(int code) {
        for (Register r : values()) {
            if (r.code == code) {
                return r;
            }
        }
        throw new IllegalArgumentException("Código de registrador inválido ou inexistente: " + code);
    }
}
