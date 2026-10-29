@echo off
echo ===============================================================
echo Creating Desktop Shortcut for Emergency Communication Network
echo ===============================================================
powershell -NoProfile -ExecutionPolicy Bypass -Command "$WshShell = New-Object -comObject WScript.Shell; $Desktop = [System.Environment]::GetFolderPath('Desktop'); $Target = (Resolve-Path 'run_app.bat').Path; $WorkDir = (Get-Item .).FullName; $Icon = (Resolve-Path 'resources\icon.ico').Path; $Shortcut = $WshShell.CreateShortcut(\"$Desktop\Emergency Communication Network.lnk\"); $Shortcut.TargetPath = $Target; $Shortcut.WorkingDirectory = $WorkDir; $Shortcut.IconLocation = $Icon; $Shortcut.Description = 'Reliable Emergency Communication Network Under Congested Conditions (v2.0)'; $Shortcut.Save(); Write-Host '[SUCCESS] Desktop shortcut created successfully on your Windows Desktop!' -ForegroundColor Green; Write-Host ('Location: ' + \"$Desktop\Emergency Communication Network.lnk\") -ForegroundColor Cyan;"
echo.
pause
