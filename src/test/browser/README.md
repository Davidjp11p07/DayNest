# Recorridos de navegador

## Idioma español / inglés

Con el fixture de `PrepararDisponibilidadHttp.java` y Jetty 8081 ejecutándose contra su esquema temporal, ejecuta `python src/test/browser/idioma.py`. Comprueba ES/EN, persistencia de la preferencia, filtros, calendario, detalle de reserva, mensajes, permisos, móvil, navegación sin JavaScript y las vistas de propietario y administrador. Guarda capturas en `.tools/reserva-idioma-en-1440.png` y `.tools/idioma-en-390.png`.

Los textos de interfaz están en `src/main/resources/i18n/mensajes.properties` (español) y `mensajes_en.properties` (inglés). Las dos versiones usan las mismas claves. Los títulos, descripciones y nombres introducidos por usuarios se conservan. El idioma no cambia la moneda ni los importes.

Estas pruebas usan Chrome y Playwright locales. Requieren el proyecto compilado y PostgreSQL en marcha. Las dependencias de Python de este equipo están en `.tools/daynest-python`; no son necesarias para ejecutar la aplicación.

## Calendarios de cliente y propietario

Ejecuta `java -cp "target/classes;target/lib/*" src/test/browser/PrepararDisponibilidadHttp.java` después de compilar. Inicia Jetty 8081 con `-Drenta.db.schema=` y el esquema UUID de `.tools/disponibilidad-http-schema.txt`. Ejecuta `python src/test/browser/calendario.py`. `disponibilidad.py` conserva el acceso al mismo recorrido por compatibilidad.

El recorrido verifica el calendario mensual de ambos roles, la selección de entrada/salida, el rechazo de rangos ocupados, la conservación de fechas al cambiar de mes, los permisos por inmueble, bloqueo y liberación desde el calendario del propietario, cancelación del cliente, tres anchuras y navegación sin JavaScript. Consume el fixture: prepara otro para repetirlo. Al terminar detén ese Jetty y elimina únicamente su esquema UUID con `PrepararVerificacionHttp --eliminar`.

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

## Fotos, calificaciones y calendario

Después de crear el fixture con `PrepararIngresosHttp.java`, inicia Jetty 8081 con el esquema de `.tools/ingresos-http-schema.txt`. Ejecuta primero `python src/test/browser/ingresos.py`, porque comprueba importes del fixture inicial. Luego:

```powershell
java -cp "target/classes;target/lib/*" src/test/browser/PrepararContenidoHttp.java
python src/test/browser/giros_automaticos.py
python src/test/browser/contenido.py
```

El preparador extiende únicamente ese esquema UUID con interiores ilustrativos, estancias finalizadas y un giro sin cuenta de destino. Genera `.tools/contenido-http.json` y los identificadores requeridos por el recorrido de giros. `contenido.py` comprueba carga y eliminación de fotos, límite de diez, permisos, CSRF, archivos inválidos y demasiado grandes, ampliación de galería, bloqueo de un inmueble situado después de los primeros diez, calificaciones de ambas partes, duplicados y escape de comentarios. Los recorridos consumen el fixture: prepara uno nuevo para repetirlos. No ejecutes de nuevo `ingresos.py` después de añadir estos datos o cambiar la comisión.
