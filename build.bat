@echo off
setlocal enabledelayedexpansion
cd /d "%~dp0"

set "JAVAC_CMD="
set "JAR_CMD="

where javac >nul 2>nul
if %errorlevel%==0 (
    set "JAVAC_CMD=javac"
    set "JAR_CMD=jar"
)

if not defined JAVAC_CMD if defined JAVA_HOME (
    set "JH=%JAVA_HOME%"
    if "!JH:~-1!"=="\" set "JH=!JH:~0,-1!"
    if exist "!JH!\bin\javac.exe" (
        set "JAVAC_CMD=!JH!\bin\javac.exe"
        set "JAR_CMD=!JH!\bin\jar.exe"
    )
)

if not defined JAVAC_CMD (
    for /d %%d in ("C:\Program Files\Eclipse Adoptium\jdk-*" "C:\Program Files\Java\jdk-*" "C:\Program Files\Microsoft\jdk-*") do (
        if exist "%%~d\bin\javac.exe" (
            set "JAVAC_CMD=%%~d\bin\javac.exe"
            set "JAR_CMD=%%~d\bin\jar.exe"
        )
    )
)

if not defined JAVAC_CMD (
    echo Error: JDK 21 ^(javac/jar^) not found.
    exit /b 1
)

echo Compiling ShadowCrypt ^(Pure Java 21^)...
if exist out\main rmdir /s /q out\main
mkdir out\main
if not exist dist mkdir dist
if not exist web mkdir web

> build_sources.txt (
    for /r src\main\java %%f in (*.java) do (
        set "p=%%f"
        set "p=!p:\=/!"
        echo "!p!"
    )
)

"%JAVAC_CMD%" -Xlint:all -Werror -encoding UTF-8 -d out\main @build_sources.txt
set "ERR=%errorlevel%"
del build_sources.txt
if not "%ERR%"=="0" (
    echo Compilation failed!
    exit /b %ERR%
)

echo Packaging ShadowCrypt.jar...
if not exist dist\jar mkdir dist\jar
"%JAR_CMD%" --create --file dist\jar\ShadowCrypt.jar --main-class com.college.shadowcrypt.Main -C out\main .
set "ERR=%errorlevel%"
if not "%ERR%"=="0" (
    echo JAR packaging failed!
    exit /b %ERR%
)

copy /Y dist\jar\ShadowCrypt.jar dist\ShadowCrypt.jar >nul
copy /Y dist\jar\ShadowCrypt.jar web\ShadowCrypt.jar >nul

set "JPACKAGE_CMD="
for /d %%d in ("C:\Program Files\Eclipse Adoptium\jdk-*" "C:\Program Files\Java\jdk-*" "C:\Program Files\Microsoft\jdk-*") do (
    if exist "%%~d\bin\jpackage.exe" set "JPACKAGE_CMD=%%~d\bin\jpackage.exe"
)
if not defined JPACKAGE_CMD where jpackage >nul 2>nul && set "JPACKAGE_CMD=jpackage"

if defined JPACKAGE_CMD (
    echo Building standalone Windows app-image ^(ShadowCrypt.exe^)...
    if exist dist\ShadowCrypt rmdir /s /q dist\ShadowCrypt
    if exist assets\ShadowCrypt.ico (
        "%JPACKAGE_CMD%" --type app-image --name ShadowCrypt --icon assets\ShadowCrypt.ico --input dist\jar --main-jar ShadowCrypt.jar --main-class com.college.shadowcrypt.Main --dest dist
    ) else (
        "%JPACKAGE_CMD%" --type app-image --name ShadowCrypt --input dist\jar --main-jar ShadowCrypt.jar --main-class com.college.shadowcrypt.Main --dest dist
    )
    if errorlevel 1 (
        echo Warning: jpackage app-image step failed, continuing with JAR and web bundle.
    )
)

echo Build complete:
echo   - Runnable JAR: dist\ShadowCrypt.jar
echo   - Standalone EXE: dist\ShadowCrypt\ShadowCrypt.exe
echo   - Web Bundle:   web\index.html + web\ShadowCrypt.jar
endlocal & exit /b 0
