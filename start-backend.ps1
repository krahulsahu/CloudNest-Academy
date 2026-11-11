# Start the Spring Boot Backend on Port 8081
$env:JAVA_HOME = "C:\Program Files\Java\jdk-26.0.1"
$mvn = "C:\Program Files\JetBrains\IntelliJ IDEA Community Edition 2025.2.6.2\plugins\maven\lib\maven3\bin\mvn.cmd"

# Load .env file if it exists
$envFile = "$PSScriptRoot\.env"
if (Test-Path $envFile) {
    Get-Content $envFile | ForEach-Object {
        $line = $_.Trim()
        if ($line -and -not $line.StartsWith("#") -and $line.Contains("=")) {
            $parts = $line.Split("=", 2)
            $name = $parts[0].Trim()
            $val = $parts[1].Trim()
            [System.Environment]::SetEnvironmentVariable($name, $val, "Process")
        }
    }
    Write-Host "Loaded configuration from .env" -ForegroundColor Green
}

Set-Location "$PSScriptRoot\backend"
Write-Host "Starting CloudNest Backend on http://localhost:8081 ..." -ForegroundColor Cyan
& $mvn spring-boot:run
