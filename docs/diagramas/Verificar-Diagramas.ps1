param([string]$RaizProyecto = (Join-Path $PSScriptRoot '../..'))
$ErrorActionPreference = 'Stop'
$raiz = (Resolve-Path -LiteralPath $RaizProyecto).Path
$fuentes = @{}
$tipos = @{}
Get-ChildItem -LiteralPath (Join-Path $raiz 'src/main/java') -Recurse -Filter '*.java' | ForEach-Object {
    $contenido = [IO.File]::ReadAllText($_.FullName)
    foreach ($m in [regex]::Matches($contenido, '\b(class|interface|enum|record)\s+(\w+)')) {
        $tipos[$m.Groups[2].Value] = $_.FullName
        $fuentes[$m.Groups[2].Value] = $contenido
    }
}
function Nombre-Simple([string]$Nombre) {
    (($Nombre -replace '<[^>]+>', '') -split '\.')[-1]
}
function Comprobar-Tipo([string]$Nombre, [string]$Archivo) {
    $simple = Nombre-Simple $Nombre
    if (-not $tipos.ContainsKey($simple)) { throw "$Archivo : tipo inexistente $Nombre" }
    return $simple
}
function Tiene-Metodo([string]$Tipo, [string]$Metodo, [int]$Nivel = 0) {
    if ($Nivel -gt 8 -or -not $fuentes.ContainsKey($Tipo)) { return $false }
    $codigo = $fuentes[$Tipo]
    # Busca una declaración, no una llamada: tipo de retorno o constructor y cuerpo/';'.
    $patron = '(?m)(?:^|[;{}])\s*(?:@Override\s*)?(?:(?:public|private|protected|static|final|abstract|default|synchronized)\s+)*(?:<[^\r\n]+?>\s*)?(?:[\w.<>,?\[\]]+\s+)+' + [regex]::Escape($Metodo) + '\s*\([^;{}]*\)\s*(?:throws\s+[^;{}]+)?[;{]'
    if ($codigo -match $patron) { return $true }
    # Los accesores de componentes de record existen por generación del compilador.
    foreach ($r in [regex]::Matches($codigo, '\brecord\s+' + [regex]::Escape($Tipo) + '\s*\(([^)]*)\)')) {
        if ($r.Groups[1].Value -match ('\b' + [regex]::Escape($Metodo) + '\s*(?:,|$)')) { return $true }
    }
    $herencia = [regex]::Match($codigo, '\b(?:class|interface)\s+' + [regex]::Escape($Tipo) + '(?:<[^>]+>)?\s+extends\s+(\w+)')
    if ($herencia.Success) { return Tiene-Metodo $herencia.Groups[1].Value $Metodo ($Nivel + 1) }
    return $false
}
function Comprobar-Metodo([string]$Tipo, [string]$Metodo, [string]$Archivo) {
    if (-not (Tiene-Metodo $Tipo $Metodo)) { throw "$Archivo : método inexistente $Tipo.$Metodo" }
}
$diagramas = @(Get-ChildItem -LiteralPath $PSScriptRoot -Filter '*.puml' | Sort-Object Name)
if ($diagramas.Count -ne 17) { throw 'Se esperaban los 17 diagramas del catálogo.' }
$cuentaTipos = 0; $cuentaMetodos = 0; $cuentaAtributos = 0
foreach ($diagrama in $diagramas) {
    $contenido = [IO.File]::ReadAllText($diagrama.FullName)
    if ([regex]::Matches($contenido, '(?m)^@startuml\s*$').Count -ne 1 -or
        [regex]::Matches($contenido, '(?m)^@enduml\s*$').Count -ne 1) { throw "Delimitadores inválidos: $($diagrama.Name)" }
    if ($contenido -match '(?im)^\s*!include(?:url)?\s+https?://') { throw 'No se permiten includes remotos.' }
    $actual = $null
    foreach ($linea in ($contenido -split '\r?\n')) {
        if ($linea -match '^\s*(?:abstract\s+)?(?:class|interface|enum)\s+(?:"([^"]+)"|([\w.]+))') {
            $nombre = if ($Matches[1]) { $Matches[1] } else { $Matches[2] }
            $actual = Comprobar-Tipo $nombre $diagrama.Name
            $cuentaTipos++
            if ($linea -notmatch '\{') { $actual = $null }
            continue
        }
        if ($linea -match '^\s*}\s*$') { $actual = $null; continue }
        if ($actual -and $linea -match '^\s*(?:\{(?:static|abstract)\}\s*)?[+#~\-](\w+)\s*\(') {
            Comprobar-Metodo $actual $Matches[1] $diagrama.Name
            $cuentaMetodos++
        } elseif ($actual -and $linea -match '^\s*[+#~\-](\w+)\s*:') {
            $atributo = $Matches[1]
            if ($fuentes[$actual] -notmatch ('\b' + [regex]::Escape($atributo) + '\s*(?:[;=,)]|\r?\n)')) {
                throw "$($diagrama.Name) : atributo inexistente $actual.$atributo"
            }
            $cuentaAtributos++
        } elseif ($actual -and $linea -match '^\s*([A-Z][A-Z_]+)\s*$') {
            if ($fuentes[$actual] -notmatch ('\b' + $Matches[1] + '\b')) { throw 'Constante enum inexistente.' }
        }
    }
    # Secuencias: comprueba clases locales de participantes y métodos de los mensajes.
    $participantes = @{}
    foreach ($linea in ($contenido -split '\r?\n')) {
        if ($linea -match '^participant\s+(\w+)\s+as\s+(\w+)\s*$') {
            $participantes[$Matches[2]] = Comprobar-Tipo $Matches[1] $diagrama.Name
            $cuentaTipos++
        }
    }
    foreach ($linea in ($contenido -split '\r?\n')) {
        if ($linea -match '^\w+\s*->\s*(\w+)\s*:\s*(\w+)\s*\(') {
            $destino = $Matches[1]; $metodo = $Matches[2]
            if ($participantes.ContainsKey($destino)) {
                Comprobar-Metodo $participantes[$destino] $metodo $diagrama.Name
                $cuentaMetodos++
            }
        }
    }
}
$schema = [IO.File]::ReadAllText((Join-Path $raiz 'sql/instalacion/schema.sql'))
$er = [IO.File]::ReadAllText((Join-Path $PSScriptRoot '01_entidad_relacion.puml'))
$tablas = [regex]::Matches($schema, '(?s)CREATE TABLE (?:IF NOT EXISTS\s+)?(\w+)\s*\((.*?)\n\);|CREATE TABLE (?:IF NOT EXISTS\s+)?(cliente|propietario)\s*\((.*?)\);')
# Analizar tabla por límites de CREATE TABLE; separa columnas de CHECK/EXCLUDE.
$cantidadColumnas = 0
$tablasSql = [regex]::Matches($schema, '(?is)CREATE TABLE (\w+)\s*\((.*?)\n\);')
foreach ($tablaSql in $tablasSql) {
    $tabla = $tablaSql.Groups[1].Value
    $sqlTabla = $tablaSql.Groups[2].Value
    $columnasSql = @([regex]::Matches($sqlTabla, '(?im)^\s+(\w+)\s+(?:bigint|varchar|character|text|boolean|smallint|integer|numeric|date|timestamptz|timestamp|jsonb)\b') | ForEach-Object { $_.Groups[1].Value })
    $bloque = [regex]::Match($er, '(?s)entity\s+"[^"]+"\s+as\s+' + $tabla + '\s*\{(.*?)\}').Groups[1].Value
    $columnasDiagrama = @([regex]::Matches($bloque, '(?m)^\s*\*?\s*(\w+)\s*:') | ForEach-Object { $_.Groups[1].Value })
    if (@(Compare-Object $columnasSql $columnasDiagrama).Count -ne 0) { throw "Columnas incoherentes en $tabla" }
    foreach ($columna in $columnasDiagrama) {
        $umlLinea = [regex]::Match($bloque, '(?m)^\s*\*?\s*' + $columna + '\s*:.*$').Value
        foreach ($clave in @(@('PRIMARY KEY','PK'),@('FOREIGN KEY','FK'),@('UNIQUE','UQ'))) {
            $tiene = $false
            foreach ($restriccion in [regex]::Matches($schema, '(?is)ALTER TABLE ONLY ' + $tabla + '\s+ADD CONSTRAINT \w+ ' + $clave[0] + '\s*\(([^)]+)\)')) {
                if (($restriccion.Groups[1].Value -split ',\s*') -contains $columna) { $tiene=$true }
            }
            if ($tabla -eq 'usuario' -and $columna -eq 'correo_normalizado' -and $clave[1] -eq 'UQ') { $tiene=$schema -match 'CREATE UNIQUE INDEX ux_usuario_correo_normalizado ON usuario' }
            if ($tiene -ne ($umlLinea -match ('\b' + $clave[1] + '\b'))) { throw "Clave incoherente: $tabla.$columna $($clave[1])" }
        }
    }
    $cantidadColumnas += $columnasSql.Count
}
if ($tablasSql.Count -ne 19 -or [regex]::Matches($er,'(?m)^entity\s').Count -ne 19) { throw 'Deben existir diez tablas del profesor y nueve de soporte.' }
Write-Output "OK: $($diagramas.Count) diagramas; $cuentaTipos tipos, $cuentaMetodos metodos, $cuentaAtributos atributos; 19 tablas y $cantidadColumnas columnas comprobadas."
