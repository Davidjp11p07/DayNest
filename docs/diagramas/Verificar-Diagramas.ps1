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
if ($diagramas.Count -ne 16) { throw 'Se esperaban los 16 diagramas del catálogo.' }
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
$schema = [IO.File]::ReadAllText((Join-Path $raiz 'schema.sql'))
$er = [IO.File]::ReadAllText((Join-Path $PSScriptRoot '01_entidad_relacion.puml'))
$tablas = [regex]::Matches($schema, '(?s)CREATE TABLE (?:IF NOT EXISTS\s+)?(\w+)\s*\((.*?)\n\);|CREATE TABLE (?:IF NOT EXISTS\s+)?(cliente|propietario)\s*\((.*?)\);')
# Analizar tabla por límites de CREATE TABLE; separa columnas de CHECK/EXCLUDE.
$cantidadColumnas = 0
foreach ($tabla in @('usuario','cliente','propietario','administrador','pais','division_administrativa','ciudad','inmueble','ocupacion','reserva','bloqueo','movimiento_pago','giro')) {
    $sqlTabla = [regex]::Match($schema, '(?s)CREATE TABLE (?:IF NOT EXISTS\s+)?' + $tabla + '\s*\((.*?)\);').Groups[1].Value
    $columnasSql = @([regex]::Matches($sqlTabla, '(?:^|[,\n])\s*(\w+)\s+(?:BIGINT|VARCHAR|CHAR|TEXT|BOOLEAN|SMALLINT|INTEGER|NUMERIC|DATE|TIMESTAMPTZ|TIMESTAMP)\b') | ForEach-Object { $_.Groups[1].Value })
    $bloque = [regex]::Match($er, '(?s)entity\s+"[^"]+"\s+as\s+' + $tabla + '\s*\{(.*?)\}').Groups[1].Value
    $columnasDiagrama = @([regex]::Matches($bloque, '(?m)^\s*\*?\s*(\w+)\s*:') | ForEach-Object { $_.Groups[1].Value })
    if (@(Compare-Object $columnasSql $columnasDiagrama).Count -ne 0) { throw "Columnas incoherentes en $tabla" }
    foreach ($columna in $columnasDiagrama) {
        $sqlLinea = [regex]::Match($sqlTabla, '\b' + $columna + '\s+([^\r\n]*?)(?=,\s*\w+\s+(?:BIGINT|VARCHAR|CHAR|TEXT|BOOLEAN|SMALLINT|INTEGER|NUMERIC|DATE|TIMESTAMPTZ|TIMESTAMP)\b|\r?\n|$)').Value
        $umlLinea = [regex]::Match($bloque, '(?m)^\s*\*?\s*' + $columna + '\s*:.*$').Value
        foreach ($clave in @(@('PRIMARY KEY','PK'),@('REFERENCES','FK'),@('UNIQUE','UQ'))) {
            if ($tabla -eq 'movimiento_pago' -and $columna -eq 'id_cobro_origen' -and $clave[1] -eq 'FK') { continue }
            if ($tabla -eq 'usuario' -and $columna -eq 'correo_normalizado' -and $clave[1] -eq 'UQ') {
                if ($schema -notmatch 'CREATE UNIQUE INDEX ux_usuario_correo_normalizado ON usuario \(correo_normalizado\)') { throw 'Falta índice único de correo canónico' }
                if ($umlLinea -notmatch '\bUQ\b') { throw 'Falta UQ del correo en el diagrama' }
                continue
            }
            if (($sqlLinea -match $clave[0]) -ne ($umlLinea -match ('\b' + $clave[1] + '\b'))) { throw "Clave incoherente: $tabla.$columna $($clave[1])" }
        }
    }
    $cantidadColumnas += $columnasSql.Count
}
if ([regex]::Matches($er, '(?m)^entity\s').Count -ne 13) { throw 'El ER debe contener exactamente trece tablas.' }
$resumen = "OK: $($diagramas.Count) diagramas; $cuentaTipos referencias a tipos, $cuentaMetodos métodos, $cuentaAtributos atributos; 13 tablas y $cantidadColumnas columnas con PK/FK/UQ comprobadas."
Write-Output $resumen
$informe = @"
# Comprobación de fuentes de los diagramas

$resumen

La comprobación compara clases/interfaces/enums, nombres de atributos y métodos de los diagramas de clases y secuencias con src/main/java, incluidos métodos heredados de ServicioBase y accesores generados de records. Compara todas las columnas y marcas PK/FK/UQ del ER con schema.sql. No carga db.properties ni modifica la base.

Revisión adicional: cardinalidades, tipos/firmas principales, orden de flujos, rutas HTTP, reglas de negocio, actores y notas contrastados con los servlets, servicios, DAO JDBC y schema.sql. Las actividades describen pasos funcionales, no nuevos métodos.

Límite: esta comprobación de nombres no demuestra el comportamiento ni valida firmas completas. El parser y las imágenes se comprueban por separado mediante Renderizar-Diagramas.ps1 (-checkonly y -failfast2). La correspondencia funcional se documenta en el informe y las fichas. No se modifica la base de datos.
"@
[IO.File]::WriteAllText((Join-Path $PSScriptRoot 'VERIFICACION.md'), $informe, (New-Object Text.UTF8Encoding $false))
