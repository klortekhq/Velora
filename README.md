# Velora

Cliente de Jellyfin para Android, Android TV y Fire TV, con interfaz cyan, navegación fluida con mando y experiencia móvil adaptada a cada pantalla.

## Identidad

- Nombre visible: **Velora**
- Application ID: `com.klortek.velora`
- Idioma principal: español, con fallback en inglés
- Sin sponsors, donaciones ni branding de terceros
- Crédito opcional: `By Ruvikitten`

## Funciones principales

- Inicio tipo biblioteca: Seguir viendo, películas y series añadidas recientemente, episodios y bibliotecas.
- Fichas de películas y series con reparto interactivo, temporadas, episodios, estudios, progreso y reproducción.
- Al abrir un actor se muestran sus películas y series relacionadas cuando Jellyfin proporciona esa información.
- Seguir viendo muestra la serie, temporada y episodio correspondientes.
- Pulsación larga para quitar un elemento de Seguir viendo sin marcarlo como visto ni borrar su progreso.
- La sesión se conserva entre reinicios y la app reintenta la conexión si el servidor no está disponible temporalmente.
- Búsqueda y filtros traducidos al español.
- Descargas móviles persistentes para reproducir películas y episodios sin conexión cuando Jellyfin autoriza la descarga.
- Televisión en directo integrada en la pestaña **Televisión en directo**, únicamente si el usuario tiene Live TV habilitada en Jellyfin.
- Canales Live TV individuales, sin agrupación ni normalización de nombres, respetando el orden recibido de Jellyfin.
- EPG, programa actual, logos y progreso cuando Jellyfin los proporciona.

## Reproductor Original First

La prioridad del reproductor es conservar la calidad original:

1. Direct Play y MediaCodec por hardware.
2. Remux o Direct Stream.
3. Transcodificación Jellyfin solo cuando sea necesaria.
4. FFmpeg como fallback.

No se aplican degradaciones preventivas de resolución, bitrate, FPS, HDR o códec. Los efectos Fake HDR, shaders y postprocesado están desactivados por defecto.

Durante la reproducción se puede seleccionar:

- Original — opción predeterminada
- Automática
- 4K / 40 Mbps
- 1080p / 20 Mbps
- 1080p / 10 Mbps
- 720p / 5 Mbps
- 480p / 2 Mbps

La selección se puede cambiar durante la reproducción, conserva audio y subtítulos y se recuerda por usuario. También están disponibles los controles de aspecto, audio y subtítulos.

## Android TV y Fire TV

- Navegación D-pad y botón Back adaptados a televisión.
- Banner y recursos Leanback incluidos.
- Pantalla completa durante la reproducción.
- Interfaz optimizada para desplazamiento fluido y bajo consumo de memoria.
- TV en directo centralizada, sin una pestaña IPTV separada.

## Rendimiento

Velora utiliza listas perezosas con claves estables, caché de imágenes en memoria y disco, cargas diferidas y actualizaciones controladas para evitar recomposiciones y llamadas de red innecesarias durante el desplazamiento.

La arquitectura toma como referencia patrones habituales de clientes nativos ligeros para dispositivos TV económicos, manteniendo Compose y el diseño de Velora.

## Compilar

Requisitos:

- JDK 17
- Android SDK configurado
- Android Studio o Gradle

```bash
./gradlew :app:assembleDebug
./gradlew :app:assembleRelease
```

La release generada por defecto es unsigned y debe firmarse con una clave propia para distribuirla.

## Limitaciones reales

HDR10, HDR10+, HLG, Dolby Vision, AV1 por hardware y audio TrueHD/DTS-HD dependen del dispositivo, firmware, pantalla, licencias y capacidades anunciadas por Jellyfin. La aplicación no puede garantizar esos formatos en hardware que no los soporte.

La reproducción Live TV depende del endpoint Live TV de Jellyfin y de los permisos del usuario. Velora no lee directamente listas M3U desde la aplicación.

Las descargas offline dependen de que Jellyfin permita descargar el archivo solicitado.

## Créditos

Velora es un cliente independiente para servidores Jellyfin. `By Ruvikitten`.
