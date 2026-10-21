@echo off
chcp 65001 >nul
cd /d "%~dp0"

if not exist out\com\team\agent\Main.class (
  echo [INFO] Build output not found, compiling first...
  call build.bat
  if errorlevel 1 exit /b 1
)

if "%DEEPSEEK_API_KEY%"=="" (
  if "%AGENT_API_KEY%"=="" (
    echo [WARN] No API key found in environment.
    echo [WARN] Starting in OFFLINE demo mode. Tools still run for real.
    echo [WARN] To use a real model, run: set DEEPSEEK_API_KEY=sk-xxxxxx
    echo.
  )
)

java -Dfile.encoding=UTF-8 -cp out com.team.agent.Main
