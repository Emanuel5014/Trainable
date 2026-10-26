param([string]$Serial = 'emulator-5554', [string]$OutputDirectory = "$PSScriptRoot/../docs/liquid-glass/v4-touch")
$ErrorActionPreference = 'Stop'
$env:ANDROID_USER_HOME = "$env:USERPROFILE/.android"
$env:ANDROID_SDK_HOME = $env:USERPROFILE
$adb = "$env:LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe"
$package = 'com.emanuel5014.trainable.audit'
$null = New-Item -ItemType Directory -Force $OutputDirectory
function Adb([string[]]$Arguments) {
    $result = & $adb -s $Serial @Arguments 2>&1
    if ($LASTEXITCODE -ne 0) { throw "adb failed: $result" }
    return $result
}
function Tree {
    for ($attempt = 0; $attempt -lt 3; $attempt++) {
        try {
            $null = Adb @('shell','uiautomator','dump','/sdcard/glass-touch.xml')
            break
        } catch {
            if ($attempt -eq 2) { throw }
            Start-Sleep -Milliseconds 400
        }
    }
    $null = Adb @('pull','/sdcard/glass-touch.xml',(Join-Path $OutputDirectory 'current.xml'))
    [xml]$tree = Get-Content (Join-Path $OutputDirectory 'current.xml') -Raw
    if (-not @($tree.SelectNodes('//node') | Where-Object { $_.package -eq $package }).Count) { throw 'Audit app is not in the foreground' }
    return $tree
}
function Snapshot([string]$Name) {
    $tree = Tree
    $tree.Save((Join-Path $OutputDirectory "$Name.xml"))
    $null = Adb @('shell','screencap','-p','/sdcard/glass-touch.png')
    $null = Adb @('pull','/sdcard/glass-touch.png',(Join-Path $OutputDirectory "$Name.png"))
    Write-Output "Captured $Name"
}
function Open([string]$Screen) {
    $null = Adb @('shell','am','start','-W','-f','0x10008000','-n',"$package/com.emanuel5014.trainable.GlassAuditActivity",'--es','screen',$Screen)
    Start-Sleep -Milliseconds 1800
}
function Tap([string]$Label) {
    $tree = Tree
    $node = $tree.SelectNodes('//node') | Where-Object { $_.package -eq $package -and ($_.text -eq $Label -or $_.'content-desc' -eq $Label) } | Select-Object -First 1
    if (-not $node) { throw "Control not found: $Label" }
    $bounds = [regex]::Match($node.bounds, '\[(\d+),(\d+)\]\[(\d+),(\d+)\]')
    $x = [int](($bounds.Groups[1].Value -as [int]) + ($bounds.Groups[3].Value -as [int])) / 2
    $y = [int](($bounds.Groups[2].Value -as [int]) + ($bounds.Groups[4].Value -as [int])) / 2
    $null = Adb @('shell','input','tap',"$([int]$x)","$([int]$y)")
    Start-Sleep -Milliseconds 700
}

Open routine
Tap 'Edit Routine'
Snapshot 'routine-edit'
Tap 'Monday'
$tree = Tree
$name = $tree.SelectNodes('//node') | Where-Object { $_.class -eq 'android.widget.EditText' } | Select-Object -First 1
$bounds = [regex]::Match($name.bounds, '\[(\d+),(\d+)\]\[(\d+),(\d+)\]')
$null = Adb @('shell','input','tap',"$([int](($bounds.Groups[1].Value -as [int]) + 100))","$([int](($bounds.Groups[2].Value -as [int]) + 80))")
$null = Adb (@('shell','input','keyevent','123') + (@('67') * 60))
$tree = Tree
$save = $tree.SelectNodes('//node') | Where-Object { $_.text -eq 'SAVE' } | Select-Object -First 1
$disabled = $false
for ($node = $save; $node -and $node.Name -eq 'node'; $node = $node.ParentNode) { if ($node.enabled -eq 'false') { $disabled = $true } }
if (-not $disabled) { throw 'Save must be disabled for a blank routine name' }
Snapshot 'routine-empty-name-disabled'
Tap 'CANCEL'
Tap 'Delete week'
Snapshot 'delete-week-confirmation'
Tap 'CANCEL'
Tap 'Barbell bench press'
Snapshot 'routine-exercise-editor'
Tap '3×10'
$null = Adb @('shell','input','swipe','640','2110','640','850','600')
Snapshot 'routine-advanced-block'
Tap 'CANCEL'
Tap 'ADD EXERCISE'
Snapshot 'routine-exercise-picker'
Write-Output 'Routine touch checks passed: weekday target, blank-name validation, cancellation, week confirmation, exercise editor.'
