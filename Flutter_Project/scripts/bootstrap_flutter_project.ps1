param(
    [string]$ProjectName = "photo_roulette_flutter",
    [string]$Org = "com.example"
)

$ErrorActionPreference = "Stop"

if (-not (Get-Command flutter -ErrorAction SilentlyContinue)) {
    Write-Host "Flutter CLI is not installed or not in PATH."
    Write-Host "Install Flutter SDK first, then rerun this script from the repository root."
    exit 1
}

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Split-Path -Parent $projectRoot

Push-Location $projectRoot
try {
    flutter create . --project-name $ProjectName --org $Org --platforms=android,ios
    Write-Host "Flutter scaffold created in $projectRoot"
    Write-Host "Next: add dependencies from docs/ARCHITECTURE.md and start feature migration."
}
finally {
    Pop-Location
}
