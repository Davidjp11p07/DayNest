# DayNest — Tu espacio, tus días.

Aplicación académica web Java 17, Jakarta Servlet/JSP/JSTL, JDBC y PostgreSQL. Fuente conceptual vigente: `docs/modelo_er_daynest.svg`, con **diez entidades** y la relación RESERVA. Su transformación utiliza **13 tablas**, incluidas OCUPACION/BLOQUEO como extensión autorizada. [Trazabilidad, normalización y adaptación](docs/ADAPTACION_SVG.md). Pagos, devoluciones y giros **simulados**, sin banco ni pasarela real. No hay impuestos, descuentos, comisión ni conversión de moneda.

## Ejecutar en este equipo

PostgreSQL debe estar iniciado. Desde la raíz, PowerShell:

```powershell
$env:JAVA_HOME = (Get-ChildItem .tools/jdk17 -Directory | Select-Object -First 1).FullName
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
$env:MAVEN_USER_HOME = Join-Path $pwd '.tools/maven-usuario'
.\mvnw.cmd -o jetty:run
```

Abra **http://localhost:8080/DayNest/inicio**. El cambio es el contexto de la aplicación; el servidor continúa en localhost:8080. El paquete es **target/DayNest.war**. Todas las rutas se construyen a partir de `request.contextPath`. Ctrl+C detiene el servidor.

JDK y dependencias Maven están preparados en `.tools/`; consérvelos al trasladar el proyecto para trabajar sin internet. Las fuentes Manrope/Inter, SVG, CSS, JS y 14 fotos se sirven localmente desde `src/main/webapp/assets`. No se necesitan Python, Docker ni servicios externos para ejecutar la aplicación.

## Instalación en otra base

Se requiere JDK 17, PostgreSQL con permiso para crear esquemas y la extensión btree_gist, y Maven Wrapper/caché. Cree `renta_inmuebles` desde Query Tool conectado a postgres:

```sql
CREATE DATABASE renta_inmuebles;
```

Copie `src/main/resources/db.properties.example` a `db.properties` y complete sus credenciales locales. La configuración usada por la aplicación es:

```properties
db.url=jdbc:postgresql://localhost:5432/renta_inmuebles
db.usuario=postgres
db.contrasena=SU_CONTRASENA_LOCAL
db.esquema=daynest
```

```powershell
.\mvnw.cmd -o package
java -cp "target/classes;target/lib/*" co.edu.rentainmuebles.verification.InicializarBD
java -cp "target/classes;target/lib/*" co.edu.rentainmuebles.verification.InspeccionarBD
```

InicializarBD crea el esquema nuevo y ejecuta schema.sql/data.sql transaccionalmente. Rechaza un esquema existente. **schema.sql no es reejecutable**: contiene CREATE TABLE/FUNCTION de la transformación adaptada al SVG. **data.sql y geografia.sql son reejecutables** y evitan duplicar el catálogo y los alojamientos demo. usuario almacena correo_normalizado canónico; movimientos y giros obtienen su moneda de la reserva histórica, sin duplicarla.

Para adaptar una instalación con el modelo anterior de trece tablas, el procedimiento incremental es:

```powershell
java -cp "target/classes;target/lib/*" co.edu.rentainmuebles.verification.MigrarModeloSvg --ensayar
# Detenga Jetty antes de aplicar. No ejecute reset ni inicializadores.
java -cp "target/classes;target/lib/*" co.edu.rentainmuebles.verification.MigrarModeloSvg --aplicar
```

El ensayo restaura un pg_dump completo en una base temporal, comprueba migración, reversión, conservación de filas y DAO, y retira únicamente esa base. Aplicar exige ensayo vigente, respaldo verificado y servidor detenido. `migrar-svg.sql`/`revertir-svg.sql` conservan IDs e importes; no modifican el archivo public. Detalles y resultados en [verificación de adaptación](docs/ADAPTACION_SVG.md#verificación-de-la-adaptación). No vuelva a aplicar si ya está adaptada.

## Migración desde las siete entidades

La migración local ya se ejecutó. No vuelva a inicializar ni reinicie los datos para probar.

```powershell
java -cp "target/classes;target/lib/*" co.edu.rentainmuebles.verification.MigrarModeloDefinitivo --confirmar
```

El comando hace pg_dump antes de crear `daynest`, importa identidades/perfiles e inmuebles con sus IDs y añade el catálogo. **No elimina ni escribe las tablas antiguas de public**. Reservas, pagos y giros anteriores permanecen como archivo íntegro y pueden consultarse desde **Administración → Historial anterior**. No se reinterpretan como giros sin comisión ni se crean nuevos giros para esas reservas. Las reservas antiguas confirmadas y todavía vigentes se representan mediante bloqueos en el calendario nuevo. Los pendientes anteriores quedan en la consulta histórica; para operar bajo las nuevas reglas se crea una reserva nueva.

Respaldo local: `.tools/respaldos/antes-modelo-20261010-005100.dump`. Contiene datos y hashes; no se distribuye públicamente. Código/documentación anteriores: `.tools/antes-modelo-definitivo/`. El comando repetido detecta `daynest.ciudad` y no recarga ni reemplaza datos. Si falla una conversión, revierte la transacción.

## Ciudades, fotos y precios

**33 ciudades**: 32 capitales colombianas y Cartagena, Región de Murcia, España. **79 inmuebles** tras migrar: 14 conservados + 64 nuevos en Colombia + uno español. Cada capital colombiana tiene al menos dos nuevos alojamientos publicados. Las tarifas demo son 125000–315000 COP/noche en Colombia y 95 EUR/noche en España; no son cotizaciones comerciales.

Bogotá usa DIVIPOLA 11001 y pertenece al Distrito Capital, división 11. Cundinamarca (25) permanece como división independiente sin inventar un código municipal para Bogotá. Cartagena Colombia usa 13001; Cartagena España, INE 30016. Un nombre no es identificador único.

El autocompletado muestra **ciudad, región y país** desde PostgreSQL. Escribir B sugiere Barranquilla, Bogotá y Bucaramanga; también funciona sin tildes. Se selecciona con clic o flechas + Enter; Escape cierra. El formulario envía `ciudadId`. Sin selección, buscar Cartagena muestra ambas ciudades; al seleccionar una se filtra por su ID. La publicación/edición exige elegir una ciudad del catálogo. Precios y totales muestran COP/EUR; los resúmenes administrativos se agrupan por moneda.

Las fotografías son ilustrativas y se asignan por ID desde una lista de archivos locales; no se agregó tabla de imágenes. Cada par de alojamientos recibe fotografías diferentes. [Créditos y fuentes](docs/diseno/CREDITOS_IMAGENES.md).

## Reglas del modelo

| Área | Comportamiento |
| --- | --- |
| Identidad | usuario + cliente/propietario/administrador por composición; especialización parcial y solapada |
| Ubicación | pais → division_administrativa → ciudad → inmueble; códigos únicos en su ámbito |
| Calendario | ocupacion → reserva o bloqueo; especialización total y disjunta; exclusión GiST de intervalos activos [inicio,fin) |
| Reserva | PENDIENTE_PAGO → CONFIRMADA → FINALIZADA; cancelación/expiración liberan fechas; los términos pactados son inmutables |
| Vencimiento | 30 minutos para pagar; tarea local cada 60 segundos expira pendientes y finaliza estancias vencidas |
| Cobros | movimiento_pago, clase COBRO/DEVOLUCION; PENDIENTE → APROBADO/RECHAZADO; claves idempotentes, moneda pactada, sin sobrecobro |
| Devolución | ligada a un cobro aprobado de la misma reserva; cancelación pagada devuelve los cobros mediante simulación transaccional |
| Giro | PENDIENTE → PROCESANDO → EJECUTADO/FALLIDO; solo tras finalizar, administrador autorizador, importe igual al neto cobrado, propietario histórico |
| Historial | prohibido DELETE de reservas, movimientos y giros; intentos fallidos se conservan y permiten nuevo intento |

Total = noches × precio pactado. Saldo = total − (cobros aprobados − devoluciones aprobadas). El medio elegido en la pantalla de pago es una opción de la simulación; el SQL definitivo no contiene columna de medio de pago. El botón de pago liquida el saldo restante. DAO/BD admiten varios movimientos y protegen sus importes.

Los bloqueos se crean/liberan en **Propietario → Mis inmuebles → Bloquear / liberar fechas**. Las fechas/inmueble/tipo de ocupación no se reescriben. Cambiar el dueño no cambia el propietario pactado de las reservas.

## Cuentas demo

| Rol | Correo | Contraseña |
| --- | --- | --- |
| Administrador | admin@renta.test | Admin2026! |
| Cliente y propietario (Paula) | propietario1@renta.test | Propietario2026! |
| Propietario | propietario2@renta.test | Propietario2026! |
| Propietario | propietario3@renta.test | Propietario2026! |
| Cliente | cliente1@renta.test | Cliente2026! |
| Cliente | cliente2@renta.test | Cliente2026! |
| Cliente | cliente3@renta.test | Cliente2026! |
| Cliente | cliente4@renta.test | Cliente2026! |

Las cuentas previas conservaron sus hashes y contraseñas. PBKDF2-HMAC-SHA256, 600000 iteraciones, sal individual y comparación constante. Los usuarios registrados no pueden darse el perfil administrador.

## Verificación

```powershell
.\mvnw.cmd -o test
# Con Jetty iniciado, solo consultas HTTP:
.\mvnw.cmd -o '-Dtest=WebHttpIT' test
.\mvnw.cmd -o '-DskipTests' package
```

JUnit crea esquemas renta_test_UUID y los elimina al terminar; no reinicia la base activa ni public. El contrato anterior de siete tablas fue sustituido por pruebas del SQL definitivo; sus fuentes se conservaron en el respaldo previo. Las verificaciones incluyen aislamiento, concurrencia, intervalos adyacentes, bloqueos, capacidad, términos históricos, cobro completo, idempotencia, devoluciones, expiración y giro neto.

El modelo impide fabricar reservas históricas mediante el flujo normal. Para comprobar un giro inmediatamente, JUnit prepara una estancia histórica únicamente en su esquema temporal; en la aplicación, el giro se habilita cuando llega la fecha de salida. **PrepararDemo del modelo anterior ya no inserta datos incompatibles.**

[Guion de prueba](docs/GUION_DEMO.md) · [Modelo y migración](docs/MODELO_DEFINITIVO.md) · [Diagramas](docs/diagramas/README.md) · [Diseño DayNest](docs/diseno/DISENO.md).

Las reorganizaciones visuales completas de las tandas 2 y 3 siguen pendientes de confirmación. Los cambios presentes corresponden al nuevo modelo y a la búsqueda autorizados.

## Filtros de alojamientos

Las cinco tarjetas de inicio consultan PostgreSQL por tipo (Apartamento, Casa, Cabaña) o entorno independiente (campo, mar). En resultados pueden combinarse con país, región, ciudad, fechas, precio y capacidad, y quitarse individualmente. Los propietarios clasifican sus alojamientos desde Publicar/Editar; pendiente no equivale a “no”.

La extensión autorizada añade solo `inmueble.es_rural` y `inmueble.cerca_mar`, permite tipo pendiente y CABAÑA. Conserva los datos y las reglas financieras y de disponibilidad. Las categorías nuevas pueden quedar vacías hasta clasificar los inmuebles. Consulte [implementación, migración y verificaciones](docs/FILTROS_INMUEBLES.md).

## Documentación del proyecto

| Documento | Para qué sirve |
| --- | --- |
| [Informe de diseño](docs/INFORME.md) · [PDF](docs/INFORME.pdf) · [HTML](docs/INFORME.html) | Diagramas actualizados y 20 fichas ampliadas; portada pendiente de datos académicos |
| [Guion de prueba](docs/GUION_DEMO.md) | Probar la aplicación y consultar ciudades en pgAdmin |
| [Modelo y transición](docs/MODELO_DEFINITIVO.md) | Comprender las tablas, reglas y archivo del modelo anterior |
| [Adaptación al SVG](docs/ADAPTACION_SVG.md) | Trazabilidad conceptual, normalización, migración y su verificación histórica |
| [Filtros de alojamientos](docs/FILTROS_INMUEBLES.md) | Clasificar inmuebles y entender los filtros actuales y su migración |
| [Diagramas](docs/diagramas/README.md) | Localizar las fuentes PlantUML y regenerarlas |
| [Comprobación de fuentes](docs/diagramas/VERIFICACION.md) | Registro de comprobación de los diagramas actuales y límites de la validación |
| [Diseño DayNest](docs/diseno/DISENO.md) | Colores, fuentes, componentes y reglas visuales |
| [Créditos de imágenes](docs/diseno/CREDITOS_IMAGENES.md) | Autores, fuentes y licencias de fotos, fuentes e iconos |

Los informes de etapas y versiones superadas se retiraron de la documentación activa. Su copia y la documentación anterior a la limpieza están en `.tools/documentacion-retirada-20261010-033253/`.
