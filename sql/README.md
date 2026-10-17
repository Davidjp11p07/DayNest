# Scripts de PostgreSQL

Ejecuta las herramientas Java y los comandos documentados desde la raiz del proyecto.

| Carpeta | Contenido |
|---|---|
| `instalacion/` | Esquema completo, catalogo geografico y datos iniciales de demostracion. |
| `demostracion/` | Administrador demo y datos ficticios opcionales del modelo del profesor. |
| `migraciones/` | Actualizaciones de versiones anteriores conservando datos. |
| `mantenimiento/` | Reinicio de datos demo y reversion del modelo SVG anterior. |

Para crear una base nueva, sigue el inicializador descrito en el README principal. Las migraciones corresponden a distintas versiones: no se ejecutan todas en secuencia por estar en esta carpeta. La base operativa actual ya esta actualizada.

Los scripts de mantenimiento pueden borrar datos o revertir estructuras; no forman parte del arranque normal de Jetty.

Los esquemas historicos de pruebas permanecen en `src/test/resources/`, donde las pruebas los utilizan como fixtures aislados.

`migraciones/pagos-transferencia.sql` actualiza funciones de una instalación financiera existente para admitir cuenta registrada u ocasional. Se aplica mediante `MigrarPagosTransferencia --aplicar`, que crea respaldo, usa una transacción y verifica que las filas de las 19 tablas no cambien. El esquema de instalación nuevo ya incorpora estas funciones.
