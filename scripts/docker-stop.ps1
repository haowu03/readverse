#Requires -Version 5.1
<#
  Stop ReadVerse Docker stack.
  Usage:
    powershell -ExecutionPolicy Bypass -File scripts\docker-stop.ps1
    powershell -ExecutionPolicy Bypass -File scripts\docker-stop.ps1 -RemoveVolumes
#>
param(
    [switch]$RemoveVolumes
)

$ErrorActionPreference = 'Continue'
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

if ($RemoveVolumes) {
    cmd.exe /c "docker compose -f deploy/docker-compose.yml down -v 2>&1" | ForEach-Object { Write-Host $_ }
} else {
    cmd.exe /c "docker compose -f deploy/docker-compose.yml down 2>&1" | ForEach-Object { Write-Host $_ }
}

Write-Host 'ReadVerse Docker stack stopped. MySQL data volume is kept unless -RemoveVolumes was used.'
