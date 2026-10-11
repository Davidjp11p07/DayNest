# DayNest: filtros por tipo y entorno

## Inspección y propuesta aprobada

Las cinco tarjetas de `inicio.jsp` eran elementos `<li>` sin enlaces ni eventos. `PublicoServlet`, `InmuebleServiceImpl` e `InmuebleDaoJdbc` consultaban ciudad (texto normalizado o ID), fechas, precio y capacidad. Conservamos esa cadena y el orden por moneda, tarifa e ID.

La inspección de `daynest.inmueble` encontró `tipo` obligatorio con 47 APARTAMENTO y 32 CASA; no existían atributos de entorno. La migración anterior había asignado APARTAMENTO automáticamente a 14 inmuebles heredados. Los catálogos reales contienen 2 países, 34 divisiones y 33 ciudades, incluidas ambas Cartagena.

El usuario aprobó expresamente: admitir CABAÑA y tipo pendiente; añadir únicamente `es_rural` y `cerca_mar`, booleanos independientes y opcionales; dejar pendientes los 14 tipos heredados; conservar los otros 65 tipos; dejar inicialmente pendientes los 79 entornos; permitir edición por el propietario. El SVG conceptual original se conserva sin cambios. Esta extensión de INMUEBLE queda documentada aquí, sin incorporar entidades, categorías ficticias ni cambios en las operaciones financieras.

## Comportamiento

| Tarjeta | Parámetro GET | Condición parametrizada |
|---|---|---|
| Apartamentos | `tipo=APARTAMENTO` | `i.tipo = ?` |
| Casas | `tipo=CASA` | `i.tipo = ?` |
| Cabañas | `tipo=CABAÑA` | `i.tipo = ?` |
| En el campo | `rural=true` | `i.es_rural = ?` |
| Junto al mar | `mar=true` | `i.cerca_mar = ?` |

`tipo` conserva HABITACION y OTRO. NULL significa pendiente; en entorno, FALSE significa que el propietario confirmó que NO tiene esa característica. NULL no coincide con filtros de sí ni de no. Una propiedad puede ser rural y costera simultáneamente, independientemente de su tipo. No deducimos el entorno por ciudad, título ni fotografía.

Los filtros se combinan mediante AND con `paisId`, `divisionId`, `ciudadId`/`ciudad`, `entrada`, `salida`, `minimo`, `maximo` y `capacidad`. Los países y regiones se consultan desde sus tablas; Cundinamarca también aparece aunque no tenga ciudad en el catálogo actual. Los homónimos mantienen la selección por ID y los nombres sin tilde mantienen la normalización existente. Un país o región incompatible con la ciudad produce cero coincidencias, sin mezclar identidades.

La búsqueda sigue excluyendo ocupaciones activas que cruzan el intervalo [entrada, salida), incluidas reservas pendientes y bloqueos. No cambian la expiración, capacidad, pagos, devoluciones, giros ni sus restricciones. Los términos económicos históricos permanecen pactados en RESERVA.

Las tarjetas son enlaces utilizables con teclado y sin JavaScript. Si JavaScript está disponible, conservan los criterios escritos en el buscador antes de enviarlo. En resultados hay selectores y etiquetas con enlaces para quitar individualmente cada filtro nuevo, manteniendo el resto. La paginación conserva los parámetros. El estado vacío permite modificar o quitar filtros. Los valores desconocidos se rechazan con HTTP 400, sin incorporarlos a SQL.

## Clasificar desde la interfaz

Entrar como propietario → Mis inmuebles → Editar (o Publicar). La sección “Clasificación del alojamiento” permite elegir tipo y responder, por separado, “¿Está en el campo?” y “¿Está cerca del mar?”. Las tres opciones admiten pendiente; los entornos también sí/no confirmado. Solo el dueño activo puede guardar la clasificación; se conserva CSRF y la salida escapada de JSTL.

La publicación compatible sin clasificación también guarda NULL: no asigna un tipo por defecto. La edición compatible que no recibe los atributos nuevos conserva su clasificación anterior. Cambiar etiquetas no cambia importes pactados ni beneficiarios históricos.

## Migración y respaldo

`schema.sql` incorpora los atributos para instalaciones nuevas. `migrar-filtros.sql` modifica solo la tabla existente, sin borrar ni recrear datos. Retira NOT NULL de tipo, amplía su CHECK y añade dos booleanos sin valor predeterminado. Deja NULL únicamente los APARTAMENTO heredados con ID 1–14 y título coincidente con `public.inmueble`.

`MigrarFiltros` ejecuta el SQL dentro de una transacción, con bloqueo y límite de espera. Compara cantidades y hashes de todas las filas de los esquemas operativo y public. Para INMUEBLE normaliza únicamente el cambio de tipo aprobado e ignora las columnas nuevas; el ensayo comprueba además que todos los entornos nuevos siguen NULL. Las filas financieras se comparan completas. Ante una diferencia inesperada hace rollback. El ejecutor permite reejecución sin modificar datos ya migrados y rechaza esquemas no autorizados.

Antes de aplicar, el ejecutor exige un ensayo exitoso y hashes SHA-256 de los SQL coincidentes. El ensayo restaura pg_dump en una base UUID independiente, comprueba migración, reejecución y lectura por DAO, y elimina solo esa base temporal. La aplicación exige Jetty detenido, verifica la base operativa y realiza otro respaldo legible con pg_restore antes de modificarla.

Respaldo de archivos antes de editar: `.tools/antes-filtros/`.

Respaldos PostgreSQL completos comprobados:

- Inicial: `.tools/respaldos/antes-modelo-20261010-022807.dump`.
- Ensayo: `.tools/respaldos/antes-modelo-20261010-024019.dump`.
- Inmediatamente antes de aplicar: `.tools/respaldos/antes-modelo-20261010-024102.dump`.

Evidencias: `.tools/ensayo-filtros-aprobado.properties`, `.tools/dao-clon-filtros.log` y `.tools/huellas-filtros-operativa.txt`. Los 79 inmuebles, cuentas, bloqueos, reservas y movimientos se conservan, al igual que el archivo public de 10 reservas, 6 pagos y 4 giros. El SVG original conserva SHA-256 `112F991FC0FEAF7EE9B8360C629DA6F6B7C1E7CB69E913D31331C8D39AB91BB2`.

Para ejecutar en este entorno (Java 17 y PostgreSQL 18 locales configurados según README):

```powershell
.\mvnw.cmd -o test
.\mvnw.cmd -o '-DskipTests' package
java '-Dfile.encoding=UTF-8' -cp 'target/classes;target/lib/*' co.edu.rentainmuebles.verification.MigrarFiltros --ensayar
# Detener Jetty antes de aplicar:
java '-Dfile.encoding=UTF-8' -cp 'target/classes;target/lib/*' co.edu.rentainmuebles.verification.MigrarFiltros --aplicar
.\mvnw.cmd -o jetty:run
```

No ejecutar reset.sql ni volver a cargar data.sql sobre la base operativa. Para recuperar valores anteriores, restaurar el respaldo en otra base y comparar; no sobrescribir datos generados después de la migración ni eliminar atributos ya clasificados.

## Datos que siguen pendientes

Después de la migración: 33 APARTAMENTO, 32 CASA y 14 tipos pendientes; 79 `es_rural` y 79 `cerca_mar` pendientes. No hay CABAÑA registrada todavía. Por eso Cabañas, En el campo y Junto al mar muestran legítimamente un estado vacío hasta que los propietarios clasifiquen sus inmuebles. No se han alterado los datos de presentación para fabricar resultados.

## Componentes

- Modelo y contratos: `FiltrosInmueble`, atributos de `Inmueble`, interfaces `InmuebleDao`, `InmuebleService` y catálogo geográfico `CiudadDao`/`CatalogoService`.
- Persistencia/servicios: `InmuebleDaoJdbc`, `CiudadDaoJdbc`, `InmuebleServiceImpl`, `MigrarFiltros` y SQL incremental.
- Controladores: `PublicoServlet`, `PropietarioServlet`, lectores tipados de parámetros en `Web`.
- JSP: inicio, resultados, buscador, filtros activos, estado vacío y formulario del propietario.
- Presentación: enlaces accesibles y etiquetas en daynest.css; conservación progresiva de criterios en daynest.js.
- Pruebas: `FiltrosInmuebleTest` y adaptación de la comparación de DDL en `MigracionSvgTest`.

## Verificación

Línea base: 58 pruebas existentes aprobadas. La suite ampliada contiene 65 pruebas, con siete casos nuevos que cubren los cinco filtros, superposición de características, NULL y FALSE, combinaciones de país/región/ciudad homónima/precio/capacidad, ordenamiento, disponibilidad de reservas pendientes y bloqueos, intervalos adyacentes, clasificación y autorización del propietario, tipos inválidos, resultados vacíos y conservación de reservas/cobros/giros durante clasificación y migración.

Las pruebas usan únicamente esquemas UUID aislados; nunca truncan daynest ni public. Los logs de la ejecución final y las comprobaciones HTTP se conservan en `.tools/`.

Resultado final JUnit: **65 pruebas aprobadas, 0 fallos, 0 errores y 0 omitidas**; `package` generó `target/DayNest.war`. Se ensayó la migración sobre un respaldo restaurado antes de aplicarla a PostgreSQL.

Verificación automatizada en Chrome real: **26 comprobaciones de recorrido aprobadas**, registradas en `.tools/verificacion-filtros-http.json`. Incluye las cinco tarjetas con teclado en 1440/768/390 px; ausencia de desbordamiento; publicación y edición de tres clasificaciones explícitas; persistencia de sí/no; formulario inválido que conserva clasificación; ciudad homónima elegida por ID; conservación de criterios escritos; eliminación de un filtro sin perder los demás; estado vacío; búsqueda sin tilde; paginación; HTTP 400 para valores desconocidos; fechas/capacidad/detalle; enlaces sin JavaScript. No hubo errores JavaScript ni solicitudes externas en el contexto controlado. Las capturas están en `.tools/filtros-inicio-1440.png`, `-768.png` y `-390.png`.

Se verificó mediante navegador automatizado y se inspeccionó visualmente la captura móvil; no se presenta como una prueba manual realizada por el usuario. Las altas y cambios de clasificación se hicieron exclusivamente en un esquema HTTP UUID, comprobado contra la línea de comandos del proceso en 8081; el esquema fue eliminado al terminar. No se fabricaron clasificaciones en daynest.

Guion reproducible: `src/test/browser/filtros_inmuebles.py`. Requiere Chrome local y el Playwright ya disponible en `.tools/daynest-python`, además de una instancia aislada en 8081. Se niega a ejecutarse si el proceso no contiene `-Drenta.db.schema=renta_http_UUID` coincidente con `.tools/http-modelo-schema.txt`. Para prepararlo, con Jetty operativo detenido (ambas instancias usan el directorio temporal de Maven), ejecutar `PrepararVerificacionHttp --crear`, iniciar `.tools/iniciar-jetty-filtros-http.ps1` y ejecutar `python src/test/browser/filtros_inmuebles.py`. Detener la instancia de prueba antes de usar `PrepararVerificacionHttp --eliminar renta_http_UUID` y volver a iniciar Jetty operativo. No repetir el guion sobre un fixture ya modificado: crear uno nuevo.

Las primeras ejecuciones del guion detectaron problemas de preparación, sin fallos de las consultas: datos temporales repetidos, un supuesto incorrecto de que descripción vacía sería rechazada y un campo de precio en un desplegable cerrado. Se renovó el fixture, se usó capacidad inválida con espera de navegación y se abrió el desplegable; el recorrido completo posterior pasó. Las 65 pruebas Java no fallaron.

Pendiente únicamente de información real: clasificar los 14 tipos heredados y los entornos de los 79 inmuebles. Las categorías sin datos confirmados siguen vacías de manera intencional. No se realizaron cambios en diagramas, datos de presentación, dependencias, ni servicios/DAO financieros.

Comprobación operativa adicional: la base conserva 79 inmuebles, **78 PUBLICADO y 1 INACTIVO**; el estado inactivo previo se conserva según las huellas de migración. Por eso la búsqueda sin filtros muestra 78 resultados, no 79. Un supuesto inicial del guion operativo se corrigió tras consultar este dato real; no se reactivó ningún inmueble para ajustar la prueba. Los entornos pendientes se corroboraron en lectura: 79 NULL en cada atributo.

Resultado del recorrido operativo: **10 comprobaciones aprobadas** en `.tools/verificacion-filtros-operativa.json`, sin errores JavaScript ni solicitudes externas. Se verificaron las cinco tarjetas con cantidades reales, los 78 publicados, la búsqueda sin tilde, el detalle, el formulario del propietario inicialmente pendiente y las vistas administrativas de inmuebles, reservas, pagos, giros y archivo anterior. Solo hubo consultas y autenticación; no se guardaron clasificaciones ni operaciones financieras. DayNest quedó iniciado en `http://localhost:8080/DayNest/inicio`.
