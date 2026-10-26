#Requires -Version 5.1
<#
  Stop ReadVerse local app processes (backend + two Vite dev servers).
  MySQL stays running. Redis is kept by default; use -IncludeRedis to stop it.
#>
param(
    [switch]$IncludeRedis
)

$ErrorActionPreference = 'Continue'

foreach ($port in 8080, 5173, 5174) {
    $listeners = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue
    if ($listeners) {
        $owner = ($listeners | Select-Object -First 1).OwningProcess
        try {
            Stop-Process -Id $owner -Force -ErrorAction Stop
            Write-Host "Stopped port $port (process $owner)."
        } catch {
            Write-Host "Could not stop port $port (process $owner): $($_.Exception.Message)" -ForegroundColor Yellow
        }
    } else {
        Write-Host "Port $port is already free."
    }
}

if ($IncludeRedis) {
    $cli = Join-Path (Split-Path -Parent $PSScriptRoot) 'tools\redis\redis-cli.exe'
    if (Test-Path $cli) {
        & $cli -a readverse shutdown nosave 2>$null
        Write-Host 'Redis stop requested.'
    } else {
        Write-Host 'Redis CLI not found; stop Redis manually if needed.' -ForegroundColor Yellow
    }
}

Write-Host 'ReadVerse application processes stopped. MySQL remains running.'
