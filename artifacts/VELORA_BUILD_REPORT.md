# Velora — informe de entrega

## Resultado

Velora se construyó sobre el snapshot solicitado de Elefin, commit base:

`c3a2e52506942597444468be78ba3281996a6576`

La fuente final usa `com.ruvik.velora`, nombre visible `Velora`, identidad cyan, español con fallback inglés, logo y banners TV/Fire TV.

## Cambios principales

- Live TV integrada mediante Jellyfin Live TV API; la app no lee directamente ninguna M3U.
- Agrupación conservadora de duplicados y EPG/programa/logos/progreso cuando Jellyfin los proporciona.
- Ocultado de Seguir viendo por servidor+usuario+item, sin marcar visto ni borrar progreso; reaparece al reproducir.
- Reproductor Original First: MediaCodec/hardware y Direct Play primero, FFmpeg como fallback, sin límites artificiales en Original.
- Selector durante reproducción: Original, Automática, 4K/40 Mbps, 1080p/20 Mbps, 1080p/10 Mbps, 720p/5 Mbps y 480p/2 Mbps; mantiene audio/subtítulos y guarda la preferencia.
- Logo Velora y banners 320x180 y 1280x720 incluidos.
- Navegación de TV en directo presentada como pestaña textual en la posición indicada, sin el icono duplicado.
- Traducciones corregidas para Películas similares, Series similares, Visto/Marcar como visto y salida de la app; la pantalla de carga usa el logo Velora.
- Discovery no se expone como opción de películas/series.
- El cambio de calidad recarga el recurso en el mismo reproductor y conserva la posición, evitando dobles players; el selector de formato reaplica el modo al PlayerView.
- Ajustes traducidos al español, incluidos diálogos, autenticación, subtítulos, caché, actualizaciones y colores del tema.
- Nueva categoría «Acerca de» con el crédito «By Ruvikitten».
- Eliminada la UI de Buy Me a Coffee, sponsors y branding ajeno.
- Se recuperaron dependencias nativas AV1 faltantes del snapshot para que el módulo compile.

## Compilación

- `:app:assembleDebug`: correcta.
- `:app:assembleRelease`: correcta; artefacto release sin firma.
- JDK usado: Temurin 17.0.20.
- Gradle usado: 8.13.
- SDK/NDK/CMake instalados localmente para la compilación.

## APK y SHA-256

- Debug: `Velora-debug.apk`
  - SHA-256: `323553ABC5F2E685A16CC97C68F450F1B860163EFD4048C059F5AB05BA906EC7`
- Release unsigned: `Velora-release-unsigned.apk`
  - SHA-256: `8FE5C57421E16E20E67CCE6959ECCB115302BA5DBBF32459EACD87158192A66C`

## Pruebas realizadas

- Parseo XML de Manifest y recursos de strings.
- Revisión estática de applicationId, branding, Live TV, selector de calidad y cadenas prohibidas.
- Compilación debug y release completa, incluido decoder AV1 nativo para ARM.

## No realizadas

- Instalación y prueba en un dispositivo físico Android TV/Fire TV.
- Validación con servidor Jellyfin real, Live TV real, D-pad, ANR, memoria y retorno Back.
- Validación física de HDR10/HDR10+/HLG/Dolby Vision, AV1 por hardware y TrueHD/DTS-HD.

## Limitaciones reales

La negociación final de HDR, Dolby Vision, AV1 y audio lossless depende del códec/pantalla/dispositivo y de las capacidades anunciadas por Jellyfin. El release no está firmado y requiere firma propia para distribución.

## Entregables

- `Velora-debug.apk`
- `Velora-release-unsigned.apk`
- `velora-source-final.zip`
- `velora.patch`
- Este informe

## Publicación

La publicación automática queda bloqueada técnicamente: `rubenqs12/Velora` devuelve 404 tanto para el conector autenticado como para la búsqueda de repositorios, y las herramientas disponibles no permiten crear repositorios nuevos. No se modificó ningún repositorio ajeno ni se publicó en otro lugar.

