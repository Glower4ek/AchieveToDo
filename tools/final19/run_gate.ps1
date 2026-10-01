param(
    [Parameter(Mandatory=$true)][string]$Name,
    [string]$Selector,
    [string[]]$Tasks=@('compileTestJava','compileGametestJava'),
    [string[]]$TestClasses=@()
)
$ErrorActionPreference='Stop'
$taskRoot='D:\Vibecode\AchieveToDo 26.2'
Set-Location -LiteralPath $taskRoot
$scratch=Join-Path $taskRoot 'build\tmp\final19_implementation'
New-Item -ItemType Directory -Path $scratch -Force | Out-Null
if($Tasks -contains 'runGameTest') {
    $actors=@(Get-CimInstance Win32_Process | Where-Object {$_.Name -match '^java(w)?\.exe$'})
    ConvertTo-Json -InputObject @($actors | Select-Object ProcessId,CommandLine) -Depth 2 | Set-Content -LiteralPath (Join-Path $scratch "$Name-processes.json")
    if($actors | Where-Object {$_.CommandLine -match 'AchieveToDo.*(gameTest|gametest|GameTestServer)'}){throw 'Overlapping AchieveToDo GameTest process'}
    if(-not $Selector){throw 'An exact GameTest selector is mandatory'}
}
$log=Join-Path $scratch "$Name.gradle.log"
if(Test-Path -LiteralPath $log){Copy-Item -LiteralPath $log -Destination (Join-Path $scratch "$Name.previous.$([DateTime]::UtcNow.Ticks).gradle.log")}
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-25.0.4.7-hotspot'
$env:GRADLE_USER_HOME=Join-Path $taskRoot '.gradle-user-home'
$env:TEMP=Join-Path $taskRoot 'build\tmp\codex_java_uds_probe'
$env:TMP=$env:TEMP
$gateArgs=@('--no-daemon','-I','tools/final19/final19.init.gradle')
if($Selector){$gateArgs+="-Dfabric-api.gametest.filter=$Selector"}
$gateArgs+=$Tasks
foreach($testClass in $TestClasses){$gateArgs+=@('--tests',$testClass)}
& .\gradlew.bat @gateArgs *> $log
$gateExit=$LASTEXITCODE
$gateExit | Set-Content -LiteralPath (Join-Path $scratch "$Name.exit")
Get-Content -LiteralPath $log -Tail 25
exit $gateExit
