$wshell = New-Object -ComObject WScript.Shell
$desktopPath = [Environment]::GetFolderPath('Desktop')
$shortcutPath = Join-Path $desktopPath "Smart Library Management System.lnk"
$shortcut = $wshell.CreateShortcut($shortcutPath)
$shortcut.TargetPath = Join-Path $PSScriptRoot "run.bat"
$shortcut.IconLocation = Join-Path $PSScriptRoot "slms_icon.ico"
$shortcut.WorkingDirectory = $PSScriptRoot
$shortcut.Save()
Write-Host "Shortcut created on Desktop successfully!" -ForegroundColor Green
Write-Host "You can now right-click the shortcut on your Desktop and select 'Pin to taskbar' if you like." -ForegroundColor Yellow
Read-Host "Press Enter to exit"
