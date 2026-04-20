$token = $env:GITHUB_TOKEN
if (-not $token) { $token = $env:GH_TOKEN }
if (-not $token) { 
    Write-Host "Error: GITHUB_TOKEN or GH_TOKEN not set"
    exit 1 
}

$owner = "JaneJane123654"
$repo = "photoRoulette"
$tagName = "v1.1.4"
$apkPath = "d:\project\photoRoulette\build\outputs\apk\release\photoRoulette-release.apk"

Write-Host "Creating GitHub Release: $tagName"

$releaseUrl = "https://api.github.com/repos/$owner/$repo/releases"

$releaseData = @{
    tag_name = $tagName
    name = "Release $tagName"
    body = "Automated Release v1.1.4"
    draft = $false
    prerelease = $false
} | ConvertTo-Json

try {
    $releaseResponse = Invoke-RestMethod -Uri $releaseUrl `
        -Method POST `
        -Headers @{
            "Authorization" = "token $token"
            "Accept" = "application/vnd.github.v3+json"
        } `
        -Body $releaseData `
        -ContentType "application/json" `
        -ErrorAction Stop

    Write-Host "Release created successfully!"
    Write-Host "Release URL: $($releaseResponse.html_url)"
    
    $uploadUrl = $releaseResponse.upload_url -replace '\{\?name,label\}', ''
    $apkFileName = (Get-Item $apkPath).Name
    $uploadUri = "$uploadUrl`?name=$apkFileName"
    
    Write-Host "Uploading APK: $apkFileName"
    $apkBytes = [System.IO.File]::ReadAllBytes($apkPath)
    
    $uploadResponse = Invoke-RestMethod -Uri $uploadUri `
        -Method POST `
        -Headers @{
            "Authorization" = "token $token"
            "Accept" = "application/vnd.github.v3+json"
            "Content-Type" = "application/octet-stream"
        } `
        -Body $apkBytes `
        -ErrorAction Stop
    
    Write-Host "APK uploaded successfully!"
    Write-Host "Download URL: $($uploadResponse.browser_download_url)"
}
catch {
    Write-Host "Error: $_"
    exit 1
}
