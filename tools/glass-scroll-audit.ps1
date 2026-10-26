param([string]$Serial = 'emulator-5554', [string]$OutputDirectory = "$PSScriptRoot/../docs/liquid-glass/v4-performance")
$ErrorActionPreference = 'Stop'
$env:ANDROID_USER_HOME = "$env:USERPROFILE/.android"
$env:ANDROID_SDK_HOME = $env:USERPROFILE
$adb = "$env:LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe"
$package = 'com.emanuel5014.trainable.audit'
$null = New-Item -ItemType Directory -Force $OutputDirectory
foreach ($screen in @('settings','routine')) {
    & $adb -s $Serial shell cmd statusbar collapse
    & $adb -s $Serial shell am start -W -f 0x10008000 -n "$package/com.emanuel5014.trainable.GlassAuditActivity" --es screen $screen | Out-Null
    Start-Sleep -Seconds 2
    & $adb -s $Serial shell uiautomator dump /sdcard/glass-scroll.xml | Out-Null
    & $adb -s $Serial pull /sdcard/glass-scroll.xml (Join-Path $OutputDirectory "$screen.xml") 2>&1 | Out-Null
    [xml]$tree = Get-Content (Join-Path $OutputDirectory "$screen.xml") -Raw
    $expected = if ($screen -eq 'settings') { 'SETTINGS' } else { 'Upper body • Strength & control' }
    if (-not @($tree.SelectNodes('//node') | Where-Object { $_.package -eq $package -and $_.text -eq $expected }).Count) {
        throw "Expected screen not visible: $screen"
    }
    & $adb -s $Serial shell dumpsys gfxinfo $package reset | Out-Null
    foreach ($iteration in 1..6) {
        & $adb -s $Serial shell input swipe 640 2200 640 900 350
        & $adb -s $Serial shell input swipe 640 900 640 2200 350
    }
    $stats = & $adb -s $Serial shell dumpsys gfxinfo $package
    $stats | Set-Content (Join-Path $OutputDirectory "$screen-gfxinfo.txt")
    Write-Output $screen
    $stats | Select-String 'Total frames rendered|Janky frames:|50th percentile|90th percentile|95th percentile|99th percentile|Number Missed Vsync|Number Slow UI thread'
}
& $adb -s $Serial shell dumpsys activity exit-info $package | Set-Content (Join-Path $OutputDirectory 'exit-info.txt')
$appPid = (& $adb -s $Serial shell pidof $package).Trim()
& $adb -s $Serial logcat -d --pid=$appPid -s AndroidRuntime:E | Set-Content (Join-Path $OutputDirectory 'runtime-errors.txt')
