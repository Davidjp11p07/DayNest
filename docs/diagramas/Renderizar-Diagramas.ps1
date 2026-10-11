param(
    [Parameter(Mandatory=$true)][string]$PlantUmlJar,
    [ValidateSet('svg','png')][string]$Formato = 'svg',
    [string]$JavaExe = 'java',
    [string]$DotExe,
    [switch]$Smetana
)
$ErrorActionPreference = 'Stop'
$jar = (Resolve-Path -LiteralPath $PlantUmlJar).Path
if ($DotExe) { $env:GRAPHVIZ_DOT = (Resolve-Path -LiteralPath $DotExe).Path }
$archivos = @(Get-ChildItem -LiteralPath $PSScriptRoot -Filter '*.puml' | Sort-Object Name | ForEach-Object { $_.FullName })
$opciones = @('-charset','UTF-8')
if ($Smetana) { $opciones += '-DUSAR_SMETANA' }
& $JavaExe '-Djava.awt.headless=true' '-Dfile.encoding=UTF-8' -jar $jar @opciones '-checkonly' @archivos
if ($LASTEXITCODE -ne 0) { throw 'PlantUML detectó errores de sintaxis; no se generaron imágenes.' }
& $JavaExe '-Djava.awt.headless=true' '-Dfile.encoding=UTF-8' -jar $jar @opciones '-failfast2' "-t$Formato" @archivos
if ($LASTEXITCODE -ne 0) { throw 'Falló el renderizado; revise PlantUML, Java y el motor de distribución.' }
foreach ($archivo in $archivos) {
    $imagen = [IO.Path]::ChangeExtension($archivo,$Formato)
    if (-not (Test-Path -LiteralPath $imagen)) { throw "No se generó $imagen" }
}
Write-Output "Generadas $($archivos.Count) imágenes $Formato junto a las fuentes."
