param([string]$Serial = 'emulator-5554', [string]$OutputDirectory = "$PSScriptRoot/../docs/liquid-glass/v4-onboarding")
$ErrorActionPreference = 'Stop'
$env:ANDROID_USER_HOME = "$env:USERPROFILE/.android"
$env:ANDROID_SDK_HOME = $env:USERPROFILE
$adb = "$env:LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe"
$package = 'com.emanuel5014.trainable.audit'
$null = New-Item -ItemType Directory -Force $OutputDirectory
& $adb -s $Serial shell am start -W -f 0x10008000 -n "$package/com.emanuel5014.trainable.GlassAuditActivity" --es screen onboarding | Out-Null
Start-Sleep -Seconds 2
foreach ($page in 1..11) {
    & $adb -s $Serial shell uiautomator dump /sdcard/glass-onboarding.xml | Out-Null
    $xmlPath = Join-Path $OutputDirectory "page-$page.xml"
    & $adb -s $Serial pull /sdcard/glass-onboarding.xml $xmlPath 2>&1 | Out-Null
    [xml]$tree = Get-Content $xmlPath -Raw
    $nodes = @($tree.SelectNodes('//node') | Where-Object { $_.package -eq $package })
    if (-not $nodes.Count) { throw "Audit app is not visible on page $page" }
    & $adb -s $Serial shell screencap -p /sdcard/glass-onboarding.png
    & $adb -s $Serial pull /sdcard/glass-onboarding.png (Join-Path $OutputDirectory "page-$page.png") 2>&1 | Out-Null
    Write-Output "Page $page : $(@($nodes | Where-Object { $_.text } | Select-Object -First 3 | ForEach-Object { $_.text }) -join ' | ')"
    if ($page -lt 11) {
        $next = $nodes | Where-Object { $_.text -eq 'Continue' } | Select-Object -First 1
        if (-not $next) { throw "Continue button missing on page $page" }
        $bounds = [regex]::Match($next.bounds, '\[(\d+),(\d+)\]\[(\d+),(\d+)\]')
        $x = [int](($bounds.Groups[1].Value -as [int]) + ($bounds.Groups[3].Value -as [int])) / 2
        $y = [int](($bounds.Groups[2].Value -as [int]) + ($bounds.Groups[4].Value -as [int])) / 2
        & $adb -s $Serial shell input tap ([int]$x) ([int]$y)
        Start-Sleep -Milliseconds 1200
    }
}
# Stop before submitting the profile or enabling any cloud connection.
