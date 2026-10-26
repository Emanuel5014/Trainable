param([switch]$Light, [switch]$SkipIntro, [switch]$SkipSearch, [string]$Serial = 'emulator-5554', [string]$OutputDirectory = '')
$ErrorActionPreference = 'Stop'
if (-not $OutputDirectory) { $theme = if ($Light) { 'light' } else { 'dark' }; $OutputDirectory = "$PSScriptRoot/../docs/liquid-glass/polish-$theme" }
. "$PSScriptRoot/glass-appearance-audit.ps1" -HelpersOnly -Light:$Light -Serial $Serial -OutputDirectory $OutputDirectory

function Checkboxes {
    $tree = Tree
    return @($tree.SelectNodes('//node') | Where-Object { $_.checkable -eq 'true' -and
        ($_.SelectNodes('.//node') | Where-Object { $_.'content-desc' -like '*LOG SET*' }) })
}
function MotionSwitch([string]$ControlLabel = 'Moving reflections') {
    $tree = Tree
    $label = $tree.SelectNodes('//node') | Where-Object { $_.text -eq $ControlLabel } | Select-Object -First 1
    if (-not $label) { throw 'Motion switch label not visible' }
    $bounds = [regex]::Matches($label.bounds,'\d+') | ForEach-Object { [int]$_.Value }
    $center = ($bounds[1] + $bounds[3]) / 2
    return $tree.SelectNodes('//node') | Where-Object { $_.checkable -eq 'true' } | Sort-Object {
        $rect = [regex]::Matches($_.bounds,'\d+') | ForEach-Object { [int]$_.Value }
        [math]::Abs((($rect[1] + $rect[3]) / 2) - $center)
    } | Select-Object -First 1
}

if (-not $SkipIntro) {
Open dashboard
Tap 'Quick Workout'
Snapshot 'quick-name-unfocused'
$tree = Tree
TapNode ($tree.SelectNodes('//node') | Where-Object { $_.class -eq 'android.widget.EditText' } | Select-Object -First 1)
$null = Adb @('shell','input','text','ciao')
Snapshot 'quick-name-focused'
$null = Adb @('shell','input','keyevent','BACK')
Tap 'Cancel'
}

Open workout-settings
$inline = MotionSwitch 'Inline Modifications'
if ($inline.checked -ne 'true') { TapNode $inline }

if (-not $SkipSearch) {
Open workout-compact
$tree = Tree
if (-not @($tree.SelectNodes('//node') | Where-Object { $_.text -eq 'ciao' }).Count) { throw 'Compact fixture title missing' }
Snapshot 'arnold-before-log'
# Scroll only the set list; the hub stays out of the gesture.
for ($attempt = 0; $attempt -lt 3; $attempt++) {
    $tree = Tree
    if (@($tree.SelectNodes('//node') | Where-Object { $_.text -eq 'ADD EXERCISE' }).Count) { break }
    $null = Adb @('shell','input','swipe','540','1440','540','1050','300')
}
Tap 'ADD EXERCISE'
Snapshot 'exercise-search-unfocused'
$tree = Tree
TapNode ($tree.SelectNodes('//node') | Where-Object { $_.class -eq 'android.widget.EditText' } | Select-Object -First 1)
$null = Adb @('shell','input','text','Arnold')
Snapshot 'exercise-search-focused'
$tree = Tree
if (-not @($tree.SelectNodes('//node') | Where-Object { $_.text -eq 'Arnold Press' }).Count) { throw 'Exercise search result missing' }
$null = Adb @('shell','input','keyevent','BACK')
$null = Adb @('shell','input','keyevent','BACK')
}

Open workout-compact
$pending = @(Checkboxes | Where-Object { $_.checked -eq 'false' })
if ($pending.Count -ne 3) { throw "Expected fresh three-set fixture; got $($pending.Count). Clear only the audit package between passes." }
TapNode $pending[0]
Snapshot 'arnold-first-logged'
if (@(Checkboxes | Where-Object { $_.checked -eq 'false' }).Count -ne 2) { throw 'Set was not logged' }
Tap 'Edit'
Snapshot 'set-editor'
Tap 'Collapse'
foreach ($index in 2..3) {
    $pending = @(Checkboxes | Where-Object { $_.checked -eq 'false' })
    if (-not $pending.Count) { throw "Pending set $index missing" }
    TapNode $pending[0]
}
Snapshot 'arnold-all-logged'
$tree = Tree
if (-not @($tree.SelectNodes('//node') | Where-Object { $_.text -eq '1/1' }).Count) { throw 'Exercise completion did not update' }

Open personalization
Snapshot 'settings-switches'
$switch = MotionSwitch
if ($switch.checked -ne 'true') { TapNode $switch; $switch = MotionSwitch }
$rect = [regex]::Matches($switch.bounds,'\d+') | ForEach-Object { [int]$_.Value }
$y = [int](($rect[1] + $rect[3]) / 2)
$start = $rect[2] - 24
$end = $rect[0] + 24
$gesture = Start-Process -FilePath $adb -ArgumentList @('-s',$Serial,'shell','input','swipe',"$start","$y","$end","$y",'2000') -WindowStyle Hidden -PassThru
Start-Sleep -Milliseconds 650
$null = Adb @('shell','screencap','-p','/sdcard/switch-drag.png')
$null = Adb @('pull','/sdcard/switch-drag.png',(Join-Path $OutputDirectory 'switch-drag-mid.png'))
$gesture.WaitForExit()
Start-Sleep -Milliseconds 700
if ((MotionSwitch).checked -ne 'false') { throw 'Swipe did not turn the glass switch off' }
Snapshot 'switch-off'
$null = Adb @('shell','input','swipe',"$end","$y","$start","$y",'500')
Start-Sleep -Milliseconds 700
if ((MotionSwitch).checked -ne 'true') { throw 'Swipe did not turn the glass switch on' }
TapNode (MotionSwitch)
if ((MotionSwitch).checked -ne 'false') { throw 'Switch tap did not toggle off' }
TapNode (MotionSwitch)
Open personalization
if ((MotionSwitch).checked -ne 'true') { throw 'Switch state did not persist' }
Snapshot 'switch-on-restarted'
Write-Output 'Compact quick-name, exercise search, set logging/editing and switch tap/drag/persistence passed.'
