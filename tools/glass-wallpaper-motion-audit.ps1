param([switch]$SkipPhoto, [string]$Serial = 'emulator-5554', [string]$OutputDirectory = "$PSScriptRoot/../docs/liquid-glass/rework-extended")
$ErrorActionPreference = 'Stop'
. "$PSScriptRoot/glass-appearance-audit.ps1" -HelpersOnly -Serial $Serial -OutputDirectory $OutputDirectory

function ExternalTree {
    $null = Adb @('shell','uiautomator','dump','/sdcard/appearance-external.xml')
    $null = Adb @('pull','/sdcard/appearance-external.xml',(Join-Path $OutputDirectory 'external.xml'))
    [xml]$result = Get-Content (Join-Path $OutputDirectory 'external.xml') -Raw
    return $result
}
function SwitchFor([string]$Label) {
    $tree = Tree
    $labelNode = $tree.SelectNodes('//node') | Where-Object { $_.text -eq $Label } | Select-Object -First 1
    if (-not $labelNode) { throw "Missing switch label: $Label" }
    $bounds = [regex]::Matches($labelNode.bounds,'\d+') | ForEach-Object { [int]$_.Value }
    $center = ($bounds[1] + $bounds[3]) / 2
    return $tree.SelectNodes('//node') | Where-Object { $_.checkable -eq 'true' } | Sort-Object {
        $candidate = [regex]::Matches($_.bounds,'\d+') | ForEach-Object { [int]$_.Value }
        [math]::Abs((($candidate[1] + $candidate[3]) / 2) - $center)
    } | Select-Object -First 1
}
function SetSwitch([string]$Label, [bool]$Enabled) {
    $node = SwitchFor $Label
    if (($node.checked -eq 'true') -ne $Enabled) { TapNode $node }
    $node = SwitchFor $Label
    if (($node.checked -eq 'true') -ne $Enabled) { throw "Unexpected switch state: $Label" }
}
function LightPosition([string]$Name) {
    $appProcess = (Adb @('shell','pidof',$package)).Trim()
    $log = Adb @('logcat','-d',"--pid=$appProcess",'-s','GlassAuditMotion:I')
    $log | Set-Content (Join-Path $OutputDirectory "$Name.log")
    return [regex]::Matches(($log -join "`n"),'Offset\([^)]*\)') | Select-Object -Last 1 -ExpandProperty Value
}

try {
    Open personalization
    SetSwitch 'Liquid glass' $true
    if (-not $SkipPhoto) {
        $null = Adb @('shell','mkdir','-p','/sdcard/Pictures')
        $null = Adb @('push',"$PSScriptRoot/../ICON/icon.png",'/sdcard/Pictures/GlassAppearanceTest.png')
        $null = Adb @('shell','am','broadcast','-a','android.intent.action.MEDIA_SCANNER_SCAN_FILE','-d','file:///sdcard/Pictures/GlassAppearanceTest.png')
        Tap 'Photo'
        $picker = ExternalTree
        if (@($picker.SelectNodes('//node') | Where-Object { $_.text -eq 'Choose photo' }).Count) { Tap 'Choose photo'; $picker = ExternalTree }
        $photo = $picker.SelectNodes('//node') | Where-Object { $_.'content-desc' -like 'Photo taken*' } | Select-Object -First 1
        TapNode $photo
        $picker = ExternalTree
        $done = $picker.SelectNodes('//node') | Where-Object { $_.text -eq 'Done' } | Select-Object -First 1
        if ($done) { TapNode $done }
        Start-Sleep -Milliseconds 1800
        AssertSelected 'Photo'
        Snapshot 'wallpaper-imported'
        Open personalization
        AssertSelected 'Photo'
        Snapshot 'wallpaper-restarted'
        Write-Output 'Photo import and restart persistence passed.'
    }

    $null = Adb @('emu','sensor','set','acceleration','0:9.81:0')
    SetSwitch 'Moving reflections' $true
    Start-Sleep -Milliseconds 1000
    $neutral = LightPosition 'motion-neutral'
    Snapshot 'motion-neutral'
    $null = Adb @('emu','sensor','set','acceleration','6:5:5')
    $null = Adb @('emu','sensor','set','gyroscope','0.4:0.3:0')
    Start-Sleep -Milliseconds 1500
    $null = Adb @('emu','sensor','set','gyroscope','0:0:0')
    Start-Sleep -Milliseconds 1500
    $tilted = LightPosition 'motion-tilted'
    Snapshot 'motion-tilted'
    if (-not $neutral -or -not $tilted -or $neutral -eq $tilted) { throw "No reflection movement: $neutral -> $tilted" }
    Write-Output "Moving reflection: $neutral -> $tilted"

    SetSwitch 'Moving reflections' $false
    $off = LightPosition 'motion-off'
    $null = Adb @('emu','sensor','set','acceleration','-6:5:5')
    $null = Adb @('emu','sensor','set','gyroscope','-0.4:-0.3:0')
    Start-Sleep -Milliseconds 1200
    $null = Adb @('emu','sensor','set','gyroscope','0:0:0')
    $still = LightPosition 'motion-disabled'
    if ($off -ne $still) { throw "Reflection moved while disabled: $off -> $still" }
    Open personalization
    if ((SwitchFor 'Moving reflections').checked -ne 'false') { throw 'Motion setting did not persist' }
    Snapshot 'motion-off-restarted'
    Adb @('shell','dumpsys','sensorservice') | Set-Content (Join-Path $OutputDirectory 'sensors-disabled.txt')

    SetSwitch 'Moving reflections' $true
    SetSwitch 'Liquid glass' $false
    if ((SwitchFor 'Moving reflections').enabled -ne 'false') { throw 'Motion option should be disabled with glass off' }
    Snapshot 'glass-disabled'
    SetSwitch 'Liquid glass' $true
    Tap 'Theme'
    AssertSelected 'Theme'
    Write-Output 'Motion off/persistence and glass-off fallback passed.'
} finally {
    $null = Adb @('emu','sensor','set','gyroscope','0:0:0')
    $null = Adb @('emu','sensor','set','acceleration','0:9.81:0')
}
