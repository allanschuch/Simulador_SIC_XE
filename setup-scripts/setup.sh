#!/bin/bash

echo "==================================================="
echo "  Configuração do Ambiente - Simulador SIC/XE"
echo "==================================================="
echo ""

# Flag para verificar se um apt update é necessário
UPDATE_NEEDED=false

# Verifica a instalação do Java
if command -v java &> /dev/null && java -version 2>&1 | grep -q "21"; then
    echo "[OK] Java 21 já está instalado."
else
    echo "[!] Java 21 não encontrado. Preparando instalação..."
    UPDATE_NEEDED=true
fi

# Verifica a instalação do Maven
if command -v mvn &> /dev/null; then
    echo "[OK] Apache Maven já está instalado."
else
    echo "[!] Apache Maven não encontrado. Preparando instalação..."
    UPDATE_NEEDED=true
fi

# Executa a instalação apenas se necessário
if [ "$UPDATE_NEEDED" = true ]; then
    echo "Atualizando a lista de pacotes..."
    sudo apt update

    if ! command -v java &> /dev/null || ! java -version 2>&1 | grep -q "21"; then
        sudo apt install -y openjdk-21-jdk
    fi

    if ! command -v mvn &> /dev/null; then
        sudo apt install -y maven
    fi
fi

echo ""
echo "==================================================="
echo "[CONCLUÍDO] Validação e Instalação terminadas!"
echo "Versões atuais:"
java -version
mvn -version
echo "==================================================="