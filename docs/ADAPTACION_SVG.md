# Trazabilidad y adaptación del modelo conceptual DayNest

Este documento describe la adaptación al SVG realizada el 10/10/2026.
La ampliación posterior de tipo y entorno se documenta en
[FILTROS_INMUEBLES.md](FILTROS_INMUEBLES.md). Los conteos iniciales y las
referencias a 71 columnas son históricos; con los dos atributos de entorno
el esquema contiene 73 columnas. Las reservas y pagos posteriores de los
usuarios no están representados por esos conteos históricos.

## Fuente y alcance

Fuente conceptual vigente: `docs/modelo_er_daynest.svg`, idéntica byte por byte a
`JuanPalacios.svg` proporcionado por el usuario. SHA-256:
`112f991fc0feaf7ee9b8360c629da6f6b7c1e7cb69e913d31331c8d39ab91bb2`.
El original se conserva sin modificaciones y es la fuente conceptual vigente. Se conserva Java 17, Servlet/JSP/JSTL, JDBC,
PostgreSQL, Jetty y Maven. Esta documentación es una auditoría técnica, no el informe
académico de entrega ni un rediseño de la interfaz.

El usuario autorizó expresamente conservar y documentar OCUPACION/BLOQUEO como
extensiones del modelo conceptual. **Diez entidades conceptuales, una relación
asociativa RESERVA y dos estructuras del calendario producen trece tablas físicas**.
La cantidad de tablas es una consecuencia del diseño, no su criterio de corrección.

## Estado inicial comprobado

Inspección real del 10/10/2026, conexión configurada por `db.properties`:
base `renta_inmuebles`, PostgreSQL 18.6, esquema operativo `daynest`, zona
`America/Bogota`. Inspección de tablas, columnas, restricciones, índices y triggers en
`.tools/auditoria-svg-inicial.txt`; no contiene contraseñas ni filas de usuarios.

| Tabla operativa | Filas iniciales |
|---|---:|
| usuario | 10 |
| cliente | 7 |
| propietario | 3 |
| administrador | 1 |
| pais | 2 |
| division_administrativa | 34 |
| ciudad | 33 |
| inmueble | 79 |
| ocupacion | 2 |
| reserva | 0 |
| bloqueo | 2 |
| movimiento_pago | 0 |
| giro | 0 |

No había reservas/cobros/giros operativos vigentes. Los dos bloqueos protegen
reservas confirmadas del archivo anterior. `public` contiene siete tablas históricas:
10 usuarios, 7 clientes, 3 propietarios, 14 inmuebles, 10 reservas, 6 pagos y 4 giros.
No se transforman ni se recalculan estas operaciones.

Línea base ejecutada antes de editar: **51 pruebas**, sin errores, fallos ni omisiones;
registro `.tools/linea-base-svg.log`. `git status --short` indica que esta carpeta
**no es un repositorio Git**. Los cambios preexistentes se conservaron mediante copia
de SQL, código, pruebas y documentos en `.tools/antes-svg/`, antes de adaptar archivos.

Respaldo previo completo: `.tools/respaldos/antes-modelo-20261010-020212.dump`,
83 803 bytes. `pg_restore --list` verificó que contiene datos de `daynest` y `public`.
Un listado legible no prueba restaurabilidad por sí solo: el ejecutor de migración
restaura además un respaldo completo en una base temporal antes de admitir aplicación.

## Lectura del SVG Chen

Se revisaron los textos y los elementos XML: rectángulos, óvalos, rombos,
triángulo ES, conexiones, flechas y caja de agregación. Hay diez rectángulos de
entidad: USUARIO, CLIENTE, PROPIETARIO, ADMINISTRADOR, INMUEBLE, CIUDAD,
DIVISIÓN ADMINISTRATIVA, PAÍS, MOVIMIENTO_PAGO y GIRO. RESERVA es un rombo
entre CLIENTE e INMUEBLE, con ocho atributos y `fecha_inicio` subrayada.

USUARIO, PAÍS, DIVISIÓN, CIUDAD, INMUEBLE, MOVIMIENTO_PAGO y GIRO tienen
identificadores propios subrayados. Los tres perfiles dependen de la identidad de
USUARIO: se implementan mediante PK/FK compartida, no mediante nuevos IDs.
El SVG no dibuja rectángulos dobles ni relaciones identificadoras dobles; por tanto
no se afirma que represente entidades débiles Chen. La dependencia identificadora
de los perfiles procede de la especialización.

Todos los atributos dibujados son simples en este modelo. `nombre_completo` y
`direccion_detallada` son escalares: no tienen óvalos componentes. No hay óvalos
dobles de atributos multivalorados ni óvalos discontinuos de atributos derivados.
No se inventan apellidos, fotos, servicios, tablas de reseñas ni cuentas bancarias.
Noches, total, saldo y etiquetas geográficas se calculan; no son columnas nuevas.

La caja gris que engloba CLIENTE–RESERVA–INMUEBLE representa **agregación**:
MOVIMIENTO_PAGO y GIRO se vinculan a una ocurrencia de la relación RESERVA, no
solo a un cliente o a un inmueble. En el modelo lógico, esa ocurrencia se referencia
por `reserva.id_ocupacion`. No exige una tabla adicional de agregaciones.

| Relación del SVG | Cardinalidad/participación que se implementa |
|---|---|
| USUARIO ES CLIENTE/PROPIETARIO/ADMINISTRADOR | Parcial y solapada: un usuario puede tener cero, uno, dos o tres perfiles; cada perfil tiene exactamente un usuario |
| PROPIETARIO publica INMUEBLE | Cada inmueble tiene un propietario; un propietario puede publicar cero o muchos |
| INMUEBLE ubicado en CIUDAD | Cada inmueble tiene una ciudad; una ciudad puede tener cero o muchos inmuebles |
| CIUDAD pertenece a DIVISIÓN | Cada ciudad pertenece a una división; una división puede tener cero o muchas ciudades |
| DIVISIÓN pertenece a PAÍS | Cada división pertenece a un país; un país puede tener cero o muchas divisiones |
| CLIENTE reserva INMUEBLE | Muchos a muchos, participación opcional de ambos; cada ocurrencia tiene exactamente un cliente y un inmueble |
| Agregación genera MOVIMIENTO_PAGO | Cada movimiento pertenece a una reserva; una reserva admite cero o muchos movimientos |
| Agregación origina GIRO | Cada giro pertenece a una reserva; una reserva admite cero o muchos intentos |
| ADMINISTRADOR autoriza GIRO | Cada giro admite cero o un autorizador; cada administrador puede autorizar cero o muchos |
| MOVIMIENTO_PAGO origina MOVIMIENTO_PAGO | Un movimiento admite cero o un cobro origen y un cobro cero o muchas devoluciones; la clase restringe los roles |

Los mínimos uno se representan con FK NOT NULL; los máximos uno de los perfiles
con PK compartida. Ninguna FK obliga a que todo padre tenga hijos, coherente con
los mínimos cero. El giro ejecutado exige autorizador; es una regla adicional de
estado que refina el mínimo opcional del giro aún pendiente.

## Matriz de trazabilidad y decisión por tabla

Los atributos que no están en el SVG se identifican explícitamente en la sección
siguiente. Nombres Java como `getCorreo()` no deben coincidir literalmente con
los nombres físicos SQL: el mapeo está en el DAO.

| Elemento conceptual → tabla | Atributos y claves | Relaciones/restricciones | Código dependiente y decisión |
|---|---|---|---|
| USUARIO → usuario | id_usuario PK; nombre_completo, correo_normalizado UQ, contrasena_hash, telefono, activo, creado_en | Correo canónico minúsculas/sin espacios extremos; hash no vacío; nombre no vacío | Usuario, UsuarioDaoJdbc, Mapeador, AutenticacionServiceImpl; renombrar `correo` y exigir representación canónica |
| CLIENTE → cliente | id_usuario PK/FK usuario | Identidad común, opcional, compatible con los otros perfiles | Cliente, UsuarioDaoJdbc, ReservaDaoJdbc; conservar |
| PROPIETARIO → propietario | id_usuario PK/FK usuario | Identidad común; padre de publicación y beneficiario histórico | Propietario, UsuarioDaoJdbc, InmuebleDaoJdbc, ReservaDaoJdbc, GiroDaoJdbc, BloqueoDaoJdbc; conservar |
| ADMINISTRADOR → administrador | id_usuario PK/FK usuario | Perfil solapado; autorización opcional de giro, obligatoria al ejecutar | Administrador, UsuarioDaoJdbc, GiroServiceImpl, AdministracionServiceImpl; conservar |
| PAÍS → pais | id_pais PK; nombre; codigo_iso2 UQ | Código ISO de dos letras mayúsculas; padre de divisiones | Pais, CiudadDaoJdbc, geografia.sql; conservar |
| DIVISIÓN → division_administrativa | id_division PK; id_pais FK; nombre, codigo_oficial; UQ(id_pais,codigo_oficial) | País obligatorio; códigos únicos dentro de país, no nombres únicos | DivisionAdministrativa, CiudadDaoJdbc; conservar |
| CIUDAD → ciudad | id_ciudad PK; id_division FK; nombre, codigo_oficial; UQ(id_division,codigo_oficial) | División obligatoria; homónimos permitidos; etiquetas se derivan por joins | Ciudad, CiudadDaoJdbc, CatalogoService, PublicoServlet/PropietarioServlet; conservar |
| INMUEBLE → inmueble | id_inmueble PK; id_propietario/id_ciudad FK; titulo, descripcion, direccion_detallada, tipo, capacidad, tarifa_noche, moneda, estado, creado_en | Propietario/ciudad obligatorios; capacidad/tarifa positivas; dominios; trigger protege capacidad futura | Inmueble, InmuebleDaoJdbc, InmuebleServiceImpl, PublicoServlet/PropietarioServlet; conservar |
| RESERVA asociativa → reserva + ocupacion tipo RESERVA | reserva.id_ocupacion PK/FK; id_cliente FK; cantidad_huespedes, precio_noche_pactado, moneda, estado, expira_en, creada_en; ocupacion aporta id_inmueble FK, fecha_inicio/fecha_fin | Cliente/inmueble obligatorios; términos inmutables, capacidad, confirmación cobrada, vencimiento y disponibilidad | Reserva, PeriodoEstadia, ReservaDaoJdbc, ReservaServiceImpl, CicloReservasDaoJdbc; conservar identificación persistente |
| Agregación → FK id_reserva en movimiento_pago/giro | Ambas referencian la PK de reserva | ON DELETE RESTRICT y triggers impiden separar finanzas de su reserva | PagoDaoJdbc, GiroDaoJdbc, Mapeador; no agregar tabla de agregación |
| MOVIMIENTO_PAGO → movimiento_pago | id_movimiento PK; id_reserva FK; clase, monto, estado, clave_idempotencia UQ, creado_en, resuelto_en | Origen nullable; FK compuesta al mismo movimiento/reserva; COBRO sin origen, DEVOLUCION con origen; límites e inmutabilidad | Pago, PagoDaoJdbc, PagoServiceImpl, CicloReservasDaoJdbc; retirar copia redundante de moneda |
| Autorrelación origina → id_cobro_origen | FK(id_cobro_origen,id_reserva)→(id_movimiento,id_reserva); UQ soporte(id_movimiento,id_reserva) | Cobro APROBADO de la misma reserva; suma de devoluciones ≤ cobro; origen no puede ser devolución | PagoDaoJdbc, validar_movimiento_pago; conservar |
| GIRO → giro | id_giro PK; id_reserva FK; id_admin_autorizador FK nullable; monto, estado, clave_idempotencia UQ, ejecutado_en | EJECUTADO exige estancia finalizada, autorizador, fecha y neto positivo; máximo un intento no fallido por reserva | Giro, GiroDaoJdbc, GiroServiceImpl, AdministracionServiceImpl; retirar copia redundante de moneda |
| Calendario técnico autorizado → ocupacion | id_ocupacion PK; id_inmueble FK; fecha_inicio, fecha_fin, tipo, activa, creada_en | Fechas ordenadas; EXCLUDE GiST de intervalos activos; subtipo exactamente RESERVA o BLOQUEO al commit | Ocupacion, ReservaDaoJdbc, BloqueoDaoJdbc, InmuebleDaoJdbc; conservar como soporte técnico |
| Bloqueo funcional autorizado → bloqueo | id_ocupacion PK/FK; id_propietario_solicitante FK; motivo | Propietario actual al crear, identidad del solicitante histórica; comparte exclusión con reservas | Bloqueo, BloqueoDaoJdbc, BloqueoService, PropietarioServlet; conservar como extensión explícita |

## Diferencias y atributos técnicos justificados

1. **Correo:** el SVG dibuja `correo_normalizado`; el esquema anterior tenía `correo`
   y UQ sobre lower(correo). Java ya normalizaba al registrar, pero SQL admitía
   mayúsculas. Se renombra la columna, se exige `lower(btrim(...))` y el índice
   único pasa a la columna canónica. No se duplica el correo ni se agrega un alias
   para conservar consultas antiguas. `public.usuario.correo` sigue intacto.
2. **Identidad de reserva:** la transformación usual de una relación M:N produce
   una tabla asociativa. `(id_cliente,id_inmueble,fecha_inicio)` sería una clave
   candidata **solo si se prohíbe repetir esa combinación en todo el historial**.
   Una reserva cancelada seguida de otra con las mismas fechas la repite. Fecha
   sola tampoco identifica reservas de inmuebles diferentes. Se conserva la PK
   técnica estable `id_ocupacion`, referenciada por finanzas. El inicio subrayado
   del SVG requiere aclarar su alcance histórico; no se impone UNIQUE global ni
   se modifica el dibujo. Corrección conceptual propuesta: describir un
   identificador de ocurrencia de reserva, o explicitar que la clave temporal es
   válida únicamente para ocurrencias activas, sin identificar por ella finanzas.
3. **OCUPACION/BLOQUEO:** conservar dos tablas evita perder la función autorizada y
   permite que una única exclusión serialice reservas y bloqueos concurrentes.
   Sustituirlas por dos calendarios y consultas previas introduce carreras si no
   se añade otro mecanismo transaccional. Fusionar bloqueos con reservas obliga
   a clientes nulos o ficticios y contradice la participación del cliente en la
   relación. Se mantiene la extensión aprobada y se distingue en el modelo lógico.
4. **Propietario pactado:** `reserva.id_propietario_pactado` es el beneficiario
   contratado, no el dueño vigente. Un cambio de dueño no modifica ese hecho ni
   sus giros; eliminarlo perdería información. Tarifa y moneda pactadas también
   son hechos históricos diferentes de tarifa/moneda actuales del inmueble.
5. **Moneda de movimiento/giro:** el esquema anterior la duplicaba y exigía por
   trigger igualdad con reserva.moneda. No representaba una conversión ni un hecho
   monetario independiente. Se retiran esas dos columnas; cada importe conserva
   su unidad mediante FK NOT NULL a la reserva de moneda inmutable. Los objetos
   Java ya la obtienen con `getReserva().getMoneda()`. Se rechaza la migración si
   encuentra monedas distintas; no se sobrescriben discrepancias.
6. **Referencias externas:** `referencia_externa` opcional y única permite conservar
   la referencia del historial de simulación y detectar duplicados. No implica
   integración bancaria. Varias filas pueden tener NULL.
7. **Giro.creado_en:** registra cuándo se creó un intento; `ejecutado_en` registra
   un evento diferente, que puede no suceder. Es información de auditoría de
   intentos, no un atributo derivado de la fecha de ejecución.
8. **Activa/tipo/fecha de calendario:** tipo identifica la alternativa total y
   disjunta, activa permite la exclusión parcial sin usar funciones de reloj en
   el índice; creada_en registra la creación de la ocupación técnica. Los cambios
   de estado y activa se verifican mediante constraint triggers diferidos.
9. **Claves idempotentes e índices de soporte:** aparecen claves idempotentes en
   el SVG; su UNIQUE protege reintentos. UQ(id_movimiento,id_reserva) es una
   superclave de soporte de la FK compuesta, no otra clave candidata mínima.
   El índice parcial de giros representa unicidad por estado, no una clave global.

Las extensiones no se presentan como elementos que estuvieran dibujados. Ninguna
tabla nueva se incorpora; se mantienen funciones ya existentes. La distinción
entre clases Java y tablas también se conserva: `Pago` adapta MOVIMIENTO_PAGO;
`MetodoPago` es una selección de simulación no persistida; las clases de comisión
anteriores no determinan el monto del giro actual. No se elimina código ajeno a la
adaptación solo para renombrar estos conceptos.

## Evaluación formal de normalización

Se analizan dependencias del dominio y restricciones, no solo la presencia de PK.
En la siguiente tabla, `K → resto` abrevia que cada clave candidata determina todos
los atributos de su relación. Una referencia externa nullable es identificación
única **cuando está presente**, no una clave candidata total de la tabla.

| Tabla | Claves candidatas / dependencias funcionales relevantes | Evaluación |
|---|---|---|
| usuario | id_usuario y correo_normalizado; cada uno → resto | 1FN/2FN/3FN/BCNF bajo las dependencias de identidad/correo declaradas |
| cliente, propietario, administrador | id_usuario → pertenencia al perfil; solo una columna | BCNF; no copiar nombre/correo en los perfiles |
| pais | id_pais y codigo_iso2; cada uno → nombre y otra clave | BCNF; nombre no identifica país |
| division_administrativa | id_division y (id_pais,codigo_oficial); K → resto | BCNF; codigo_oficial solo no identifica una división |
| ciudad | id_ciudad y (id_division,codigo_oficial); K → resto | BCNF; nombre no identifica ciudad, país no se almacena aquí |
| inmueble | id_inmueble → propietario, ciudad y atributos actuales | BCNF bajo las dependencias declaradas; no ciudad→tarifa ni propietario→dirección |
| ocupacion | id_ocupacion → inmueble, inicio, fin, tipo, activa, creada_en | BCNF bajo las dependencias declaradas; inicio/inmueble no son clave histórica global |
| reserva | id_ocupacion → cliente, propietario pactado y términos históricos | BCNF bajo las dependencias declaradas; inmueble→propietario pactado/tarifa pactada NO vale a través de contratos de épocas distintas |
| bloqueo | id_ocupacion → solicitante histórico,motivo | BCNF bajo las dependencias declaradas; inmueble→solicitante histórico no se asume tras cambios de dueño |
| movimiento_pago anterior | id_movimiento y clave_idempotencia → resto; id_reserva→moneda | 1FN y 2FN, pero NO 3FN/BCNF por la moneda duplicada y determinantе id_reserva no clave |
| giro anterior | id_giro y clave_idempotencia → resto; id_reserva→moneda | 1FN y 2FN, pero NO 3FN/BCNF por la moneda duplicada; varios intentos fallidos impiden tratar id_reserva como clave |
| movimiento_pago resultante | id_movimiento y clave_idempotencia → resto | Elimina la dependencia transitiva monetaria; preserva una redundancia condicional de integridad descrita debajo |
| giro resultante | id_giro y clave_idempotencia → resto; referencia_externa identifica cuando no NULL | 3FN/BCNF bajo las dependencias globales declaradas; la unicidad parcial por estado es otra restricción |

**1FN:** un valor escalar por columna; perfiles, movimientos e intentos son filas
separadas. Nombre completo/dirección escalar no infringen 1FN por tener palabras.
No se almacenan listas de ciudades ni teléfonos multivalorados.

**2FN:** las claves geográficas compuestas no tienen atributos no primos dependientes
de un componente solo. La identidad de la división depende del país y código;
la de ciudad depende de división y código. Las claves simples de otras tablas no
permiten dependencias parciales. UQ de soporte no minimal no se confunde con clave.

**3FN:** se evita ciudad→división→país dentro de inmueble mediante joins; el nombre
del dueño no se duplica. Moneda de reserva permanece por ser unidad contractual
histórica, que no depende de moneda actual. Moneda repetida en movimientos/giros
sí dependía de su FK a reserva y se elimina, con descomposición sin pérdida:
cada fila financiera referencia exactamente una reserva existente de moneda fija.

**BCNF y excepción condicional de devoluciones:** no se afirma BCNF absoluta para
MOVIMIENTO_PAGO. En el subconjunto DEVOLUCION, el cobro origen determina la
reserva y se conserva `id_reserva` para representar directamente la relación con
la agregación y para la FK compuesta que garantiza mismo contrato. Varios retornos
parciales pueden tener un mismo origen; no se debe declarar ese origen clave.
La FD condicional `id_cobro_origen → id_reserva` es redundancia controlada; SQL la
impone por FK compuesta y trigger. En toda la tabla el origen es NULL para cobros,
por lo que el análisis clásico sin NULL debe hacerse por subconjuntos. El esquema
resultante corrige la infracción monetaria global, pero no oculta esta excepción.
Descomponer COBRO y DEVOLUCION en tablas separadas, o derivar reserva exclusivamente
del origen para devoluciones, sería otra transformación posible; no se incorpora
una jerarquía conceptual/tabla adicional sin necesidad. La excepción preserva las
referencias existentes y no permite anomalías de asignación entre reservas.

Las dependencias son del dominio completo, no coincidencias accidentales del
catálogo demo. La aplicación elige COP/EUR al publicar según el catálogo actual,
pero SQL no declara país→moneda ni impide cambiar moneda actual: no se usa esa
coincidencia como una FD geográfica para eliminar la moneda de inmueble.

## Restricciones conservadas y ajustadas

Se mantienen PK/FK ON DELETE RESTRICT, identidad compartida de perfiles,
UQ geográficas, homónimos, fechas [inicio,fin), GiST de ocupaciones activas,
subtipos totales/disjuntos de ocupación al commit y parciales/solapados de usuario,
capacidad máxima, importes positivos y límites SMALLINT/NUMERIC(16,2).

Se mantienen serialización de finanzas por bloqueo de reserva, cobros sin exceso,
devoluciones contra cobro aprobado de la misma reserva y por importe limitado,
confirmación completamente pagada, expiración, cancelación, liberación de fechas,
beneficiario histórico, idempotencia, autorizador y ejecución del giro neto sin
comisión tras terminar la estancia. Se conservan intentos fallidos y se prohíbe
DELETE de reservas, movimientos y giros. No se suma COP con EUR.

Cambian el nombre y comprobación canónica del correo, su índice único, y la
representación de moneda de movimientos/giros. Sus triggers ya no comparan
columnas duplicadas: la unidad se obtiene por la FK a reserva inmutable. No cambian
los límites monetarios ni los estados y no se permite conversión de monedas.

Índices existentes de FK/búsqueda y exclusión se conservan. La adaptación no añade
índices especulativos sin un cambio de acceso o evidencia de necesidad.

## Migración, verificación y reversión

`migrar-svg.sql` es la adaptación incremental; `revertir-svg.sql` reconstruye la
estructura previa desde la moneda contractual. `migrar-modelo.sql` es un script
**histórico incompatible**, que contiene DROP de las tablas originales y no debe
ejecutarse en daynest. Tampoco se ejecutan reset.sql, InicializarBD, ReiniciarDatos
ni migraciones previas en la base activa.

`MigrarModeloSvg --ensayar` genera un respaldo nuevo y lo verifica, restaura toda
la base en `daynest_svg_test_UUID`, migra, comprueba reejecución inocua, revierte,
compara huellas de filas, migra otra vez y ejecuta los DAO sobre el clon restaurado.
Retira únicamente la base temporal identificada por UUID y su archivo temporal de
credenciales. La aprobación del ensayo queda vinculada por SHA-256 a ambos scripts
schema/migración; modificarlos obliga a repetir el ensayo.

`MigrarModeloSvg --aplicar` exige ensayo válido, Jetty detenido, configuración
daynest/renta_inmuebles y otro respaldo completo verificado. Adquiere bloqueos
exclusivos de usuario/reserva/movimiento/giro, valida datos previos y aplica en una
transacción. Un timeout de bloqueo o discrepancia aborta. No introduce una tabla
de versión ni sobrescribe esquemas.

Las huellas comparan todas las filas de todas las tablas de daynest y public,
normalizando solamente el nombre de correo y excluyendo las dos monedas cuya
igualdad contractual se verifica previamente. Se conservan IDs, referencias,
hashes, perfiles, importes, fechas, estados y relaciones. La base completa
restaurada y la prueba JUnit con cobro/giro ejecutado cubren los dos casos: datos
operativos reales sin finanzas nuevas y migración con finanzas pobladas.

Antes del COMMIT, cualquier error causa ROLLBACK. Después del COMMIT, con Jetty
detenido, la reversión añade las monedas desde las reservas, recupera el nombre
anterior del correo y funciones/índice. Bajo bloqueo y transacción suspende solo
los triggers de movimiento/giro necesarios para reconstruir columnas de registros
resueltos, y los habilita de nuevo antes del commit. No se usa una restauración
destructiva del respaldo sobre la base activa como reversión automática.
Restaurar también los archivos Java/SQL previos de `.tools/antes-svg/` y recompilar
antes de arrancar con estructura antigua. El dump permite recuperar un clon para
investigar; no se borra public ni se sobrescribe otra base sin autorización.

## Cambios de código y alcance de regresión

UsuarioDaoJdbc y Mapeador usan correo_normalizado; PagoDaoJdbc/GiroDaoJdbc y
CicloReservasDaoJdbc dejan de escribir la moneda duplicada. MigrarModeloDefinitivo
adapta solo el destino de nuevas instalaciones/transiciones, leyendo el correo
del archivo public mediante lower/btrim. No se ejecuta esa migración antigua en
la base activa. MigrarModeloSvg es el ejecutor de la adaptación incremental.

Las firmas de modelos, interfaces, servicios y servlets siguen iguales. Los getters
monetarios ya derivaban moneda de la reserva. ReservaDaoJdbc, BloqueoDaoJdbc,
InmuebleDaoJdbc y CiudadDaoJdbc se revisaron y conservan su SQL compatible.
No se editan JSP, CSS, JavaScript, formularios, CSRF, arquitectura ni dependencias.

`ModeloProfesorTest.treceTablasExactas` se renombra a
`tablasDeEntidadesRelacionYExtensionAutorizada`: la lista comprobada corresponde
a la transformación justificada y autorizada, no a contar entidades del SVG.
`MigracionSvgTest` añade equivalencia de instalación/migración, finanzas pobladas,
reversión, rechazo de moneda/correo inconsistente, identidad tras cancelación,
moneda histórica y especialización parcial sin perfiles.

Los resultados ejecutados y el respaldo de aplicación están consolidados en
la sección “Verificación de la adaptación” de este documento.

## Problemas y límites explícitos

- El inicio subrayado en RESERVA es ambiguo para reservas históricas repetidas.
  Se conserva ID persistente por autorización expresa del alcance; SVG intacto.
- Bloqueos no estaban en el SVG; extensión aprobada expresamente por el usuario.
- La redundancia condicional de devoluciones se documenta; no se declara BCNF
  indiscriminada ni se elimina la FK que impide devolver otro contrato.
- Las mínimas obligaciones de autorizar/ejecutar giros refinan estados del SVG;
  un intento pendiente puede carecer de autorizador, una ejecución no.
- Los pagos, devoluciones y giros siguen siendo simulaciones académicas locales.
- PlantUML tiene fuentes verificables; su renderizado visual no forma parte de
  esta adaptación ni se afirma realizado.
- Git sigue sin repositorio: no hay diff Git ni commits, push o merge que reportar.

## Verificación de la adaptación

Resultados históricos de la adaptación al SVG del 10/10/2026, consolidados para evitar un informe separado:

- Línea base: 51 pruebas Java; tras adaptar el SVG: 58 pruebas aprobadas, sin fallos, errores ni omisiones. La ampliación posterior a 65 pruebas se explica en [FILTROS_INMUEBLES.md](FILTROS_INMUEBLES.md).
- Se restauró un pg_dump completo en una base temporal y se comprobó migración, reejecución, reversión, huellas de filas y lectura por DAO. El ensayo y las pruebas con cobros/giros poblados preservaron importes, beneficiarios, fechas, moneda, IDs e historial.
- Respaldo inmediatamente anterior a la aplicación: `.tools/respaldos/antes-modelo-20261010-021731.dump`; respaldo del ensayo: `antes-modelo-20261010-021559.dump`; copia de código anterior: `.tools/antes-svg/`.
- Aplicación confirmada con `MigrarModeloSvg --aplicar`, Jetty detenido y transacción completa. Evidencias: `.tools/aplicacion-svg.log`, `.tools/ensayo-svg.log`, `.tools/restauracion-svg.log`, `.tools/huellas-svg-operativa.txt`, `.tools/auditoria-svg-final.txt` y `.tools/dao-clon-svg.log`.
- Chrome verificó 18 vistas públicas a 1440/768/390 px y 14 autenticadas, perfiles y cierre POST/CSRF, autocompletado sin tildes y ambas Cartagenas, sin desbordamientos, errores JS ni recursos externos. Evidencia: `.tools/verificacion-svg-web.json`. No creó operaciones financieras en la base activa.
- Se generó `target/DayNest.war` y se comprobaron las fuentes de 15 diagramas contra la versión de aquel momento (13 tablas y 71 columnas). No se renderizó PlantUML. Estos resultados no certifican los diagramas después de incorporar los filtros de entorno.
- La migración conservó 79 inmuebles y el archivo public de 10 reservas, 6 pagos y 4 giros. Los conteos de cero reservas/pagos nuevos correspondían exclusivamente al momento de la migración; no describen el estado actual tras el uso de la aplicación.

El informe original completo se conserva en el respaldo de limpieza `.tools/documentacion-retirada-20261010-033253/docs/VERIFICACION_ADAPTACION_SVG.md`.
