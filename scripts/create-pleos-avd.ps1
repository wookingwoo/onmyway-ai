# Run after placing the checksum-verified official Pleos Connect 2.1.0 image in
# .tools/pleos-unpack/crp_avd_out_x86_64. SDK licenses require user acceptance.
$ErrorActionPreference = 'Stop'
$repo = Split-Path -Parent $PSScriptRoot
$imagePath = Join-Path $repo '.tools\pleos-unpack\crp_avd_out_x86_64'
foreach ($name in @('system.img','userdata.img','kernel-ranchu','ramdisk.img','source.properties')) {
    if (!(Test-Path (Join-Path $imagePath $name))) { throw "Missing official image file: $name" }
}
$avdHome = Join-Path $repo '.tools\avds'
$avdPath = Join-Path $avdHome 'OnMyWay_Pleos.avd'
New-Item -ItemType Directory -Force $avdPath | Out-Null
$config = @"
AvdId=OnMyWay_Pleos
avd.ini.displayname=OnMyWay Pleos Connect 2.1.0
avd.ini.encoding=UTF-8
abi.type=x86_64
hw.cpu.arch=x86_64
hw.cpu.ncore=4
hw.ramSize=8192
hw.gpu.enabled=yes
hw.gpu.mode=host
hw.lcd.width=2560
hw.lcd.height=1440
hw.lcd.density=160
hw.keyboard=yes
hw.mainKeys=no
hw.device.name=Pleos_Connect_v2_0
hw.device.manufacturer=42dot
hw.multi_display_window=no
hw.hotplug_multi_display=no
image.sysdir.1=$imagePath\
tag.id=android-automotive
tag.display=Pleos Connect 2.1.0
target=android-34
disk.dataPartition.size=12G
fastboot.forceColdBoot=yes
fastboot.forceFastBoot=no
showDeviceFrame=no
"@
Set-Content -LiteralPath (Join-Path $avdPath 'config.ini') -Value $config -Encoding utf8
Set-Content -LiteralPath (Join-Path $avdHome 'OnMyWay_Pleos.ini') -Value "avd.ini.encoding=UTF-8`npath=$avdPath`ntarget=android-34" -Encoding utf8
Write-Output "AVD created at $avdPath. Always use -no-snapshot for VHAL."
