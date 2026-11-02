# Recorridos de navegador

Estas pruebas usan Chrome y Playwright locales. Requieren el proyecto compilado y PostgreSQL en marcha. Las dependencias de Python de este equipo están en `.tools/daynest-python`; no son necesarias para ejecutar la aplicación.

## Reserva con pago inmediato

Desde la raíz del proyecto:

```powershell
.\mvnw.cmd -o package
java -cp "target/classes;target/lib/*" src/test/browser/PrepararTransferenciasHttp.java
```

El comando crea un esquema nuevo `renta_http_<UUID>`, carga datos ficticios y escribe `.tools/transferencias-http-schema.txt` y `.tools/transferencias-http.json`. No reutiliza datos operativos ni genera reservas previas al cobro.

En una terminal aparte, inicia Jetty con el esquema recién creado:

```powershell
$esquemaPrueba = (Get-Content .tools/transferencias-http-schema.txt -Raw).Trim()
if ($esquemaPrueba -notmatch '^renta_http_[a-f0-9]{32}$') { throw 'Esquema inválido' }
.\mvnw.cmd -o '-Ddaynest.http.port=8081' "-Drenta.db.schema=$esquemaPrueba" jetty:run
```

Luego ejecuta:

```powershell
python src/test/browser/transferencias.py
```

El recorrido comprueba pago inmediato con cuenta registrada y ocasional, fallo sin reserva persistente, protección de cuentas de perfil, historial, pantallas móviles y funcionamiento sin JavaScript. La prueba verifica que el proceso que atiende el puerto use el esquema UUID esperado. Para cambiar el puerto define `DAYNEST_HTTP_PORT` y usa el mismo puerto al iniciar Jetty.

Cada ejecución consume fechas del fixture. Crea uno nuevo para repetir. Al terminar, detén el Jetty de prueba y elimina únicamente su esquema:

```powershell
java -cp "target/classes;target/lib/*" co.edu.rentainmuebles.verification.PrepararVerificacionHttp --eliminar $esquemaPrueba
```

El eliminador solo acepta nombres HTTP UUID; no admite los esquemas operativos.

## Otros recorridos

`continuidad_reserva.py`, `cuentas_bancarias.py`, `perfiles.py`, `filtros_inmuebles.py`, `ingresos.py` y `giros_automaticos.py` conservan sus validaciones de aislamiento y sus fixtures específicos. Consulta sus cabeceras antes de ejecutarlos: no todos utilizan el mismo archivo de configuración ni son reejecutables sobre el mismo fixture.

`PrepararIngresosHttp.java` prepara datos para ingresos; las reservas nuevas ya quedan pagadas al crearlas. Los nombres «pendiente» de este fixture se refieren a ingresos pendientes de liquidación, no a reservas pendientes de pago.
