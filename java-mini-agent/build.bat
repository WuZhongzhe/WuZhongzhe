@echo off
chcp 65001 >nul
cd /d "%~dp0"

if not exist src\com\team\agent\Main.java (
  echo [ERROR] Cannot find src\com\team\agent\Main.java
  echo [ERROR] Please run this script inside the ai-agent folder.
  exit /b 1
)

if exist out rd /s /q out
mkdir out

dir /s /b src\*.java > .sources.txt
javac -encoding UTF-8 -d out @.sources.txt
if errorlevel 1 (
  del .sources.txt
  echo [FAIL] Compilation failed. Check the error messages above.
  exit /b 1
)
del .sources.txt

echo [OK] Compiled successfully. Output folder: out
echo [OK] Next step: run.bat
