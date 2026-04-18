@echo off
echo =========================================
echo       Starting VaultX Server Setup       
echo =========================================

echo.
echo [1/2] Compiling backend Java files...
if not exist out mkdir out
dir /s /B backend\src\main\java\*.java > sources.txt
javac -cp "lib\mysql-connector-j.jar" -d out @sources.txt
del sources.txt

if %ERRORLEVEL% neq 0 (
    echo.
    echo Compilation Failed! Please check your code for errors.
    exit /b %ERRORLEVEL%
)

echo [2/2] Starting VaultX Server on port 8080...
echo.
java -cp "out;lib\mysql-connector-j.jar" com.vaultx.VaultXServer
