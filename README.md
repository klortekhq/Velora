# Velora

### Cliente Jellyfin multiplataforma

Versión de código: **1.2.73** · Por **Klørtek**

Velora es una aplicación independiente para disfrutar de bibliotecas Jellyfin con una interfaz rápida, limpia y adaptada a cada pantalla. La experiencia móvil está pensada para tocar y deslizar; la experiencia de televisión, para mando a distancia, D-pad y pantalla grande.

## Qué ofrece

- Inicio tipo streaming con **Seguir viendo**, películas y series añadidas recientemente; los episodios se consultan desde la ficha de cada serie y **Seguir viendo**, sin una fila redundante de episodios recientes en el inicio.
- Fichas completas de películas y series con temporadas, episodios, reparto, estudios, detalles técnicos y títulos relacionados.
- Reparto interactivo: al abrir una persona se muestran sus películas y series disponibles en Jellyfin.
- Búsqueda, ordenación y filtros en español.
- Sesión persistente y reconexión automática cuando el servidor vuelve a estar disponible.
- Televisión en directo integrada en la navegación principal cuando el usuario tiene Live TV habilitada.
- Canales individuales respetando el orden y los nombres recibidos de Jellyfin, sin agrupar ni normalizar fuentes IPTV.
- EPG, programa actual, logos y progreso cuando el servidor los proporciona.
- Descargas gestionadas dentro de la aplicación para reproducir contenido compatible sin conexión en móvil y tablet; no se muestran en TV ni en navegador.
- Interfaz cyan, navegación redondeada y layouts específicos para móvil y televisión.

## Reproducción

Velora sigue una estrategia **Original First**:

1. Direct Play con aceleración MediaCodec cuando el dispositivo es compatible.
2. Remux o Direct Stream.
3. Transcodificación del servidor únicamente cuando es necesaria.
4. Un backend alternativo únicamente cuando la plataforma o el formato lo necesitan.

En Android, Media3/ExoPlayer es el reproductor predeterminado en móvil, tablet,
Android TV y Fire TV. El backend alternativo solo se utiliza si el usuario lo
selecciona o si la compatibilidad del dispositivo lo requiere.

No se limita preventivamente la resolución, bitrate, FPS, HDR ni códec. HDR10, HDR10+, HLG, Dolby Vision, AV1, TrueHD y DTS-HD dependen del hardware, firmware, pantalla, licencias y capacidades del servidor.

Durante la reproducción se puede cambiar la calidad sin salir del reproductor:

- Original — opción predeterminada
- Automática
- 4K / 40 Mbps
- 1080p / 20 Mbps
- 1080p / 10 Mbps
- 720p / 5 Mbps
- 480p / 2 Mbps

También están disponibles los controles de aspecto, audio, subtítulos y ajustes del reproductor. Las preferencias de calidad se recuerdan por usuario y se conservan las pistas seleccionadas cuando Jellyfin lo permite.

## Plataformas

| Plataforma | Experiencia |
| --- | --- |
| Android móvil | Interfaz táctil, inicio por filas y reproducción a pantalla completa |
| Tablet | Layout adaptable y navegación optimizada para pantallas grandes |
| Android TV | D-pad, botón Back, foco visible y navegación horizontal |
| Fire TV | Diseño Leanback, banner de TV y reproducción optimizada para el mando |
| Samsung Tizen | Cliente web de TV con navegación por mando; `.wgt` al usar Tizen Studio |
| LG webOS | Cliente web de TV con navegación por mando; `.ipk` con `ares-package` |
| Hisense VIDAA | Bundle HTML5 para publicación en la tienda/portal VIDAA |

La base nativa compartida de Apple está en [`apple/`](apple/) y se valida como
Swift Package en macOS. Las aplicaciones SwiftUI completas de iPhone, iPad y
Apple TV todavía no están terminadas; se desarrollan como clientes Apple nativos,
no como una versión Android estirada. Los paquetes Tizen, webOS y VIDAA requieren validación en sus
SDK, emuladores o dispositivos reales antes de considerarse certificados.

La pestaña **Televisión en directo** solo aparece cuando Jellyfin informa de que el usuario tiene esa función disponible. No existe una pestaña IPTV separada y la aplicación no lee directamente la lista M3U. Las descargas, la calidad de descarga y el almacenamiento sin conexión solo aparecen en Android móvil/tablet e iPhone/iPad; nunca en TV ni navegador.

## Descargar

Las versiones compiladas se publican en [Releases](https://github.com/klortekhq/Velora/releases). Cada release etiquetada reúne los cuatro APK de Android (móvil y TV, debug y release unsigned), el cliente web y los paquetes web disponibles, junto con sus sumas SHA-256. Para instalar manualmente en Android TV o Fire TV, descarga el APK correspondiente y realiza una instalación local.

## Cliente web y Smart TV

El cliente web común está en [`web/`](web/). Incluye conexión Jellyfin,
biblioteca, búsqueda, Live TV y reproducción HTML5 responsive para navegador,
móvil y tablet. Los adaptadores de empaquetado para Samsung Tizen, LG webOS y
Hisense VIDAA están en [`web/platforms/`](web/platforms/). Tizen genera `.wgt`
cuando Tizen Studio está instalado; webOS genera `.ipk` con `ares-package`.
VIDAA utiliza un bundle HTML5 alojado y su publicación requiere validación,
región y certificado del portal VIDAA, no un `.vpk` universal.

## Compilar desde código fuente

Requisitos:

- JDK 17
- Android SDK
- Windows, macOS o Linux con Gradle disponible mediante el wrapper incluido

```bash
git clone https://github.com/klortekhq/Velora.git
cd Velora
./gradlew :app:testMobileDebugUnitTest \
  :app:assembleMobileDebug :app:assembleTvDebug
```

Para generar los APK release unsigned de móvil/tablet y TV:

```bash
./gradlew :app:assembleMobileRelease :app:assembleTvRelease
```

Las variantes debug son instalables para pruebas. Las variantes release
generadas aquí no están firmadas; deben firmarse con una clave propia antes de
distribuirse. El flujo de GitHub Actions ejecuta estas mismas tareas y publica
los artefactos juntos cuando se crea una etiqueta `v*`.

## Rendimiento

Velora utiliza Compose, listas perezosas con claves estables, carga diferida de imágenes, caché local y actualizaciones controladas para reducir recomposiciones, consumo de memoria y tráfico innecesario. La reproducción usa una única sesión de reproductor y evita reinicios de Activity al cambiar opciones durante la reproducción.

## Estado del proyecto

Velora está en desarrollo activo. El estado verificable por plataforma, las
pruebas ejecutadas y las limitaciones conocidas se mantienen en
[`docs/progress/STATUS.md`](docs/progress/STATUS.md). La reproducción concreta
de HDR, Dolby Vision, audio passthrough, AV1 y Live TV puede variar según el
dispositivo y el servidor Jellyfin.

## Privacidad

Velora se conecta al servidor Jellyfin que configura el usuario. No incluye patrocinadores, donaciones, publicidad ni servicios de terceros obligatorios.

## Créditos

Proyecto de Klørtek. Para cualquier incidencia o propuesta, utiliza [Issues](https://github.com/klortekhq/Velora/issues).

## Licencia

Consulta el archivo [LICENSE](LICENSE) para conocer los términos de distribución del proyecto y sus componentes.
