# Estado verificable de Velora

Updated: 2026-09-02

Este dashboard registra únicamente el estado verificable. Una función no se
marca como completa solo porque exista código o un artefacto anterior.

La única rama publicada es `main`. Las notas históricas que aparecen más abajo
se conservan como referencia y no representan el estado actual de la release.

La política de publicación mantiene las releases públicas para hitos funcionales
relevantes: los workflows automáticos solo se activan con etiquetas `vX.Y.0`.
Los builds de parche se reservan para QA y no generan una release salvo que la
corrección sea crítica o de seguridad.

Verificación local del 2026-09-02: `node web/scripts/test-platform.mjs`,
`testMobileDebugUnitTest` y `compileTvDebugKotlin` pasan. No se ha creado una
nueva release para esta verificación porque no incorpora un bloque funcional
grande.

El flujo de inicio de sesión Android ya no muestra mensajes técnicos ni textos
hardcodeados en inglés: los fallos de credenciales y Quick Connect usan el
recurso localizado de error genérico. Se mantiene el detalle técnico fuera de
la interfaz y la compilación móvil/TV se valida tras el cambio.

El lint móvil detectó y se corrigió el uso de APIs no disponibles en Android
21 en los servicios de música y en el reloj; la compilación móvil/TV posterior
supera el cambio. El lint completo aún queda abierto por deuda previa (232
errores, 297 advertencias y 43 sugerencias en el informe actual, principalmente
compatibilidad API, opt-in de Media3 y estilo); no se presenta como un lint
limpio.

En la siguiente pasada también se corrigieron accesos incompatibles con APIs
antiguas en el control de versión, el cálculo de tamaños de descargas, la
limpieza de subtítulos y el foco del OSD. `testMobileDebugUnitTest`,
`compileMobileDebugKotlin` y `compileTvDebugKotlin` vuelven a pasar; el lint
completo sigue pendiente por la deuda anterior ya contabilizada. En esta ronda
se añadieron opt-ins AndroidX explícitos para los puntos que usan Media3 y se
evitaron más llamadas nullable a DownloadManager; el informe bajó de 304 a 232
errores. `node --check web/app.js`, las pruebas web y `git diff --check` pasan.

Después se corrigieron cuatro errores adicionales del informe: dos bloques con
indentación sospechosa, el tipo de contenido de audio incorrecto en Media3 y
la construcción de un Intent que podía borrar la URI al fijar el MIME. También
se hizo segura la consulta de DownloadManager cuando el servicio no está
disponible. `testMobileDebugUnitTest`, `compileMobileDebugKotlin` y
`compileTvDebugKotlin` pasan de nuevo. El informe completo de lint debe
recalcularse en la siguiente ejecución; los avisos de APIs obsoletas y las
dependencias pendientes siguen siendo deuda conocida.

La autenticación Android ya no imprime excepciones completas ni trazas de red:
el log conserva únicamente el tipo de error y el código HTTP, evitando exponer
por accidente el host configurado o detalles de la petición. La corrección se
ha validado con los unit tests móviles y la compilación de TV; no genera una
release aislada.

La auditoría se amplió al resto del cliente Android: se eliminaron trazas y
mensajes crudos de excepciones en API, descubrimiento, reproducción, trailers,
subtítulos, actualizaciones y descargas. Los logs conservan solo contexto
seguro y el tipo de excepción; la interfaz muestra mensajes genéricos. La
compilación móvil y TV vuelve a pasar tras el cambio.

El cliente web añade ahora filtros funcionales de Live TV por canales favoritos
y por grupo. Los grupos se derivan de `Tags`, `ChannelType` y `ServiceName` de
Jellyfin, con una opción explícita para canales sin grupo; las preferencias se
conservan localmente. La sintaxis, las pruebas web y el empaquetado web se han
verificado de nuevo. Este cambio se sube a `main`, pero no genera APK ni
release por sí solo.

El fallback MPV de Android ya no desactiva la verificación TLS. Las conexiones
HTTPS respetan ahora la validación de certificados por defecto; los servidores
locales HTTP siguen funcionando y no se ha añadido ningún bypass oculto. La
prueba móvil y la compilación TV pasan tras este cambio.

Live TV Android ya no muestra mensajes brutos de excepciones de red ni un
mensaje de favorito codificado en español: los errores visibles son genéricos,
localizados y los detalles técnicos quedan únicamente en el log local. La
clave tiene valor base y traducciones en español, inglés, francés y alemán.
La corrección se ha validado con `testMobileDebugUnitTest` y
`compileTvDebugKotlin`.

La base Apple incorpora ahora entrypoints SwiftPM separados `VeloraMobile` y
`VeloraTV`, ambos sobre `VeloraKit`; el primero selecciona iPhone/iPad y el
segundo fuerza tvOS, manteniendo streaming-only en televisión. La compilación
de estos targets y la creación de los bundles `.app` siguen pendientes de un
runner macOS con Xcode y no se presentan como validadas en este host Windows.

El cliente web ya negocia Live TV mediante `PlaybackInfo` con
`AutoOpenLiveStream=true` antes de abrir el reproductor, y pasa el destino por
el proxy autenticado del mismo origen. Se añadió una regresión para impedir
que una URL de Live TV lleve `api_key` o que una ruta de disco se use como
fuente web. Al cerrar el reproductor también notifica
`Sessions/Playing/Stopped`, con posición y `PlaySessionId` cuando existe. La
prueba web pasa; la reproducción en hardware Smart TV sigue pendiente de
validación física.

El empaquetado web completo del 2026-09-02 también se ha ejecutado: el bundle
web y el bundle Samsung se preparan, webOS genera un IPK mediante
`ares-package`, y VIDAA genera su bundle HTML5 con metadatos de portal. Tizen
CLI/perfil de firma no está instalado en este host, por lo que Samsung no se
marca como WGT instalable; VIDAA tampoco se marca como paquete firmado.

La release pública más reciente con artefactos verificados sigue siendo
`v1.2.85`; `v1.3.0` ya está etiquetada y subida a `main`, con los workflows de
GitHub encargados de publicar sus artefactos. Las validaciones locales de
Android móvil/TV terminaron correctamente tras endurecer el motor de decisión
de reproducción. El modelo Apple usa ahora los nombres de
campos reales de la API Jellyfin (`Id`, `Name`, `Type`, `Overview`,
`ImageTags`) y tiene una prueba de regresión.

El bloque actual añade Smart Downloads móvil/tablet de forma opt-in. La
migración SQLite conserva los estados de visto y protegido, y la limpieza no
elimina nunca una descarga marcada para conservar. La validación móvil pasa;
la política y la identidad de transferencias gestionadas tienen pruebas de
regresión. La release 1.3.0 agrupa este bloque funcional con los cambios
offline e i18n anteriores. La compilación reproducible local terminó
correctamente; la publicación final queda pendiente de confirmar desde GitHub
Actions.

Desde esa compilación se han subido dos correcciones verificadas a `main`:
`5eb06e3` codifica los identificadores de fuente en la ruta MPV y amplía la
redacción de credenciales en diagnósticos; `02e972d` codifica correctamente
los secretos de Quick Connect. Las pruebas unitarias de móvil y TV pasan.
`9b85acb` localiza las etiquetas del selector de aspecto del reproductor en
español, inglés, francés y alemán; su prueba de presentación y la compilación
móvil pasan. Ninguno de estos cambios aislados genera una nueva release.

La auditoría de seguridad más reciente corrigió una fuga en los diagnósticos de
Quick Connect: las URLs de sondeo y los mensajes de excepción ya se redactan
antes de llegar al log, por lo que el secreto temporal no se expone. Se añadió
una prueba de regresión y pasaron los unit tests móviles, la compilación TV y
las pruebas web. Este cambio se sube como commit; no genera una release por sí
solo.

El panel de ajustes del reproductor se ha hecho legible en móviles y tablets
con un ancho mínimo adaptable, y sus etiquetas visibles (audio, subtítulos,
velocidad, calidad y estados de pista) ya están localizadas en español, inglés,
francés y alemán. Pasaron `testMobileDebugUnitTest`, `compileTvDebugKotlin`,
las pruebas web y `git diff --check`; no se ha validado todavía en hardware
real ni se genera una release por este cambio aislado.

Las descargas móviles ahora permiten activar “solo Wi‑Fi” desde Ajustes y esa
preferencia se traduce en una restricción `UNMETERED` real de WorkManager; por
defecto se conserva el comportamiento anterior de cualquier red conectada.
Además, Media3 ya no fuerza el bitrate máximo: la selección adaptativa queda
habilitada para reducir cortes en Live TV y redes variables. Al cerrar el
reproductor se informa también la detención de Live TV aunque su duración sea
indefinida, para liberar la sesión del servidor. La navegación web ya no trata
Backspace como Atrás y las releases exigen un paquete instalable cuando el
workflow se ejecuta sobre un tag. Estos cambios están validados en Android
(pruebas móviles y compilación TV) y en web (pruebas de plataforma, sintaxis y
diff limpio); los critics han señalado que la validación en hardware y la app
Apple ejecutable siguen pendientes.

El cliente web ahora puede reproducir películas y episodios sin depender de
que el alojamiento haya activado el Service Worker: usa una petición
autenticada y un Blob local sin exponer el token. Live TV conserva el requisito
del proxy autenticado para permitir reproducción continua sin credenciales en
la URL. La sintaxis y las pruebas web pasan; queda pendiente validar el flujo
en navegadores y televisores físicos.

La base Apple ya persiste y restaura la sesión de Jellyfin mediante Keychain en
las plataformas Apple, con eliminación explícita al cerrar sesión y una prueba
de ciclo completo sobre el almacén inyectable. Sigue siendo una base SwiftPM,
no un `.app` firmado: los targets iOS/iPadOS/tvOS y su validación requieren
macOS/Xcode y permanecen pendientes.

La base Apple ya puede abrir un tuner de Live TV mediante `PlaybackInfo`, elegir
la URL de transcodificación o direct stream devuelta por Jellyfin, comprobar que
pertenece al servidor configurado y construir un `AVPlayer` autenticado. La
interfaz de canal y la validación en hardware Apple siguen pendientes.

El shell SwiftUI ya muestra una pantalla de Live TV cuando hay canales y cada
fila inicia el `AVPlayer` autenticado del canal seleccionado. Las cargas se
cancelan al cambiar rápidamente y la salida notifica `Sessions/Playing/Stopped`
para liberar el tuner; la validación de foco remoto y reproducción sostenida en
hardware Apple sigue pendiente.

La corrección `bec2ed0` añade el contrato Codable de detención y evita que una
carga de canal obsoleta sustituya al canal elegido. Los tests Android móviles y
las pruebas web de regresión siguen pasando; Swift/Xcode no está instalado en
este host.

La revisión actual elimina botones sin acción en los detalles de Jellyseerr:
los estados Disponible y Solicitud pendiente se presentan como indicadores no
interactivos, mientras que Solicitar mantiene su acción real. `testMobileDebugUnitTest`
y `compileTvDebugKotlin` pasan después del cambio.

La capa SwiftUI Apple consulta ahora el bundle `VeloraKit` al presentar login,
ajustes, reproducción y Live TV, en lugar de dejar esos textos fuera de
`Localizable.strings`. Se han añadido las claves equivalentes en español,
inglés, francés y alemán. Swift/Xcode sigue pendiente en este host; Android y
Web no presentan regresiones en sus pruebas disponibles.

La superficie GL experimental de Android ya no se selecciona para AV1 ni cuando
Jellyfin no declara el códec antes de iniciar la reproducción. En esos casos
Media3 usa su SurfaceView estándar, evitando pantallas negras por decodificación
software en televisores; la regla tiene prueba unitaria y pasan las pruebas
móviles y la compilación TV.

El shell Apple acepta ahora el servidor en el formulario de inicio, permite
cambiarlo antes de autenticar y asocia la sesión persistida al servidor exacto.
Las sesiones antiguas sin esa asociación se descartan de forma segura, y el
token nunca se reutiliza contra otra URL.

La petición de autenticación Apple usa ahora los nombres y encabezados estándar
de Jellyfin (`Password` y `X-Emby-Authorization`), con una prueba que verifica
el payload sin enviar credenciales a la red.

La base Apple incorpora modelos y consultas autenticadas para canales y
programación de Live TV, con ventana temporal y límite de resultados. Se
integran en la sesión al iniciar y se limpian al cerrar sesión; todavía no se
declara como completada la reproducción de canales en Apple.

La mejora más reciente de Live TV pagina los canales y divide las consultas de
EPG en bloques, manteniendo el orden devuelto por Jellyfin y evitando perder
canales cuando la lista supera los límites habituales de la API. La prueba
unitaria móvil y la compilación de las variantes móvil y TV pasan. Es un cambio
de robustez que queda en `main`; no genera APK/release por sí solo.

El formulario de inicio de sesión ahora respeta los insets de las barras del
sistema antes de aplicar su desplazamiento, evitando que el título, los campos
o el botón queden ocultos en móviles, tablets y televisores. Se verificará junto
con la siguiente compilación Android.

La base Apple ahora persiste los ajustes locales del shell SwiftUI mediante un
almacén Codable sobre `UserDefaults`, con pruebas de guardar, recuperar y
eliminar. Swift/Xcode no está instalado en este host, por lo que la compilación
Apple y la validación en hardware siguen pendientes; no se presenta como una
verificación de Xcode.

Películas y series usan ahora el mismo `TrailerResolver`, que prioriza trailers
oficiales de YouTube en el idioma preferido y evita que cada pantalla aplique
una política distinta. La prueba unitaria móvil y la compilación móvil/TV pasan;
la validación del extractor y de reproducción sigue pendiente en hardware real.

La revisión `v1.2.82` añade una acción Atrás visible a la filmografía y ha
superado `compileMobileDebugKotlin` y `testMobileDebugUnitTest` localmente.

La revisión `v1.2.83` añade ordenado y filtrado funcional por
estado de reproducción en las bibliotecas de películas y series, con opciones
de Todos, Vistos, No vistos y Favoritos. La compilación y los unit tests Android
han pasado tras este cambio; la release `v1.2.83` está publicada con los
artefactos generados por GitHub Actions.

Desde la última release se han añadido componentes SwiftUI nativos reutilizables
para las superficies Apple y un shell compartido que conecta inicio de sesión,
biblioteca, detalle, ajustes y AVPlayer, manteniendo la regla de no mostrar
descargas en tvOS. La prueba Android móvil y sus unit tests vuelven a pasar;
iOS/iPadOS/tvOS siguen pendientes de validación en macOS/Xcode y de sus targets
de aplicación finales.

## Oleada actual

Oleada 3 — convergencia de la consulta real de bibliotecas y publicación.

## Verified in source

- Android namespace and application id use `com.klortek.velora`.
- Android product flavors exist for TV and mobile/tablet installs.
- ExoPlayer/Media3 is the default Android playback path; MPV remains an
  explicit fallback/option in the current implementation.
- A pure Kotlin `PlaybackDecisionEngine` now encodes Original First ordering,
  device/preset constraints and an explainable `PlaybackDecision` result for
  native backends.
- `JellyfinPlaybackMapper` converts real `MediaSource`/`MediaStream` metadata
  (container, codecs, HDR, dimensions, FPS, bitrate, multichannel audio and
  subtitle type) into that common playback contract.
- `PlatformCapabilities.supportsOfflineDownloads` is false for TV builds and
  gates mobile-only offline UI.
- Jellyfin Live TV client and a conditional TV entry point exist.
- A shared web client and Samsung/LG packaging scripts exist.
- App language and preferred audio/subtitle settings have been started in
  Android and web.
- Generated local build/device artifacts are now ignored by Git.
- The public README now identifies the current release and uses the exact
  mobile/TV build and test tasks used by CI.
- Release metadata is synchronized at source version 1.3.0 across Android, webOS,
  Samsung and VIDAA manifests.
- The current source includes live application of preferred audio and subtitle
  settings to the existing Media3 player; the Android tag workflow is the
  release gate for its public APKs and platform bundles.
- The mobile download-quality selector is localized in Spanish, English,
  French and German and uses the same Original/High/Medium/Low contract as the
  offline queue.
- Smart Downloads is now an explicit mobile/tablet opt-in. Watched cleanup,
  configurable unwatched-episode retention and user-protected downloads are
  persisted in SQLite; TV/browser capability gating keeps the feature out of
  unsupported UIs.
- Managed offline transfers now use their WorkManager name as a stable identity
  when the provider download id is zero, preventing progress/state collisions.
- The offline downloads screen and materialization path now use the same stable
  identity for managed transfers, preventing duplicate Compose keys and media
  overwrites; integrity metadata is persisted against that identity as well.
- Offline records now persist Jellyfin's selected `mediaSourceId`; requesting a
  different offline quality no longer reuses an existing entry for the same
  item, and the regression is covered by mobile unit tests.
- Offline SQLite schema v8 migrates the old single-item key to the composite
  `(item_id, quality)` key, so multiple quality representations can coexist
  without persistence collisions; mobile and TV unit tests pass afterward.
- Smart Downloads labels are now present in every Android locale offered by the
  app (Spanish, English, Portuguese, French, German, Italian, Japanese,
  Korean, Chinese, Russian, Arabic and Turkish); mobile compilation and tests
  pass after the catalog update.
- Web playback now refuses credential-bearing URL fallbacks and waits for the
  same-origin media proxy, keeping Jellyfin tokens out of media URLs.
- The ExoPlayer playback URL path now relies on MediaBrowser/X-Emby request
  headers instead of putting the Jellyfin token in the playback query string.
- Mobile/tablet library sorting and filtering preferences now persist locally
  (direction, favorites, playback state and genre), with a localized reset
  action; the shared query remains covered by unit tests.
- The album artwork action now starts album playback instead of being a
  decorative, non-interactive control; it is included in verified release
  `v1.2.40`.
- Jellyseerr detail actions now use Android string resources instead of
  hardcoded English labels. The additional English, German and French resources
  are included in tagged release `v1.2.43`.
- The base Spanish resource now keeps Live TV filters and favorite actions in
  Spanish, matching the existing German, French and English locale resources.
  This is included in verified release `v1.2.40`.
- Mobile detail action rows no longer render Audio, Cast or Back controls when
  the screen has no corresponding handler; the mobile debug compile and unit
  tests pass for this correction.
- Apple playback semantics now mirror Android's Original First ordering,
  including device limits, passthrough checks and quality presets; Swift tests
  are delegated to the macOS CI runner because Swift/Xcode is unavailable here.
- Apple Jellyfin catalog decoding now maps the server's capitalized JSON field
  names and is covered by a Swift regression test.
- The mobile bottom navigation now omits Downloads whenever the platform
  capability is false instead of rendering an inert destination; both Android
  mobile and TV debug tests pass for this change.
- Movie and series detail screens now expose the existing trailer resolver and
  launcher instead of hiding the functional trailer action behind a disabled
  branch; mobile and TV Kotlin compilation plus unit tests pass after this
  correction.
- El cliente web Live TV usa ahora la API específica de Jellyfin para cargar
  canales, programa actual y próxima emisión, y los presenta en filas compactas
  con progreso y navegación táctil/teclado/mando. `node --check`, los tests web
  y el empaquetado web/webOS pasan localmente; no se afirma validación física.
- El aviso de incompatibilidad AV1 y el selector de formato del reproductor
  Android ya usan recursos localizables en español e inglés; las variantes
  móvil y TV compilan y pasan sus unit tests tras el cambio.
- Apple `VeloraKit` incorpora `VeloraAppShell`, un flujo SwiftUI nativo para
  iniciar sesión, consultar películas/series, abrir detalles y reproducir con
  AVPlayer usando headers autenticados. El shell no muestra descargas en tvOS;
  Swift/Xcode no está disponible en este host, por lo que queda pendiente la
  validación de compilación y dispositivo en macOS.
- La puerta de espacio de las descargas Android ahora mide el mismo volumen
  privado que usa `OfflineDownloadWorker` (`filesDir/offline/media`), evitando
  aceptar descargas basándose en el espacio de otro volumen. La compilación y
  los unit tests móviles pasan tras este cambio.

## Release actual: v1.3.0 (preparación)

La fuente actual es `1.3.0`. Incluye las correcciones de presentación para el
renderizador GL y el contenedor móvil de MPV, además de una negociación de
reproducción que respeta la calidad elegida, los límites reales del dispositivo
y el passthrough de audio. La pantalla de detalles móvil usa ahora componentes
Material 3 táctiles en detalles, inicio y biblioteca, mientras la variante TV
conserva sus componentes de foco, además del endurecimiento de URLs MPV y Live TV.
Los tests Android móvil/TV pasan en local. Todavía
no se afirma validación física en un móvil, Fire TV o televisor.

La revisión `v1.2.86` conserva el modo de formato de imagen al recrear la
actividad y unifica el ordenado/filtrado de las pantallas reales de Películas y
Series con la consulta común cubierta por tests. La compilación Android móvil,
sus unit tests y las pruebas web pasan localmente. La release pública anterior contiene
las cuatro APK verificadas (móvil/TV debug y release unsigned), además de los
paquetes web, Samsung, webOS y VIDAA. La publicación Android automática quedó
cancelada tras bloquearse Gradle en el runner; las APK se adjuntaron y
comprobaron manualmente desde la interfaz de GitHub, sin afirmar validación
física en dispositivos. La release pública `v1.2.85` corresponde al commit
anterior; los APK y paquetes de `v1.2.86` quedan pendientes de publicar tras
la compilación reproducible de esta revisión.

La acción de trailers de películas y series ya está habilitada en el código
actual y la compilación/tests Android móvil y TV, junto con los tests web,
han pasado localmente.

La compilación local de `1.2.86` también ha generado y verificado los cuatro
APK Android (móvil/TV, debug y release unsigned). No se han publicado como
release porque la política actual agrupa los cambios pequeños y reserva las
releases públicas para hitos `vX.Y.0`; los workflows de Android y web ya
aplican ese criterio. Las correcciones de parche siguen pudiéndose compilar
manualmente para QA.

La siguiente tabla es el histórico verificado de `v1.2.80`:

| Artefacto | SHA-256 publicado |
| --- | --- |
| `Velora-mobile-debug.apk` | `a5a8a445266e03be71a5f5133557ceb0f8fc4915306efb25bb2199189b11be91` |
| `Velora-mobile-release-unsigned.apk` | `d53e01b3978a8dda671217bade20decff6d745d0ce0b8b1bc4ed4d4a4b7cfd5a` |
| `Velora-tv-debug.apk` | `d24913764e1a102f862025ee9d6af367a571367053c4b9090aeec5b5bd05fb06` |
| `Velora-tv-release-unsigned.apk` | `b3cfac01d91c7927bcd6710fdd4dc4ecba6811537c50c50e311d0e1205ddbc7a` |

La release contiene además `Velora-Web-all.zip`, `Velora-Web-all.tar.gz`,
`Velora-samsung-bundle-1.2.80.zip`, `Velora-webos-bundle-1.2.80.zip`,
`Velora-vidaa-bundle-1.2.80.zip`, `SHA256SUMS.txt` y `SHA256SUMS-web.txt`.
El paquete Tizen `.wgt` no se afirma porque el CLI/certificado de Tizen no está
disponible en este entorno.

Artefactos Android verificados en `v1.2.73`:

| Artefacto | SHA-256 |
| --- | --- |
| `Velora-mobile-debug.apk` | `489e7ac81aae43bad987593304e30809d5e57f1d4c6806913a27926d2903846a` |
| `Velora-mobile-release-unsigned.apk` | `0a6487e2a9f69fc9a017a373bfdb740cd31801dd8517d5e411355d2b95998a91` |
| `Velora-tv-debug.apk` | `a99a217621084335f1d0d6200d1e38552beb025f015b118033dd157df14b93e7` |
| `Velora-tv-release-unsigned.apk` | `e55b07e8589ec649c514b0c5c3ab00a673ecfb65d21d7e549b3ec0781f8bc576` |

La release `v1.2.73` permanece pública con las cuatro APK, los bundles
web/Smart TV y sus hashes: https://github.com/klortekhq/Velora/releases/tag/v1.2.73

Los workflows de release se activan únicamente con etiquetas de hito `vX.Y.0`
o mediante ejecución manual; los cambios de parche se validan sin publicar una
release pública automáticamente.

Release `v1.2.52` is prepared from person-navigation/i18n/home-localization, accessibility, Apple foundation and private offline storage changes; its Android and web workflows are the
current release gate and their public assets have not yet been independently
verified from this host. The tag includes the localized subtitle download
dialogs and synchronized platform metadata.

Release `v1.2.42` is publicly verified at
https://github.com/klortekhq/Velora/releases/tag/v1.2.42. GitHub Actions completed
the Android and web workflows successfully; the release contains the four
Android APKs, the web archives, platform bundles and both checksum manifests (in addition to
GitHub's source archives). The published Android APK digests are:

| Artifact | SHA-256 |
| --- | --- |
| `Velora-mobile-debug.apk` | `acff9a8484ac4123c382cbf6184f2242cd2bb37305699952a433c9c702fd89a3` |
| `Velora-mobile-release-unsigned.apk` | `47ce7823c56786365fd62af300f390120898db470980859ae5b5335ae12a7b88` |
| `Velora-tv-debug.apk` | `701bc2c3df333d9d75c364cb919ae217b73ffc8f25d0f154028bf1b4db724536` |
| `Velora-tv-release-unsigned.apk` | `a5bda29594cb1fce3abcd4deccea8d86f0d75bd89d3f6d110e910836a7d82f38` |

The release also contains the combined web archives and the current
Samsung/VIDAA/webOS bundles and both checksum manifests. It retains three
superseded `1.2.41` Smart TV bundle assets from an earlier publish attempt;
future tag releases remove these automatically. Samsung remains an unsigned bundle because
Tizen Studio/signing is not installed on this build host; VIDAA is an HTML5
submission bundle. No hardware validation is implied by a successful build.

Release `v1.2.33` is publicly verified at
https://github.com/klortekhq/Velora/releases/tag/v1.2.33. Both Android build
variants and all web/platform bundles completed successfully in GitHub Actions.
The release contains 11 assets; Android APK digests reported by GitHub are:

| Artifact | SHA-256 |
| --- | --- |
| `Velora-mobile-debug.apk` | `4e9d4ea5cc48645ab366b23f792d35f03141faa335cda47b4c72a12a4ed519a6` |
| `Velora-mobile-release-unsigned.apk` | `6ad9394037d5a1a1311ce2921dbeb3001f7b35949d9082db41abfa3e3e76b7ff` |
| `Velora-tv-debug.apk` | `ceffd1c9c4dd8a8e86e666f9549384d22f9491d837d2bef29e0a83ce76ba0e1a` |
| `Velora-tv-release-unsigned.apk` | `b6b3bfd81a8514a956dfdb3031c6dde7a81791fcf0c51f141827fa0eb0c6100a` |

The release was built from tag commit `ff02fb8`; subsequent `main` commits only
contain the retained regression test and additional diagnostic-log hardening.

Release `v1.2.30` is published at
https://github.com/klortekhq/Velora/releases/tag/v1.2.30. The Android workflow
completed successfully and the release contains the four expected APKs:

- `Velora-mobile-debug.apk`
- `Velora-mobile-release-unsigned.apk`
- `Velora-tv-debug.apk`
- `Velora-tv-release-unsigned.apk`

The same release also contains the combined web archive plus Samsung, webOS and
VIDAA bundles and the `SHA256SUMS.txt` manifest. Published APK digests are:

| Artifact | SHA-256 |
| --- | --- |
| `Velora-mobile-debug.apk` | `52178390725a4bc8366138ec3cea85dac858cdf8992b56d432557a288c5229b3` |
| `Velora-mobile-release-unsigned.apk` | `fe4298f57e292ad5a2cbaa0751535dcd95f6b5306629a976be5fd4c7394c3618` |
| `Velora-tv-debug.apk` | `7ebf5e854e731a638b6020d56786cc037cfe376989388b6306fb464c155d6874` |
| `Velora-tv-release-unsigned.apk` | `6dfe2f5359f77321c5ecb71cc05c7a1e6a50db00331c2ca2648c6c8e0bb4ce2c` |

Release `v1.2.32` is now publicly verified at
https://github.com/klortekhq/Velora/releases/tag/v1.2.32. It is a public,
non-draft release with 11 assets: the four Android APKs, web archives,
Samsung/webOS/VIDAA bundles and both checksum manifests. GitHub-reported APK
digests are:

| Artifact | SHA-256 |
| --- | --- |
| `Velora-mobile-debug.apk` | `9b0e74bac8898ca5ef3dc4ec62003cc3e606e497b70725705b4ef4cf085ec38c` |
| `Velora-mobile-release-unsigned.apk` | `311f11ef63535ce46bddb790bcd2a0a788616ff09ff1872316b0d2ed47be52f3` |
| `Velora-tv-debug.apk` | `df5a18170e1322ab59320d5f65a8ab7c932b75f660d3064c718148031a77f0f4` |
| `Velora-tv-release-unsigned.apk` | `6f3d2532a14c7976c26ef6cf58d87767a787babaf00eef3ce1353bccda046170` |

- Android-managed downloads and the MPV fallback now also keep the Jellyfin
  token in request headers rather than embedding it in playback/download URLs.
- Quick Connect diagnostics no longer log access-token fragments or polling
  secrets.
- Authentication diagnostics no longer print full request URLs, auth headers,
  token-bearing error bodies, or generated playback URLs; MPV's legacy launcher
  also uses its authenticated request headers without an `api_key` query.
- TMDB trailer diagnostics now redact user-provided API keys; the regression
  test prevents secret values from returning to Logcat.
- The mobile download enqueue path now checks available space and managed
  offline usage before replacing a failed entry, uses Jellyfin source size
  when available, and surfaces rejection without closing the app. The
  configurable maximum defaults to 25 GiB in `AppSettings`.
- Offline deletion now routes provider-backed `content://` URIs through
  `ContentResolver`, while retaining compatibility with `file://` and legacy
  filesystem paths.
- The Android player now reapplies the selected aspect mode after every Media3
  `VideoSize` update, preventing fullscreen or stream changes from silently
  restoring the source ratio.
- The login form now uses an adaptive, bounded and vertically scrollable layout
  with IME-safe padding, so credentials and actions remain visible on short
  phones, tablets and TVs.
- ExoPlayer subtitle policy now treats `auto` as a server/stream decision,
  disables the entire text renderer for `off`, re-enables it for manual
  selection, and carries the active Jellyfin subtitle index into quality and
  transcode URL requests.
- The mobile video surface now toggles the controls even when the native
  PlayerView consumes the touch-up event; overlaid child buttons retain their
  own touch handling.
- The web client now scopes Jellyfin tokens and user IDs to `sessionStorage`,
  migrates legacy localStorage values once, and removes the durable token copy;
  server URL and non-sensitive UI preferences remain persistent.
- The web client authentication header now uses the current release version
  constant, keeping the Jellyfin client identity synchronized with package metadata.
- Subtitle stream URLs and Jellyfin music stream URLs are also tokenless; the
  Media3 music service applies Jellyfin authentication through request headers.
- Android Jellyfin access tokens and passwords now use an Android Keystore-backed
  encrypted store, with a transparent migration from legacy plaintext preferences;
  API 21–22 use an RSA-wrapped AES compatibility key.
- Offline-download availability is covered by a platform-surface contract and
  unit test: mobile/tablet and iOS mobile are eligible; TV, browser, tvOS,
  Tizen, webOS and VIDAA are not.
- The shared web/Smart TV adapter now exposes the same explicit capability
  contract with `supportsOfflineDownloads: false`; its Node test covers web,
  Tizen, webOS and VIDAA user agents.
- The shared playback mapper/decision engine is now invoked from the Android
  MediaSource loading path and records the selected path for diagnostics.
- Mobile cast cards now make the complete actor/crew card touch target open the
  person filmography, not only the circular portrait.
- The mobile Settings header now uses localized resources for its title and
  back accessibility label instead of fixed Spanish literals.
- The mobile bottom navigation no longer horizontally scrolls actions off-screen;
  all available destinations are distributed across the device width, including
  Search and Settings.
- Current verification on 2026-08-31: `node web/scripts/test-platform.mjs`,
  `:app:testMobileDebugUnitTest`, `:app:testTvDebugUnitTest`,
  `:app:compileMobileDebugKotlin` and `:app:compileTvDebugKotlin` pass locally.
- Release automation is present for the same tag release: the Android workflow
  publishes four APKs plus checksums, while the web workflow publishes the
  browser and Smart TV bundles. GitHub authentication is unavailable from this
  host, so `v1.2.44` assets remain unverified here; no publication is claimed
  beyond the pushed tag and configured workflow.
- Person navigation now keeps actor/crew cards actionable even when a lightweight
  Jellyfin item response omits the person ID: Velora resolves the name through
  Jellyfin before opening the filmography, while retaining direct-ID navigation.
- The mobile library header now leaves clear space below the Android status bar,
  exposes an accessible, full-size touch target for the sort/filter action, and
  uses localized movie/series titles.
- The mobile library sort/filter surface now uses resource-backed labels in
  Spanish, English, French and German; its query is applied consistently to
  both Recommendations and All tabs, and the tab controls expose a 48dp
  minimum touch target.
- The shared search screen now switches from the six-column TV grid to a
  two-column, width-aware mobile grid, preventing result clipping on phones
  while retaining the dense TV layout.
- When a server exposes multiple movie or series libraries, mobile navigation
  now presents a touch-friendly library chooser; a single library remains a
  one-tap destination.
- The playback mapper also falls back to Jellyfin's numeric `Channels` field
  when `ChannelLayout` is missing, with a regression test for multichannel audio.
- Android playback capabilities now probe installed MediaCodec decoders and
  display HDR types conservatively; unknown container/passthrough support is
  left unspecified.
- Aspect selection is applied to both the PlayerView and its real
  `AspectRatioFrameLayout`, including forced 4:3/16:9/cinema modes.
- The GL video surface now distinguishes stretch from fill: fill crops while
  preserving the source proportions, while stretch intentionally fills both
  axes. This keeps the selector effective when the enhanced GL path is used.
- Mobile player tap handling now ignores pointer sequences consumed by child
  controls, preventing the parent gesture layer from swallowing aspect,
  settings, audio, subtitle and playback button taps.
- Mobile detail navigation and action controls now use touch-native Material
  controls instead of TV focus controls; episode download actions are omitted
  entirely on platforms that do not support offline downloads.
- Filmography cards in the person/actor screen now keep D-pad activation on TV
  while exposing an explicit touch target on phones and tablets, so tapping an
  actor's related filmography works on mobile as well.
- The mobile player keeps fullscreen exit inside the player: Back exits
  fullscreen before leaving playback, and the visible landscape controls use
  the same orientation/fullscreen state instead of closing the activity.
- Movie and series mobile libraries now share a deterministic, tested content
  query for sorting by name/date/runtime/ratings and filtering by favorites,
  watched state and genre; the selected sort mode is persisted consistently.
- The mobile sort/filter panel is height-bounded and vertically scrollable, so
  long genre lists remain reachable on small phones and tablets.
- TV movie and series libraries now expose the same expanded sort choices
  (runtime, random, critic rating and community rating) and persist the
  selected mode through the shared settings preference.
- The home UI does not render a separate recently-added episode row; episodes
  remain reachable from series details and Continue Watching.
- Offline download metadata now uses an app-private SQLite index with a
  one-time migration from the former JSON preference store; completed media
  metadata survives process recreation without depending on SharedPreferences.
- Managed mobile downloads now preserve a partial file across transient network
  failures and resume with an HTTP Range request; exponential retry backoff is
  configured in WorkManager.
- Offline transfer filenames now use stable SHA-256 keys, with migration from
  the earlier hash-based temporary and media paths.
- Mobile series details now let the user select individual episodes before choosing
  download quality; selected episodes are queued through the managed offline
  engine, while the control remains absent on TV and browser platforms.
- The Apple shared foundation now includes Jellyfin username/password
  authentication, typed sessions, local playback preferences and tokenless
  authenticated request construction for artwork/media; Swift validation still
  requires macOS/Xcode and is not claimed on this Windows host.
- DownloadManager statuses are translated at the offline boundary into
  provider-neutral Velora states (`QUEUED`, `WAITING_FOR_NETWORK`,
  `DOWNLOADING`, `PAUSED`, `COMPLETED` and `FAILED`), preparing a future
  managed-transfer provider without coupling the UI to integer constants.
- The offline enqueue boundary now enforces the mobile/tablet capability guard
  itself, so TV builds cannot start a download even if a future caller bypasses
  the UI. The mobile episode download control also uses the mobile Material
  control rather than the TV-specific control.
- Completed DownloadManager entries now retain their complete local `content://`
  or `file://` URI, so offline playback does not depend on an invalid filesystem
  path conversion.
- Offline playback now enters the shared Media3/ExoPlayer screen directly and
  does not require a configured Jellyfin session; the explicit MPV setting and
  fallback remain available only for streamed playback.
- Offline completion semantics have regression coverage for provider-backed
  `content://` URIs, successful downloads without a filesystem path, and
  incomplete entries.
- A backend-neutral `OfflineIntegrityVerifier` now calculates and compares
  SHA-256 digests without taking ownership of the caller's stream; its two
  regression tests pass and it is ready for the managed transfer engine.
- Mobile and tablet download actions now present a quality chooser with
  Original, Alta (1080p), Media (720p) and Baja (480p); the selected profile
  is persisted in the offline index and lower profiles explicitly request a
  Jellyfin transcode rather than being mislabeled as the original file.
- A device-independent `OfflineStoragePolicy` now rejects invalid/overflowing
  sizes and enforces both a safety reserve and configurable minimum-free-space
  and managed-offline limits; it has deterministic unit coverage and is
  integrated with the download queue and mobile settings UI.
- Live TV now requests the current programme with the channel list and a
  bounded six-hour upcoming guide window, maps the next programme per channel,
  and renders the guide lazily in the existing virtualized `LazyColumn`; every
  channel row remains an actionable Jellyfin playback entry.
- Live TV channels now expose Jellyfin-backed favorites and tag-based groups;
  the Android screen provides touch/focus-safe filters and favorite toggles,
  with deterministic JVM coverage for the filtering rules.
- Live TV rows now expose a real programme-details action for the current
  programme, showing channel, schedule and Jellyfin synopsis in a dismissible
  dialog; the action is available through touch and TV focus navigation.
- Live TV playback now requests `AutoOpenLiveStream=true` before resolving the
  source, so Jellyfin can allocate tuner, M3U or Acestream streams and return
  the `MediaSourceId`/`LiveStreamId` required by the ExoPlayer and MPV paths.
- Login UI labels and authentication state are now resource-backed in the
  Spanish, English, French and German catalogs, including the server name
  placeholder and both mobile and TV login actions.
- Live TV labels are now covered by dedicated resources in all supported
  Android locale catalogs; English, French and German no longer fall back to
  Spanish for the screen title, loading/error states, programme details or
  channel actions.
- Web artwork URLs no longer contain the Jellyfin token; the browser requests
  artwork with `X-Emby-Token` and assigns a short-lived object URL, covered by
  the web regression test. Modern browsers and Smart TV web runtimes now use a
  same-origin service-worker media proxy that adds `X-Emby-Token` to streaming
  requests; a Jellyfin-compatible query-token fallback remains only for legacy
  runtimes without service-worker support. Logout clears the proxy credentials.
- Web item details now expose Jellyfin cast/guest-star buttons; selecting a
  person loads that person's movie and series filmography through the API and
  keeps the result keyboard-accessible.
- The web library now has persistent sorting and filtering for movies and
  series (name, added date, premiere, runtime, rating, favorites and playback
  state), with the required fields requested directly from Jellyfin.
- Resolved Android trailers now enter the canonical Media3/ExoPlayer player
  surface, preserving the same controls and fullscreen behavior as normal
  playback; the old direct MPV trailer handoff is removed.
- Release workflows use independent ref-scoped concurrency groups, so Android
  and web validation cannot cancel each other; their distinct release assets
  can be added to the same tag release and web checksums cannot overwrite the
  Android checksum manifest.
- Release `v1.2.4` is published at https://github.com/klortekhq/Velora/releases/tag/v1.2.4
  with mobile/TV debug and unsigned release APKs plus `SHA256SUMS.txt`; the
  four APKs were rebuilt and verified before upload.
- Release `v1.2.5` is published at https://github.com/klortekhq/Velora/releases/tag/v1.2.5
  with the same four APK variants and a verified `SHA256SUMS.txt`; it includes
  the TMDB diagnostic secret-redaction fix.
- Release `v1.2.6` is published at https://github.com/klortekhq/Velora/releases/tag/v1.2.6
  with four rebuilt APK variants and a remotely verified `SHA256SUMS.txt`; it
  includes the pre-enqueue offline storage safety gate.
- Release `v1.2.7` is published at https://github.com/klortekhq/Velora/releases/tag/v1.2.7
  with four rebuilt APK variants and a remotely verified `SHA256SUMS.txt`; it
  includes the mobile/tablet storage-limit selector and keeps that control out
  of TV settings.
- GitHub repository visibility is verified as public, with source, licensing,
  dashboard and release artifacts available at https://github.com/klortekhq/Velora.
- Release `v1.2.18` is publicly verified with four Android APKs, the web
  archives, Samsung and VIDAA bundles, the webOS package and both checksum
  manifests: https://github.com/klortekhq/Velora/releases/tag/v1.2.18.
- Release `v1.2.19` is publicly verified with four Android APKs, the web
  archives, Samsung and VIDAA bundles, the webOS package and both checksum
  manifests: https://github.com/klortekhq/Velora/releases/tag/v1.2.19.
- Release `v1.2.20` is publicly verified with four Android APKs, the web
  archives, Samsung and VIDAA bundles, the webOS package and both checksum
  manifests: https://github.com/klortekhq/Velora/releases/tag/v1.2.20.
- Release `v1.2.21` is publicly verified with its Android APKs, web packages,
  Smart TV bundles, combined archives and checksum manifests.
- Release `v1.2.22` is publicly verified with 11 assets, including mobile and
  TV debug/release APKs, web packages, Samsung/VIDAA/webOS bundles and
  checksum manifests: https://github.com/klortekhq/Velora/releases/tag/v1.2.22
- Release `v1.2.26` contains the adaptive login-layout, tactile seek, and subtitle-preservation fixes; its Android and
  web artifacts are pending the release workflow and hardware validation.

## Explicitly incomplete or requiring verification

- Android now has a tested locale catalog, translated core language/audio/subtitle settings, and resource-backed mobile navigation/search labels in Spanish, English, French and German; full source-wide internationalization is not yet verified because legacy hardcoded strings remain.
- The mobile offline-download screen now also uses localized resources for empty, progress, availability and delete states in those four languages.
- Jellyseerr/request and Discover legacy source remains only for migration
  compatibility and is now unreachable from the product surface; it should be
  removed in a later cleanup once migration coverage exists. TMDB remains only
  behind the trailer integration.
- The playback contract is introduced; platform-specific capability population
  and runtime validation remain incomplete.
- Runtime validation is still required for the revised subtitle behavior,
  quality switching and the login layout on physical phone/TV hardware.
- Runtime validation is also still required for touch control toggling over
  SurfaceView/PlayerView on real mobile hardware.
- Apple shared `VeloraKit` foundation is present with mobile-only offline
  capability semantics, header-based Jellyfin access and Original First
  decision tests; the SwiftUI iOS/iPadOS/tvOS clients are not yet present or
  hardware-verified.
- VIDAA support is not validated on a real device or certified runtime.
- Offline queue/storage recovery, persistence of integrity metadata, settings
  integration and the full end-to-end offline journey are not yet verified;
  the storage policy and digest contracts have unit coverage.
  the durable metadata foundation is now in place.
- Completed provider downloads are now copied into Velora's private
  `filesDir/offline/media` storage and verified before offline playback; the
  transfer provider remains only as a compatibility bridge for existing
  installations and background transfers.
- New mobile downloads are queued through WorkManager with network constraints,
  progress reporting, retry semantics and Keystore-backed credential lookup;
  legacy DownloadManager entries remain readable during migration.
- Theme music, a Jellyfin-first trailer resolver, persistent preview, and large-library
  virtualization need implementation and critic testing.
- Live TV channel zapping/previous-channel shortcut and mini-player still need
  implementation; programme details are now implemented but need hardware QA.
- No hardware test result is claimed: the connected Fire TV/phone were not
  available to this automated run.

## Platform evidence

Release `v1.2.32` is publicly verified with four Android APKs, browser
archives, Samsung/Tizen and VIDAA bundles, the webOS bundle and separate
SHA-256 manifests. The only remote branch is `main`.

- The updater now selects the exact mobile or TV APK by asset name instead of
  assuming the first release asset is installable.
- Android release CI now validates both mobile and TV unit tests and checks
  that a version tag matches Android and web metadata before packaging.

| Platform | Source/build state | Hardware/runtime validation |
| --- | --- | --- |
| Android mobile/tablet | `assembleMobileDebug` passes with AV1 native decoder; unit tests pass | Hardware validation pending |
| Android TV / Fire TV | Debug + release APK built; TV download guard compiled | Hardware validation pending |
| Web | `npm run build:all` passes; webOS IPK generated | Browser/device validation pending |
| Samsung Tizen | Packaging path exists | Device/emulator validation pending |
| LG webOS | Packaging path exists | Device/emulator validation pending |
| Hisense VIDAA | Preparation only | Not verified |
| iOS/iPadOS/tvOS | `VeloraKit` foundation package present; SwiftUI apps pending | macOS/Xcode validation pending |

## Quality gates

- `git diff --check`: passing for the current working tree.
- The repository-wide `gradlew test` passed on 2026-08-31 after making the
  vendored NewPipe test classpath explicit. Its upstream catalog-integration
  suite is intentionally not used as a Velora release gate because its
  third-party fixtures are nondeterministic; Velora's mobile and TV unit-test
  tasks remain enabled and passed.
- Offline SQLite schema v3 now stores an optional SHA-256 digest and the
  managed-download screen verifies local media before launching playback;
  existing v2/JSON entries migrate without a digest and establish it on first
  successful playback. Mobile unit tests and TV compilation passed on
  2026-08-31. Full background transfer recovery and WorkManager migration are
  still incomplete.
- Android `testMobileDebugUnitTest`, `assembleMobileDebug`,
  `assembleTvDebug`, `assembleMobileRelease`, and `assembleTvRelease`:
  passing on 2026-08-29. The full four-variant build completed online after
  the offline cache was found incomplete.
- `testMobileDebugUnitTest` passed on 2026-08-31 after the provider-neutral
  offline state mapping was added.
- After synchronizing the repository checkout, `compileMobileDebugKotlin`
  passed again on 2026-08-29; this confirms the tracked offline download
  manager source compiles, including its Android content-URI path.
- The tracked checkout also passes `testMobileDebugUnitTest` on 2026-08-29
  after that correction.
- The Media3 player was corrected so `Rellenar` uses proportional zoom/crop
  while `Estirar` remains the only deliberately distorting mode; mobile
  Kotlin compilation passed after the change.
- Android `testMobileDebugUnitTest` and `compileMobileDebugKotlin` passed on
  2026-08-29 after the security/capability changes; the build emitted only
  existing deprecation/KAPT warnings.
  Instrumentation and real-device playback validation were not run.
- The mobile Media3 timeline now uses a real horizontal touch-drag gesture,
  committing the selected position on release; TV/D-pad seeking remains
  available. Mobile and TV Kotlin compilation plus mobile unit tests passed
  on 2026-08-31 after this change.
- The Android playback screen now applies `PlaybackDecisionEngine` to the
  effective Jellyfin negotiation: capability-driven transcode is requested
  when allowed, while explicit codec settings and external-subtitle direct
  streaming remain respected. Mobile unit tests and TV compilation passed
  after this integration.
- Commit `8404e71` also passed `compileMobileDebugKotlin`,
  `testMobileDebugUnitTest`, and `assembleMobileDebug` with the native AV1
  decoder on 2026-08-29.
- Web `npm test` and `npm run build:all`: passing; Tizen CLI unavailable, webOS IPK generated,
  VIDAA hosted HTML5 bundle generated.
- GitHub Release `v1.2.4` assets and their published checksums were verified
  after upload.
- Android `testMobileDebugUnitTest` passed after the TMDB log-redaction change;
  the new security regression is included in the suite.
- GitHub Actions runs are currently rejected before the runner starts because
  GitHub reports failed recent account payments or an exceeded spending limit;
  this remains true after retrying the v1.2.5 tag runs even after making the
  repository public. Manual release upload remains verified until the account
  billing issue is resolved.
- Release `v1.2.3` was verified on GitHub with the four uniquely named APK
  assets and `SHA256SUMS.txt`; the release URL is
  `https://github.com/klortekhq/Velora/releases/tag/v1.2.3`.
- Web `npm test`: passing on 2026-08-29; platform capability regression test
  passes. Tizen packaging remains unvalidated without Tizen Studio/signing.
- `compileMobileDebugKotlin` and `testMobileDebugUnitTest`: passing after the
  ExoPlayer trailer routing change; only existing deprecation/KAPT warnings
  were emitted.
- `compileTvDebugKotlin`: passing on 2026-08-30 after adding the Live TV
  programme-details action; only existing deprecation/KAPT warnings were
  emitted.
- A fresh local Android verification was blocked before compilation because
  this machine has no Android SDK installed; no new Android hardware result is
  claimed. The Windows non-ASCII path guard is enabled in `gradle.properties`.
- Packaging and SHA-256: generated locally; see `outputs/` (ignored).
- Latest mobile debug APK from the 1.2.8 build is listed in the release assets
  below; no physical-device validation is claimed when ADB has no device.
- GitHub Android workflow now builds and publishes both mobile and TV debug /
  unsigned-release variants from a version tag.
- GitHub web workflow validates on `main`/pull requests and packages the common
  web client plus Samsung, webOS and VIDAA targets together; a version tag
  publishes browser archives, platform bundles/packages and checksums to the
  same release as the Android APKs.
- Manual web workflow runs default safely to `all` when no target is selected.
- Independent critic review: pending.

- Live TV programme metadata now includes series, episode and season context
  when Jellyfin provides it; programme time-range/progress formatting has
  deterministic unit coverage. Hardware playback validation remains pending.

The requested historical base `c3a2e52506942597444468be78ba3281996a6576` is
not present in this clone. The reproducible local patch is therefore against
the actual branch base `418383f`.

## Latest local artifacts

| Artifact | SHA-256 |
| --- | --- |
| `Velora-mobile-debug.apk` | `5E92FE771372D2EDFAF42BDCCAA38952EBC6F5F65B9C37095DC96376228FD690` |
| `Velora-tv-debug.apk` | `AE5FB29B68C4C2C6BEEE58AC37568116F5D69880D728A9002F953F87F9D5E41E` |
| `Velora-mobile-release-unsigned.apk` | `B5829774BCF5936E87FEC7D451F9B8BB63F2DE6F733F123BF1346A7177EE2C85` |
| `Velora-tv-release-unsigned.apk` | `C8449FEA0AE25422ECAE0ADC9CE7C0E81CB8B7CE8C39DAB2BE15FC9572C6F841` |

Release `v1.2.12`: https://github.com/klortekhq/Velora/releases/tag/v1.2.12

Release `v1.2.12` contains 10 remotely verified assets: mobile/TV debug and
release-unsigned APKs, web archive, combined web tarball, Samsung/VIDAA
bundles, webOS IPK and SHA-256 checksums. No device validation is implied by a
successful build.

Release `v1.2.12` SHA-256 values:

| Artifact | SHA-256 |
| --- | --- |
| `Velora-mobile-debug.apk` | `5852200687027DD23BB3EBF0289AE579013E15B9C07831311DA551365D2BEC08` |
| `Velora-mobile-release-unsigned.apk` | `4FC34C6000D2865B2B61A6AD0528BD955B1E5647583E13B8552EBEF9328FEA92` |
| `Velora-tv-debug.apk` | `F0941CA8E8174E30E5A0F9AB594DF2E410BB81169C3F1D877BB6125E8E69258E` |
| `Velora-tv-release-unsigned.apk` | `F4FFE7404D9A0E03155F8AB9B75E3103553AC6DB810E619F2BFD2BD44548540D` |
| `Velora-web-1.2.12.zip` | `966AF734DBFF88C26FE7E65DDA7697B91E2DE227F48591638F1C4416107A3906` |
| `Velora-Web-all.tar.gz` | `F67953BB1644EF8394803633E87BD560679552C7D882A6693C018A98329D7CA9` |
| `Velora-samsung-bundle-1.2.12.zip` | `05D77D199FC4E78A4AAA79B756C13220A63750D06CCDB66819DCA55CCC7AACA2` |
| `Velora-webos-1.2.12.ipk` | `D92BB765CAAB7D496353C929471EB4808B636F0E2FBB72B8EA25B07315F0750C` |
| `Velora-vidaa-bundle-1.2.12.zip` | `7D69BD414D00619FC8C6E099CC860405F82C40457FD31AF14707A7737FADF3EC` |

Release `v1.2.13`: https://github.com/klortekhq/Velora/releases/tag/v1.2.13

This release contains the playback touch-event fix and 10 remotely verified
assets. Its checksum manifest is published as `SHA256SUMS-1.2.13.txt`.

Release `v1.2.14` adds the browser service-worker media proxy, which keeps
the Jellyfin token in an authenticated request header for modern browsers and
Smart TV web runtimes. Android mobile/TV tests and all four APK assemblies
passed locally. Tizen Studio/CLI is not installed on this build host, VIDAA
remains a hosted HTML5 bundle, and no physical device was connected for this
release, so those validations remain open.

Release `v1.2.14`: https://github.com/klortekhq/Velora/releases/tag/v1.2.14

The public release contains 11 verified assets, including all four APKs,
web/webOS/Samsung/VIDAA packages, combined web archives and
`SHA256SUMS-1.2.14.txt`.

Release `v1.2.15`: https://github.com/klortekhq/Velora/releases/tag/v1.2.15

The public release contains 11 verified build artifacts, including all four
APK variants, web/webOS/Samsung/VIDAA packages, combined web archives and
`SHA256SUMS-1.2.15.txt`. The Samsung artifact remains an unsigned bundle when
Tizen Studio/signing is unavailable; VIDAA remains an HTML5 submission bundle.

Version 1.2.16 is prepared in the tracked source and has passed local mobile
unit tests, mobile release assembly, TV release assembly and web platform
tests/build. Its GitHub release is created only after the tag workflows finish.
La revisión `v1.2.84` conserva el modo de formato de imagen del
reproductor al recrear la actividad durante el cambio de orientación. La
compilación y los unit tests Android han pasado tras este cambio; sus
artefactos están pendientes de verificación en GitHub Actions.

La revisión local `1.2.86` endurece la autenticación de OpenSubtitles:
codifica las credenciales mediante el serializador JSON y evita registrar
respuestas o mensajes de error que puedan contener tokens o datos sensibles.
La compilación y los unit tests de móvil pasan; no se ha creado una release
por tratarse de una corrección de seguridad acotada.

El motor canónico de reproducción distingue ahora entre HDR desconocido y HDR
detectado explícitamente: en Android, un display SDR conocido ya no se trata
como compatible con HDR. Los presets de calidad también comprueban anchura y
altura, mientras `Original` sigue sin imponer límites artificiales. Las
variantes Android móvil y TV compilan y sus unit tests pasan conjuntamente.

El índice SQLite offline sube a la migración 5 y conserva las marcas de
creación, finalización y última reproducción de cada descarga. El worker y la
actualización de WorkManager las rellenan sin perder las instalaciones
existentes, y la capa de gestión expone `markPlayed` para futuras reglas de
limpieza o Smart Downloads. La compilación móvil y sus tests pasan tras la
migración.

El límite de almacenamiento offline mide ahora todos los archivos del volumen
privado `offline/media`, incluidos `.part` de transferencias interrumpidas, y
usa el tamaño registrado como suelo conservador para filas heredadas. Así una
descarga parcial o un reinicio no puede hacer que Velora subestime el espacio
ocupado.

La ordenación de películas y series ya permite cambiar explícitamente entre
orden ascendente y descendente desde el mismo diálogo de ordenar y filtrar,
tanto con pantalla táctil como con mando. La preferencia se conserva por
dispositivo y se aplica a todos los criterios de ordenación. La prueba de
consulta de biblioteca y la compilación Kotlin de TV pasan; no se publica una
nueva APK por este cambio aislado.

Live TV incorpora navegación de canales durante la reproducción: arriba/abajo
en el mando y botones anterior/siguiente en el OSD táctil respetan el orden de
Jellyfin y hacen ciclo al alcanzar un extremo. La selección se mantiene en
la lista filtrada y la reproducción normal no muestra esos controles. La
lógica tiene pruebas unitarias y las compilaciones móvil y TV pasan; no se
publica una release hasta acumular un bloque funcional mayor.

La ficha de reproducción ahora hace interactiva la fila de reparto: tocar o
enfocar una persona abre su ficha de Velora y permite consultar sus películas y
series disponibles. Si Jellyfin no entrega el identificador de la persona, la
ficha conserva la resolución por nombre. La compilación móvil/TV y las pruebas
unitarias relevantes pasan; este arreglo aislado no genera una nueva release.

El botón personalizado de ajustes del reproductor ya usa una tuerca propia de
Velora en las dos rutas del OSD Media3, en lugar de un icono genérico del
sistema. Su descripción accesible está localizada en español, inglés, francés
y alemán, y el título del panel reutiliza esa traducción. La compilación móvil
y TV y las pruebas de aspecto, navegación Live TV y reproducción pasan; no se
publica una release por este cambio aislado.

La base Apple endurece la construcción de URLs de imágenes y vídeo: los
identificadores vacíos o con separadores de ruta se rechazan y los caracteres
especiales se codifican como parte del componente. Se añadió cobertura Swift
para IDs con espacios, separadores y traversal. En este host Windows no hay
toolchain Swift; la validación queda delegada al workflow macOS de Apple y no
 se afirma validación local de iOS, iPadOS ni tvOS.

El reproductor web ahora permite entrar y salir de pantalla completa desde el
propio botón, incluyendo la API estándar, la variante WebKit y el modo de
reserva usado por navegadores/Smart TV sin fullscreen nativo. El botón cambia
su etiqueta y limpia sus listeners al cerrar el reproductor. Las pruebas de
plataforma, el empaquetado web y el paquete webOS pasan; Tizen Studio sigue sin
estar disponible en este host, por lo que no se afirma validación de un
paquete Tizen real. Es un cambio puntual y no genera una nueva release APK.

La migración de descargas Android ahora materializa automáticamente las
entradas completas heredadas de DownloadManager al refrescar la pantalla de
descargas, las copia al almacenamiento privado de Velora, calcula su SHA-256
y elimina la identidad del proveedor. Las descargas nuevas ya usan
WorkManager y las antiguas siguen siendo legibles durante la migración. Pasan
las pruebas offline, la compilación Kotlin móvil y la compilación Kotlin TV;
la recuperación E2E en hardware real continúa pendiente.

El worker de descargas distingue ahora errores permanentes de cliente de
errores transitorios: 408, 429 y respuestas 5xx se reintentan; errores como
401 y 404 terminan correctamente. Si WorkManager detiene una ejecución por
cambio temporal de ciclo de vida o restricciones, se conserva el archivo
parcial para reanudarlo. Las pruebas offline y las compilaciones móvil/TV
pasan; la prueba E2E en dispositivos reales sigue pendiente.
