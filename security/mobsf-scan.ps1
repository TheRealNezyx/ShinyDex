# Uploads an APK to a running MobSF server and downloads the static + dynamic report.
#
#   $env:MOBSF_API_KEY = "..."
#   powershell -File security\mobsf-scan.ps1 -Apk app\build\outputs\apk\debug\app-debug.apk

param(
    [string]$Apk = "app\build\outputs\apk\debug\app-debug.apk",
    [string]$Server = $(if ($env:MOBSF_SERVER) { $env:MOBSF_SERVER } else { "http://localhost:8000" })
)

$ErrorActionPreference = "Stop"

if (-not $env:MOBSF_API_KEY) {
    Write-Error "MOBSF_API_KEY is not set. See security/DAST.md."
}
if (-not (Test-Path $Apk)) {
    Write-Error "APK not found: $Apk  (run: gradlew.bat :app:assembleDebug)"
}

$outDir = Join-Path $PSScriptRoot "reports"
if (-not (Test-Path $outDir)) { New-Item -ItemType Directory -Path $outDir | Out-Null }

$headers = @{ Authorization = $env:MOBSF_API_KEY }

Write-Output "==> Uploading $Apk to $Server"
$form = @{ file = Get-Item $Apk }
$upload = Invoke-RestMethod -Uri "$Server/api/v1/upload" -Method Post -Headers $headers -Form $form
$hash = $upload.hash

Write-Output "==> Scanning (hash $hash)"
$scan = Invoke-RestMethod -Uri "$Server/api/v1/scan" -Method Post -Headers $headers `
    -Body @{ hash = $hash }
$scan | ConvertTo-Json -Depth 12 | Out-File -Encoding utf8 (Join-Path $outDir "mobsf-report.json")

Write-Output "==> Generating PDF"
Invoke-WebRequest -Uri "$Server/api/v1/download_pdf" -Method Post -Headers $headers `
    -Body @{ hash = $hash } -OutFile (Join-Path $outDir "mobsf-report.pdf") | Out-Null

Write-Output "==> Reports written to $outDir"
