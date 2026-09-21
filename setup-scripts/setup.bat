@echo off
echo ===================================================
echo   Configuracao do Ambiente - Simulador SIC/XE
echo ===================================================
echo.

:: 1. Verificacao de Privilegios de Administrador
net session >nul 2>&1
if %errorLevel% neq 0 (
    echo [ERRO] Permissao negada.
    echo Este script precisa ser executado como Administrador para instalar pacotes.
    echo Feche, clique com o botao direito no arquivo e selecione "Executar como administrador".
    pause
    exit /b 1
)

:: Forca o terminal a trabalhar na pasta onde o script esta salvo (sai da System32)
cd /d "%~dp0"

:: 2. Verificacao e Instalacao do Java
javac -version >nul 2>&1
if %errorLevel% EQU 0 (
    echo [OK] Compilador Java ja esta instalado.
    goto CHECAR_MAVEN
)

echo [!] Java nao encontrado. Instalando Java 21 (Eclipse Temurin) via Winget...
winget install EclipseAdoptium.Temurin.21.JDK --accept-package-agreements --accept-source-agreements
echo.

:CHECAR_MAVEN
:: 3. Verificacao e Instalacao do Maven
mvn -version >nul 2>&1
if %errorLevel% EQU 0 (
    echo [OK] Apache Maven ja esta instalado.
    goto FIM_INSTALACOES
)

echo [!] Apache Maven nao encontrado. Verificando dependencias...
choco -v >nul 2>&1
if %errorLevel% EQU 0 (
    goto INSTALAR_MAVEN
)

echo [!] Chocolatey nao encontrado. Instalando o Chocolatey via Winget...
winget install --id chocolatey.chocolatey --source winget
set "PATH=%PATH%;%ALLUSERSPROFILE%\chocolatey\bin"
echo.

:INSTALAR_MAVEN
echo Instalando o Apache Maven via Chocolatey...
choco install maven -y
echo.

:FIM_INSTALACOES
echo ===================================================
echo            VALIDACAO DAS INSTALACOES
echo ===================================================
:: Recarrega variaveis de ambiente na sessao atual
call refreshenv >nul 2>&1

echo.
echo Verificando versao do Java:
java -version
echo.
echo Verificando versao do Maven:
mvn -version

echo.
echo ===================================================
echo [CONCLUIDO] Processo de configuracao finalizado!
echo ATENCAO: Caso o Maven tenha sido recem-instalado, 
echo feche e abra o seu VSCode/Terminal novamente.
echo ===================================================
pause