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
    echo Error: JDK 21 ^(javac/java^) not found.
    exit /b 1
)

if not exist out\main mkdir out\main
if not exist out\test mkdir out\test

> main_sources.txt (
    for /r src\main\java %%f in (*.java) do (
        set "p=%%f"
        set "p=!p:\=/!"
        echo "!p!"
    )
)
"%JAVAC_CMD%" -encoding UTF-8 -d out\main @main_sources.txt
set "ERR=%errorlevel%"
del main_sources.txt
if not "%ERR%"=="0" exit /b %ERR%

> test_sources.txt (
    for /r src\test\java %%f in (*.java) do (
        set "p=%%f"
        set "p=!p:\=/!"
        echo "!p!"
    )
)
"%JAVAC_CMD%" -encoding UTF-8 -cp out\main -d out\test @test_sources.txt
set "ERR=%errorlevel%"
del test_sources.txt
if not "%ERR%"=="0" exit /b %ERR%

echo Running ShadowCrypt Automated Test Suite...
"%JAVA_CMD%" -ea -cp "out\main;out\test" com.college.shadowcrypt.GameTestSuite
set "TEST_ERR=%errorlevel%"
endlocal & exit /b %TEST_ERR%
