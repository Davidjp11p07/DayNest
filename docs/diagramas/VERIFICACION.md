# Comprobación de fuentes de los diagramas

OK: 16 diagramas; 114 referencias a tipos, 292 métodos, 90 atributos; 13 tablas y 73 columnas con PK/FK/UQ comprobadas.

La comprobación compara clases/interfaces/enums, nombres de atributos y métodos de los diagramas de clases y secuencias con src/main/java, incluidos métodos heredados de ServicioBase y accesores generados de records. Compara todas las columnas y marcas PK/FK/UQ del ER con schema.sql. No carga db.properties ni modifica la base.

Revisión adicional: cardinalidades, tipos/firmas principales, orden de flujos, rutas HTTP, reglas de negocio, actores y notas contrastados con los servlets, servicios, DAO JDBC y schema.sql. Las actividades describen pasos funcionales, no nuevos métodos.

Límite: esta comprobación de nombres no demuestra el comportamiento ni valida firmas completas. El parser y las imágenes se comprueban por separado mediante Renderizar-Diagramas.ps1 (-checkonly y -failfast2). La correspondencia funcional se documenta en el informe y las fichas. No se modifica la base de datos.