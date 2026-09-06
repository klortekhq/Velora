# Estado verificable de Velora

Updated: 2026-09-06

Revisión funcional actual: el estado de `main` que contiene este dashboard.

Este documento registra únicamente comprobaciones reproducibles. La ausencia de
una comprobación no se interpreta como soporte certificado.

Oleada actual: 25–26 (empaquetado multiplataforma y QA cruzada). Las oleadas
de certificación de Apple, Tizen, VIDAA y hardware real siguen abiertas.

## Estado del repositorio

- Rama de trabajo: `main`; la política del repositorio exige que sea la única
  rama pública.
- La consulta independiente a `origin` del 2026-09-06 confirma únicamente
  `refs/heads/main` en GitHub, apuntando al commit que contiene este dashboard.
  No se presenta ninguna otra rama pública.
- Último cambio funcional documentado: la navegación de Live TV comparte la
  fila centrada de bibliotecas y su selector descarta duplicados de proveedor
  sin perder fuentes realmente distintas.
- Identidad pública: Velora por Klørtek. Los identificadores de paquete se
  conservan únicamente donde los exige el sistema de distribución de cada
  plataforma y no forman parte de la identidad visible del producto.
- CI comprueba que README, atribuciones, documentación y código público no
  reintroduzcan referencias heredadas a otros clientes o identidades antiguas.

## Funcionalidad implementada

- Android móvil/tablet y Android TV/Fire TV comparten dominio Jellyfin, pero
  mantienen layouts táctil y de mando separados.
- Media3/ExoPlayer es el backend Android predeterminado; MPV solo se usa si el
  usuario lo selecciona o si el fallback configurado resulta necesario.
- La reproducción sigue la estrategia Original First: Direct Play, Direct
  Stream/Remux y transcodificación solo cuando las capacidades lo requieren.
- El selector de aspecto de Media3/ExoPlayer y de la superficie GL reaplica el
  modo seleccionado después de cambios de tamaño, orientación y fullscreen;
  las reaplicaciones retrasadas obsoletas ya no pueden sobrescribir una
  selección nueva.
- Live TV usa exclusivamente fuentes devueltas por Jellyfin. No hay ingestión
  M3U arbitraria en Velora.
- Live TV solicita `MediaSources`, agrupa filas con el mismo ID de Jellyfin y
  permite seleccionar las variantes bajo un único canal. El cambio de canal
  no reutiliza la fuente del canal anterior.
- Web/Smart TV abre ahora ese selector al pulsar o confirmar con teclado una
  fila agrupada; los canales sin variantes siguen entrando directamente.
- La comprobación de disponibilidad de Live TV en el inicio libera su cliente
  HTTP al cambiar de sesión o salir de la pantalla.
- Películas y series tienen búsqueda, ordenación, filtros, favoritos y estado
  de reproducción persistente. El cliente web carga la biblioteca por páginas
  y permite ampliar los resultados sin bloquear el inicio.
- El reparto abre la filmografía disponible en Jellyfin.
- En Apple, las fichas también solicitan el reparto y permiten abrir su
  filmografía en una vista nativa; las tarjetas de esa filmografía abren ahora
  la ficha completa del título seleccionado. La compilación Swift queda
  pendiente de la ejecución CI en macOS.
- Las descargas gestionadas, su base SQLite, reanudación, integridad y Smart
  Downloads están limitadas a móvil/tablet. No aparecen en TV, Smart TV ni web.
- Apple móvil/tablet conserva la calidad elegida en cada descarga offline y
  migra catálogos anteriores sin ese campo a Original; la suite Swift sigue
  pendiente de ejecución en macOS porque este host no tiene Xcode.
- La migración Android desde el índice JSON antiguo conserva también checksum,
  fuente, trabajo, progreso, fechas, estado de visto y protección de descarga
  antes de pasar a SQLite.
- Auditoría multiplataforma de offline: Android TV/Fire TV bloquea tanto la
  navegación como la apertura directa de descargas; Apple TV no renderiza las
  acciones ni inicializa la transferencia; web, Tizen, webOS y VIDAA mantienen
  `supportsOfflineDownloads: false`. La regla queda cubierta por tests de
  capacidades Android/Apple/web.
- Las descargas de subtítulos externos siguen la misma regla: sus resultados,
  almacenamiento y ajustes de OpenSubtitles solo se muestran en móvil/tablet;
  las superficies de TV conservan únicamente las pistas que ya entrega
  Jellyfin.
- Android migra credenciales a Keystore; Apple usa Keychain; el cliente web
  usa su proxy autenticado sin poner tokens en la URL del vídeo.
- El proxy multimedia web aplica una segunda limpieza de parámetros sensibles
  (`api_key`, `access_token`, `token` y equivalentes) antes de reenviar la
  reproducción al servidor.
- El fallback MPV Android comparte el constructor de cabeceras MediaBrowser
  con la ruta de reproducción y sus pruebas de seguridad siguen pasando.
- El verificador de empaquetado Smart TV no permite declarar un WGT/IPK
  instalable si el artefacto no existe; tampoco confunde el bundle HTML5 de
  VIDAA con un paquete firmado.
- El workflow de releases web ejecuta esa verificación después del build y
  antes de publicar los assets.
- Los workflows de release vuelven a validar identidad pública y contrato de
  autenticación Jellyfin antes de producir o publicar cualquier artefacto.
- Apple: `PrivacyInfo.xcprivacy` está incluido en los recursos compartidos y
  el CI comprueba sus claves y la declaración de no-tracking; la firma y la
  validación final del bundle siguen requiriendo Xcode/macOS.
- Android: el botón de reproducción opcional no construye ya una referencia
  `RawRes(0)`; el modo sin Lottie queda cubierto por la ruta normal de foco,
  toque y mando.
- Android TV: Live TV se integra en la misma fila centrada de navegación que
  las bibliotecas de películas y series; la pestaña sigue siendo condicional a
  los canales visibles para el usuario.
- Android móvil/tablet: la navegación inferior mantiene destinos condicionales
  y ahora expone foco, rol de botón y estado visual para toque, teclado, mando
  y tecnologías de asistencia.
- Live TV: la normalización Android y web descarta duplicados idénticos sin
  `MediaSource`, pero conserva cada fuente identificada como una opción
  seleccionable.
- Web/Smart TV: la agrupación usa mapas sin prototipo y está cubierta frente a
  identificadores de proveedor con nombres especiales.
- Hay catálogos localizados Android, Apple y web con selección manual y locale
  del sistema como valor inicial.

## Pruebas locales pasadas

- `:app:testMobileDebugUnitTest`: `BUILD SUCCESSFUL`.
- `:app:testTvDebugUnitTest`: `BUILD SUCCESSFUL`.
- Las suites móvil y TV vuelven a terminar correctamente tras hacer enfocables
  y accesibles los destinos de navegación inferior móvil/tablet.
- La suite móvil pasa 85 pruebas tras cubrir la migración completa del índice
  offline; la suite TV también vuelve a terminar correctamente.
- `:app:compileMobileDebugKotlin`, `:app:compileTvDebugKotlin`,
  `:app:testMobileDebugUnitTest` y `:app:testTvDebugUnitTest`: `BUILD
  SUCCESSFUL` tras corregir la carga opcional de Lottie.
- Ejecución fresca sobre el estado actual: `:app:testMobileDebugUnitTest`
  y `:app:testTvDebugUnitTest` terminaron en `BUILD SUCCESSFUL` en 3m49s.
- Las suites móvil y TV vuelven a terminar correctamente tras ocultar el
  almacenamiento y la gestión de subtítulos externos en TV.
- La compilación y los tests móvil/TV vuelven a terminar correctamente tras
  el ajuste de los textos de información técnica.
- Los controles Android del reproductor se recompilaron con semántica de botón
  para mando, toque y tecnologías de asistencia; las suites móvil y TV vuelven
  a terminar correctamente.
- `:app:testMobileDebugUnitTest`: `BUILD SUCCESSFUL` tras el ajuste del cierre
  del cliente HTTP de autenticación.
- La validación de login cubre también el ciclo de vida de Quick Connect; el
  test móvil volvió a terminar correctamente después del cambio.
- `:app:compileMobileDebugKotlin`: `BUILD SUCCESSFUL` tras el ajuste del ciclo
  de vida del cliente Live TV.
- `:app:compileTvDebugKotlin`: `BUILD SUCCESSFUL` tras el mismo ajuste.
- `:app:testTvDebugUnitTest`: `BUILD SUCCESSFUL`.
- `node scripts/test-platform.mjs` desde `web/`: correcto para capacidades,
  seguridad, autenticación, biblioteca paginada, subtítulos, agrupación Live
  TV, etiquetas accesibles de tarjetas y pestañas con selección anunciada.
- `node web/scripts/build-web.mjs all`: genera correctamente el bundle web,
  el bundle Samsung, el IPK webOS y el bundle HTML5 VIDAA; Tizen sigue
  pendiente de Tizen Studio/CLI y firma.
- Los artefactos Samsung y webOS incluyen `velora-build.json` con su estado
  real (`installablePackage`, tipo de empaquetado y requisito de SDK/firma).
- `node scripts/check-tv-packaging-output.mjs`: correcto; valida Samsung,
  webOS y VIDAA tras generar sus bundles y paquetes.
- `:app:assembleMobileRelease` y `:app:assembleTvRelease`: `BUILD SUCCESSFUL`.
- `git diff --check`: sin errores en los cambios publicados.
- `node scripts/check-version-consistency.mjs`: versiones coherentes en `1.4.0`.
- `node scripts/check-jellyfin-auth-contract.mjs`: contrato `Pw` coherente en
  Android, Apple, web y smoke test.
- `node scripts/check-android-locales.mjs`: 434 claves en los catálogos Android.
- `node scripts/check-apple-locales.mjs`: 48 claves coherentes en `en`, `es`,
  `fr` y `de`.
- `node scripts/check-web-locales.mjs`: 69 claves efectivas coherentes en los
  12 idiomas de la web, incluyendo los fallbacks y overrides traducidos.
- `:app:testMobileDebugUnitTest`, `:app:testTvDebugUnitTest`,
  `:app:compileMobileDebugKotlin` y `:app:compileTvDebugKotlin`: correctos tras
  blindar la ruta de subtítulos offline para superficies TV.
- Ejecución fresca del estado actual `b35f482` el 2026-09-06:
  `:app:testMobileDebugUnitTest :app:testTvDebugUnitTest` terminó en `BUILD
  SUCCESSFUL` en 6m35s.
- Las pruebas de `UpdateService` cubren la selección de APK firmado por formato
  y la exclusión de un artefacto unsigned como actualización instalable.
- `node scripts/check-release-workflows.mjs`: correcto; verifica además el
  contrato entre los nombres de APK firmados publicados y el actualizador.
- `UpdateServiceTest`: correcto; cubre URLs de assets oficiales y rechaza
  hosts, rutas, esquemas o credenciales incrustadas no confiables.
- `node web/scripts/test-platform.mjs`: correcto; valida también CSP,
  `frame-ancestors`, `object-src` y la política `no-referrer` del cliente web.
- Apple: la cabecera de autenticación usa ahora el idioma seleccionado por el
  usuario; la compilación Swift queda pendiente de la ejecución macOS de CI.
- Apple: se añadió una prueba de regresión para impedir que el token se aplique
  a rutas fuera del prefijo del servidor configurado; requiere la ejecución
  Swift en macOS para quedar verificada.
- `SensitiveDataRedactorTest`: cubre también credenciales incrustadas en el
  usuario de una URL y parámetros sensibles en query/fragmento.
- El workflow de Android fue revisado sintácticamente: los APK debug se guardan
  como artefactos de QA y no se incluyen en las Releases públicas.
- Los workflows de publicación solo se activan con tags de versión o ejecución
  manual; los pull requests y los pushes normales quedan en el workflow de CI.
- `node scripts/check-release-workflows.mjs`: correcto; protege esa política y
  evita publicar APKs por comodín.
- La validación de releases cubre también la limpieza correcta de artefactos
  obsoletos cuando se reconstruye una etiqueta mediante ejecución manual.
- `node scripts/check-public-identity.mjs`: correcto; no hay referencias
  heredadas de identidad en el contenido público revisado.
- `node web/scripts/test-platform.mjs` y `node web/scripts/build-web.mjs all`:
  correctos; el bundle web y el IPK de webOS se regeneraron. Tizen sigue
  pendiente por falta de Tizen Studio/CLI y firma.

## Artefactos Android locales

Son builds `unsigned` para QA, no releases de distribución:

Builds debug del estado actual `7346a09`:

- Móvil: `app/build/outputs/apk/mobile/debug/app-mobile-debug.apk`, 76,2 MB,
  SHA-256 `CD46C39A53CE54E71CFED23A7A82ACD29C49B8F698F561F10FD65A051E411F68`.
- TV: `app/build/outputs/apk/tv/debug/app-tv-debug.apk`, 76,2 MB,
  SHA-256 `5E18C2559B44D06F7FD4D5C4068FBFA557923EB44BB200CD9CAE3E60A29D20C0`.

Ambas variantes se generaron correctamente en QA el 2026-09-06; no se han
instalado en hardware porque ningún dispositivo ADB respondió.

- Móvil: `app/build/outputs/apk/mobile/release/velora-release-unsigned.apk`
  SHA-256 `9B59B4D0CF60000F1864D012F307591E8DFC9D544146FD476265C8232B2467BE`.
- TV: `app/build/outputs/apk/tv/release/velora-release-unsigned.apk`
  SHA-256 `195931A715A72046600924A20A14B462EAAAB3A191E593DD4CB40F41E440B89C`.

Las releases etiquetadas `vX.Y.0` exigen secretos de firma Android. Sin ellos,
el workflow falla deliberadamente en vez de publicar un APK no instalable como
release pública. El changelog de release está en castellano y agrupa cambios
relevantes.

## Plataformas

| Plataforma | Estado verificable |
| --- | --- |
| Android móvil/tablet | Compila y pasa tests unitarios locales |
| Android TV / Fire TV | Compila y pasa tests unitarios; hardware no validado en este host |
| Web | Bundle y tests locales correctos |
| Samsung Tizen | Bundle preparado; falta Tizen Studio, firma y dispositivo/emulador |
| LG webOS | IPK generado con `ares-package`; falta dispositivo/emulador |
| Hisense VIDAA | Bundle HTML5 y metadatos preparados; falta portal/certificación |
| iPhone/iPad/tvOS | Base Swift Package y CI macOS configuradas; no hay Xcode en este host |

La validación Apple se ejecuta en `macos-14` mediante GitHub Actions. No se
declara certificación de tienda ni de hardware sin esa ejecución o dispositivo.

## Jellyfin y hardware conectado

- `http://192.168.100.201:8096/health` respondió HTTP 200 en la última prueba.
- El smoke test autenticado confirmó que el servidor responde, pero rechazó
  las credenciales configuradas con HTTP 401; no se certifican en esta sesión
  biblioteca, reproducción ni Live TV real. No se han registrado credenciales,
  contraseñas ni tokens en este documento.
- La última prueba se realizó el 2026-09-06 con el servidor operativo; el
  health check fue correcto y la autenticación devolvió HTTP 401.
- ADB está disponible en el host, pero no hay ningún dispositivo conectado ni
  servicio mDNS del Fire TV visible; no se declara instalación ni prueba
  física reciente en Fire TV o móvil.

## Pendiente verificable

- Obtener una ejecución autenticada completa contra Jellyfin para probar
  biblioteca, Live TV, reproducción ExoPlayer, audio, subtítulos y zapping.
- Ejecutar la suite Swift en macOS y validar iOS/iPadOS/tvOS en simulador o
  hardware.
- Instalar Tizen Studio y validar `.wgt` firmado en runtime real.
- Validar webOS, VIDAA y Fire TV en sus dispositivos reales.
- Configurar firma Android y credenciales de publicación antes de crear una
  release pública.
- Reconciliar el tag/release `v1.4.0` con una build firmada del estado que se
  quiera distribuir; no se mueve ni sobrescribe el tag existente
  automáticamente.
- Completar pruebas de rendimiento con bibliotecas sintéticas de 1.000,
  10.000 y 50.000 elementos.

## Regla de publicación

Solo se publica una release cuando el cambio sea un bloque funcional o una
corrección crítica/seguridad, el changelog esté actualizado, los artefactos
estén firmados y las sumas SHA-256 coincidan. Las builds unsigned permanecen
en QA y no se presentan como releases públicas.
