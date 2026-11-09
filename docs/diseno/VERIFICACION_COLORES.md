# Verificación funcional y colores — 8 de noviembre de 2026

Fotos interiores y calificaciones son requisitos del profesor y se conservan. Se mantuvieron la estructura y la distribución visual de la aplicación.

## Colores

Se revisaron las imágenes originales incrustadas y los colores vectoriales del PDF. La compresión introduce pequeñas variaciones de píxel en las capturas. Los colores principales quedaron en coral `#c74448`, azul oscuro `#203044` y marfil `#faf9f6`. La cabecera usa marfil y la marca completa utiliza coral. Se conservaron los colores semánticos de éxito, advertencia y error.

El recorrido comprobó el fondo calculado en 21 pantallas públicas, de cliente, propietario y administrador. Se revisaron capturas en 390, 768 y 1440 px, sin desbordamiento horizontal del inicio.

## Correcciones

- El verificador de migración reconoce las 19 tablas financieras y las dos extensiones de fotos y calificaciones, conservando las huellas de todas. La prueba de migración completa incluye ambas extensiones.
- La prueba histórica restaura las funciones de transferencias incluso si falla, evitando contaminar las pruebas posteriores.
- Un servicio valida puntuaciones y longitudes y convierte los rechazos de fotos y reseñas en mensajes recuperables. Una calificación repetida no inserta otra fila.
- El calendario permite seleccionar cualquier inmueble del propietario, incluidos los situados después de los primeros diez. El selector ya no se recorta por la paginación general.
- Una foto con identificador inválido devuelve 404. Las cargas inválidas o demasiado grandes se rechazan con 400 o 413, sin HTTP 500 ni fotos guardadas.
- Se actualizaron expectativas antiguas de los recorridos: las reservas de paginación también se cobran al crearlas y generan comisiones pendientes. La búsqueda con/sin tilde compara todos los resultados, sin asumir que Bogotá solo contiene dos inmuebles.

## Pruebas

`mvnw.cmd -o package`: **173 pruebas, 0 fallos, 0 errores, 0 omitidas**. Tras el último ajuste del filtro de carga se repitieron las 20 pruebas de contenido, perfiles y continuidad: todas aprobadas. WAR generado correctamente.

Los recorridos de Chrome operaron exclusivamente en un esquema PostgreSQL HTTP UUID temporal:

| Recorrido | Comprobaciones aprobadas |
| --- | --- |
| Reserva y transferencia inmediata, incluida cuenta ocasional y uso sin JavaScript | 7 |
| Perfiles y permisos | 11 |
| Cuentas, publicación y edición de inmuebles | 9 |
| Filtros, categorías, paginación y homónimos | 26 |
| Ingresos por moneda, permisos y paginación | 7 |
| Giros automáticos, comisión histórica e incidencias de cuenta | 7 |
| Fotos, calificaciones y calendario | 7 |

También pasaron la continuidad búsqueda → detalle → acceso → reserva precargada y la consulta de 21 pantallas más el detalle. No hubo errores JavaScript en los recorridos completados. La prueba de contenido cubre permisos, CSRF, archivos inválidos/grandes, límite de diez fotos, ampliación y eliminación, bloqueo/liberación, reseñas de ambas partes, duplicados y escape de comentarios. `git diff --check` correcto.

## Interiores agregados

En el esquema operativo `daynest` había **81 inmuebles sin interiores**. Se agregaron **243 imágenes interiores ilustrativas**, tres por inmueble, reutilizando fotografías locales acreditadas. Resultado: **81 de 81 inmuebles tienen interiores**. La segunda ejecución agregó cero fotos. Se conservaron las **13 calificaciones existentes**; no se crearon reservas ni pagos de prueba en la base operativa.

La galería identifica estas imágenes como «INTERIOR · Referencia»: son ilustrativas, no fotografías verificadas de esos alojamientos. El propietario puede reemplazarlas mediante las funciones existentes de carga y eliminación. La herramienta conserva cualquier interior previamente cargado.

## Actualización: calendarios de cliente y propietario

La lista de fechas fue sustituida por un calendario mensual con leyenda: disponible en verde claro, reservado en coral y bloqueado en ámbar con borde discontinuo. El cliente lo encuentra junto al formulario de reserva; el propietario tiene un botón «Calendario» en cada tarjeta de «Mis inmuebles», que abre «Tus días disponibles» con un calendario grande, formulario de bloqueo y liberación de bloqueos propios. Se mantienen el resto de la estructura y la paleta.

Se pueden elegir entrada y salida pulsando los días. Las flechas cambian de mes conservando la selección; no se permiten noches solapadas, pero sí salir al comenzar un rango ocupado. Las fechas escritas también se validan. Sin JavaScript se mantienen los colores, la navegación mensual y los formularios manuales. La disponibilidad se vuelve a comprobar en el servidor al reservar o bloquear. El cliente solo ve fechas y estados; los motivos de bloqueo son privados del propietario.

Validación final: **181 pruebas JUnit aprobadas, sin fallos ni errores**, y **9 comprobaciones de Chrome** aprobadas para calendarios, selección, permisos, bloqueo/liberación, cancelación, tres anchuras y uso sin JavaScript. Se probaron también febrero bisiesto, cambio de año, salida exclusiva y días pasados. El WAR incluye la versión final del JavaScript verificada en navegador. Todas las operaciones de navegador se realizaron en un esquema HTTP UUID temporal, posteriormente retirado.

Evidencia: `.tools/calendario-pruebas.log`, `.tools/calendario-navegador.log`, `.tools/calendario-navegador.json` y `.tools/calendario-war-final.log`. Capturas: [propietario](pantallas/calendario-propietario-1440.png) y [cliente en móvil](pantallas/calendario-cliente-390.png). Los recorridos reutilizables están en `src/test/browser/calendario.py` y su entrada de compatibilidad `disponibilidad.py`.

## Evidencia local

- `.tools/verificacion-general-final.log`: 173 pruebas.
- `.tools/general-pruebas-carga.log`: 20 pruebas repetidas y construcción final.
- `.tools/general-{transferencias,perfiles,cuentas,ingresos,giros,continuidad}.log`.
- `.tools/general-filtros-validado.log`, `.tools/general-contenido-exito.log`, `.tools/general-pantallas.log`.
- `.tools/frontend-inicio-{390,768,1440}.png`: paleta final.
- `.tools/general-interiores-aplicados.log`, `.tools/general-interiores-reejecucion.log`, `.tools/general-base-despues.log`.

Los archivos de `.tools` son evidencia local; no forman parte del despliegue. Las pruebas reutilizables nuevas están en `src/test/browser/contenido.py` y `src/test/java/co/edu/rentainmuebles/service/ContenidoInmuebleTest.java`.
