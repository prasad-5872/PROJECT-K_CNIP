@echo off
echo ===================================================
echo Compiling Reliable Emergency Communication Network
echo ===================================================
if not exist out mkdir out
powershell -Command "$sources = Get-ChildItem -Recurse -Path 'src', 'test' -Filter *.java | ForEach-Object { $_.FullName }; javac -d out $sources"
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Compilation failed!
    pause
    exit /b %ERRORLEVEL%
)

echo ===================================================
echo Running Automated Self-Tests
echo ===================================================
java -cp out emergency.ProjectSelfTest
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Tests failed!
    pause
    exit /b %ERRORLEVEL%
)
pause
