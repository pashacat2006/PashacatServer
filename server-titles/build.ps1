$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location -LiteralPath $projectRoot
$jdkBin = 'C:\Program Files\Java\jdk-21.0.11\bin'
$buildPath = Join-Path $PSScriptRoot 'build'
New-Item -ItemType Directory -Path (Join-Path $buildPath 'classes'),(Join-Path $buildPath 'test-classes') -Force | Out-Null
$apiJars = @(Get-ChildItem libraries -Recurse -Filter '*.jar' | Where-Object {
    $_.Name -match '^folia-api-|^bungeecord-chat-|^snakeyaml-' -or
    $_.FullName -match 'adventure-(api|key)\\4\.20\.0\\|examination-(api|string)\\1\.3\.0\\|guava\\33\.3\.1-jre\\|annotations\\26\.0\.2\\'
} | ForEach-Object FullName)
$apiJars += @(
    (Join-Path $projectRoot 'plugins/FoliaUpdater/multithread/LuckPerms-Bukkit-5.5.71 (1).jar'),
    (Join-Path $projectRoot 'plugins/FoliaUpdater/multithread/PlaceholderAPI-2.12.3 (1).jar'),
    (Join-Path $projectRoot 'plugins/FoliaUpdater/singlethread/Vault-1.7.3.jar')
)
$arguments = @('--release','21','-encoding','UTF-8','-classpath',('"'+($apiJars -join ';')+'"'),'-d',('"'+(Join-Path $buildPath 'classes')+'"'))
$arguments += @(Get-ChildItem (Join-Path $PSScriptRoot 'src') -Recurse -Filter '*.java' | ForEach-Object { '"'+$_.FullName+'"' })
$arguments = @($arguments | ForEach-Object { $_.Replace('\','/') })
$argsPath = Join-Path $buildPath 'javac.args'
[IO.File]::WriteAllLines($argsPath,$arguments,[Text.UTF8Encoding]::new($false))
& (Join-Path $jdkBin 'javac.exe') ('@'+$argsPath)
if ($LASTEXITCODE -ne 0) { throw 'Compilation failed' }
& (Join-Path $jdkBin 'javac.exe') --release 21 -encoding UTF-8 -cp (Join-Path $buildPath 'classes') -d (Join-Path $buildPath 'test-classes') (Join-Path $PSScriptRoot 'test/TitleTransactionsTest.java')
if ($LASTEXITCODE -ne 0) { throw 'Test compilation failed' }
& (Join-Path $jdkBin 'java.exe') -cp ((Join-Path $buildPath 'classes')+';'+(Join-Path $buildPath 'test-classes')) TitleTransactionsTest
if ($LASTEXITCODE -ne 0) { throw 'Title transaction tests failed' }
& (Join-Path $jdkBin 'jar.exe') --create --file (Join-Path $projectRoot 'plugins/FoliaUpdater/multithread/ServerTitles.jar') -C (Join-Path $buildPath 'classes') . -C (Join-Path $PSScriptRoot 'resources') .
if ($LASTEXITCODE -ne 0) { throw 'Jar creation failed' }
Write-Output 'Built plugins/FoliaUpdater/multithread/ServerTitles.jar'
