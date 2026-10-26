#Requires -Version 5.1
<#
  ReadVerse Docker Desktop one-click start.
  Usage:
    powershell -ExecutionPolicy Bypass -File scripts\docker-start.ps1
    powershell -ExecutionPolicy Bypass -File scripts\docker-start.ps1 -Build   # after code changes
#>
param(
    [switch]$Build
)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    throw 'Docker CLI not found. Open Docker Desktop first.'
}
$dockerInfo = cmd.exe /c "docker info --format {{.ServerVersion}} 2>&1"
if ($LASTEXITCODE -ne 0) {
    throw "Docker engine is not ready. Open Docker Desktop and retry.`n$dockerInfo"
}

Write-Host 'ReadVerse Docker startup' -ForegroundColor Cyan
Write-Host 'Starting MySQL, Redis, backend, admin and portal...'

if ($Build) {
    $output = cmd.exe /c "docker compose -f deploy/docker-compose.yml up -d --build 2>&1"
} else {
    $output = cmd.exe /c "docker compose -f deploy/docker-compose.yml up -d 2>&1"
}
$composeExit = $LASTEXITCODE
$output | ForEach-Object { Write-Host $_ }
if ($composeExit -ne 0) { throw 'docker compose up failed.' }
if (-not $Build) {
    Write-Host 'Tip: after changing backend/frontend code, run with -Build to rebuild images.' -ForegroundColor DarkYellow
}

Write-Host 'Waiting for backend health...'
$healthy = $false
for ($i = 0; $i -lt 60; $i++) {
    Start-Sleep -Seconds 2
    try {
        $health = Invoke-RestMethod -Uri 'http://localhost:18080/actuator/health' -TimeoutSec 3
        if ($health.status -eq 'UP') { $healthy = $true; break }
    } catch { }
}
if (-not $healthy) {
    docker compose -f deploy/docker-compose.yml ps
    docker compose -f deploy/docker-compose.yml logs --tail=80 backend
    throw 'Backend did not become healthy. See docker compose logs backend.'
}

Write-Host ''
Write-Host 'ReadVerse Docker stack is ready' -ForegroundColor Green
Write-Host '--------------------------------'
Write-Host 'Admin : http://localhost:8081   admin / Admin@123   (operator / Operator@123)'
Write-Host 'Portal: http://localhost:8082   reader / Reader@123'
Write-Host 'API   : http://localhost:18080  Swagger: http://localhost:18080/swagger-ui.html'
Write-Host 'Health: http://localhost:18080/actuator/health'
Write-Host ''
Write-Host 'MySQL host port: 13306, Redis host port: 6380 (internal app uses mysql:3306 / redis:6379)'
Write-Host 'Logs : docker compose -f deploy/docker-compose.yml logs -f'
Write-Host 'Stop : powershell -ExecutionPolicy Bypass -File scripts\docker-stop.ps1'
