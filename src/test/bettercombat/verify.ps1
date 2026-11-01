<#
After compiling the project, run this optional check with the downloaded Better Combat NeoForge jar
and the matching NeoForge development jar. It never downloads dependencies, starts Minecraft, or runs
Gradle. Generated classes stay under build/bettercombat-offline and are separate from ordinary tests.
The existing build/moddev/serverLegacyClasspath.txt supplies locally cached Mixin, ASM, Gson and Guava.
The behavior check retains actual BC swap/attribute bytecode and WCWT cell/hand bytecode; Minecraft
and AE inventory boundaries are fixtures, so passing this check is not an in-game validation.
#>
[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)][string]$BetterCombatJar,
    [Parameter(Mandatory = $true)][string]$NeoForgeJar,
    [string]$JavaHome = $env:JAVA_HOME,
    [string]$ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..\..')).Path
)
$ErrorActionPreference = 'Stop'
$BetterCombatJar = (Resolve-Path -LiteralPath $BetterCombatJar).Path
$NeoForgeJar = (Resolve-Path -LiteralPath $NeoForgeJar).Path
if ($JavaHome) {
    $java = Join-Path $JavaHome 'bin\java.exe'
    $javac = Join-Path $JavaHome 'bin\javac.exe'
    $javap = Join-Path $JavaHome 'bin\javap.exe'
} else {
    $java = (Get-Command java.exe -ErrorAction Stop).Source
    $javac = (Get-Command javac.exe -ErrorAction Stop).Source
    $javap = (Get-Command javap.exe -ErrorAction Stop).Source
}
$verifyDir = Join-Path $ProjectRoot 'build\bettercombat-offline'
$classes = Join-Path $verifyDir 'classes'
New-Item -ItemType Directory -Force -Path $classes | Out-Null
$libs = Get-Content -LiteralPath (Join-Path $ProjectRoot 'build\moddev\serverLegacyClasspath.txt') |
    Where-Object { (Split-Path $_ -Leaf) -match '^(sponge-mixin-|mixin-\d|asm(?:-|\.)|gson-|guava-)' }
if (-not ($libs | Where-Object { (Split-Path $_ -Leaf) -match 'mixin-' })) {
    throw 'Mixin is missing from serverLegacyClasspath.txt; prepare the NeoForge run artifacts first.'
}
$compiled = Join-Path $ProjectRoot 'build\classes\java\main'
$baseCp = (@($classes, $compiled, $NeoForgeJar) + $libs) -join [IO.Path]::PathSeparator
& $javap -classpath $BetterCombatJar -c -p -s net.bettercombat.logic.PlayerAttackHelper |
    Out-File -Encoding utf8 (Join-Path $verifyDir 'PlayerAttackHelper.javap.txt')
if ($LASTEXITCODE -ne 0) { throw 'BC descriptor inspection failed' }
Get-FileHash -Algorithm SHA1 -LiteralPath $BetterCombatJar
& $javac -proc:none -cp $baseCp -d $classes (Join-Path $PSScriptRoot 'OfflineMixinCheck.java') (Join-Path $PSScriptRoot 'BehaviorCheck.java')
if ($LASTEXITCODE -ne 0) { throw 'offline verifier compilation failed' }
$services = Join-Path $classes 'META-INF\services'
New-Item -ItemType Directory -Force -Path $services | Out-Null
Set-Content -Encoding ascii -Path (Join-Path $services 'org.spongepowered.asm.service.IMixinService') -Value 'OfflineMixinCheck$Service'
Set-Content -Encoding ascii -Path (Join-Path $services 'org.spongepowered.asm.service.IGlobalPropertyService') -Value 'OfflineMixinCheck$Properties'
Set-Content -Encoding ascii -Path (Join-Path $classes 'bc-offline.mixins.json') -Value '{"required":true,"minVersion":"0.8","package":"com.lhy.wcwt.mixin","compatibilityLevel":"JAVA_21","mixins":["WcwtBetterCombatMixin"],"injectors":{"defaultRequire":1}}'
& $java -cp "$baseCp$([IO.Path]::PathSeparator)$BetterCombatJar" OfflineMixinCheck (Join-Path $verifyDir 'PlayerAttackHelper.transformed.class')
if ($LASTEXITCODE -ne 0) { throw 'BC-present Mixin verification failed' }
# This JVM deliberately has no Better Combat jar on its classpath.
& $java -cp $baseCp OfflineMixinCheck absent
if ($LASTEXITCODE -ne 0) { throw 'BC-absent Mixin verification failed' }
& $java -cp $baseCp BehaviorCheck $verifyDir $compiled
if ($LASTEXITCODE -ne 0) { throw 'BC behavior verification failed' }
