# Verificación de comisión y conexiones — 12 de octubre de 2026

Se añadió comisión configurable al generar el giro: el cliente paga el total pactado y el propietario recibe el neto cobrado menos la comisión. El porcentaje y el importe se persisten y son inmutables. La configuración inicial permanece en cero; 0.1000 representa 10%. Cambiar la configuración requiere reiniciar Jetty.

La migración local se aplicó con respaldo previo en `.tools/respaldos/antes-modelo-20261012-124213.dump`. Se verificó su listado con pg_restore. Los giros existentes reciben porcentaje e importe cero. El script es transaccional y reejecutable. Las migraciones históricas deben completarse antes de registrar giros con comisión.

Las reservas recibidas se consultan mediante SQL parametrizado por id_propietario_pactado; utiliza el índice existente y conserva el beneficiario histórico.

Las conexiones utilizan HikariCP. La configuración se carga una vez por pool; cada préstamo restablece esquema y zona horaria. Se verificaron reutilización física, rollback al devolver una transacción abierta, autoCommit, aislamiento de esquemas y cierre/reinicio del pool. El máximo predeterminado es 10 conexiones y la espera 10 segundos. Esto no constituye una prueba de carga.

Validación final:

- Maven offline package con 92 pruebas Java: cero fallos, errores o pruebas omitidas; WAR generado.
- Comisión del 10% desde configuración: reserva de 450000, comisión 45000, giro 405000.
- Giros históricos, inmutabilidad, migración desde columnas ausentes y reejecución.
- Navegador: 11 comprobaciones de perfiles, 26 comprobaciones de filtros y recorrido completo de continuidad de reserva.
- Filtros: publicación, edición, parámetros inválidos, homónimos, paginación, teclado, ausencia de JavaScript y tamaños 390/768/1440; sin errores JavaScript ni solicitudes externas.
- Las pruebas web escribieron exclusivamente en esquemas HTTP temporales. Se cerraron los servidores y se retiraron esos esquemas.
- git diff --check sin errores.

Se corrigió la prueba del enlace al detalle para comparar su identificador mediante parse_qs, sin exigir un orden concreto de parámetros.

Los informes HTML/PDF y diagramas históricos siguen describiendo la entrega original. README.md documenta la extensión vigente. No se verificó integración bancaria: los cobros y giros continúan simulados.

Registros completos: `.tools/calidad-validacion-total.log`, `.tools/calidad-browser-perfiles.log`, `.tools/calidad-browser-filtros-final.log`, `.tools/calidad-browser-reserva.log`.

Referencia del pool: https://github.com/brettwooldridge/HikariCP
