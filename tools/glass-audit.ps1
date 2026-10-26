param(
    [string]$Serial = 'emulator-5554',
    [string]$OutputDirectory = "$PSScriptRoot/../docs/liquid-glass/audit",
    [switch]$Light,
    [switch]$Oled,
    [string[]]$Screens = @('dashboard', 'routines', 'history', 'analytics', 'routine', 'workout', 'edit', 'compare',
        'settings', 'workout-settings', 'personalization', 'notifications', 'ai', 'exercises', 'journal',
        'import', 'donors', 'maxes', 'physical', 'physical-settings', 'physical-compare', 'report')
)
$ErrorActionPreference = 'Stop'
$env:ANDROID_USER_HOME = "$env:USERPROFILE/.android"
$env:ANDROID_SDK_HOME = $env:USERPROFILE
$adb = "$env:LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe"
$package = 'com.emanuel5014.trainable.audit'
$null = New-Item -ItemType Directory -Path $OutputDirectory -Force

function Invoke-Adb([string[]]$Arguments) {
    $result = & $adb -s $Serial @Arguments 2>&1
    if ($LASTEXITCODE -ne 0) { throw "adb $Arguments failed: $result" }
    return $result
}

$results = @()
foreach ($screen in $Screens) {
    $tab = switch ($screen) { 'routines' { 1 } 'history' { 2 } 'analytics' { 3 } default { 0 } }
    $null = Invoke-Adb @('shell', 'am', 'force-stop', $package)
    $null = Invoke-Adb @('shell', 'cmd', 'statusbar', 'collapse')
    $arguments = @('shell', 'am', 'start', '-W', '-f', '0x10008000', '-n', "$package/com.emanuel5014.trainable.GlassAuditActivity",
        '--es', 'screen', $screen, '--ei', 'tab', "$tab", '--ez', 'light', "$($Light.IsPresent)".ToLowerInvariant(),
        '--ez', 'oled', "$($Oled.IsPresent)".ToLowerInvariant())
    $null = Invoke-Adb $arguments
    Start-Sleep -Milliseconds 1800
    $null = Invoke-Adb @('shell', 'uiautomator', 'dump', '/sdcard/glass-audit.xml')
    $xmlPath = Join-Path $OutputDirectory "$screen.xml"
    $null = Invoke-Adb @('pull', '/sdcard/glass-audit.xml', $xmlPath)
    [xml]$tree = Get-Content $xmlPath -Raw
    $appNodes = @($tree.SelectNodes('//node') | Where-Object { $_.package -eq $package })
    if ($appNodes.Count -eq 0) { throw "Expected audit app on $screen, found another app or a crash" }
    $null = Invoke-Adb @('shell', 'screencap', '-p', '/sdcard/glass-audit.png')
    $null = Invoke-Adb @('pull', '/sdcard/glass-audit.png', (Join-Path $OutputDirectory "$screen.png"))
    $titles = @($appNodes | Where-Object { $_.text } | Select-Object -First 4 | ForEach-Object { $_.text })
    $results += [pscustomobject]@{ Screen = $screen; Text = ($titles -join ' | '); Nodes = $appNodes.Count }
    Write-Output "$screen : $($titles -join ' | ')"
}
$results | Export-Csv (Join-Path $OutputDirectory 'screens.csv') -NoTypeInformation

# Contact sheets are only for quickly reviewing the real emulator screenshots.
Add-Type -AssemblyName System.Drawing
$font = [Drawing.Font]::new('Segoe UI', 12)
for ($offset = 0; $offset -lt $Screens.Count; $offset += 6) {
    $group = @($Screens | Select-Object -Skip $offset -First 6)
    $canvas = [Drawing.Bitmap]::new(1080, 1680)
    $graphics = [Drawing.Graphics]::FromImage($canvas)
    $graphics.Clear([Drawing.Color]::FromArgb(24, 28, 34))
    foreach ($index in 0..($group.Count - 1)) {
        $x = ($index % 3) * 360
        $y = [Math]::Floor($index / 3) * 840
        $graphics.DrawString($group[$index], $font, [Drawing.Brushes]::White, $x + 8, $y + 5)
        $sourceImage = [Drawing.Image]::FromFile((Join-Path $OutputDirectory "$($group[$index]).png"))
        $graphics.DrawImage($sourceImage, [Drawing.Rectangle]::new($x + 4, $y + 32, 352, 785))
        $sourceImage.Dispose()
    }
    $canvas.Save((Join-Path $OutputDirectory "contact-$([int]($offset / 6) + 1).png"))
    $graphics.Dispose()
    $canvas.Dispose()
}
$font.Dispose()
