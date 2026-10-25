param([string]$Output='artifacts/evidence/spoken-plan')
$ErrorActionPreference='Stop'
# Offline Windows speech for explicitly prepared sample-user lines only.
# App explanations are recorded directly from Pleos ON_DEVICE output.
$dialogue=Get-Content "$PSScriptRoot/spoken-demo-dialogue.json" -Raw -Encoding UTF8 | ConvertFrom-Json
$folder=New-Item -ItemType Directory -Force "$Output/audio"
$speaker=New-Object -ComObject SAPI.SpVoice
$speaker.Voice=@($speaker.GetVoices()) | Where-Object { $_.GetDescription() -eq 'Microsoft Heami Desktop - Korean' } | Select-Object -First 1
if ($null -eq $speaker.Voice) { throw 'Offline Korean voice is required; no network fallback.' }
$speaker.Rate=0
$records=@()
foreach ($chapter in $dialogue.chapters) {
  $wave=Join-Path $folder.FullName ($chapter.id+'.wav')
  $target=Join-Path $folder.FullName ($chapter.id+'.mp3')
  $stream=New-Object -ComObject SAPI.SpFileStream
  $stream.Format.Type=22
  $stream.Open($wave,3,$false)
  try { $speaker.AudioOutputStream=$stream; [void]$speaker.Speak($chapter.narration) } finally { $stream.Close() }
  & ffmpeg -y -v error -i $wave -c:a libmp3lame -b:a 160k $target
  if ($LASTEXITCODE -ne 0) { throw 'Prepared dialogue encoding failed' }
  $records+=@{id=$chapter.id;sha256=(Get-FileHash $target).Hash.ToLower();text=$chapter.narration}
}
@{provider='Offline Windows SAPI';voice='Microsoft Heami Desktop - Korean';purpose='Prepared sample user dialogue; not app ASR or app plan output';chapters=$records} | ConvertTo-Json -Depth 5 | Set-Content "$Output/audio/prepared-provenance.json" -Encoding UTF8
