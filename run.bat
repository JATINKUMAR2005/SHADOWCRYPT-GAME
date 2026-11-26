@echo off
setlocal enabledelayedexpansion
cd /d "%~dp0"

set "JAVAC_CMD="
set "JAVA_CMD="

where javac >nul 2>nul
if %errorlevel%==0 (
    set "JAVAC_CMD=javac"
    set "JAVA_CMD=java"
)

if not defined JAVAC_CMD if defined JAVA_HOME (
    set "JH=%JAVA_HOME%"
    if "!JH:~-1!"=="\" set "JH=!JH:~0,-1!"
    if exist "!JH!\bin\javac.exe" (
        set "JAVAC_CMD=!JH!\bin\javac.exe"
        set "JAVA_CMD=!JH!\bin\java.exe"
    )
)

if not defined JAVAC_CMD (
    for /d %%d in ("C:\Program Files\Eclipse Adoptium\jdk-*" "C:\Program Files\Java\jdk-*" "C:\Program Files\Microsoft\jdk-*") do (
        if exist "%%~d\bin\javac.exe" (
            set "JAVAC_CMD=%%~d\bin\javac.exe"
            set "JAVA_CMD=%%~d\bin\java.exe"
        )
    )
)

if not defined JAVAC_CMD (
    echo Error: JDK 21 ^(javac/java^) not found. Please install JDK 21 or add it to PATH.
    exit /b 1
)

echo Compiling ShadowCrypt ^(Pure Java 21^)...
if not exist out\main mkdir out\main

> sources.txt (
    for /r src\main\java %%f in (*.java) do (
        set "p=%%f"
        set "p=!p:\=/!"
        echo "!p!"
    )
)

"%JAVAC_CMD%" -encoding UTF-8 -d out\main @sources.txt
set "ERR=%errorlevel%"
del sources.txt
if not "%ERR%"=="0" (
    echo Compilation failed!
    exit /b %ERR%
)

echo Launching ShadowCrypt...
"%JAVA_CMD%" -cp out\main com.college.shadowcrypt.Main
set "APP_ERR=%errorlevel%"
endlocal & exit /b %APP_ERR%
