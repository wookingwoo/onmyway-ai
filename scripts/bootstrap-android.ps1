param([string]$JavaHome = 'C:\Program Files\JetBrains\PyCharm 2025.1\jbr')
$ErrorActionPreference = 'Stop'
$repo = Split-Path -Parent $PSScriptRoot
$toolRoot = Join-Path $repo '.tools'
New-Item -ItemType Directory -Force -Path $toolRoot | Out-Null
$env:JAVA_HOME = $JavaHome
if (!(Test-Path (Join-Path $JavaHome 'bin\javac.exe'))) { throw 'Set -JavaHome to a JDK with javac.' }
$downloads = @(
    @{Url='https://services.gradle.org/distributions/gradle-8.9-bin.zip';File='gradle-8.9-bin.zip'},
    @{Url='https://dl.google.com/android/repository/commandlinetools-win-15859902_latest.zip';File='commandlinetools.zip'}
)
foreach ($item in $downloads) {
    $archive = Join-Path $toolRoot $item.File
    if (!(Test-Path $archive)) { Invoke-WebRequest -Uri $item.Url -OutFile $archive }
}
$hashResponse = (Invoke-WebRequest 'https://services.gradle.org/distributions/gradle-8.9-bin.zip.sha256').Content
$gradleHash = if ($hashResponse -is [byte[]]) { [Text.Encoding]::UTF8.GetString($hashResponse).Trim() } else { $hashResponse.Trim() }
if ((Get-FileHash (Join-Path $toolRoot 'gradle-8.9-bin.zip') -Algorithm SHA256).Hash.ToLower() -ne $gradleHash) { throw 'Gradle checksum mismatch.' }
if ((Get-FileHash (Join-Path $toolRoot 'commandlinetools.zip') -Algorithm SHA256).Hash.ToLower() -ne '90ae805d20434428bffcb699c290860f19bb5f66a67e6b330067e3de801fb04a') { throw 'Android command-line tools checksum mismatch.' }
if (!(Test-Path (Join-Path $toolRoot 'gradle-8.9'))) { Expand-Archive (Join-Path $toolRoot 'gradle-8.9-bin.zip') -DestinationPath $toolRoot }
$sdkRoot = Join-Path $toolRoot 'android-sdk'
$cmdlineParent = Join-Path $sdkRoot 'cmdline-tools'
if (!(Test-Path (Join-Path $cmdlineParent 'latest'))) {
    New-Item -ItemType Directory -Force -Path $cmdlineParent | Out-Null
    Expand-Archive (Join-Path $toolRoot 'commandlinetools.zip') -DestinationPath (Join-Path $toolRoot 'android-unpack') -Force
    Move-Item -LiteralPath (Join-Path $toolRoot 'android-unpack\cmdline-tools') -Destination (Join-Path $cmdlineParent 'latest')
}
Write-Output "JAVA_HOME=$JavaHome"
Write-Output "ANDROID_HOME=$sdkRoot"
Write-Output 'Tools downloaded and checksums verified. SDK package license acceptance is a separate user action.'
