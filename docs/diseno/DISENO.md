# DayNest — Tu espacio, tus días.

Plan definido a partir de la descripción del usuario y autorizado el 9 de octubre de 2026. No existían el plan original, capturas ni assets en este proyecto. Este documento es la referencia visual propia; no se copiaron diseños, marcas ni contenidos de terceros.

## Tokens

| Uso | Token | Valor |
| --- | --- | --- |
| Fondo marfil | `--paper` | `#FAF8F5` |
| Superficie | `--surface` | `#FFFFFF` |
| Texto / azul noche | `--ink`, `--night` | `#182D42` |
| Texto secundario | `--muted` | `#607080` |
| Acción terracota | `--accent` | `#B64D35` |
| Hover | `--accent-hover` | `#953C28` |
| Acento suave | `--accent-soft` | `#FAEAE2` |
| Sol | `--sun` | `#F2BC68` |
| Bordes | `--line` | `#E4E7EB` |
| Éxito | `--success` | `#24684E` |
| Error | `--error` | `#A52D35` |
| Foco | `--focus` | `#245CA6` |

Títulos: Manrope, pesos 400–800. Cuerpo: Inter, pesos 400–700. Fuentes WOFF2 variables locales, subconjunto latín (incluye español), `font-display: swap`. Espaciado: escala de 8 px (`--space-1` a `--space-8`). Radios: 8, 16 y 24 px; sombras suaves en tarjetas, buscador y panel de reserva. Contenedor máximo 1200 px. Header fijo de 76 px. Objetivos táctiles ≥44 px.

## Tanda 1

- Inicio: hero panorámico con texto propio, buscador superpuesto, tarjetas que activan filtros reales de tipo o entorno, alojamientos con datos reales y sección para propietarios.
- Resultados: filtros GET reales con identificador de ciudad para homónimos (país, región, ciudad, entrada, salida, mínimo, máximo, capacidad, tipo y entorno); tarjetas 4:3 y paginación existentes.
- Detalle: galería editorial 1+4, datos reales y tarjeta lateral con estimación de noches × tarifa. El cálculo es informativo y el servicio sigue validando y calculando la reserva.
- Login y registro: dos columnas con fotografía y formulario existente; un único campo Nombre completo y los perfiles de registro existentes (cliente/propietario/ambos; administración se asigna fuera del registro público).
- Base común: navegación móvil desplegable, footer, botones/estados de carga, campos/errores, tarjetas, tablas/paginación, insignias, alertas, vacío y modal nativo accesible para ampliar imágenes.

## Reglas y marcadores

La Tanda 1 original solo modificó presentación. El cambio funcional autorizado el 10/10/2026 adapta backend/SQL al modelo definitivo de 13 tablas y añade catálogo geográfico, autocompletado, bloqueos y devoluciones. Conserva CSRF y escape JSTL. Se mantiene el escape JSTL. No se añaden frameworks ni recursos remotos al navegador.

Las fotos son ilustrativas, no fotografías verificadas de los inmuebles. Una asignación local determinista por ID proporciona variedad sin persistencia nueva. La galería añade cuatro referencias editoriales, explícitamente señaladas. Las categorías activan filtros reales: Apartamento, Casa y Cabaña corresponden al tipo; Campo y Mar son características independientes confirmadas por el propietario. La clasificación pendiente no se infiere de las fotos ni de la ciudad. Véase [FILTROS_INMUEBLES.md](../FILTROS_INMUEBLES.md). No hay valoraciones, servicios inventados, recuperación de contraseña o edición de perfil.

Responsive: 1440 px (tres tarjetas), 768 px (dos), 390 px (una); formularios sin desplazamiento horizontal; tablas con scroll propio. Navegación mediante teclado, foco visible, etiquetas y alt; `prefers-reduced-motion`. Las pantallas autenticadas reciben solamente el estilo común durante la Tanda 1; sus reorganizaciones quedan para las tandas siguientes.

## Tandas pendientes de confirmación

2. Cliente y perfil/cambio de modo (solo ambos perfiles).
3. Panel lateral de propietario y administrador; errores 404/500.

No se avanza de tanda sin confirmación. Cada entrega ejecuta `mvnw test` y `WebHttpIT` con JDK 17 y conserva la evidencia de revisión visual en los registros locales de `.tools/` y en la documentación de la funcionalidad correspondiente.
