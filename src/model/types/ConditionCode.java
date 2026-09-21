package src.model.types;

/**
 * Representa os Códigos de Condição (Condition Codes - CC) utilizados pela
 * arquitetura SIC/XE para avaliar saltos condicionais (JEQ, JLT, JGT).
 * Estes valores são armazenados internamente no registrador SW (Status Word).
 */
public enum ConditionCode {
    /** Indica que a comparação resultou em "Menor que" (Less Than) */
    LESS_THAN,
    /** Indica que a comparação resultou em "Igual" (Equal) */
    EQUAL,
    /** Indica que a comparação resultou em "Maior que" (Greater Than) */
    GREATER_THAN,
    /** Estado inicial, nenhuma comparação foi feita ainda */
    NONE
}