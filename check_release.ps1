#!/usr/bin/env powershell
$token = $env:GITHUB_TOKEN
$owner = "JaneJane123654"
$repo = "photoRoulette"
$releaseId = 305404144

$assetsUrl = "https://api.github.com/repos/$owner/$repo/releases/$releaseId/assets"
Write-Host "Checking assets at: $assetsUrl"

$assets = curl.exe -s -H "Authorization: token $token" $assetsUrl | ConvertFrom-Json

if ($assets -is [array]) {
    Write-Host "Found $($assets.Count) assets:"
    foreach ($asset in $assets) {
        Write-Host "  - $($asset.name): $($asset.size) bytes"
    }
} elseif ($null -ne $assets.name) {
    Write-Host "Found asset: $($assets.name): $($assets.size) bytes"
    Write-Host "Download: $($assets.browser_download_url)"
} else {
    Write-Host "No assets found or error: $($assets.message)"
}

# 检查是否需要上传
$apkPath = "build\outputs\apk\release\photoRoulette-release.apk"
if (Test-Path $apkPath) {
    $apkSize = (Get-Item $apkPath).Length
    Write-Host "`nLocal APK size: $apkSize bytes"
    
    $hasApk = $false
    if ($assets -is [array]) {
        $hasApk = $assets | Where-Object { $_.name -eq "photoRoulette-release.apk" } | Measure-Object | Select-Object -ExpandProperty Count
    } elseif ($null -ne $assets.name -and $assets.name -eq "photoRoulette-release.apk") {
        $hasApk = $true
    }
    
    if ($hasApk) {
        Write-Host "APK already uploaded to Release!"
    } else {
        Write-Host "APK not found in Release, uploading..."
    }
}
