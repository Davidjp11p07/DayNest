# Diagramas de diseño DayNest

Actualizados contra Java y `schema.sql` el 10 de octubre de 2026. Se incluyen **16 fuentes PlantUML y 16 SVG**. Finanzas se separó del calendario para mejorar la lectura. El [informe editable](../INFORME.md), [HTML autónomo](../INFORME.html) y [PDF](../INFORME.pdf) integran los cuatro tipos UML solicitados y 20 fichas principales de casos de uso.

| Fuente e imagen | Contenido |
| --- | --- |
| [01_entidad_relacion.puml](01_entidad_relacion.puml) · [SVG](01_entidad_relacion.svg) | 19 tablas, 120 columnas, claves y cardinalidades; tipo/entorno opcionales |
| [02_casos_uso.puml](02_casos_uso.puml) · [SVG](02_casos_uso.svg) | Visitante, cliente, propietario y administrador; CU01–CU20, include y extend |
| [03_clases_modelo_usuarios.puml](03_clases_modelo_usuarios.puml) · [SVG](03_clases_modelo_usuarios.svg) | Identidad, perfiles por composición, interfaces y ubicación |
| [04_clases_modelo_reservas_finanzas.puml](04_clases_modelo_reservas_finanzas.puml) · [SVG](04_clases_modelo_reservas_finanzas.svg) | Inmueble, contrato y calendario; conserva el nombre anterior del archivo |
| [05_clases_servlet_service.puml](05_clases_servlet_service.puml) · [SVG](05_clases_servlet_service.svg) | Entradas HTTP, Web, servicios y tarea periódica |
| [06_clases_service_impl.puml](06_clases_service_impl.puml) · [SVG](06_clases_service_impl.svg) | Interfaces, implementaciones, ServicioBase y record FiltrosInmueble |
| [07_clases_dao_jdbc.puml](07_clases_dao_jdbc.puml) · [SVG](07_clases_dao_jdbc.svg) | DAO/JDBC y contexto anidado UnidadTrabajoDao.Daos |
| [08_secuencia_login.puml](08_secuencia_login.puml) · [SVG](08_secuencia_login.svg) | CU02: credenciales, perfiles, sesión y errores |
| [09_secuencia_busqueda.puml](09_secuencia_busqueda.puml) · [SVG](09_secuencia_busqueda.svg) | CU04: catálogo, homónimos y filtros combinados |
| [10_secuencia_reserva.puml](10_secuencia_reserva.puml) · [SVG](10_secuencia_reserva.svg) | CU06: capacidad, cruce, bloqueo y contrato atómico |
| [11_secuencia_pago.puml](11_secuencia_pago.puml) · [SVG](11_secuencia_pago.svg) | CU08: saldo, cobro simulado y confirmación |
| [12_secuencia_giro.puml](12_secuencia_giro.puml) · [SVG](12_secuencia_giro.svg) | CU19: salida cumplida, propietario pactado y giro neto |
| [13_actividad_reserva_pago.puml](13_actividad_reserva_pago.puml) · [SVG](13_actividad_reserva_pago.svg) | Reserva, pago, vencimiento y cancelación/devolución |
| [14_actividad_publicacion.puml](14_actividad_publicacion.puml) · [SVG](14_actividad_publicacion.svg) | Publicar/editar con clasificación opcional |
| [15_actividad_giros.puml](15_actividad_giros.puml) · [SVG](15_actividad_giros.svg) | Validación y ejecución simulada administrativa |
| [16_clases_finanzas.puml](16_clases_finanzas.puml) · [SVG](16_clases_finanzas.svg) | MovimientoFinanciero abstracta, Pago y Giro |

Las firmas con `(...)` tienen parámetros abreviados. Se muestran operaciones principales y se omiten accesores repetitivos. Las secuencias describen el callback del servicio dentro de la unidad de trabajo; SQL y commit/rollback no son métodos Java añadidos. Las actividades son pasos funcionales. Pago y giro son simulaciones, sin pasarela externa.

El ER es el modelo lógico con extensiones autorizadas. El [SVG conceptual original](../modelo_er_daynest.svg) permanece sin cambios; consulte [su adaptación](../ADAPTACION_SVG.md). `public` conserva el archivo histórico, separado de `daynest`.

## Comprobar y regenerar sin internet

Se usó **PlantUML 1.2025.10** y Java 17. Dieciseis diagramas usan **Smetana**; casos de uso utiliza el **Graphviz local integrado en PlantUML para Windows**, para evitar un recorte detectado con Smetana. No se instaló Graphviz por separado ni se usó servidor remoto. JAR local: `.tools/plantuml/plantuml-1.2025.10.jar`. SHA-256:

```text
4A01EA09B317180FB8E7EEF712DFDCA725409D2EE1919E4B5ADFE9D8362B6FE5
```

El JAR se obtuvo de [Maven Central](https://repo.maven.apache.org/maven2/net/sourceforge/plantuml/plantuml/1.2025.10/plantuml-1.2025.10.jar). Solo su descarga inicial necesitó internet. Al trasladar herramientas, copie también el JAR y `estilo.iuml`.

Desde la raíz del proyecto:

```powershell
$javaDiagramas = (Get-Command java).Source
powershell -NoProfile -ExecutionPolicy Bypass -File docs/diagramas/Verificar-Diagramas.ps1

powershell -NoProfile -ExecutionPolicy Bypass -File docs/diagramas/Renderizar-Diagramas.ps1 `
  -PlantUmlJar .tools/plantuml/plantuml-1.2025.10.jar `
  -JavaExe $javaDiagramas -Formato svg -Smetana

# PNG opcional:
powershell -NoProfile -ExecutionPolicy Bypass -File docs/diagramas/Renderizar-Diagramas.ps1 `
  -PlantUmlJar .tools/plantuml/plantuml-1.2025.10.jar `
  -JavaExe $javaDiagramas -Formato png -Smetana
```

El verificador muestra su resultado en la consola. Comprueba nombres y columnas con claves; no demuestra firmas completas ni comportamiento. El renderizador ejecuta `-checkonly` y `-failfast2`, y comprueba una imagen por fuente. Condiciones y relaciones también se revisaron contra código. Ningún script modifica PostgreSQL.

En otro sistema operativo, el archivo 02 requiere Graphviz disponible localmente; puede indicarlo con `-DotExe`. El `!undef USAR_SMETANA` de esa fuente conserva su motor alternativo aunque el resto se genere con `-Smetana`.

## Regenerar el informe

La fuente es `docs/INFORME.md`. Complete allí los datos académicos de la portada. Después de modificar diagramas, renderice SVG y ejecute:

```powershell
# HTML autónomo, solo Python estándar:
python docs/Generar-Informe.py

# PDF con Playwright y Chrome locales:
python docs/Generar-Informe.py --pdf
```

El generador usa Playwright existente en `.tools/daynest-python` o en el entorno Python. Otra ruta de Chrome se indica mediante `--chrome 'C:/ruta/chrome.exe'`. Si no tiene Playwright, genere HTML y use Imprimir → Guardar como PDF en Chrome, con fondos y tamaños CSS de página.

El HTML incorpora SVG y fuentes; el generador PDF bloquea HTTP/HTTPS. Texto A4 y diagramas A3 con orientación según su forma permiten conservar lectura. Revise la impresión antes de reducir todo a A4. HTML/SVG se pueden ampliar sin perder nitidez. La plantilla no implica aprobación del profesor.

El modelo vigente tiene diez tablas del profesor y nueve de soporte. Las cuentas bancarias se documentan en `17_clases_cuentas.puml`; `modelo_er_daynest.svg` muestra las seis entidades conceptuales.
