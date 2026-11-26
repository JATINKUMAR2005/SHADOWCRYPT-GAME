@echo off
setlocal enabledelayedexpansion
cd /d "%~dp0"

if not defined JAVA_HOME (
    for /d %%d in ("C:\Program Files\Eclipse Adoptium\jdk-*" "C:\Program Files\Java\jdk-*" "C:\Program Files\Microsoft\jdk-*") do (
        if exist "%%~d\bin\javac.exe" set "JAVA_HOME=%%~d"
    )
)

if defined JAVA_HOME (
    if "!JAVA_HOME:~-1!"=="\" set "JAVA_HOME=!JAVA_HOME:~0,-1!"
)

if not defined ANDROID_HOME if exist "%LOCALAPPDATA%\Android\Sdk" (
    set "ANDROID_HOME=%LOCALAPPDATA%\Android\Sdk"
)

echo Building ShadowCrypt Android APK (Pure Java)...
pushd android-app
call gradlew.bat assembleDebug --no-daemon
set "ERR=%errorlevel%"
popd
if not "%ERR%"=="0" (
    echo Android APK build failed!
    exit /b %ERR%
)

if not exist dist mkdir dist
copy /Y "android-app\app\build\outputs\apk\debug\app-debug.apk" "dist\ShadowCrypt-Android.apk" >nul
echo Build complete:
echo   - Android APK: dist\ShadowCrypt-Android.apk
endlocal & exit /b 0
