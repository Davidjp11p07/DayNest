# DayNest — Tu espacio, tus días

Aplicación académica web Java 17, Jakarta Servlet/JSP/JSTL, JDBC y PostgreSQL. El modelo vigente sigue las seis entidades del PDF del profesor y las aclaraciones sobre cuentas bancarias de clientes y propietarios. La version actual tiene diez tablas principales y nueve extensiones tecnicas que conservan autenticación, calendario y finanzas. [Modelo y trazabilidad](docs/MODELO_DEFINITIVO.md).

Los pagos, devoluciones y giros son simulados: no hay banco ni pasarela real. No hay impuestos, descuentos ni conversión monetaria. La comisión es configurable y se conserva en cada giro.

## Ejecutar en este equipo

Inicia PostgreSQL. Desde la raíz en PowerShell:

```powershell
$env:JAVA_HOME = (Get-ChildItem .tools/jdk17 -Directory | Select-Object -First 1).FullName
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
$env:MAVEN_USER_HOME = Join-Path $pwd '.tools/maven-usuario'
.\mvnw.cmd -o jetty:run
```

Abre **http://localhost:8080/DayNest/inicio**. Ctrl+C detiene Jetty. El WAR es `target/DayNest.war`. Los recursos se sirven localmente; no se necesitan Python ni Docker para ejecutar la aplicación. Conserva `.tools/` para disponer de JDK y dependencias sin internet. Cada puerto Jetty usa su propio directorio temporal.

## Estado de las migraciones

La adaptacion, la nomenclatura del profesor y la extension financiera estan aplicadas en la base operativa. Se conservaron los 80 inmuebles y las columnas originales de las 17 tablas. La comision inicial es 30 %, editable en Administracion > Giros. Respaldo previo: `.tools/respaldos/antes-modelo-20261017-004616.dump`.

```powershell
.\mvnw.cmd -o package
java -cp "target/classes;target/lib/*" co.edu.rentainmuebles.verification.MigrarGirosAutomaticos --ensayar
```

El ensayo crea un respaldo y una copia independiente. Compara las columnas originales de las 17 tablas y conserva la copia para revision; no aplica sobre la base operativa. Consulta [el plan financiero](docs/GIROS_AUTOMATICOS.md) y [el esquema relacional](docs/esquema_relacional.md).

Los migradores SVG y de modelos anteriores son historicos y usan fixtures separados en pruebas. No ejecutes reset, inicializadores ni reversiones sobre una base existente.

## Instalación nueva

Se requiere JDK 17, PostgreSQL con permiso para crear esquemas y extensión btree_gist, y Maven Wrapper/dependencias. Crea una base nueva:

```sql
CREATE DATABASE renta_inmuebles;
```

Copia `src/main/resources/db.properties.example` a `db.properties` y completa las credenciales locales. No publiques ese archivo.

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

`sql/instalacion/schema.sql` instala directamente las 19 tablas y las vistas de compatibilidad. El inicializador rechaza un esquema existente. `sql/instalacion/data.sql` carga geografía, alojamientos y usuarios de demostración; su recarga nunca crea administradores. `sql/demostracion/admin-demo.sql` crea explícitamente el administrador de una instalación demo nueva y rechaza bases con administrador. No cambia las credenciales personalizadas de una instalación existente.

## Modelo del profesor

Las diez tablas principales son `propietarios`, `inmuebles`, `ciudades`, `paises`, `banco`, `clientes`, `asocia`, `relaciona`, `vincula` y `renta`.

- Cliente y propietario son entidades completas separadas; `usuario` es el soporte técnico de acceso compartido. Puedes registrarte como cliente, propietario o ambos, y activar después el perfil complementario desde **Mis perfiles**.
- El precio por noche está en **ASOCIA**. El inmueble incluye dirección, área en m², habitaciones, capacidad y ciudad.
- **VINCULA** guarda banco, número y tipo de cuenta del cliente; **RELACIONA**, los del propietario. Son independientes. En **Mi cuenta → Mis datos y cuentas** puedes cambiarlos o copiar los tres datos a ambos perfiles mediante una casilla explícita. Deben estar activos ambos perfiles para copiar.
- El número se guarda como texto para conservar ceros iniciales: de 4 a 34 dígitos. Los tipos son AHORROS y CORRIENTE. No hay unicidad global que impida usar la misma cuenta en ambos perfiles.
- El cliente selecciona su ciudad de residencia; ciudad y banco pertenecen a un país. Administración → **Bancos** permite ampliar el catálogo. Los bancos demo son ficticios.
- Los datos desconocidos de registros anteriores (cuentas, residencia, área y habitaciones) aparecen pendientes de completar; no se inventan datos personales.

Nueve extensiones conservan acceso, geografia, calendario, pagos, giros, versiones de comision y liquidaciones. Las vistas de compatibilidad no duplican almacenamiento.

## Reservas, pagos y giros

La exclusión de intervalos activos `[entrada,salida)` impide solapamientos. La entrada debe ser posterior a hoy. La reserva conserva precio, moneda y propietario pactados aunque cambie el alojamiento. Total = noches × precio pactado. Los resúmenes separan COP y EUR; nunca los suman ni convierten.

El plazo de pago es de 30 minutos desde la creación, establecido por PostgreSQL. Una tarea cada 60 segundos expira pendientes y finaliza estancias. La base rechaza cobros y confirmaciones tardíos. Si Jetty está apagado, procesa los vencimientos al arrancar. La cancelación pagada previa a la entrada devuelve los cobros mediante simulación transaccional.

En la version actual, el cliente paga mediante transferencia bancaria simulada, usando su cuenta registrada o una cuenta ocasional. La cuenta ocasional solo se conserva en ese pago y no cambia las cuentas predeterminadas de ninguno de los perfiles. DayNest registra el importe y lo administra hasta la salida. El primer cobro aprobado fija la comision historica. El ciclo liquida las estancias finalizadas, con autor SISTEMA y cuenta capturada al ejecutar, si el pago esta completo y no hay devoluciones ni incidencias. Los casos pendientes o fallidos conservan motivo y se reintentan sin duplicar giros.

**Administracion > Giros** permite editar el porcentaje para los pagos nuevos y supervisar/revisar incidencias. El valor inicial acordado es 30 %. La tasa de operaciones ya pagadas y los giros anteriores no cambia al editarla. negocio.properties conserva el porcentaje de respaldo para esquemas anteriores y la zona horaria; tras la migracion, la configuracion persistida es la autoridad del porcentaje. No se necesita reiniciar para editarlo desde la cuenta administrativa.

El archivo de operaciones previas en `public` se consulta desde **Administración → Historial anterior**. No se convierte en nuevos cobros o giros.

## Cuentas demo de una instalación nueva

| Perfil | Correo | Contraseña |
| --- | --- | --- |
| Administrador | admin@renta.test | Admin2026! |
| Cliente y propietario | propietario1@renta.test | Propietario2026! |
| Cliente | cliente1@renta.test | Cliente2026! |

Usa las credenciales configuradas localmente si ya cambiaste estas cuentas. Los cambios locales no modifican las cuentas iniciales del script ni se publican aquí.

## Conexiones y pruebas

HikariCP reutiliza conexiones. Opciones de `db.properties`: `db.pool.maximo=10`, `db.pool.minimo=1`, `db.pool.esperaMs=10000`. Cada préstamo restablece esquema y zona horaria. El pool se cierra al detener la aplicación.

```powershell
.\mvnw.cmd -o test
```

JUnit crea esquemas temporales y los retira al terminar; no reinicia la base activa. Las pruebas incluyen perfiles, cuentas independientes/copiadas, permisos, residencia, área/habitaciones, migración, fechas, concurrencia, pagos, devoluciones, comisión, giros y pool. Los recorridos de `src/test/browser` exigen Jetty 8081 con un esquema HTTP UUID aislado; rechazan la base real.

## Documentación

- [Modelo vigente](docs/MODELO_DEFINITIVO.md) y [transformación del diagrama](docs/ADAPTACION_SVG.md).
- [Guion de demostración](docs/GUION_DEMO.md).
- [Informe Markdown](docs/INFORME.md), [HTML](docs/INFORME.html) y [PDF](docs/INFORME.pdf).
- [Fuentes y herramientas de diagramas](docs/diagramas/README.md).
- [Créditos de imágenes](docs/diseno/CREDITOS_IMAGENES.md).

## Demo opcional

sql/demostracion/datos-demo-profesor.sql agrega un pais, ciudad y banco ficticios, completa cuentas y residencia solo de correos @renta.test, y area/habitaciones pendientes solo de inmuebles de propietarios demo. Conserva cuentas existentes. **No se ejecuto sobre la base operativa.** Se recomienda una base de demostracion independiente.

Con el esquema final instalado y sql/instalacion/data.sql cargado, desde psql:

```sql
SET search_path TO daynest, public;
\i sql/demostracion/datos-demo-profesor.sql
```

Para instalar desde un clon nuevo sin dependencias descargadas, ejecuta primero `.\mvnw.cmd package` sin `-o`, con JDK 17 instalado. `-o` corresponde al equipo que ya tiene Maven y dependencias en cache. PostgreSQL debe estar iniciado y las credenciales locales en db.properties.

## Organizacion de scripts SQL

### Comisiones e ingresos por inmueble

**Administración → Comisiones e ingresos** muestra comisiones liquidadas de giros ejecutados, comisiones pendientes estimadas y detalle por inmueble y arriendo. **Propietario → Mis ingresos** muestra el neto liquidado a su favor y el pendiente estimado por cada inmueble, incluyendo inmuebles sin rentas e ingresos históricos como beneficiario de reservas anteriores.

Los importes se separan por moneda y usan la tasa histórica. Reservas canceladas/devueltas y pagos incompletos no se presentan como ganancias pendientes. El historial se filtra por inmueble y tiene páginas de 20 arriendos; los acumulados abarcan todo el filtro, no solo la página visible. Son importes de la simulación, no una billetera ni un saldo bancario. Estas consultas no añaden tablas o columnas ni modifican operaciones.

Verificación de esta ampliación: 159 pruebas sin fallos, incluyendo 14 casos de ingresos; siete controles en Chrome sobre un esquema HTTP aislado. Se comprobaron monedas, cancelaciones, varios cobros sin duplicar importes, tasa histórica, cambio de propietario, filtros, paginación y permisos.

### Transferencias y cuenta de uso único

En el pago se elige **Usar mi cuenta registrada** o **Usar otra cuenta solo para este pago**. La segunda opción pide banco del catálogo, número y tipo de cuenta; no reemplaza la cuenta predeterminada del cliente ni la del propietario. También permite pagar si aún no existe una cuenta predeterminada. El único método admitido es transferencia bancaria simulada.

Cada cobro conserva método, banco, cuenta, tipo y uso en `movimiento_pago.cuenta_bancaria_pactada` (JSONB). Administración e historial muestran la cuenta enmascarada. Una devolución conserva la cuenta del cobro original. Los pagos antiguos sin método registrado no se reclasifican como transferencias.

Para actualizar una instalación financiera anterior, con Jetty detenido y Java configurado:

```powershell
java -cp "target/classes;target/lib/*" co.edu.rentainmuebles.verification.MigrarPagosTransferencia --aplicar
```

El comando genera un respaldo y reemplaza funciones en una transacción, comparando las huellas de las 19 tablas. No crea tablas, no añade columnas y no modifica filas históricas. Una instalación nueva incluye estas funciones y comienza con la comisión del 30 %.

Actualización aplicada en este equipo con respaldo `.tools/respaldos/antes-modelo-20261017-054219.dump`. Verificación: 145 pruebas sin fallos y siete controles de navegador en un esquema aislado, incluyendo cuenta ocasional sin predeterminada, ausencia de cambios en ambos perfiles e historial administrativo.

Los scripts estan agrupados en `sql/instalacion`, `sql/demostracion`, `sql/migraciones` y `sql/mantenimiento`. Consulta [la guia de SQL](sql/README.md) para identificar su uso. Las herramientas Java se ejecutan desde la raiz del proyecto; los fixtures historicos permanecen en `src/test/resources`.

## Limpieza del proyecto

Se retiraron 223 archivos temporales locales de construccion y verificacion, las 15 capturas de la primera propuesta visual y los directorios generados de helpers y Jetty 8081. Se conservan las capturas definitivas, respaldos, herramientas locales, scripts de migracion y fixtures necesarios para comprobar compatibilidad.
