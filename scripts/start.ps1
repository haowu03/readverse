#Requires -Version 5.1
<#
  ReadVerse one-click local start (Windows PowerShell 5.1+).
  Usage:
    powershell -ExecutionPolicy Bypass -File scripts\start.ps1
    powershell -ExecutionPolicy Bypass -File scripts\start.ps1 -SkipFrontend
#>
param(
    [switch]$SkipBackend,
    [switch]$SkipFrontend
)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

$logs = Join-Path $root 'logs'
$tmp  = Join-Path $root '.tmp'
New-Item -ItemType Directory -Force $logs | Out-Null
New-Item -ItemType Directory -Force $tmp  | Out-Null
$env:TEMP = $tmp
$env:TMP  = $tmp

$jdkHome = 'C:\Program Files\Java\jdk-17'
if (Test-Path (Join-Path $jdkHome 'bin\java.exe')) {
    $env:JAVA_HOME = $jdkHome
    $env:Path = "$jdkHome\bin;$env:Path"
}

function Test-Port([int]$Port) {
    return [bool](Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue)
}

function Wait-Port([int]$Port, [int]$Seconds = 60) {
    for ($i = 0; $i -lt $Seconds; $i++) {
        if (Test-Port $Port) { return $true }
        Start-Sleep -Seconds 1
    }
    return $false
}

function Show-LogTail([string]$Path, [int]$Lines = 20) {
    if (Test-Path $Path) {
        Write-Host "--- $Path (last $Lines lines) ---" -ForegroundColor DarkYellow
        Get-Content $Path -Tail $Lines -Encoding UTF8
    }
}

Write-Host 'ReadVerse startup' -ForegroundColor Cyan
Write-Host "Project: $root"

# 1. MySQL
$mysql = Get-Service -Name 'MySQL80' -ErrorAction SilentlyContinue
if ($mysql -and $mysql.Status -ne 'Running') {
    Write-Host '[1/5] Starting MySQL80 service...'
    try { Start-Service -Name 'MySQL80' } catch { Write-Host '[WARN] Start MySQL80 failed; start it manually.' -ForegroundColor Yellow }
} elseif (-not $mysql) {
    Write-Host '[1/5] MySQL80 service not found; assuming MySQL is provided another way.' -ForegroundColor Yellow
} else {
    Write-Host '[1/5] MySQL80 already running.'
}
if (-not (Test-Port 3306)) {
    Write-Host '[WARN] Port 3306 is not listening. Backend needs MySQL at localhost:3306/readverse.' -ForegroundColor Yellow
}

# 2. Redis
if (Test-Port 6379) {
    Write-Host '[2/5] Redis already running on 6379.'
} else {
    Write-Host '[2/5] Starting local Redis...'
    $redisExe = Join-Path $root 'tools\redis\redis-server.exe'
    $redisConf = Join-Path $root 'tools\redis\redis.conf'
    if (Test-Path $redisExe) {
        Start-Process -FilePath $redisExe -ArgumentList 'redis.conf' -WorkingDirectory (Split-Path $redisExe) -WindowStyle Hidden | Out-Null
    } else {
        $redisCmd = Get-Command redis-server -ErrorAction SilentlyContinue
        if ($redisCmd) {
            Start-Process -FilePath $redisCmd.Source -ArgumentList '--port','6379','--requirepass','readverse' -WindowStyle Hidden | Out-Null
        } else {
            throw 'Redis is not running and neither tools\redis\redis-server.exe nor redis-server in PATH was found. Start Redis on localhost:6379 with password "readverse", or add --rv.redis.enabled=false to the backend command.'
        }
    }
    if (-not (Wait-Port 6379 20)) {
        throw 'Redis did not start on port 6379. Check logs/redis or run it manually.'
    }
    Write-Host 'Redis started.'
}

# 3. Backend
$backendPort = 8080
if ($SkipBackend) {
    Write-Host '[3/5] Skip backend (-SkipBackend).'
} elseif (Test-Port $backendPort) {
    Write-Host '[3/5] Backend already running on 8080.'
} else {
    Write-Host '[3/5] Starting backend...'
    $jar = Join-Path $root 'rv-server\target\rv-server-1.0.0.jar'
    if (-not (Test-Path $jar)) {
        $mvn = Join-Path $root 'tools\apache-maven-3.9.16\bin\mvn.cmd'
        if (-not (Test-Path $mvn)) {
            $mvnCmd = Get-Command mvn -ErrorAction SilentlyContinue
            if ($mvnCmd) { $mvn = $mvnCmd.Source } else { throw 'rv-server jar not found and Maven not available. Run "mvn -B -DskipTests package" first.' }
        }
        Write-Host 'Building backend jar (first run)...'
        & $mvn -B -DskipTests package
        if ($LASTEXITCODE -ne 0) { throw 'Backend build failed.' }
    }
    $java = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin\java.exe' } else { 'java' }
    Start-Process -FilePath $java `
        -ArgumentList '-jar', $jar `
        -WorkingDirectory $root `
        -RedirectStandardOutput (Join-Path $logs 'backend.log') `
        -RedirectStandardError  (Join-Path $logs 'backend.err') `
        -WindowStyle Hidden | Out-Null

    $healthy = $false
    for ($i = 0; $i -lt 40; $i++) {
        Start-Sleep -Seconds 2
        try {
            $health = Invoke-RestMethod -Uri 'http://localhost:8080/actuator/health' -TimeoutSec 3
            if ($health.status -eq 'UP') { $healthy = $true; break }
        } catch { }
    }
    if (-not $healthy) {
        Show-LogTail (Join-Path $logs 'backend.log') 40
        throw 'Backend did not become healthy. See logs/backend.log and logs/backend.err.'
    }
    Write-Host 'Backend is healthy (http://localhost:8080).'
}

# 4. web-admin
if ($SkipFrontend) {
    Write-Host '[4/5] Skip web-admin (-SkipFrontend).'
} elseif (Test-Port 5173) {
    Write-Host '[4/5] web-admin already running on 5173.'
} else {
    Write-Host '[4/5] Starting web-admin...'
    $npm = (Get-Command npm.cmd -ErrorAction SilentlyContinue).Source
    if (-not $npm) { throw 'npm.cmd not found. Install Node.js 22.' }
    if (-not (Test-Path (Join-Path $root 'web-admin\node_modules'))) {
        Write-Host 'Installing web-admin dependencies...'
        & $npm install --no-audit --no-fund --prefix (Join-Path $root 'web-admin')
    }
    Start-Process -FilePath $npm -ArgumentList 'run','dev' -WorkingDirectory (Join-Path $root 'web-admin') `
        -RedirectStandardOutput (Join-Path $logs 'admin-dev.log') `
        -RedirectStandardError  (Join-Path $logs 'admin-dev.err') `
        -WindowStyle Hidden | Out-Null
    if (-not (Wait-Port 5173 60)) { throw 'web-admin did not start on 5173. See logs/admin-dev.err.' }
    Write-Host 'web-admin is running on 5173.'
}

# 5. web-portal
if ($SkipFrontend) {
    Write-Host '[5/5] Skip web-portal (-SkipFrontend).'
} elseif (Test-Port 5174) {
    Write-Host '[5/5] web-portal already running on 5174.'
} else {
    Write-Host '[5/5] Starting web-portal...'
    $npm = (Get-Command npm.cmd -ErrorAction SilentlyContinue).Source
    if (-not $npm) { throw 'npm.cmd not found. Install Node.js 22.' }
    if (-not (Test-Path (Join-Path $root 'web-portal\node_modules'))) {
        Write-Host 'Installing web-portal dependencies...'
        & $npm install --no-audit --no-fund --prefix (Join-Path $root 'web-portal')
    }
    Start-Process -FilePath $npm -ArgumentList 'run','dev' -WorkingDirectory (Join-Path $root 'web-portal') `
        -RedirectStandardOutput (Join-Path $logs 'portal-dev.log') `
        -RedirectStandardError  (Join-Path $logs 'portal-dev.err') `
        -WindowStyle Hidden | Out-Null
    if (-not (Wait-Port 5174 60)) { throw 'web-portal did not start on 5174. See logs/portal-dev.err.' }
    Write-Host 'web-portal is running on 5174.'
}

Write-Host ''
Write-Host 'ReadVerse is ready' -ForegroundColor Green
Write-Host '----------------------------'
Write-Host 'Admin : http://localhost:5173   admin / Admin@123   (operator / Operator@123)'
Write-Host 'Portal: http://localhost:5174   reader / Reader@123'
Write-Host 'API   : http://localhost:8080   Swagger: /swagger-ui.html'
Write-Host 'Health: http://localhost:8080/actuator/health'
Write-Host 'Logs  : logs\backend.log, logs\admin-dev.log, logs\portal-dev.log'
Write-Host 'Stop  : powershell -ExecutionPolicy Bypass -File scripts\stop.ps1'
