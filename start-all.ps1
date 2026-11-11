# Start both Backend and Frontend in separate windows
Write-Host "Launching Backend and Frontend..." -ForegroundColor Yellow
Start-Process powershell -ArgumentList "-NoExit", "-File", "`"$PSScriptRoot\start-backend.ps1`""
Start-Process powershell -ArgumentList "-NoExit", "-File", "`"$PSScriptRoot\start-frontend.ps1`""
Write-Host "Services started! Open http://localhost:3000 once ready." -ForegroundColor Green

