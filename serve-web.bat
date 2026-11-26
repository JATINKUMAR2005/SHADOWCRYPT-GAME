@echo off
setlocal
cd /d "%~dp0"

if not exist web\ShadowCrypt.jar (
    call build.bat
    if errorlevel 1 exit /b 1
)

echo Serving ShadowCrypt Web Edition at http://127.0.0.1:8765/
echo Press Ctrl+C in this window to stop the server.
start "" "http://127.0.0.1:8765/"
python -m http.server 8765 --bind 127.0.0.1 --directory web
endlocal
