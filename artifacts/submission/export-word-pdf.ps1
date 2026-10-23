param([Parameter(Mandatory=$true)][string]$InputDocx, [Parameter(Mandatory=$true)][string]$OutputPdf)
$ErrorActionPreference = 'Stop'
$taskWord = $null
$taskDocument = $null
try {
    $taskWord = New-Object -ComObject Word.Application
    $taskWord.Visible = $false
    $taskWord.DisplayAlerts = 0
    $taskWord.AutomationSecurity = 3
    $taskWord.Options.UpdateLinksAtOpen = $false
    $taskDocument = $taskWord.Documents.Open($InputDocx, $false, $true, $false)
    $taskDocument.ExportAsFixedFormat($OutputPdf, 17)
    Write-Output "Exported $OutputPdf"
} finally {
    if ($null -ne $taskDocument) { $taskDocument.Close(0); [void][Runtime.InteropServices.Marshal]::FinalReleaseComObject($taskDocument) }
    if ($null -ne $taskWord) { $taskWord.Quit(); [void][Runtime.InteropServices.Marshal]::FinalReleaseComObject($taskWord) }
}
