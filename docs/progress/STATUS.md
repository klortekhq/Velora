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
- Último cambio funcional documentado: la ficha móvil etiqueta el bloque de
  códecs, resolución, fps, HDR/SDR, audio y contenedor como información
  técnica, sin presentar nombres ni rutas de archivo.
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
- Live TV usa exclusivamente fuentes devueltas por Jellyfin. No hay ingestión
  M3U arbitraria en Velora.
- Live TV solicita `MediaSources`, agrupa filas con el mismo ID de Jellyfin y
  permite seleccionar las variantes bajo un único canal. El cambio de canal
  no reutiliza la fuente del canal anterior.
- La comprobación de disponibilidad de Live TV en el inicio libera su cliente
  HTTP al cambiar de sesión o salir de la pantalla.
- Películas y series tienen búsqueda, ordenación, filtros, favoritos y estado
  de reproducción persistente. El cliente web carga la biblioteca por páginas
  y permite ampliar los resultados sin bloquear el inicio.
- El reparto abre la filmografía disponible en Jellyfin.
- En Apple, las fichas también solicitan el reparto y permiten abrir su
  filmografía en una vista nativa; la compilación Swift queda pendiente de la
  ejecución CI en macOS.
- Las descargas gestionadas, su base SQLite, reanudación, integridad y Smart
  Downloads están limitadas a móvil/tablet. No aparecen en TV, Smart TV ni web.
- Apple móvil/tablet conserva la calidad elegida en cada descarga offline y
  migra catálogos anteriores sin ese campo a Original; la suite Swift sigue
  pendiente de ejecución en macOS porque este host no tiene Xcode.
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
- Hay catálogos localizados Android, Apple y web con selección manual y locale
  del sistema como valor inicial.

## Pruebas locales pasadas

- `:app:testMobileDebugUnitTest`: `BUILD SUCCESSFUL`.
- `:app:testTvDebugUnitTest`: `BUILD SUCCESSFUL`.
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

## Artefactos Android locales

Son builds `unsigned` para QA, no releases de distribución:

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
- ADB no está instalado/disponible en el host actual; no se declara instalación
  ni prueba física reciente en Fire TV o móvil.

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
