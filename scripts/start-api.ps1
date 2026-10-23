param([int]$Port = 8000)
$ErrorActionPreference = 'Stop'
$repo = Split-Path -Parent $PSScriptRoot
Set-Location $repo
if (!(Test-Path '.venv\Scripts\python.exe')) { throw 'Run scripts/setup-api.ps1 first.' }
if (!(Test-Path '.env')) { Copy-Item '.env.example' '.env' }
& '.\.venv\Scripts\python.exe' -m uvicorn onmyway.main:app --app-dir services/api --env-file .env --host 127.0.0.1 --port $Port
exit $LASTEXITCODE
