# Modelo definitivo y transición

Fuente conceptual vigente: `modelo_er_daynest.svg`, conservado sin modificaciones e idéntico al `JuanPalacios.svg` suministrado. La adaptación relacional, matriz de trazabilidad, dependencias funcionales y excepciones están en [ADAPTACION_SVG.md](ADAPTACION_SVG.md). schema.sql deriva del SVG y de las extensiones autorizadas. El usuario confirmó simulación académica local y conservación de OCUPACION/BLOQUEO como extensión documentada.

El esquema **daynest** contiene 13 tablas: diez entidades conceptuales, la relación asociativa RESERVA y dos estructuras del calendario autorizado. **public** conserva las siete tablas previas y todos sus datos; es el archivo de transición. MigrarModeloDefinitivo describe la transición previa; la adaptación incremental actual usa MigrarModeloSvg y migrar-svg.sql, con ensayo/restauración/reversión aislados antes de aplicar. No borra ni modifica public ni recalcula importes históricos. usuario.correo_normalizado es canónico y único; movimiento_pago/giro derivan moneda de la reserva histórica inmutable.

Las cuentas y los inmuebles previos se copian con sus IDs. Las operaciones previas se consultan desde la pantalla administrativa de historial anterior, sin nuevos cobros, devoluciones o giros. Sus reservas confirmadas aún vigentes crean bloqueos en daynest. No se duplican como reservas nuevas elegibles para cobrar o girar nuevamente. Los pendientes antiguos permanecen en el archivo; hay que crear una reserva nueva para operar bajo las nuevas reglas.

El catálogo tiene 2 países, 34 divisiones y 33 ciudades: 32 departamentos de Colombia + Distrito Capital, y Región de Murcia. Bogotá es la capital usada para Cundinamarca, pero administrativamente se registra bajo Distrito Capital (11001); la división Cundinamarca (25) está presente sin atribuirle falsamente Bogotá. Son 32 capitales colombianas distintas, no 33 municipios colombianos.

Fuentes oficiales: [DANE DIVIPOLA](https://www.dane.gov.co/index.php/sistema-estadistico-nacional-sen/normas-y-estandares/nomenclaturas-y-clasificaciones/nomenclaturas/codificacion-de-la-division-politica-administrativa-de-colombia-divipola), [Mapa oficial de Bogotá, división 11](https://geoportal.dane.gov.co/descargas/descarga_divipola/pdf/Bogota.pdf), [INE, Cartagena 30016](https://www.ine.es/intercensal/intercensal.do?L=0&codigoMunicipio=16&codigoProvincia=30&search=3). El código MC de Región de Murcia corresponde a su identificación autonómica; 30016 identifica al municipio en INE.

El autocompletado carga el catálogo desde PostgreSQL al HTML, con salida escapada JSTL. JavaScript local busca prefijos sin tildes y envía el identificador seleccionado. El backend valida dicho ID contra el catálogo. Para texto libre, la consulta normaliza tildes/mayúsculas y puede recuperar más de una ciudad homónima. País/región se derivan mediante joins; no se duplican en inmueble.

Se añadieron Pais, DivisionAdministrativa, Ciudad, Administrador, Ocupacion y Bloqueo. Pago actúa como representación Java de movimiento_pago e incluye clase, origen de devolución e idempotencia. Las reservas conservan moneda, expiración y propietario pactado. Los adaptadores JDBC utilizan los nombres de columnas del SQL definitivo. Giro deriva su beneficiario desde reserva, no desde el dueño actual.

La confirmación exige el importe contratado completo. Se guarda primero el movimiento PENDIENTE y la simulación lo resuelve como APROBADO dentro de la misma transacción. Las devoluciones siguen la misma transición. Los giros pasan PENDIENTE → PROCESANDO → EJECUTADO con autorizador y fecha. Los fallidos se conservan; el índice parcial admite un nuevo intento sin duplicar uno activo.

Las reservas pendientes vencen 30 minutos después de crearse. CicloReservas se ejecuta cada 60 segundos, expira pendientes y finaliza estancias confirmadas al vencer su salida; la aplicación detiene la tarea al destruir el contexto. Si Jetty está detenido no se ejecuta; al arrancar procesa los vencimientos acumulados. La disponibilidad es exclusión de ocupaciones activas, no del estado CONFIRMADA solamente.

Las fotos existentes se reutilizan por ID con distintas imágenes por par de alojamientos. No hay tabla ni columnas de fotos. Los precios COP/EUR son ficticios para pruebas; los agregados no convierten ni suman monedas diferentes.

Respaldo ejecutado: .tools/respaldos/antes-modelo-20261010-005100.dump. Resultado inicial: 10 usuarios, 7 clientes, 3 propietarios, 1 administrador, 33 ciudades, 79 inmuebles y 2 bloqueos heredados. Las nuevas operaciones empiezan con historial independiente; el archivo previo conserva sus 10 reservas, 6 pagos y 4 giros.

El dominio/servidor sigue siendo localhost:8080; cambian el contexto Jetty y el nombre del WAR a DayNest. JSP utiliza contextPath para rutas de formularios, CSRF, recursos y enlaces.
