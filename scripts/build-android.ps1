param([switch]$Release, [switch]$GenerateSigningKey)
$ErrorActionPreference = 'Stop'
$repo = Split-Path -Parent $PSScriptRoot
$sdk = Join-Path $repo '.tools\android-sdk'
if (!(Test-Path $sdk)) { throw 'Install Android SDK first and set ANDROID_HOME.' }
$env:ANDROID_HOME = $sdk
if (!$env:JAVA_HOME) {
    $jdk = Get-ChildItem (Join-Path $repo '.tools\jdk17') -Directory | Select-Object -First 1
    if (!$jdk) { throw 'Set JAVA_HOME to JDK 17.' }
    $env:JAVA_HOME = $jdk.FullName
}
# This short cache path also avoids transform rename issues in synced folders.
$env:GRADLE_USER_HOME = Join-Path $env:TEMP 'omw-gradle'
$env:JAVA_TOOL_OPTIONS = (($env:JAVA_TOOL_OPTIONS + ' -Djava.nio.channels.spi.SelectorProvider=sun.nio.ch.WindowsSelectorProvider').Trim())
Push-Location (Join-Path $repo 'apps\automotive')
try {
    $tasks = @('testDemoDebugUnitTest','assembleDemoDebug','assembleLiveDebug')
    if ($Release) { $tasks += @('assembleDemoRelease','assembleLiveRelease') }
    & '.\gradlew.bat' @tasks --no-daemon --no-watch-fs
    if ($LASTEXITCODE -ne 0) { throw 'Android test/build failed.' }
} finally { Pop-Location }
if (!$Release) { exit 0 }
$signing = Join-Path $repo '.tools\signing'
$keyFile = Join-Path $signing 'onmyway-release.jks'
$passwordFile = Join-Path $signing 'password.txt'
New-Item -ItemType Directory -Force $signing | Out-Null
if (!(Test-Path $keyFile)) {
    if (!$GenerateSigningKey) { throw 'First release: use -GenerateSigningKey to create an ignored local development signing identity.' }
    $env:OMW_SIGNING_PASSWORD = [Guid]::NewGuid().ToString('N') + [Guid]::NewGuid().ToString('N')
    Set-Content -LiteralPath $passwordFile -Value $env:OMW_SIGNING_PASSWORD -NoNewline -Encoding utf8
    & (Join-Path $env:JAVA_HOME 'bin\keytool.exe') -genkeypair -noprompt -keystore $keyFile -storetype JKS -alias onmyway -storepass:env OMW_SIGNING_PASSWORD -keypass:env OMW_SIGNING_PASSWORD -keyalg RSA -keysize 3072 -validity 10000 -dname 'CN=OnMyWay AI,OU=Development,O=OnMyWay,C=KR'
    if ($LASTEXITCODE -ne 0) { throw 'Signing key creation failed.' }
}
if (!(Test-Path $passwordFile)) { throw 'The existing keystore password file is missing; restore it privately.' }
$env:OMW_SIGNING_PASSWORD = [IO.File]::ReadAllText($passwordFile).Trim()
try {
    $output = Join-Path $repo 'apk'
    New-Item -ItemType Directory -Force $output | Out-Null
    foreach ($mode in @('demo','live')) {
        $unsigned = Join-Path $repo "apps\automotive\app\build\outputs\apk\$mode\release\app-$mode-release-unsigned.apk"
        $aligned = Join-Path $signing "$mode-aligned.apk"
        $final = Join-Path $output "OnMyWay_AI_${mode}_release.apk"
        & (Join-Path $sdk 'build-tools\35.0.0\zipalign.exe') -f -p 4 $unsigned $aligned
        if ($LASTEXITCODE -ne 0) { throw "APK alignment failed: $mode" }
        & (Join-Path $sdk 'build-tools\35.0.0\apksigner.bat') sign --ks $keyFile --ks-key-alias onmyway --ks-pass env:OMW_SIGNING_PASSWORD --key-pass env:OMW_SIGNING_PASSWORD --out $final $aligned
        if ($LASTEXITCODE -ne 0) { throw "APK signing failed: $mode" }
        & (Join-Path $sdk 'build-tools\35.0.0\apksigner.bat') verify --verbose $final
        if ($LASTEXITCODE -ne 0) { throw "APK signature verification failed: $mode" }
        Get-FileHash -LiteralPath $final -Algorithm SHA256 | Select-Object Path,Hash
    }
} finally { Remove-Item Env:OMW_SIGNING_PASSWORD -ErrorAction SilentlyContinue }
Write-Output 'Keep .tools/signing private and backed up; it is required to sign compatible updates.'
