@echo off
setlocal enabledelayedexpansion

:: 1. ACAO: Limpar o ambiente
if "%~1"=="clean" (
    if exist bin (
        rmdir /s /q bin
        echo [OK] Pasta 'bin' removida. Ambiente limpo!
    ) else (
        echo [INFO] A pasta 'bin' ja esta limpa.
    )
    exit /b 0
)

:: Cria o diretorio de saida se nao existir
if not exist bin mkdir bin

echo [INFO] Buscando arquivos .java no diretorio src\...
dir /s /b src\*.java > sources.txt 2>nul

for %%A in (sources.txt) do if %%~zA==0 (
    echo [ERRO] Nenhum arquivo .java encontrado na pasta src.
    del sources.txt
    exit /b 1
)

echo [INFO] Compilando o projeto...
javac -d bin @sources.txt
set BUILD_STATUS=%ERRORLEVEL%
del sources.txt

:: 2. ACAO: Executar se a compilacao for bem-sucedida
if %BUILD_STATUS% EQU 0 (
    echo [OK] Compilacao concluida com SUCESSO!
    
    if "%~1"=="run" (
        set CLASS_NAME=%~2
        
        :: PADRAO: Se nenhuma classe for informada, executa o teste
        if "!CLASS_NAME!"=="" (
            set CLASS_NAME=tests.Test
            echo [INFO] Nenhuma classe informada. Executando o padrao: !CLASS_NAME!
        )
        
        echo.
        echo === EXECUTANDO: !CLASS_NAME! ===
        java -cp bin !CLASS_NAME!
        echo ==================================================
    )
) else (
    echo [ERRO] Falha na compilacao. Verifique os logs acima.
)
exit /b %BUILD_STATUS%