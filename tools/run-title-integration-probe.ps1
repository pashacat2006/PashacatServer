$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location -LiteralPath $projectRoot
$buildRoot = Join-Path $projectRoot 'server-titles/build'
$classes = Join-Path $buildRoot 'probe-classes'
New-Item -ItemType Directory -Path $classes -Force | Out-Null
$classPath = ([IO.File]::ReadAllLines((Join-Path $buildRoot 'javac.args')))[5].Trim('"')
$jdkBin = 'C:\Program Files\Java\jdk-21.0.11\bin'
& (Join-Path $jdkBin 'javac.exe') --release 21 -encoding UTF-8 -cp $classPath -d $classes server-titles/test/TitleIntegrationProbe.java
if ($LASTEXITCODE -ne 0) { throw 'Probe compilation failed' }
$probeJar = Join-Path $buildRoot 'TitleIntegrationProbe.jar'
& (Join-Path $jdkBin 'jar.exe') --create --file $probeJar -C $classes . -C server-titles/test/probe-resources .
if ($LASTEXITCODE -ne 0) { throw 'Probe packaging failed' }
$installedProbe = Join-Path $projectRoot 'plugins/FoliaUpdater/multithread/TitleIntegrationProbe.jar'
if (Test-Path -LiteralPath $installedProbe) { throw 'Probe already installed' }
Copy-Item -LiteralPath $probeJar -Destination $installedProbe
try {
    $report = & node tools/rcon.cjs 'foliaupdater load' 'papi parse pashacat2015 %servertitles_playtime% / %servertitles_kills% / %servertitles_selected_id%'
    if ($LASTEXITCODE -ne 0) { throw 'Probe load failed' }
    $report | Select-String 'TitleIntegrationProbe|ServerTitles|ч\.|мин\.|none'
} finally {
    Remove-Item -LiteralPath $installedProbe
}
