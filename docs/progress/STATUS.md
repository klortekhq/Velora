# Estado verificable de Velora

Updated: 2026-09-06

Revisión funcional actual: commit `a58101f`.

Este documento registra únicamente comprobaciones reproducibles. La ausencia de
una comprobación no se interpreta como soporte certificado.

## Estado del repositorio

- Rama de trabajo y rama remota: `main`.
- No hay ramas de producto adicionales en GitHub.
- Último cambio funcional documentado: normalización del parseo de idiomas de
  audio en las fichas de películas y series Android.
- Identidad pública: Velora por Klørtek. Los identificadores de paquete se
  conservan únicamente donde los exige el sistema de distribución de cada
  plataforma y no forman parte de la identidad visible del producto.

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
- Android migra credenciales a Keystore; Apple usa Keychain; el cliente web
  usa su proxy autenticado sin poner tokens en la URL del vídeo.
- Hay catálogos localizados Android, Apple y web con selección manual y locale
  del sistema como valor inicial.

## Pruebas locales pasadas

- `:app:testMobileDebugUnitTest`: `BUILD SUCCESSFUL`.
- `:app:testTvDebugUnitTest`: `BUILD SUCCESSFUL`.
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
  TV y etiquetas accesibles de tarjetas.
- `node scripts/build-web.mjs all` desde `web/`: genera bundle web, bundle
  Samsung, IPK webOS y bundle HTML5 VIDAA. Tizen queda pendiente de validar
  hasta disponer de Tizen Studio/CLI y un perfil de firma.
- `:app:assembleMobileRelease` y `:app:assembleTvRelease`: `BUILD SUCCESSFUL`.
- `git diff --check`: sin errores en los cambios publicados.
- `node scripts/check-version-consistency.mjs`: versiones coherentes en `1.4.0`.
- `node scripts/check-jellyfin-auth-contract.mjs`: contrato `Pw` coherente en
  Android, Apple, web y smoke test.
- `node scripts/check-android-locales.mjs`: 434 claves en los catálogos Android.
- `node scripts/check-apple-locales.mjs`: 48 claves coherentes en `en`, `es`,
  `fr` y `de`.
- `SensitiveDataRedactorTest`: cubre también credenciales incrustadas en el
  usuario de una URL y parámetros sensibles en query/fragmento.
- El workflow de Android fue revisado sintácticamente: los APK debug se guardan
  como artefactos de QA y no se incluyen en las Releases públicas.

## Artefactos Android locales

Son builds `unsigned` para QA, no releases de distribución:

- Móvil: `app/build/outputs/apk/mobile/release/velora-release-unsigned.apk`
  SHA-256 `1F1F2DCFD9EE1FE95EC0A6136213469319437EF700A929D8A3A6C88877A99189`.
- TV: `app/build/outputs/apk/tv/release/velora-release-unsigned.apk`
  SHA-256 `4AA24F7D93E3CBA16274C455B880F1B294673F0C2DD512DDCBAD561980A2449A`.

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
- El smoke test autenticado respondió HTTP 401 para la cuenta configurada;
  no se certifican en esta sesión biblioteca, reproducción ni Live TV real.
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
- Completar pruebas de rendimiento con bibliotecas sintéticas de 1.000,
  10.000 y 50.000 elementos.

## Regla de publicación

Solo se publica una release cuando el cambio sea un bloque funcional o una
corrección crítica/seguridad, el changelog esté actualizado, los artefactos
estén firmados y las sumas SHA-256 coincidan. Las builds unsigned permanecen
en QA y no se presentan como releases públicas.
