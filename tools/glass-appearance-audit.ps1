param([switch]$Light, [switch]$HelpersOnly, [string]$Serial = 'emulator-5554', [string]$OutputDirectory = '')
$ErrorActionPreference = 'Stop'
$env:ANDROID_USER_HOME = "$env:USERPROFILE/.android"
$env:ANDROID_SDK_HOME = $env:USERPROFILE
$adb = "$env:LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe"
$package = 'com.emanuel5014.trainable.audit'
if (-not $OutputDirectory) { $theme = if ($Light) { 'light' } else { 'dark' }; $OutputDirectory = "$PSScriptRoot/../docs/liquid-glass/rework-$theme" }
$null = New-Item -ItemType Directory -Force $OutputDirectory
function Adb([string[]]$Arguments) {
    $result = & $adb -s $Serial @Arguments 2>&1
    if ($LASTEXITCODE -ne 0) { throw "adb failed: $result" }
    return $result
}
function Tree {
    for ($attempt = 0; $attempt -lt 3; $attempt++) {
        try { $null = Adb @('shell','uiautomator','dump','/sdcard/glass-regression.xml'); break }
        catch { if ($attempt -eq 2) { throw }; Start-Sleep -Milliseconds 400 }
    }
    $null = Adb @('pull','/sdcard/glass-regression.xml',(Join-Path $OutputDirectory 'current.xml'))
    [xml]$tree = Get-Content (Join-Path $OutputDirectory 'current.xml') -Raw
    if (-not @($tree.SelectNodes('//node') | Where-Object { $_.package -eq $package }).Count) { throw 'Audit app is not visible' }
    return $tree
}
function Open([string]$Screen, [int]$Tab = 0) {
    $null = Adb @('shell','am','force-stop',$package)
    $null = Adb @('shell','am','start','-W','-f','0x10008000','-n',"$package/com.emanuel5014.trainable.GlassAuditActivity",
        '--es','screen',$Screen,'--ei','tab',"$Tab",'--ez','light',"$($Light.IsPresent)".ToLowerInvariant(),'--ez','motionProbe','true')
    Start-Sleep -Milliseconds 1800
}
function TapNode($Node) {
    if (-not $Node) { throw 'Control missing' }
    $bounds = [regex]::Match($Node.bounds, '\[(\d+),(\d+)\]\[(\d+),(\d+)\]')
    $x = [int](($bounds.Groups[1].Value -as [int]) + ($bounds.Groups[3].Value -as [int])) / 2
    $y = [int](($bounds.Groups[2].Value -as [int]) + ($bounds.Groups[4].Value -as [int])) / 2
    $null = Adb @('shell','input','tap',"$([int]$x)","$([int]$y)")
    Start-Sleep -Milliseconds 650
}
function Tap([string]$Label) {
    $tree = Tree
    $node = $tree.SelectNodes('//node') | Where-Object { $_.package -eq $package -and ($_.text -eq $Label -or $_.'content-desc' -eq $Label) } | Select-Object -First 1
    if (-not $node) { throw "Control missing: $Label" }
    TapNode $node
}
function Snapshot([string]$Name) {
    $tree = Tree
    $tree.Save((Join-Path $OutputDirectory "$Name.xml"))
    $null = Adb @('shell','screencap','-p','/sdcard/glass-regression.png')
    $null = Adb @('pull','/sdcard/glass-regression.png',(Join-Path $OutputDirectory "$Name.png"))
    Write-Output "Captured $Name"
}
function AssertCreate([bool]$ExpectedEnabled) {
    $tree = Tree
    $label = $tree.SelectNodes('//node') | Where-Object { $_.text -eq 'CREATE' } | Select-Object -First 1
    if (-not $label) { throw 'Create label missing' }
    $disabled = $false
    for ($node = $label; $node -and $node.Name -eq 'node'; $node = $node.ParentNode) {
        if ($node.enabled -eq 'false') { $disabled = $true }
    }
    if ((-not $disabled) -ne $ExpectedEnabled) { throw "Unexpected Create state: expected enabled=$ExpectedEnabled" }
}

function AssertSelected([string]$Label) {
    $tree = Tree
    $node = $tree.SelectNodes('//node') | Where-Object { $_.text -eq $Label } | Select-Object -First 1
    for (; $node -and $node.Name -eq 'node'; $node = $node.ParentNode) {
        if ($node.selected -eq 'true' -or $node.checked -eq 'true') { return }
    }
    throw "Expected selected option: $Label"
}

if ($HelpersOnly) { return }

Open routines 1
Tap 'Create'
AssertCreate $false
Snapshot 'routine-create-disabled'
Tap 'Monday'
Snapshot 'routine-selected-day'
$tree = Tree
$field = $tree.SelectNodes('//node') | Where-Object { $_.class -eq 'android.widget.EditText' } | Select-Object -First 1
TapNode $field
$name = if ($Light) { 'Glass%slight%sregression' } else { 'Glass%sdark%sregression' }
$null = Adb @('shell','input','text',$name)
AssertCreate $true
Snapshot 'routine-create-enabled'
Tap 'CREATE'
$tree = Tree
$expected = $name.Replace('%s',' ')
if (-not @($tree.SelectNodes('//node') | Where-Object { $_.text -eq $expected }).Count) { throw 'Created routine not shown' }
Write-Output 'Routine creation and Create enabled/disabled checks passed.'

Open history 2
Snapshot 'history-fab'
Tap 'Add'
Snapshot 'history-menu'
$tree = Tree
if (-not @($tree.SelectNodes('//node') | Where-Object { $_.text -eq 'Add workout' }).Count) { throw 'History menu did not open' }
Tap 'Close'
Write-Output 'History FAB opens and closes its menu.'


Open personalization
Snapshot 'appearance-default'
Tap 'Solid color'
AssertSelected 'Solid color'
Snapshot 'appearance-solid'
Open personalization
AssertSelected 'Solid color'
Snapshot 'appearance-solid-restarted'
Tap 'Theme'
AssertSelected 'Theme'
Snapshot 'appearance-theme-restored'
Write-Output 'Appearance color selection and persistence checks passed.'
