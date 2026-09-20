package types;

/**
 * Representa os Modos de Endereçamento da arquitetura SIC/XE
 * para instruções dos Formatos 3 e 4, definidos pelas flags 'n' e 'i'.
 */
public enum AddressingMode {
    /** n=0, i=1: O operando é o próprio valor embutido na instrução. */
    IMMEDIATE,
    
    /** n=1, i=0: O endereço calculado aponta para outro endereço que contém o operando. */
    INDIRECT,
    
    /** n=1, i=1: O endereço calculado aponta diretamente para o operando. */
    SIMPLE,
    
    /** n=0, i=0: Compatibilidade com a máquina SIC padrão. */
    SIC_STANDARD
}