param([string]$Serial = 'emulator-5556', [int]$Port = 8000)
$ErrorActionPreference = 'Stop'
$repo = Split-Path -Parent $PSScriptRoot
Set-Location $repo
$adb = Join-Path $repo '.tools/android-sdk/platform-tools/adb.exe'
$python = Join-Path $repo '.venv/Scripts/python.exe'
$apk = Join-Path $repo 'apk/OnMyWay_AI_demo_release.apk'
if ($Serial -notlike 'emulator-*') { throw 'Use an isolated official Pleos emulator for the fixture demo.' }
if (!(Test-Path $adb) -or !(Test-Path $python) -or !(Test-Path $apk)) {
    throw 'Local SDK, Python environment and signed demo APK are required. See docs/demo-guide-ko.md.'
}
$state = & $adb -s $Serial get-state 2>$null
if ($LASTEXITCODE -ne 0 -or $state -ne 'device') {
    throw 'Start the official OnMyWay_Pleos AVD in Android Studio, then rerun with its -Serial.'
}
$avdName = & $adb -s $Serial emu avd name
if ($LASTEXITCODE -ne 0 -or ($avdName -join ' ') -notmatch '^OnMyWay_Pleos') {
    throw 'Use the isolated OnMyWay_Pleos AVD. No application data were changed.'
}
$healthy = $false
try { $healthy = (Invoke-RestMethod "http://127.0.0.1:$Port/health" -TimeoutSec 2).status -eq 'ok' } catch {}
if (!$healthy) {
    if (!(Test-Path '.env')) { Copy-Item '.env.example' '.env' }
    Start-Process -FilePath $python -ArgumentList @('-m','uvicorn','onmyway.main:app','--app-dir','services/api','--env-file','.env','--host','127.0.0.1','--port',"$Port") -WorkingDirectory $repo -WindowStyle Hidden -RedirectStandardOutput '.tools/demo-api.log' -RedirectStandardError '.tools/demo-api-error.log' | Out-Null
    for ($attempt = 0; $attempt -lt 20; $attempt++) {
        Start-Sleep -Milliseconds 500
        try { $healthy = (Invoke-RestMethod "http://127.0.0.1:$Port/health" -TimeoutSec 1).status -eq 'ok' } catch {}
        if ($healthy) { break }
    }
    if (!$healthy) { throw 'Local API did not start. See .tools/demo-api-error.log.' }
}
$schema = Invoke-RestMethod "http://127.0.0.1:$Port/openapi.json" -TimeoutSec 3
$parseProperties = $schema.components.schemas.ParseRequest.properties.PSObject.Properties.Name
if (@('answers','previous_errands','previous_deadline') | Where-Object { $_ -notin $parseProperties }) {
    throw 'The API is from an older checkout. Stop its known server process, restart scripts/start-api.ps1, then rerun. No app data were changed.'
}
& $adb -s $Serial install -r $apk
if ($LASTEXITCODE -ne 0) { throw 'Demo installation failed. Existing app data were not removed.' }
& $adb -s $Serial shell am start --user current -n ai.onmyway.car.demo/ai.onmyway.car.MainActivity
if ($LASTEXITCODE -ne 0) { throw 'Demo launch failed.' }
Write-Host 'Demo ready. Park / 0 required. Select the explicitly labelled question-and-answer example.'
Write-Host 'No external provider keys are required for the example. Navigation is simulated.'
