param([string]$Serial = 'emulator-5556')
$ErrorActionPreference='Stop'
$repo=Split-Path -Parent $PSScriptRoot
Set-Location $repo
$adb=Join-Path $repo '.tools\android-sdk\platform-tools\adb.exe'
$output=Join-Path $repo 'artifacts\evidence'
New-Item -ItemType Directory -Force $output | Out-Null
& $adb -s $Serial shell am force-stop ai.onmyway.car.demo
& $adb -s $Serial shell am start -n ai.onmyway.car.demo/ai.onmyway.car.MainActivity
$record=Start-Process -FilePath $adb -ArgumentList @('-s',$Serial,'shell','screenrecord','--size','1920x1080','--bit-rate','6000000','--time-limit','150','/sdcard/onmyway-demo.mp4') -WindowStyle Hidden -RedirectStandardOutput "$output\recording.log" -RedirectStandardError "$output\recording-errors.log" -PassThru
try {
    python scripts/smoke-android.py --adb $adb --serial $Serial --output $output --complete-first --pause 2
    if ($LASTEXITCODE -ne 0) { throw 'Recording scenario did not pass; preserve footage as failed evidence only.' }
} finally {
    $remotePid=(& $adb -s $Serial shell pidof screenrecord).Trim()
    if ($remotePid -match '^\d+$') { & $adb -s $Serial shell kill -2 $remotePid }
    $record.WaitForExit(10000) | Out-Null
    & $adb -s $Serial pull /sdcard/onmyway-demo.mp4 (Join-Path $output 'OnMyWay_AI_Pleos_fixture_raw.mp4')
}
