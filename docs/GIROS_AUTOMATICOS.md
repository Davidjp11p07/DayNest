# Giros automáticos y comisión histórica

## Estado al 17 de octubre de 2026

La nomenclatura y la extension financiera estan aplicadas en la base operativa desde el 17 de octubre de 2026, con autorizacion del usuario. Las operaciones son simulaciones academicas.

## Comportamiento

El cliente vincula su banco, número y tipo de cuenta y paga. DayNest registra el cobro aprobado, confirma la reserva y conserva el importe durante la estancia. El primer cobro aprobado fija la tasa histórica de esa renta. Administración puede cambiar el porcentaje en **Giros**, sin reiniciar y sin cambiar tasas ya fijadas.

El ciclo existente expira pendientes y finaliza estancias. Después procesa cada liquidación en su propia transacción. Se ejecuta cada minuto y al arrancar, de modo que recupera las estancias terminadas durante una interrupción. Java y PostgreSQL usan la zona horaria configurada en negocio.properties; la fecha de salida deja de ocupar noches.

Para ejecutar un giro exige reserva FINALIZADA, salida cumplida, pago aprobado por el total exacto, ausencia de movimientos pendientes o devoluciones aprobadas, tasa histórica y cuenta válida del propietario pactado. Cambiar el propietario actual del inmueble no cambia al beneficiario.

El giro conserva neto, porcentaje, comisión, fecha, cuenta usada al ejecutarse y autor **SISTEMA**, sin atribuirlo a un administrador ficticio. Los giros humanos existentes conservan autor y valores. Un fallo revierte el intento; queda una incidencia con motivo para su revisión y reintento. La interfaz ofrece revisar incidencias, sin exigir autorizar manualmente cada giro correcto.

## Persistencia propuesta

- `renta.porcentaje_comision`: tasa fijada al primer cobro aprobado e inmutable después.
- `configuracion_comision`: versiones de tasa con fecha y administrador; las versiones no se editan ni borran mediante el flujo ordinario.
- `giro.origen_autorizacion`: ADMINISTRADOR o SISTEMA. La restricción de ejecución exige fecha y autor compatible: administrador real para la primera opción, administrador nulo para la segunda.
- `liquidacion_giro`: estado, motivo, revisiones y cambios históricos. Una revisión solicitada por un administrador identifica al solicitante, sin convertirlo en autorizador humano del giro automático.
- La instantánea bancaria del giro se captura al ejecutarlo. Las cuentas históricas de operaciones ejecutadas son inmutables.

El núcleo mantiene diez tablas del profesor. La versión nueva contiene nueve extensiones técnicas, **19 tablas y 120 columnas** en total. Las vistas de compatibilidad no agregan almacenamiento.

La tasa inicial es **30 %**, según la decisión del usuario. Las reservas ya pagadas sin giro vigente reciben esa tasa al migrar. Los giros existentes conservan su porcentaje; las reservas asociadas a giros vigentes conservan ese porcentaje. No se inventan cuentas bancarias para operaciones antiguas. Con 500.000 COP y 30 %, la comisión es 150.000 COP y el neto es 350.000 COP.

## Integridad y continuidad

La liquidación bloquea la renta. La exclusión existente de giros vigentes y las claves de idempotencia impiden duplicados por llamadas repetidas o concurrentes. Los reintentos conservan el historial y nunca convierten un fallo en un giro ejecutado. PostgreSQL verifica también el desglose monetario y la autorización.

Los importes usan NUMERIC/BigDecimal y redondeo monetario a dos decimales; COP y EUR permanecen separados. Se conserva la restricción de giros estrictamente positivos: una liquidación cuyo neto no permita cumplirla registra fallo, sin ejecutar una transferencia ficticia de importe inválido.

## Verificación

La suite general y las nuevas pruebas cubren: estancia futura, estancia finalizada, pago parcial, cancelación, repetición y concurrencia, modificación posterior de comisión y cuenta, falta de cuenta y recuperación, procesamiento por una instancia nueva del ciclo, fallo transaccional/reintento, cuenta capturada al ejecutar, beneficiario pactado, permisos y rangos. Se conservan las pruebas de migraciones anteriores con sus fixtures históricos.

En Chrome se verificaron edición administrativa de tasa, rechazo del 100 %, conservación de tasas pagadas, supervisión y reintento, permisos del propietario, ejecución al arrancar y presentación en tres tamaños. También se repitieron los nueve controles de cuentas bancarias y publicación de inmuebles. Solo se usaron datos ficticios de 8081.

El ensayo de migración restauró el respaldo `.tools/respaldos/antes-modelo-20261017-003441.dump` en `daynest_giros_test_927988f8bbe94c74aa138b7fd98b8d41`. Conservó los 80 inmuebles y las huellas de todas las columnas originales de las 17 tablas. Se ejecutó el migrador dos veces. La copia queda conservada para revisión; la aplicacion operativa posterior se realizo con un respaldo actualizado.

## Activacion completada

Jetty se detuvo antes de crear el respaldo `.tools/respaldos/antes-modelo-20261017-004616.dump`. Se aplico la migracion transaccional y se ejecuto de nuevo para comprobar idempotencia. Las huellas de las columnas originales de las 17 tablas y los 80 inmuebles se conservaron. Jetty se reinicio; se comprobaron acceso administrativo, nueve rutas HTTP y tasa inicial del 30 %.

## Comandos

Con JAVA_HOME y Maven configurados según README:

```powershell
.\mvnw.cmd -o package '-Dtest=*Test'
java -cp "target/classes;target/lib/*" co.edu.rentainmuebles.verification.MigrarGirosAutomaticos --ensayar
.\mvnw.cmd -o jetty:run
```

El comando de ensayo crea y conserva una base independiente. No aplica sobre la base operativa.

Resultado final: 134 pruebas, cero fallos, errores u omitidas; package BUILD SUCCESS. Chrome: siete controles financieros y nueve de cuentas. Verificador: 17 diagramas, 19 tablas y 120 columnas. El servidor temporal de 8081 quedo detenido; se conservan las copias de ensayo sin borrar datos. La web principal sigue operativa y conserva el acceso administrativo; la extension financiera esta activa.

Se verifico ademas la migracion desde el esquema anterior: un giro humano al 10 % conserva importe, autor e instantanea bancaria desconocida; un pago antiguo sin giro recibe el 30 % acordado, y puede liquidarse despues aunque la tasa administrativa cambie al 50 %. Tambien se conserva el giro fallido anterior al crear un nuevo intento, sin duplicar giros ejecutados.
