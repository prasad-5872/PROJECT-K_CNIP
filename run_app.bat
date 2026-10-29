@echo off
echo ===============================================================
echo Launching Reliable Emergency Communication Network (EOC GUI)
echo ===============================================================
if not exist out (
    echo [INFO] Compiling sources first...
    mkdir out
    powershell -Command "$sources = Get-ChildItem -Recurse -Path 'src', 'test' -Filter *.java | ForEach-Object { $_.FullName }; javac -d out $sources"
)
java -cp out emergency.EmergencyNetworkApp
