# Estado verificable de Velora

Updated: 2026-09-05

## Revisión actual (commit `cd0da5d`)

- Repositorio sincronizado en una única rama: `main` y `origin/main`.
- Fire TV conectado por ADB (`192.168.31.112:5555`) con `1.4.0-tv` activo;
  el arranque y los logs recientes no muestran `FATAL EXCEPTION` ni errores de
  ExoPlayer atribuibles a Velora.
- La prueba web de seguridad, capacidades y agrupación de Live TV pasa. Las
  pruebas separan ahora `localStorage` de `sessionStorage` y comprueban que el
  token no vuelva a persistirse de forma duradera.
- `build-web.mjs all` pasa y genera el bundle web, el IPK webOS y los bundles
  preparados de Samsung/VIDAA. Tizen sigue pendiente de Tizen Studio/CLI y un
  perfil de firma real.
- El servidor Jellyfin responde en `192.168.100.201:8096`, pero la
  autenticación disponible en esta sesión devuelve `HTTP 401`; no se declara
  validada la biblioteca, la reproducción autenticada ni Live TV real.
- No se declara una release pública ni un APK firmado a partir de esta
  revisión: la consulta pública no confirmó una release existente y este
  entorno no dispone de credenciales de firma/GitHub CLI para publicarla.
- El gate `scripts/check-version-consistency.mjs` pasa con `1.4.0` y se ejecuta
  en CI, Android Release y Web Release; la etiqueta `v1.4.0` coincide con el
  commit de `main` que contiene ese gate.
- Apple Live TV ya presenta las fuentes agrupadas con etiquetas localizadas
  (`Fuente principal`, `Fuente IPTV` u `Opción N`) en lugar de IDs internos.
  `scripts/check-apple-locales.mjs` valida las 45 claves de los catálogos
  inglés, español, francés y alemán, y se ejecuta en ambos workflows Apple.
- Las acciones visibles de detalle y solicitudes Android (reanudar, reproducir,
  audio, subtítulos, tráiler, visto y atrás) ya usan recursos traducibles en
  las pantallas móvil/TV; el comprobador Android valida 431 claves en los
  catálogos garantizados.

## Última verificación

La verificación del 2026-09-05 sobre `3798886` pasó
`testMobileDebugUnitTest`, `testTvDebugUnitTest`, `compileMobileDebugKotlin` y
`compileTvDebugKotlin` con `BUILD SUCCESSFUL`. Persisten únicamente avisos de
deprecaciones del SDK/Kotlin y del SDK XML local; no hay errores de compilación.

El commit documental `cd0da5d` conserva esa misma evidencia y actualiza este
panel para que su revisión actual coincida con `main`.

La validación del 2026-09-05 sobre el commit actual compiló `assembleTvDebug`
con éxito, instaló la APK resultante en el Fire TV AFTSS y la lanzó mediante
ADB. El dispositivo informa `1.4.0-tv`/`versionCode 10400`; la actividad llegó
a `Displayed ... MainActivity` sin `FATAL EXCEPTION` ni `AndroidRuntime`. Las
pruebas `testMobileDebugUnitTest` y `testTvDebugUnitTest` del mismo checkout
también terminaron con `BUILD SUCCESSFUL`. La única advertencia relevante del
log es del sistema Fire OS (`CoreComponentFactory`), no un cierre de Velora.

La compilación local del 2026-09-05 también completó `assembleMobileRelease` y
`assembleTvRelease` con lint vital incluido. Generó los dos APK release
`unsigned` de aproximadamente 67,9 MB; no se publican como release oficial
porque este entorno no tiene la firma de distribución configurada.

La misma build incorpora una corrección del selector de aspecto: las opciones
se exponen como controles seleccionables accesibles para táctil y mando, y el
modo elegido se reaplica después de los ciclos de medida de Media3, al recibir
el tamaño real del vídeo y al cambiar a pantalla completa. La compilación y
las pruebas Android pasan; la reproducción autenticada y el cambio visual en
contenido real quedan pendientes de una cuenta Jellyfin que acepte el acceso.

La base Apple incorpora ahora una recarga explícita de biblioteca y Live TV al
restaurar una sesión guardada. Antes el shell podía marcar la sesión como
válida y mostrar una biblioteca vacía hasta volver a iniciar sesión; la nueva
ruta usa el mismo modelo de contenido para login nuevo y reapertura. Swift/Xcode
no están disponibles en este host Windows, por lo que la prueba final queda
delegada al job macOS de GitHub Actions.

La verificación local posterior al changelog volvió a terminar con
`BUILD SUCCESSFUL` en `testMobileDebugUnitTest` y `testTvDebugUnitTest`; las
pruebas de capacidades, seguridad e interacción del cliente web también pasan.

La auditoría de los entrypoints Apple detectó que `VeloraMobile` y `VeloraTV`
referenciaban `Bundle.module` desde sus propios ejecutables, aunque los
recursos localizados pertenecen a `VeloraKit`. Se corrigió usando una copia
localizada pública del módulo compartido y se añadió una regresión unitaria.
Swift no está instalado en este host Windows; la compilación y el test quedan
para el job macOS, sin presentarlos como ejecutados aquí.

La descarga móvil de Apple ahora permite seleccionar Original, Alta, Media o
Baja y envía a Jellyfin los límites de tamaño y bitrate correspondientes. La
interfaz sigue ocultando por completo las descargas en tvOS. La transferencia
usa una sesión URLSession de fondo con identificador estable, metadatos sin
credenciales y staging seguro del archivo antes de incorporarlo al catálogo;
al reabrir la aplicación también recupera la tarea activa para evitar colas
duplicadas;
su validación final requiere macOS/iOS porque Swift/Xcode no están instalados
en este host.

La comprobación de ramas del mismo momento devuelve únicamente `main` y
`origin/main`; no hay ramas adicionales en el checkout ni en el remoto
visible.

El workflow continuo se amplió el 2026-09-05 con un job `macos-14` que ejecuta
`swift test` y compila los entrypoints `VeloraMobile` y `VeloraTV` del paquete
Apple. Hasta que GitHub Actions complete esa ejecución no se presenta como una
validación realizada; este host Windows no puede sustituirla.

La agrupación Live TV de `VeloraKit` también quedó estabilizada: las fuentes
que Jellyfin devuelve sin identificadores ya no reciben UUIDs aleatorios, sino
una clave determinista, y una regresión unitaria confirma que no duplican la
fila del canal. La prueba Swift queda para el job macOS porque Swift/Xcode no
están instalados en este host.

Se añadió `.github/workflows/ci.yml` para que cada cambio en `main` y cada
pull request relevante valide de forma reproducible los catálogos Android, las
pruebas de interacción/seguridad web, los bundles web/Smart TV y las pruebas y
compilaciones Kotlin de las variantes móvil y TV. La ejecución local equivalente
del 2026-09-05 terminó correctamente: web y empaquetado webOS pasaron, y
`testMobileDebugUnitTest`, `testTvDebugUnitTest`, `compileMobileDebugKotlin` y
`compileTvDebugKotlin` terminaron con `BUILD SUCCESSFUL`. El workflow no afirma
validación física de Tizen, VIDAA, Apple ni de reproducción autenticada.

La comprobación de integración del 2026-09-05 contra el servidor Jellyfin
proporcionado devolvió `HTTP 200` en `System/Info/Public`, pero `HTTP 401` en
`Users/AuthenticateByName` con las credenciales disponibles en esta sesión.
No se guardó ni se imprimió ningún token o contraseña y, por tanto, no se
presentan como verificadas la consulta autenticada de canales, `PlaybackInfo`,
la reproducción ni Live TV real. El smoke test sí mantiene estas etapas para
ejecutarlas cuando el acceso sea válido.

La nueva comprobación autenticada realizada tras actualizar los datos de
conexión volvió a devolver `HTTP 401`; no se obtuvo token y no se consultó la
lista autenticada de Live TV.

La revisión web/Smart TV del 2026-09-05 añadió una regresión explícita para la
regla de producto de descargas: `platform.js` mantiene
`supportsOfflineDownloads: false` y el catálogo de interfaz no contiene
acciones `download`/`offline` en navegador, Tizen, webOS o VIDAA. Las pruebas
de capacidades, seguridad, agrupación de Live TV y el build web completo pasan.
Tizen Studio/CLI no está instalado en este entorno, por lo que el resultado
Tizen es un bundle preparado y no un `.wgt` certificado.

La auditoría de seguridad del 2026-09-05 amplió `SensitiveDataRedactor` para
sanitizar también tokens en encabezados (`Token=`, `Bearer` y
`X-Emby-Token`) cuando una excepción llega a Logcat. Las nuevas regresiones
del redactor pasan en móvil y TV; los resultados XML muestran cero fallos y
cero errores.

La higiene pública del repositorio se revisó el 2026-09-05: no quedan nombres
de usuarios o productos antiguos en la documentación pública. La política de
terceros conserva solo las dependencias realmente distribuidas y una nota
genérica de independencia de identidad, sin presentar proyectos externos como
parte de Velora.

La revisión de lint del 2026-09-05 detectó y corrigió la configuración del
cambio de idioma en App Bundle: todos los locales se mantienen en el bundle
base para que el selector interno funcione sin Play Core. `lintMobileDebug` y
`lintTvDebug` regeneraron sus informes con cero errores; permanecen avisos de
calidad no bloqueantes de dependencias y compatibilidad, documentados por la
herramienta.

La auditoría de privacidad Android sustituyó `ANDROID_ID` por una identidad
aleatoria, persistente y exclusiva de Velora para login normal y Quick Connect.
La búsqueda fuente confirma que ya no quedan usos de `ANDROID_ID` o
`Settings.Secure` en el cliente; las variantes móvil/TV compilan y sus pruebas
unitarias siguen pasando.

La revisión de internacionalización del 2026-09-05 migró a recursos los
textos de Ajustes relacionados con Jellyseerr, autenticación, Descubrir y
tráilers de TMDB. Las claves están presentes en español, inglés, francés y
alemán; `check-android-locales.mjs` valida 374 claves y las variantes móvil/TV
compilan y pasan sus pruebas unitarias. Esto mejora la traducción efectiva al
cambiar el idioma desde Ajustes, sin alterar la lógica de conexión.

La segunda pasada del 2026-09-05 migró también el selector de autenticación,
la configuración de clave API, acceso/cierre de sesión y mensajes de login de
Jellyseerr. El catálogo validado asciende a 394 claves; compilación y pruebas
unitarias de móvil y TV vuelven a pasar. Los errores de credenciales ya no
presentan el mensaje técnico bruto en la interfaz.

La tercera pasada migró los ajustes visibles de procesado de vídeo, mapeo de
tonos, HDR simulado, nitidez y mezcla de fotogramas. El catálogo alcanza 412
claves; la comprobación de locales y las compilaciones Kotlin móvil/TV pasan.

La cuarta pasada migró los estados de subtítulos descargados, verificación de
TMDB, comprobación de actualizaciones, cierre de sesión y encabezado de Ajustes.
El catálogo alcanza 425 claves; `check-android-locales.mjs`, compilación Kotlin
y tests unitarios de móvil/TV pasan de nuevo.

La quinta pasada migró también la versión y la atribución de autor de la sección
Acerca de de Ajustes. El catálogo alcanza 427 claves; la comprobación de locales
y la compilación Kotlin móvil/TV vuelven a pasar. No se generó una release por
ser un cambio de traducción y metadatos, sin impacto funcional en reproducción.

La build TV posterior se compiló e instaló en el Fire TV AFTSS
mediante ADB el 2026-09-05. La actividad se lanzó y el
logcat no mostró `FATAL EXCEPTION`; esto valida instalación y arranque de esta
build, no reproducción autenticada ni Live TV.

La comprobación directa más reciente en el Fire TV AFTSS confirmó el paquete TV
`1.4.0-tv` (`versionCode 10400`), resolvió `MainActivity` y registró el evento
`Displayed ... MainActivity` tras el arranque explícito. Los eventos recientes
no contienen `FATAL EXCEPTION` ni un fallo `AndroidRuntime` atribuible a Velora;
las advertencias observadas pertenecen al sistema Fire TV (Alexa/NTP). Sigue
siendo una prueba de arranque, no una validación autenticada de reproducción o
Live TV.

El pipeline de publicación ahora acepta correctamente hitos `vX.Y.0` con
componentes de más de un dígito (`v1.10.0`, por ejemplo), manteniendo la
política de releases significativas y la publicación conjunta de Android y web.

La verificación web del 2026-09-05 volvió a pasar las pruebas de capacidades,
seguridad e interacción de biblioteca. `build-web.mjs all` generó de nuevo el
bundle de navegador, los paquetes preparados de Samsung y VIDAA y el IPK de
webOS. La salida confirma que Tizen Studio/CLI sigue ausente en este host, por
lo que no se afirma un WGT firmado ni validación en hardware Samsung.

Además, la prueba web ejecuta ahora la función real de agrupación de Live TV
con dos fuentes del mismo canal y verifica que produce una sola fila con ambas
opciones; ya no depende únicamente de aserciones textuales sobre el código.

La batería unitaria móvil/TV se ejecutó de nuevo el 2026-09-05 y terminó con
éxito. Incluye la agrupación de Live TV por ID de canal, el selector de fuentes
(principal/IPTV), decisiones de reproducción, aspecto, seguridad de URLs y
estados de descargas. La agrupación evita duplicar filas cuando Jellyfin ofrece
varias fuentes del mismo canal y abre el selector al pulsar la fila.

La auditoría de logs de red del 2026-09-05 sanitiza los mensajes de excepción
en `JellyfinApi`, `LiveTvActivity`, `JellyfinRepository` y `ServerEntryScreen`.
Usan `SensitiveDataRedactor.message` en lugar de registrar el `Throwable`
completo, evitando que una URL con credenciales o datos de sesión termine en
Logcat. Tests unitarios móviles/TV y compilación Kotlin de ambas variantes pasan
tras este cambio. Quedan otros logs no relacionados con esas rutas en revisión
separada; esto no se considera una certificación de seguridad completa.

La comprobación web del 2026-09-05 vuelve a pasar `test-platform.mjs` y las
pruebas de seguridad/interacción de biblioteca. `build-web.mjs all` genera de
forma reproducible los bundles de navegador, Samsung y VIDAA, además del IPK
webOS `com.klortek.velora_1.4.0_all.ipk`. El comando Tizen no está instalado en
este host, por lo que Samsung sigue siendo un bundle preparado y no se afirma
un WGT firmado. La APK TV debug de `1b42b70` se instaló en Fire TV
(`192.168.31.112:5555`) y la actividad principal quedó ejecutándose sin
`FATAL EXCEPTION`; esto es smoke test de arranque, no validación autenticada de
reproducción o Live TV.

La revisión adicional del reproductor Android sanitiza los errores de
preparación, transcodificación, pistas de audio/subtítulos y reporte de
progreso en `JellyfinVideoPlayerScreen`, evitando registrar `Throwable` con
posibles datos de sesión. `testMobileDebugUnitTest`, `testTvDebugUnitTest`,
`compileMobileDebugKotlin` y `compileTvDebugKotlin` pasan después del cambio.

La misma revisión se extendió el 2026-09-05 a Quick Connect, comprobación de
actualizaciones, instalación de APK y Jellyseerr. Esas rutas ya usan
`SensitiveDataRedactor.message` en vez de registrar excepciones completas; las
pruebas unitarias móviles/TV y las compilaciones Kotlin de ambas variantes
vuelven a pasar.

También se retiraron de los logs de diagnóstico el código de Quick Connect, el
identificador de usuario autenticado y la URI local del instalador de APK.
Solo se conservan mensajes de estado neutros; las pruebas móviles/TV y ambas
compilaciones Kotlin siguen pasando.

La regresión `MediaUrlSecurityTest.mpvAuthenticationIsCarriedByHeadersInsteadOfTheMediaUrl`
confirma que el fallback MPV conserva el token únicamente en las cabeceras
`X-Emby-Authorization` y no en la URL de reproducción; la prueba pasa en la
variante móvil.

Auditoría de evidencia de releases (2026-09-05): las consultas directas a la
API y a la URL pública de GitHub para `v1.2.32` y `v1.4.0` devolvieron HTTP
404. Por tanto, las notas antiguas de releases que aparecen más abajo se
consideran archivo histórico no revalidado; no se usan para afirmar que exista
una release pública actual. Las APK locales documentadas aquí son artefactos
unsigned verificables, no publicaciones de tienda.

La verificación adicional del 2026-09-05 corrige la configuración de lint para
los catálogos Android con fallback parcial y vuelve a ejecutar `lintMobileDebug`:
finaliza con 0 errores de lint. La comprobación oficial
`node scripts/check-android-locales.mjs` mantiene los tres catálogos garantizados
(inglés, alemán y francés) completos con 350 claves; los demás idiomas siguen
siendo opcionales y heredan el español base de forma controlada.

La pantalla de solicitudes Jellyseerr ya no muestra estados hardcodeados en
inglés: `Available`, `Requested`, `Pending` y `Request` pasan por recursos
traducibles. La comprobación de locales quedó en 350 claves en ese cambio y
volvieron a pasar los tests unitarios móviles y la compilación Kotlin de TV.

El bloque de Ajustes de subtítulos y transcodificación AAC también dejó de
depender de textos fijos: sus acciones y mensajes pasan por recursos traducibles
en los catálogos garantizados. La comprobación oficial queda en 356 claves;
`lintMobileDebug` vuelve a terminar con 0 errores, junto con los tests móviles y
la compilación TV.

El commit `2859512` vuelve a generar las APK release con estos cambios y supera
lint vital: móvil (67 836 307 bytes, SHA-256
`225CABA35693C12393F8E14D2C0EF74B75D3E4B05E26BCCADCFC9FCC42A071B0`) y TV
(67 836 279 bytes, SHA-256
`F12AAF82D05FB2821EE98A5D0AE688613EB5CF67EAF45E53D0010C52F0D3B3AE`).

Los botones repetidos de Ajustes (`Guardar`, `Cancelar` e `Iniciar sesión`)
también reutilizan recursos traducibles. Tras la migración, los tests unitarios
móviles y la compilación Kotlin de TV vuelven a pasar; no quedan esos literales
en `SettingsScreen`.

La verificación de release sobre el commit actual `a649075` terminó con
`BUILD SUCCESSFUL` en 13m 12s ejecutando tests unitarios móviles, compilación
Kotlin de TV, lint móvil y `assembleMobileRelease`/`assembleTvRelease` con lint
vital. Los APK unsigned recién generados son: móvil (67 836 447 bytes,
SHA-256 `DC9B7F5AF7D106C865C448A545270351270466520EE4A21596CB373E5EDA5FB3`)
y TV (67 836 687 bytes, SHA-256
`67FE56B116B20FF3BC7334714F6328A014D8C810F453667EB57F70DCBDCF7B9C`).
Siguen siendo artefactos locales sin firma de distribución y no equivalen a una
release pública de GitHub.

La prueba física posterior del 2026-09-05 sí detectó un Fire TV AFTSS por
ADB en `192.168.31.112:5555`. El APK release unsigned fue rechazado por el
dispositivo por carecer de certificado, como corresponde a un artefacto sin
firma; se instaló la variante debug firmada del mismo commit y la actividad
`com.klortek.velora.MainActivity` arrancó correctamente en 1920x1080. El
arranque tardó aproximadamente 9,2 s y no produjo `FATAL EXCEPTION` ni error
de Velora en los 500 eventos recientes. Esta es una prueba de arranque, no una
certificación completa de Live TV o reproducción contra el servidor.

La auditoría funcional del 2026-09-05 confirma que películas y series usan la
misma consulta determinista: género, favoritos, vistos/no vistos, ordenación
por nombre/fecha/estreno/duración/valoración/aleatorio y dirección ascendente
o descendente. Las preferencias se conservan por dispositivo y el diálogo
ofrece una salida táctil y otra enfocada a mando. `testMobileDebugUnitTest`,
`testTvDebugUnitTest`, `compileMobileDebugKotlin` y `compileTvDebugKotlin`
terminaron correctamente. El Fire TV volvió a quedar en primer plano con
`com.klortek.velora.tv` activo y sin excepciones de la aplicación.

La prueba autenticada contra `http://192.168.100.201:8096` del mismo día llegó
al servidor, pero Jellyfin respondió HTTP 401. No se considera validado el
login ni Live TV contra ese servidor hasta revisar las credenciales o el
usuario activo; el smoke test no guarda ni imprime la contraseña.

El workflow Android queda endurecido: una etiqueta `vX.Y.0` ya no puede crear
una release pública con APK unsigned; exige los cuatro secretos de firma. Los
builds unsigned siguen disponibles únicamente en ejecuciones de QA sin
`release_tag`.

El selector de fuentes Live TV ya recibe el texto de respaldo desde recursos
localizados; una fuente sin nombre del proveedor no fuerza «Opción» en español
cuando la interfaz está en inglés, francés o alemán. La regresión de
`LiveTvChannelQueryTest` y la compilación móvil/TV pasan tras el cambio.

El flujo Android queda preparado para firma de distribución opcional mediante
los cuatro secretos `VELORA_ANDROID_*` de GitHub Actions. Sin ellos, conserva
los APK release unsigned de QA; con ellos, cambia el nombre del artefacto a
release firmado para que pueda instalarse directamente. No se ha creado ni
publicado ninguna clave en el repositorio.

Después de esa modificación, el commit `2b83d57` vuelve a pasar
`testMobileDebugUnitTest`, `testTvDebugUnitTest` y la compilación Kotlin de
ambas variantes. La validación de firma con una clave temporal local también
pasó; la clave fue eliminada y no forma parte del árbol de trabajo.

Tras esa corrección, el commit `54bbfb7` vuelve a pasar `assembleMobileRelease`
y `assembleTvRelease` con lint vital. Las APK unsigned vigentes son: móvil
(67 832 683 bytes, SHA-256
`94F9A56822AB6717EFF8C1108CC1DF425A8BA3D661CAD27D226E18CC0BC86175`) y TV
(67 832 683 bytes, SHA-256
`D6DF0FF57D0AABC708E04825674CEDC82795EB07637F9C62BAFFFE60EDB24907`).

Desde el commit `25e7158` también pasan `assembleMobileRelease` y
`assembleTvRelease` con lint vital. Los artefactos unsigned resultantes son:
móvil (67 830 323 bytes, SHA-256
`1037924C2102DB0CAD26371AD89A6AEF6D6B5B9251C0F044ADFA984D84C18590`) y TV
(67 830 447 bytes, SHA-256
`B0505DC10D63497BAFB98095688A8B9AC6D1A4BB11AF0CB68D5983637D636A1D`). No se
presentan como APK de tienda porque siguen sin firma de distribución.

La verificación del 2026-09-05 se ejecutó sobre `main` en el commit
`f73bc45`. Los
tests unitarios Android, `lintMobileDebug`, las compilaciones Kotlin móvil/TV,
`node --check`, las pruebas web y `npm run build:all` pasan. El empaquetado
webOS genera su IPK y los bundles de Samsung/VIDAA se preparan honestamente
como no firmados cuando faltan sus herramientas o certificados. La rama
remota comprobada es únicamente `main`.

El commit `3a31985` endurece la ruta Apple: las URLs de VOD y Live TV devueltas
por Jellyfin se validan contra el servidor configurado y se eliminan sus
parámetros de credencial antes de llegar a AVPlayer; la cabecera de sesión no
se envía a hosts externos. Se añadieron regresiones Swift para ambos casos.
Swift/Xcode no está instalado en este host Windows, por lo que la ejecución
nativa queda pendiente del workflow macOS.

El commit `a85b192` completa la revisión Android de logs de URL: el actualizador
APK y las importaciones CSS pasan por el redactor común, que también cubre
parámetros de autorización y `X-Emby-Token`. `testMobileDebugUnitTest` y
`compileTvDebugKotlin` pasan después del cambio.

La compilación release local del 2026-09-05 también genera las dos APK sin
firmar: móvil (67 825 027 bytes, SHA-256
`CA28E86EC624076961AD70C87FF1D27C4E738026081D9569447EBEB4B2D87A66`) y TV
(67 825 147 bytes, SHA-256
`84D9EED46BCC15187E38C76ED893CA6FBA3D8C53FDD60765D88EB6BCF5DC90AD`). No se
presentan como APK de tienda porque siguen sin firma de distribución.

La recompilación posterior de `main` en `47be085` vuelve a pasar
`assembleMobileRelease` y `assembleTvRelease`. Sus artefactos unsigned son:
móvil (67 825 599 bytes, SHA-256
`C85E1380D07E534440E07B0FEC17CA08E532076726528D4F29B4B8895309FE67`) y TV
(67 825 507 bytes, SHA-256
`2E942D0EFD149A5536354ABC226924CE34703659AB5899270438002BDC20DC36`).

La validación web posterior vuelve a pasar las pruebas de capacidades,
seguridad e interacción. El bundle web y el paquete webOS se generan; Samsung
queda preparado sin firma por ausencia de Tizen Studio/CLI y VIDAA queda como
bundle HTML5, sin inventar un paquete firmado que no se ha validado.

El commit `04ca0ab` añade un mini-reproductor persistente para Live TV en web:
se puede minimizar sin detener la sesión, seguir navegando por la biblioteca,
restaurarlo con foco de teclado/mando o cerrarlo liberando la sesión. Las
pruebas web cubren la ruta y se regeneraron los bundles web, webOS y VIDAA;
Tizen sigue pendiente de Tizen Studio/CLI y un perfil de firma disponibles.

El commit `122e20a` conserva el foco en el primer control al restaurar el
mini-reproductor, de forma que teclado y mando no pierden la navegación.

La revisión Apple posterior añade tamaño y SHA-256 al índice offline, valida
la integridad del archivo gestionado antes de reproducirlo y rechaza contenido
alterado. Los metadatos antiguos sin hash siguen siendo compatibles y se
comprueban de forma segura. Swift/Xcode no está instalado en este host Windows,
por lo que esta cobertura queda pendiente de ejecución en el workflow macOS.

La comprobación ADB de esta pasada no detectó ningún dispositivo conectado;
por tanto no se declara una prueba física nueva en Fire TV.

La regresión Apple también cubre la lectura de metadatos offline antiguos sin
los campos de tamaño/hash, además de la detección de archivos manipulados.

Tras el cambio Apple, `testMobileDebugUnitTest`, `compileTvDebugKotlin`,
`node web/scripts/test-platform.mjs` y `node --check web/app.js` vuelven a
pasar. La compilación Android emitió únicamente los avisos ya conocidos del
SDK local y no errores de código.

Los catálogos Android garantizados para la experiencia principal —español base,
inglés, alemán y francés— ya contienen las 346 claves actuales. El workflow de
release ejecuta `scripts/check-android-locales.mjs` para impedir que vuelvan a
quedar incompletos. Los demás idiomas instalados mantienen fallback seguro al
catálogo base mientras se amplía su traducción.

La auditoría de seguridad añade cobertura directa para la redacción sin
distinguir mayúsculas, mensajes de error y rutas/nombres locales. El conjunto
`testMobileDebugUnitTest` continúa pasando.

La etiqueta remota `v1.4.0` se realineó con el `main` actual (`1dbb8a3`) para
evitar que el workflow de publicación reconstruya un commit antiguo. La API
pública de GitHub sigue devolviendo 404 al consultar releases y ejecuciones
desde este entorno, por lo que no se afirma que la release haya sido creada
hasta disponer de evidencia directa.

El commit `933c99f` añade semántica accesible a las pestañas de detalle y a
los selectores de temporada móviles, sin cambiar su interacción táctil o de
mando. La compilación móvil/TV y los tests unitarios pasan después del cambio.

El commit `65ab892` migra los controles visibles de subtítulos y OpenSubtitles
en Ajustes a recursos traducibles. Los tests unitarios móviles y las
compilaciones Android móvil/TV pasan después del cambio.

La revisión de seguridad posterior elimina de los logs del reproductor y MPV
las rutas y nombres locales de subtítulos/fuentes, manteniendo solo marcadores
redactados. Tras el cambio pasan `testMobileDebugUnitTest` y la compilación
Kotlin TV.

La auditoría de capacidades de descarga confirma que el contenido sin conexión
solo se expone en móvil/tablet Android y en iPhone/iPad. TV, navegador, tvOS,
Tizen, webOS y VIDAA mantienen `supportsOfflineDownloads = false`; no se
renderizan sus controles de descarga, temporadas sin conexión ni gestión de
almacenamiento. Las pruebas de capacidades web y Android pasan. En esta
máquina no hay ningún dispositivo visible por ADB, así que no se declara una
instalación o una prueba física del Fire TV.

El commit `ebb7685` sustituye los textos incrustados de los controles críticos
del reproductor por recursos traducibles y completa esas claves en los 13
catálogos Android. La validación XML, los tests unitarios móviles y las
compilaciones Kotlin móvil/TV pasan después del cambio.

El commit `e2c8ac3` completa las traducciones del nombre de los ajustes del
reproductor que aún aparecían en inglés en ocho catálogos. El merge de recursos
móvil/TV y la validación XML pasan después del cambio.

El commit `c332ead` migra los ajustes de reproducción de Android —MPV, saltos,
transcodificación, códec, bitrate y reproducción automática— a recursos
traducibles y añade las traducciones principales en español, inglés, francés y
alemán. La validación XML y la batería Android móvil/TV pasan después del
cambio.

El commit `fb90491` completa esas mismas claves en los nueve catálogos restantes
(español regional, italiano, portugués, turco, ruso, árabe, japonés, coreano y
chino). La validación XML y el merge de recursos móvil/TV pasan después del
cambio, sin dejar el menú de reproducción con un idioma parcial.

El commit `c0a9942` elimina los últimos títulos incrustados de las categorías
especiales de Ajustes y los lleva a los 13 catálogos Android, incluyendo
Jellyseerr y Tráilers. La compilación de recursos y los tests unitarios móviles
pasan después del cambio.

El commit `ccdb357` corrige la base Apple para no proponer `localhost` como
servidor inicial, localiza el título de biblioteca y la pista de accesibilidad
de Live TV en los cuatro catálogos Swift disponibles. Se revisa estáticamente;
la compilación Apple continúa pendiente de macOS/Xcode.

La corrección posterior migra a recursos las etiquetas accesibles de Ajustes,
Buscar, Inicio, Ordenar y Vistos en las pantallas Android principales. La
compilación Kotlin móvil/TV y `testMobileDebugUnitTest` pasan después del
cambio.

La validación web posterior ejecuta las pruebas de capacidades/seguridad, el
chequeo de sintaxis y `build-web.mjs all`: el IPK webOS se genera; Samsung y
VIDAA quedan como paquetes no firmados para sus herramientas/portales. La API
pública de GitHub devuelve HTTP 404 para `/releases`, por lo que actualmente
no se declara ninguna release pública ni APK publicado aunque existan flujos
preparados para producirlos al crear una etiqueta válida.

El workflow de releases no bloquea una publicación por la ausencia de Tizen
Studio o de un certificado Samsung en el runner: conserva el bundle web
preparado y solo publica un WGT cuando la herramienta y la firma están
disponibles. Así se mantiene explícito el límite de validación de Samsung sin
hacer fallar los artefactos verificables de las demás plataformas.

Los workflows permiten ahora reconstruir una etiqueta existente desde
GitHub Actions mediante el parámetro `release_tag`, comprobando antes que
coincide con la versión del código. Un lanzamiento manual sin ese parámetro
solo ejecuta QA y no crea una release.

El servidor Jellyfin indicado responde a la información pública (`HTTP 200`),
pero la autenticación con las credenciales facilitadas para esta sesión
devuelve `HTTP 401`; por eso no se marca como verificada la consulta autenticada
de Live TV ni la reproducción remota. No hay ningún dispositivo ADB conectado en este
host y el Fire TV tampoco responde en la dirección conocida, así que no se
declara instalación ni prueba real de hardware. GitHub CLI no tiene sesión
autenticada y la consulta pública de releases devuelve 404; no se presenta una
release APK como publicada sin confirmación de Actions. Apple sigue pendiente
de una ejecución observada en macOS/Xcode; el repositorio ya incluye
`apple.yml`, que compila y ejecuta el Swift Package en macOS cuando GitHub
Actions dispone del runner correspondiente. Esto valida el paquete compartido,
pero no equivale todavía a certificar una app firmada para iOS, iPadOS o tvOS.

El commit `69b45ae` añade entrypoints macOS mínimos y condicionados para que
los ejecutables del Swift Package puedan enlazar en CI; no los presenta como
una aplicación macOS distribuible.

Tras publicar `083d222`, se repitieron las comprobaciones locales: 75 tests
JUnit Android, `lintMobileDebug` sin errores, compilación Kotlin móvil/TV,
parseo y tests web, y `npm run build:all` con IPK webOS generado. Tizen Studio,
la firma Samsung/VIDAA, macOS/Xcode y el hardware real siguen sin estar
disponibles en este host, por lo que permanecen explícitamente sin certificar.

La revisión web posterior detectó que las claves nuevas de reparto, filtros y
Live TV no estaban presentes en todos los catálogos. `257dd6a` normaliza cada
idioma contra el catálogo inglés como fallback neutral, mantiene las
traducciones existentes y evita que una clave ausente reaparezca en español.
Parseo, tests web y empaquetado multiplataforma pasan después del cambio.

`6954aa8` completa además los textos nuevos en portugués, francés, alemán e
italiano para que reparto, ordenación, filtros y Live TV no dependan del
fallback inglés en esos idiomas. Las regresiones web y los paquetes vuelven a
pasar.

`e73d724` completa las mismas etiquetas para japonés, coreano, chino, ruso,
árabe y turco, incluido el control de salida de pantalla completa. La web y
los paquetes Smart TV vuelven a pasar sus comprobaciones.

`05dfd8a` lleva el agrupado de Live TV también al modelo Apple: varias filas
con la misma ID se presentan como un canal y conservan sus `MediaSources`.
Incluye regresiones de decodificación y agrupado; queda pendiente la
ejecución de Swift en el runner macOS, no se declara validado en Windows.

`66de3b5` completa el flujo Apple: el menú de fuentes ofrece las alternativas
del canal y la elegida se transmite como `MediaSourceId` a `PlaybackInfo`.
La selección ya no es solo visual; su compilación sigue pendiente del runner
macOS por la ausencia de Swift/Xcode local.

La verificación Android posterior ejecutó `testMobileDebugUnitTest`,
`lintMobileDebug`, `assembleMobileDebug` y `assembleTvDebug`: BUILD SUCCESSFUL.
Los APK de QA generados quedan en `app/build/outputs/apk/` y no se presentan
como release publicada ni como instalación en hardware, porque ADB no detecta
ningún dispositivo.

La repetición sobre `858273c` volvió a completar esas cuatro tareas Android
con éxito. También pasaron `node --check app.js`, las pruebas de capacidades y
seguridad web y `npm run build:all`; el empaquetado volvió a generar el IPK de
webOS y los bundles HTML5 de Samsung/VIDAA sin afirmar firma ni certificación
de hardware.

`71da73f` elimina de los logs Android rutas de archivos locales, rutas internas
de subtítulos y enlaces de descarga sin redacción. La suite de tests y lint
Android vuelven a pasar después del endurecimiento.

`70f5c1d` desacopla el estado de las pestañas de detalle móvil de sus etiquetas
traducidas y elimina un fallback de audio hardcodeado. Tests y APK de móvil/TV
vuelven a compilar correctamente después del cambio.

La base Apple solicita ahora `PlaybackInfo` antes de reproducir VOD y elige la
fuente directa o remuxada devuelta por Jellyfin, conservando el stream genérico
como fallback. El cambio está revisado estáticamente y queda pendiente de
compilación y prueba en macOS/Xcode.

La reproducción Apple aplica también las preferencias locales de audio y
subtítulos al `AVPlayerItem`: idioma preferido, subtítulos desactivados,
preferidos, forzados o automáticos. La API se integra de forma asíncrona tras
crear el elemento nativo; la compilación en Xcode sigue pendiente.

La misma aplicación se reutiliza en reproducción offline y Live TV, evitando
que esas rutas tengan un comportamiento distinto al VOD.

El acceso offline de Apple identifica las descargas por el servidor configurado
y no por una sesión de red activa; así el contenido local puede seguir
reproduciéndose tras abrir la aplicación sin conectividad.

## Release en preparación: 1.4.0

El código actual queda versionado como `1.4.0` para agrupar el bloque
funcional de Live TV con fuentes seleccionables y la recuperación offline. La
etiqueta anotada `v1.4.0` existe en remoto, pero apunta a un commit anterior al
`main` actual; este host no tiene autenticación CLI de GitHub, así que la
existencia de la release, sus checks y sus artefactos todavía deben confirmarse
desde Actions. No se presenta como publicada hasta verificarlo.

Auditoría de trazabilidad del 2026-09-04: la etiqueta remota `v1.4.0` apunta a
un commit anterior al `main` actual. Por ello no se considera la build actual
ni se reescribe automáticamente la etiqueta existente; moverla requeriría una
decisión explícita sobre la historia de la versión. Los workflows ya permiten
reconstruir una etiqueta existente cuando esa referencia sea la elegida.

Smoke test remoto del 2026-09-04: `192.168.100.201:8096` responde, pero la
autenticación con el usuario facilitado devuelve HTTP 401; por ello no se han
marcado como verificadas la consulta autenticada de Live TV ni la reproducción
contra ese servidor. El Fire TV tampoco acepta actualmente ADB en la dirección
anterior y no se presenta como instalado allí. El resultado 401 se volvió a
confirmar en esta ronda con `scripts/qa/jellyfin-smoke.ps1`, sin imprimir
contraseña ni token.

Verificación local de 1.4.0 del 2026-09-05 sobre `main` en `5abafdc`: 75 tests
JUnit Android pasan sin fallos ni errores; `lintMobileDebug` informa 0 errores
(mantiene advertencias de
dependencias, estilo y recursos heredados); las compilaciones Kotlin móvil y
TV pasan. `node --check`, las pruebas web y `npm run build:all` pasan. El
empaquetado webOS genera su IPK; Samsung sigue siendo bundle no firmado por
falta de Tizen Studio/CLI y VIDAA sigue requiriendo la firma de su portal.
La auditoría de capacidades confirma que las descargas solo se exponen en
móvil/tablet y permanecen ocultas en TV, navegador, tvOS, Tizen, webOS y VIDAA.
La base Apple ya no depende de `jellyfin.local`: el formulario usa el servidor
guardado por el usuario y, en una instalación nueva, muestra un valor local
editable sin iniciar conexiones automáticamente. Este cambio queda pendiente
de compilación en macOS/Xcode porque esas herramientas no están disponibles en
este host.

La revisión posterior también normaliza en web una única fila Live TV que
contiene varias `MediaSources`, igual que Android, y añade una regresión de
estructura para impedir que el selector desaparezca en ese caso. Tests web,
parseo y empaquetado webOS pasan después del cambio.

Este dashboard registra únicamente el estado verificable. Una función no se
marca como completa solo porque exista código o un artefacto anterior.

La única rama publicada es `main`. Las notas históricas que aparecen más abajo
se conservan como referencia y no representan el estado actual de la release.
El workflow `branch-policy.yml` comprueba automáticamente que la única rama
pública siga siendo `main`; la comprobación no sustituye la configuración de
permisos de GitHub, pero hace visible cualquier desviación en CI.

La política de publicación mantiene las releases públicas para hitos funcionales
relevantes: los workflows automáticos solo se activan con etiquetas `vX.Y.0`.
Los builds de parche se reservan para QA y no generan una release salvo que la
corrección sea crítica o de seguridad.

Verificación local del 2026-09-02: `node web/scripts/test-platform.mjs`,
`testMobileDebugUnitTest` y `compileTvDebugKotlin` pasan. No se ha creado una
nueva release para esta verificación porque no incorpora un bloque funcional
grande.

La recuperación offline Android se ha endurecido: si una fila SQLite pendiente
sobrevive al cierre de la aplicación pero su trabajo de WorkManager desaparece,
Velora lo vuelve a programar con sus restricciones de red; una descarga marcada
como completada cuyo archivo privado ya no existe pasa a fallida para no ofrecer
una reproducción rota. La validación conjunta de `testMobileDebugUnitTest`,
`compileTvDebugKotlin` y `lintMobileDebug` pasa; quedan advertencias no
bloqueantes de APIs antiguas del proyecto.

Live TV conserva ahora el agrupado visual por ID de canal y transmite además el
`MediaSourceId` de la alternativa elegida a `PlaybackInfo` en Android y web.
Esto permite seleccionar de forma real fuentes como Principal/IPTV cuando
Jellyfin las expone; si la respuesta no incluye fuentes identificables, Velora
usa la selección estándar del servidor. La regresión web y
`testMobileDebugUnitTest` pasan tras este cambio.

La consulta de canales solicita explícitamente `MediaSources` y el contador de
la interfaz representa filas agrupadas, no entradas duplicadas. La regresión
también cubre que dos entradas con la misma ID conserven sus fuentes y que se
propague la segunda alternativa.

En la auditoría posterior, `npm run build:all` prepara el cliente web, webOS y
VIDAA; Samsung queda honestamente pendiente de Tizen Studio/CLI. `lintMobileDebug`
también pasa después de corregir el opt-in de Media3 en la música de tema y de
completar las traducciones Android del selector Live TV. El lint aún informa
advertencias y sugerencias no bloqueantes de SDK, APIs/dependencias antiguas.

El pipeline Android fija Node.js 20 antes de validar la versión de las etiquetas,
para que las releases de milestone sean reproducibles en GitHub Actions.

La base Apple incorpora ahora un catálogo offline privado para iPhone/iPad:
descarga autenticada sin token en la URL, reproducción local preferente y
eliminación desde Velora. La transferencia en segundo plano, calidades y la
validación en macOS/hardware siguen pendientes; tvOS continúa sin descargas.

Después de esa verificación se subieron dos correcciones adicionales a
`main`: `b5e298f` centraliza la aplicación del aspecto entre reproducción
vertical y pantalla completa, con regresiones para todos los modos; `55c08cf`
valida también dentro del service worker web el servidor y las credenciales del
proxy multimedia. Las pruebas web, unitarias Android y la compilación móvil/TV
han pasado tras ambos cambios. Ninguno genera una release aislada.

El commit `038c5dd` limita además el proxy multimedia web a rutas de vídeo y
Live TV del mismo servidor y evita que pueda reutilizarse como un proxy
autenticado genérico. `node --check web/media-proxy-sw.js` y
`node web/scripts/test-platform.mjs` pasan después de la regresión. Se mantiene
como corrección de seguridad puntual, sin release independiente.

La auditoría de autenticación añadió validación estricta y compartida para la
URL del servidor: solo se aceptan HTTP/HTTPS con host y puerto válidos, sin
credenciales, query ni fragmentos embebidos. También se eliminó una lectura
innecesaria del cuerpo de errores de login. Se añadió cobertura unitaria para
IPs locales, IPv6, rutas válidas y entradas inseguras. `testMobileDebugUnitTest`,
`compileMobileDebugKotlin` y `compileTvDebugKotlin` pasan; no es una release
aislada y queda agrupado para el próximo hito publicable.

En esta misma verificación, `lintMobileDebug` termina correctamente. El
informe mantiene advertencias y sugerencias no bloqueantes, pero no quedan
errores de lint en el cliente móvil. La diferencia de SDK XML y la ubicación
alternativa de Android 36 que aparecen durante Gradle son avisos del SDK local,
no fallos del código de Velora.

La persistencia offline ya no elimina por accidente otras calidades del mismo
título al reemplazar una descarga. El índice SQLite admite ahora el caso real
de tener, por ejemplo, Original y Medium simultáneamente, y se añadió una
regresión para protegerlo. `testMobileDebugUnitTest`, las compilaciones móvil
y TV, `lintMobileDebug`, las pruebas web y el parseo de recursos pasan después
del cambio. Es un commit de corrección agrupable, no una release aislada.

El flujo de inicio de sesión Android ya no muestra mensajes técnicos ni textos
hardcodeados en inglés: los fallos de credenciales y Quick Connect usan el
recurso localizado de error genérico. Se mantiene el detalle técnico fuera de
la interfaz y la compilación móvil/TV se valida tras el cambio.

El lint móvil detectó y se corrigió el uso de APIs no disponibles en Android
21 en los servicios de música y en el reloj; la compilación móvil/TV posterior
supera el cambio. El histórico de deuda queda conservado como referencia; el
último `lintMobileDebug` ya no presenta errores bloqueantes.

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

La cobertura de idiomas principales se ha saneado: inglés, francés, alemán y
español mantienen catálogos explícitos con las claves completas. También se
han completado las claves estructurales de árabe, italiano, japonés, coreano,
portugués, ruso, turco y chino; donde aún no existe traducción humana se usa
un fallback inglés explícito, sin dejar la interfaz parcialmente sin recursos.
Las claves de Live TV están separadas y presentes en todos los catálogos. Las
pruebas de recursos, `testMobileDebugUnitTest`, `compileMobileDebugKotlin`,
`compileTvDebugKotlin` y `lintMobileDebug` pasan tras el cambio.

También se normalizó el formato de tiempos, puntuaciones y metadatos numéricos
con `Locale.ROOT`, evitando resultados dependientes del idioma del dispositivo.
La batería posterior de tests y compilaciones móvil/TV vuelve a pasar.

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

La etiqueta de versión más reciente publicada en el repositorio es
`v1.3.0`, y los commits posteriores permanecen en `main` como correcciones
agrupables. La existencia de la etiqueta no se interpreta por sí sola como
una release de GitHub con artefactos verificados: esa publicación debe quedar
confirmada por los workflows de GitHub Actions. Las validaciones locales de
Android móvil/TV terminaron correctamente tras endurecer el motor de decisión
de reproducción. El modelo Apple usa ahora los nombres de
campos reales de la API Jellyfin (`Id`, `Name`, `Type`, `Overview`,
`ImageTags`) y tiene una prueba de regresión.

La publicación de releases queda serializada entre los workflows Android y web
mediante un grupo común por referencia. Solo una etiqueta semántica `vX.Y.0`
publica; ambos workflows aportan sus artefactos a la misma release y usan un
changelog breve en castellano con los cambios funcionales principales.

La base Apple ya no depende de un proveedor externo opcional para mostrar
carátulas: las tarjetas pueden solicitar artwork autenticado a Jellyfin y
mantienen un placeholder durante la carga o ante un error. La compilación
SwiftUI y la validación en iOS, iPadOS y tvOS siguen pendientes de macOS/Xcode
y hardware Apple, por lo que todavía no se marcan como verificadas.

La validación estricta de servidor se aplica también al cliente Apple y tiene
regresión para IP local, ruta base, credenciales, parámetros y esquemas no
permitidos. No se ha ejecutado `swift test` en este host porque Windows no
dispone de Swift/Xcode.

Los entrypoints Apple dejaron de usar `127.0.0.1:8096` como servidor inicial,
ya que en un dispositivo real eso apunta al propio equipo. Ahora muestran un
valor editable de ejemplo y esperan la dirección Jellyfin real del usuario.

El login web aplica ahora la misma validación estricta de servidor: esquema
HTTP(S), host y puerto válidos, sin usuario, contraseña, query ni fragmento.
La prueba de plataforma verifica que esta validación no se elimine por error.

La navegación inferior móvil ya no deja el botón «Inicio» sin callback: ahora
usa el estado real de la lista y vuelve suavemente al primer bloque del home.
`compileMobileDebugKotlin` y `testMobileDebugUnitTest` pasan después del cambio.

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

Live TV agrupa ahora las entradas que comparten `ChannelId` en una sola fila,
conservando las fuentes alternativas y mostrando un selector táctil/por mando
al tocar el canal. El zapeo usa la lista agrupada para no repetir canales. La
cobertura unitaria y la compilación Android móvil/TV pasan; queda pendiente
validarlo en hardware conectado y confirmar qué etiquetas de fuente entrega
cada proveedor Jellyfin.

El cliente web aplica la misma agrupación y selector accesible por teclado; sus
pruebas de seguridad, capacidades y renderizado pasan. La selección real de
una fuente depende de que Jellyfin entregue entradas/fuentes alternativas bajo
la misma identidad; el cliente no inventa fuentes ni incorpora M3U directo.
Las etiquetas de cantidad y opción del selector web están cubiertas para los
idiomas configurados, con fallback seguro al catálogo español.

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
- Apple shared `VeloraKit` foundation and SwiftUI iOS/iPadOS/tvOS shell are
  present with mobile-only offline capability semantics, header-based Jellyfin
  access, Original First decision tests, native AVPlayer playback and Live TV;
  signed app projects and hardware validation remain pending.
- VIDAA support is not validated on a real device or certified runtime.
- Offline queue/storage recovery, background transfer, quality selection,
  persistence of integrity metadata, settings integration and the full
  end-to-end offline journey are implemented across Android and the Apple
  package, but the Apple path still needs macOS/iOS execution and the complete
  journey remains unverified; storage policy and digest contracts have unit
  coverage.
- Completed provider downloads are now copied into Velora's private
  `filesDir/offline/media` storage and verified before offline playback; the
  transfer provider remains only as a compatibility bridge for existing
  installations and background transfers.
- New mobile downloads are queued through WorkManager with network constraints,
  progress reporting, retry semantics and Keystore-backed credential lookup;
  legacy DownloadManager entries remain readable during migration.
- Persistent visual preview and large-library virtualization still need
  product integration and critic testing. The preview lifecycle contract now
  covers dwell timing, stale-focus invalidation and cleanup without owning a
  player instance. Android now exposes an opt-in, persisted theme-music
  preference, configurable volume and connects one lifecycle-managed ExoPlayer
  to Jellyfin theme songs after the Home focus debounce; hardware QA is still
  pending. Trailer selection now prefers Jellyfin
  local/remote trailer metadata and falls back to TMDB only when the server
  has no trailer available; this Android path is covered by a successful
  mobile/TV compilation but still needs hardware playback QA.
- Live TV channel zapping and previous-channel shortcut are implemented in the
  ExoPlayer OSD and covered by deterministic navigation tests. The web client
  now has a persistent mini-player in the browsing surface; Android/Smart TV
  programme playback and the complete cross-device flow still need hardware QA.
- La evidencia de hardware debe leerse junto con la sección «Última
  verificación»: el Fire TV sí tuvo una prueba de instalación y arranque por
  ADB, pero no se ha certificado todavía reproducción ni Live TV autenticados.

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
| Android TV / Fire TV | Debug + release APK built; TV download guard and Live TV zapping compiled | Hardware validation pending |
| Web | `npm run build:all` passes; webOS IPK generated | Browser/device validation pending |
| Samsung Tizen | Packaging path exists | Device/emulator validation pending |
| LG webOS | Packaging path exists | Device/emulator validation pending |
| Hisense VIDAA | Preparation only | Not verified |
| iOS/iPadOS/tvOS | `VeloraKit` SwiftUI shell and native AVPlayer entrypoints present | macOS/Xcode, signed app and hardware validation pending |

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
- `testMobileDebugUnitTest` and `compileTvDebugKotlin`: passing on 2026-09-02
  after routing Jellyfin-managed trailers through the canonical ExoPlayer
  player; only existing SDK/deprecation/KAPT warnings were emitted.
- `testMobileDebugUnitTest`: passing on 2026-09-02 with preview lifecycle
  coverage for dwell timing, focus cancellation and stop cleanup.
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
