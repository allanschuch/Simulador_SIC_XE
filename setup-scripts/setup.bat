@echo off
:: 1. Verificacao de Privilegios de Administrador
net session >nul 2>&1
if %errorLevel% neq 0 (
    echo [ERRO] Permissao negada.
    echo Este script precisa ser executado como Administrador para instalar pacotes de sistema.
    echo Feche, clique com o botao direito e selecione "Executar como administrador".
    pause
    exit /b 1
)

echo ===================================================
echo   Configuracao do Ambiente - Simulador SIC/XE
echo ===================================================
echo.

:: 2. Verificacao e Instalacao do Java
javac -version >nul 2>&1
if %errorLevel% EQU 0 (
    echo [OK] Compilador Java ja esta instalado.
) else (
    echo [!] Java nao encontrado. Instalando Java 21 (Eclipse Temurin) via Winget...
    winget install EclipseAdoptium.Temurin.21.JDK --accept-package-agreements --accept-source-agreements
    echo.
)

:: 3. Verificacao e Instalacao do Maven
mvn -version >nul 2>&1
if %errorLevel% EQU 0 (
    echo [OK] Apache Maven ja esta instalado.
) else (
    echo [!] Apache Maven nao encontrado. Verificando dependencias...
    
    :: Verifica se o Chocolatey existe antes de tentar instalar
    choco -v >nul 2>&1
    if %errorLevel% NEQ 0 (
        echo Instalando o Chocolatey via Winget...
        winget install --id chocolatey.chocolatey --source winget
        set "PATH=%PATH%;%ALLUSERSPROFILE%\chocolatey\bin"
    )
    
    echo Instalando o Apache Maven via Chocolatey...
    choco install maven -y
    echo.
)

echo ===================================================
echo            VALIDACAO DAS INSTALACOES
echo ===================================================
:: Recarrega variaveis de ambiente
call refreshenv >nul 2>&1

echo.
java -version
echo.
mvn -version

echo.
echo ===================================================
echo [CONCLUIDO] Processo de configuracao finalizado!
echo ATENCAO: Caso o Maven tenha sido recem-instalado, 
echo feche e abra o seu VSCode/Terminal novamente.
echo ===================================================
pause