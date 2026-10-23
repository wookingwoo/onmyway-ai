$ErrorActionPreference = 'Stop'
$repo = Split-Path -Parent $PSScriptRoot
Set-Location $repo
if (!(Test-Path '.venv\Scripts\python.exe')) {
    python -m venv .venv
    if ($LASTEXITCODE -ne 0) { throw 'Python virtual environment creation failed.' }
}
& '.\.venv\Scripts\python.exe' -m pip install -r './services/api/requirements.lock'
if ($LASTEXITCODE -ne 0) { throw 'Locked API dependency installation failed.' }
& '.\.venv\Scripts\python.exe' -m pip install --no-deps -e './services/api[test]'
if ($LASTEXITCODE -ne 0) { throw 'API dependency installation failed.' }
