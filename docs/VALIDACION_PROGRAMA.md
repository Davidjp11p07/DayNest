# Validación del programa — 12 de octubre de 2026

Resultado: las comprobaciones ejecutadas pasaron. No se modificaron el código de la aplicación ni la configuración de negocio durante esta validación. Se generó de nuevo target/DayNest.war y este informe.

- Maven offline package: 92 pruebas Java, cero fallos, errores y omitidas; BUILD SUCCESS.
- Navegador Chrome: 11 comprobaciones de perfiles, 26 de filtros y recorrido de continuidad de reserva aprobado.
- Flujos cubiertos: registro/autenticación y perfiles mediante pruebas Java y navegador; publicación/edición; búsqueda, fechas, capacidad, homónimos, paginación; reglas de reservas, pagos, devoluciones, giros y comisión mediante la suite Java; gestión y aislamiento del pool.
- Filtros: teclado y pantallas 390/768/1440, sin errores JavaScript ni solicitudes externas.
- PostgreSQL activo accesible; inspección de las 13 tablas. Consultas de integridad: cero ocupaciones activas superpuestas, cero reservas con fechas o huéspedes inválidos, cero correos sin normalizar. La tabla giro activa está vacía; la validación de giros y comisión corresponde a los datos aislados de las pruebas.
- git diff --check sin errores.

Las pruebas web utilizaron exclusivamente renta_http_7e6b31d27bdf47529c28b0636dd553ca. El servidor temporal se cerró y el esquema se eliminó. La inspección del esquema daynest fue de solo lectura. Las pruebas Java utilizan sus propios esquemas temporales.

Observaciones menores, sin correcciones durante esta revisión:

1. pom.xml declara HikariCP tanto como dependencia de la aplicación como del plugin Jetty. La segunda declaración es redundante para el pool de la aplicación.
2. SLF4J avisa que no hay proveedor de logs. La aplicación funciona, pero los mensajes internos de HikariCP no se registran.
3. Jetty muestra advertencias de escaneo duplicado de clases JSTL y recursos de esquemas XML; no impidieron arrancar ni ejecutar los recorridos.

Comisión vigente: negocio.comision=0.0000. Se probó por separado la comisión del 10% desde configuración. Los cobros y giros siguen siendo simulados. Esta validación no constituye prueba de carga ni auditoría exhaustiva de seguridad.

Evidencias: .tools/validacion-programa-java.log, validacion-programa-perfiles.log, validacion-programa-filtros.log, validacion-programa-reserva.log, validacion-programa-bd.log, validacion-programa-integridad.log y validacion-programa-jetty.log.
