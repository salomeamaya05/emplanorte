@echo off
title EMPLANORTE - Arranque del Sistema
chcp 65001 >nul

echo.
echo  ==========================================
echo   EMPLANORTE S.A.S. - Gestor Comercial
echo  ==========================================
echo.

set "PROJECT_DIR=%~dp0"
set "JAR=%PROJECT_DIR%src\backend\target\emplanorte-0.0.1-SNAPSHOT.jar"
set "FRONTEND=%PROJECT_DIR%src\frontend\index.html"

REM ---- 1. Verificar Java ----
echo [1/3] Verificando Java...
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo.
    echo  [ERROR] Java no encontrado. Instala JDK 17 o superior.
    echo  Descarga: https://adoptium.net
    echo.
    pause
    exit /b 1
)
echo        Java OK.

REM ---- 2. Verificar que el JAR existe ----
if not exist "%JAR%" (
    echo.
    echo  [ERROR] No se encontro el JAR del backend.
    echo  Ruta esperada: %JAR%
    echo  Ejecuta: mvn package -DskipTests dentro de src\backend
    echo.
    pause
    exit /b 1
)

REM ---- 3. Verificar PostgreSQL ----
echo [2/3] Verificando PostgreSQL...
sc query type= all state= running | findstr /i "postgresql" >nul 2>&1
if %errorlevel% neq 0 (
    echo        PostgreSQL no esta corriendo. Intentando iniciar...
    REM Intenta los nombres mas comunes del servicio
    net start postgresql-x64-17 >nul 2>&1
    net start postgresql-x64-16 >nul 2>&1
    net start postgresql-x64-15 >nul 2>&1
    net start postgresql >nul 2>&1
    timeout /t 2 /nobreak >nul
    sc query type= all state= running | findstr /i "postgresql" >nul 2>&1
    if %errorlevel% neq 0 (
        echo.
        echo  [ADVERTENCIA] No se pudo iniciar PostgreSQL automaticamente.
        echo  Verifica que el servicio este activo en services.msc
        echo  Continuando de todas formas...
        echo.
    ) else (
        echo        PostgreSQL iniciado correctamente.
    )
) else (
    echo        PostgreSQL ya esta corriendo.
)

REM ---- 4. Iniciar Backend ----
echo [3/3] Iniciando Backend en http://localhost:8080 ...
start "EMPLANORTE Backend" cmd /k "java -jar "%JAR%" && echo. && echo Backend detenido. && pause"

REM ---- 5. Esperar a que el backend levante y abrir frontend ----
echo.
echo  Esperando 12 segundos para que el backend arranque...
timeout /t 12 /nobreak
echo.
echo  Abriendo Frontend...
start "" "%FRONTEND%"

echo.
echo  ==========================================
echo   Sistema en ejecucion
echo   Backend : http://localhost:8080
echo   Frontend: %FRONTEND%
echo  ==========================================
echo.
echo  Cierra la ventana "EMPLANORTE Backend" para detener el servidor.
echo.
pause
