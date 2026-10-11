# Créditos de imágenes — DayNest

14 fotografías descargadas de Unsplash el 9 de octubre de 2026. Todas se obtuvieron de páginas identificadas como gratuitas bajo la [licencia Unsplash](https://unsplash.com/license). No se utilizan fotos Unsplash+ ni de plataformas de reservas. Los enlaces son documentación; la aplicación solo solicita los archivos locales.

Se redimensionaron y recomprimieron en JPEG: máximo 1600 px de ancho y menos de 300.000 bytes cada una. El hero se recortó a 1600 × 900. Son referencias editoriales, no fotos verificadas de los inmuebles. No implican que el fotógrafo o propietario de la imagen recomiende DayNest.

| Archivo local | Autor | Fuente / foto | Dimensiones | Tamaño |
| --- | --- | --- | --- | --- |
| `hero.jpg` | Dmitry Spravko | [Unsplash](https://unsplash.com/photos/GT9BT478ZJQ) | 1600 × 900 | 258,147 bytes |
| `cabana-01.jpg` | Marek Piwnicki | [Unsplash](https://unsplash.com/photos/T-diK7VcHmY) | 1600 × 900 | 272,746 bytes |
| `cabana-02.jpg` | Edoardo Bortoli | [Unsplash](https://unsplash.com/photos/xxC9ooYnMSo) | 1600 × 1067 | 294,517 bytes |
| `cabana-03.jpg` | Marija L | [Unsplash](https://unsplash.com/photos/4_TfkPM9MBw) | 1600 × 1067 | 223,349 bytes |
| `apto-01.jpg` | Lisa Anna | [Unsplash](https://unsplash.com/photos/PfeKrK-V_XA) | 1600 × 1067 | 116,267 bytes |
| `interior-01.jpg` | Hannah Busing | [Unsplash](https://unsplash.com/photos/U-k6XLlml1I) | 1600 × 1067 | 228,592 bytes |
| `apto-02.jpg` | Bozica Uglesic | [Unsplash](https://unsplash.com/photos/7MGj75YLNTw) | 1600 × 984 | 124,322 bytes |
| `finca-01.jpg` | Andreas Slotosch | [Unsplash](https://unsplash.com/photos/hfi6t6fTL74) | 1600 × 1067 | 249,588 bytes |
| `finca-02.jpg` | Tanya Barrow (@tanyabarrow, nombre visible T) | [Unsplash](https://unsplash.com/photos/mFT7YwTBlc8) | 1600 × 1061 | 256,078 bytes |
| `casa-01.jpg` | Drake | [Unsplash](https://unsplash.com/photos/c08tsGEHKlQ) | 1067 × 1600 | 282,146 bytes |
| `casa-02.jpg` | Aalo Lens | [Unsplash](https://unsplash.com/photos/eWOgoFHlE8g) | 1600 × 900 | 271,762 bytes |
| `casa-03.jpg` | Umair Ali Asad | [Unsplash](https://unsplash.com/photos/brr4IdZAiP4) | 1600 × 1067 | 299,171 bytes |
| `playa-01.jpg` | Arno Smit | [Unsplash](https://unsplash.com/photos/qY_yTu7YBT4) | 1600 × 946 | 215,397 bytes |
| `casa-mar-01.jpg` | Johan Rydberg | [Unsplash](https://unsplash.com/photos/-DB6G0_VO2Q) | 1597 × 1600 | 229,559 bytes |

## Asignación de referencias

`WEB-INF/views/layout/imagenes.jspf` contiene una lista de 13 nombres locales. El índice es `(id - 1) % cantidad`: mismo ID, misma foto, sin tabla ni persistencia nueva. `hero.jpg` se reserva para el hero. Se reutilizan fotos locales como referencias secundarias de la galería 1+4. Para nuevos IDs la lista se repite de forma determinista.

## Fuentes e iconografía

- Inter: [proyecto oficial](https://github.com/rsms/inter), [OFL 1.1](https://github.com/google/fonts/blob/main/ofl/inter/OFL.txt). Licencia redistribuida en `assets/fonts/Inter-OFL.txt`.
- Manrope: [proyecto oficial](https://github.com/googlefonts/manrope), [OFL 1.1](https://github.com/google/fonts/blob/main/ofl/manrope/OFL.txt). Licencia redistribuida en `assets/fonts/Manrope-OFL.txt`.
- WOFF2 variables latín suministrados por Google Fonts durante la preparación; los archivos se sirven localmente e incluyen las letras y acentos del español.
- `daynest-logo.svg`: dibujo original de casa geométrica con sol naciente, creado para este proyecto.
- `iconos.svg`: dibujos SVG propios de trazo redondeado, estilo Lucide; no se incorporó una biblioteca ni paquete de iconos externo.

`imagenes.json` conserva los enlaces de descarga, dimensiones y tamaño de cada archivo. Los scripts de preparación y revisión viven en `.tools/`; Python no es una dependencia de la aplicación Java ni se necesita para ejecutarla.
