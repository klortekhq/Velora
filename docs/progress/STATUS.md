# Estado verificable de Velora

Updated: 2026-09-08

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
- Último cambio funcional documentado: las peticiones de reproducción usan una
  identidad de cliente coherente con la variante Android móvil o TV, incluida
  la cabecera que Jellyfin recibe al resolver el `PlaybackInfo`.
- Identidad pública: Velora por Klørtek. Los identificadores de paquete se
  conservan únicamente donde los exige el sistema de distribución de cada
  plataforma y no forman parte de la identidad visible del producto.
- CI comprueba que README, atribuciones, documentación y código público no
  reintroduzcan referencias heredadas a otros clientes o identidades antiguas.
- Auditoría pública de GitHub del 2026-09-06: no hay ninguna Release publicada
  actualmente; solo permanecen etiquetas históricas. No se presenta ningún APK
  unsigned como descarga pública.

## Funcionalidad implementada

- Android móvil/tablet y Android TV/Fire TV comparten dominio Jellyfin, pero
  mantienen layouts táctil y de mando separados.
- Android incorpora perfiles de rendimiento persistentes (Automático, Calidad,
  Equilibrado y Rendimiento) que aplican de forma conjunta las optimizaciones
  de animación, tarjetas, fondos y resolución; el mapeo de claves tiene prueba
  unitaria para evitar perder la preferencia al actualizar.
- El cliente web expone los mismos cuatro perfiles, guarda la elección por
  dispositivo y aplica el modo Rendimiento reduciendo transiciones y animaciones
  y solicitando artwork de menor resolución; Calidad solicita artwork de mayor
  resolución, sin ofrecer descargas offline en navegador.
- Web: la música de tema de Jellyfin usa un único elemento de audio persistente,
  espera 700 ms de foco antes de consultar `ThemeSongs`, cancela solicitudes
  obsoletas y hace fade-in/fade-out. El proxy del navegador autoriza únicamente
  la ruta de audio de tema además de las rutas multimedia existentes; el token
  sigue viajando solo en la cabecera del proxy. La función está desactivada por
  defecto y se controla desde Ajustes con volumen persistente.
- Media3/ExoPlayer es el backend Android predeterminado; MPV solo se usa si el
  usuario lo selecciona o si el fallback configurado resulta necesario.
- Android Live TV: la ruta predeterminada de ExoPlayer conserva ahora los
  códecs y la decisión de copia de Jellyfin; la ruta MPV explícita mantiene sus
  parámetros propios sin alterar el backend predeterminado.
- Las cabeceras de autenticación y reproducción distinguen Android móvil de
  Android TV/Fire TV sin registrar credenciales, tokens ni cabeceras sensibles.
- Android difiere la creación de los clientes HTTP principal y Live TV hasta la
  primera petición, evitando inicializar el motor de red durante la primera
  composición de la pantalla de inicio; la mejora está cubierta por compilación
  y tests, pero la medición final de arranque en Fire TV queda pendiente de una
  reconexión ADB estable.
- Android presenta como credenciales inválidas las respuestas `400`, `401` y
  `403` de `AuthenticateByName`; el contrato está cubierto por prueba unitaria.
- Verificación del 2026-09-08: `:app:testMobileDebugUnitTest` y
  `:app:testTvDebugUnitTest` pasan; `node web/scripts/test-platform.mjs` también
  pasa, incluyendo agrupación de canales Live TV, selección de fuentes,
  seguridad web y preferencias de subtítulos.
- El smoke test autenticado contra el Jellyfin LAN respondió correctamente a
  `/System/Info/Public` (HTTP 200), pero `AuthenticateByName` devolvió HTTP 400;
  el test lo clasifica ahora como credenciales rechazadas sin imprimir el cuerpo
  de la respuesta. La prueba de canales y `PlaybackInfo` queda pendiente hasta
  que exista una credencial válida.
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
- Android conserva también `ChannelType` y `ServiceName` de Jellyfin para que
  el selector pueda rotular variantes como `IPTV` o por proveedor; solo cae a
  `Opción N` cuando no existe ningún nombre legible.
- Web/Smart TV abre ahora ese selector al pulsar o confirmar con teclado una
  fila agrupada; los canales sin variantes siguen entrando directamente.
- Web/Smart TV también prioriza el nombre de `MediaSources` al rotular cada
  variante; la prueba de interacción y seguridad web volvió a pasar tras
  este ajuste (`c15ae02`). El bundle web se generó correctamente en un
  directorio QA aislado.
- Las variantes sin nombre legible ya no muestran IDs técnicos en el selector
  de Live TV: se presentan como opciones numeradas, conservando el ID solo
  para solicitar la fuente correcta a Jellyfin.
- Web/Smart TV: el reproductor permite cambiar de forma real entre Ajustar,
  Rellenar y Original, también en pantalla completa, con la preferencia
  guardada localmente y textos traducidos en los 12 idiomas web.
- La comprobación de disponibilidad de Live TV en el inicio libera su cliente
  HTTP al cambiar de sesión o salir de la pantalla.
- El descubrimiento de Jellyfin prioriza ahora HTTP para IPs locales con puerto
  explícito (como el endpoint LAN habitual 8096), evitando consumir primero el
  timeout TLS sobre un puerto HTTP; HTTPS sigue disponible como fallback.
- La pantalla de conexión usa recursos localizados para título, dirección,
  descubrimiento, detección automática, selección de servidor y errores; los
  catálogos garantizados español, inglés, francés y alemán mantienen las 452
  claves sincronizadas.
- Los avisos de trailers que aparecen en las fichas de películas y series
  también usan recursos localizados; la cobertura Android garantizada queda en
  452 claves.
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
- Apple: la biblioteca inicial se solicita en páginas de 100 elementos y la
  cuadrícula pide la siguiente página al alcanzar el final, sin imponer un
  límite artificial al catálogo.
- Apple: las páginas de catálogo ya no solicitan `MediaSources` completos por
  tarjeta; la decisión de reproducción se resuelve al iniciar la reproducción.
- Apple: la carga incremental también funciona con respuestas Jellyfin que no
  incluyen `TotalRecordCount`, sin truncar el catálogo a la primera página.
- La migración Android desde el índice JSON antiguo conserva también checksum,
  fuente, trabajo, progreso, fechas, estado de visto y protección de descarga
  antes de pasar a SQLite.
- Auditoría multiplataforma de offline: Android TV/Fire TV bloquea tanto la
  navegación como la apertura directa de descargas; Apple TV no renderiza las
  acciones ni inicializa la transferencia; web, Tizen, webOS y VIDAA mantienen
  `supportsOfflineDownloads: false`. La regla queda cubierta por tests de
  capacidades Android/Apple/web y por un guardia adicional en el reproductor
  Android que rechaza intents locales en builds de TV.
- Las descargas de subtítulos externos siguen la misma regla: sus resultados,
  almacenamiento y ajustes de OpenSubtitles solo se muestran en móvil/tablet;
  las superficies de TV conservan únicamente las pistas que ya entrega
  Jellyfin.
- Android migra credenciales a Keystore; Apple usa Keychain; el cliente web
  usa su proxy autenticado sin poner tokens en la URL del vídeo.
- El proxy multimedia web aplica una segunda limpieza de parámetros sensibles
  (`api_key`, `access_token`, `token` y equivalentes), sin depender de la
  capitalización, antes de reenviar la reproducción al servidor; el cliente
  aplica la misma defensa antes de entregar el destino al proxy.
- El fallback MPV Android comparte el constructor de cabeceras MediaBrowser
  con la ruta de reproducción y sus pruebas de seguridad siguen pasando.
- Android: Media3/ExoPlayer y MPV solo envían cabeceras Jellyfin a recursos
  del servidor configurado; una URL directa externa de Live TV o tráiler no
  recibe el token y Live TV vuelve al endpoint HLS del servidor cuando existe.
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
- Android: el selector de formato del reproductor declara explícitamente el
  rol de botón, igual que el resto de controles, para que tacto, mando y
  tecnologías de asistencia reciban la misma interacción.
- Android TV: Live TV se integra en la misma fila centrada de navegación que
  las bibliotecas de películas y series; la pestaña sigue siendo condicional a
  los canales visibles para el usuario.
- Android móvil/tablet: la navegación inferior mantiene destinos condicionales
  y ahora expone foco, rol de botón y estado visual para toque, teclado, mando
  y tecnologías de asistencia.
- Live TV: la normalización Android y web descarta duplicados idénticos sin
  `MediaSource`, pero conserva cada fuente identificada como una opción
  seleccionable.
- Live TV: Android y web eligen como fila principal la variante del canal con
  metadatos más completos (programa actual, favorito, imagen o número), para
  que una fuente alternativa no oculte el estado visible del canal.
- Web/Smart TV: la agrupación usa mapas sin prototipo y está cubierta frente a
  identificadores de proveedor con nombres especiales.
- Hay catálogos localizados Android, Apple y web con selección manual y locale
  del sistema como valor inicial.
- Apple: el reproductor nativo de la ficha ofrece ahora un control explícito
  de pantalla completa y una vista AVPlayer inmersiva para iPhone/iPad/tvOS;
  los textos de entrada y salida están presentes en los cuatro catálogos.
  La compilación Swift sigue pendiente de macOS/Xcode, por lo que esta mejora
  queda documentada como código validado estáticamente, no como prueba de
  hardware Apple.
- Apple: el reproductor nativo añade un selector funcional de relación de
  aspecto (`Ajustar`, `Rellenar` y `Original`) tanto en la ficha como en la
  vista de pantalla completa; sus cuatro etiquetas están localizadas.
- Apple Live TV: el reproductor incrustado comparte ahora la pantalla completa
  y el selector de aspecto con las fichas, conservando la misma sesión AVPlayer
  y evitando iniciar una segunda emisión.
- Apple: la tuerca del reproductor ya expone selección real de audio y
  subtítulos sobre el `AVPlayerItem`, incluida la desactivación de subtítulos,
  en fichas, Live TV y pantalla completa; las etiquetas están localizadas.
- Apple: las fichas pueden resolver la primera música de tema gestionada por
  Jellyfin cuando el ajuste está activado y reproducirla con un `AVPlayer`
  autenticado mediante cabeceras, sin incluir el token en la URL. El ciclo de
  vida se detiene al salir de la ficha; la compilación y prueba de AVPlayer
  siguen pendientes de macOS/Xcode.

## Pruebas locales pasadas

- Corrección de filtrado Live TV validada el 2026-09-08: Android móvil y TV
  agrupan primero y filtran después, por lo que favoritos o grupos no eliminan
  fuentes alternativas del mismo canal. `:app:testMobileDebugUnitTest` y
  `:app:testTvDebugUnitTest` terminaron en `BUILD SUCCESSFUL`; el cliente web
  también cubre este caso en `web/scripts/test-platform.mjs`.

- La semántica de grupos Live TV quedó alineada entre Android y web: además de
  etiquetas, los filtros reconocen `ChannelType` y `ServiceName` (incluidos
  proveedores IPTV). La prueba focalizada Android y la suite web terminaron
  correctamente tras este ajuste.

- Revalidación del ajuste de inicialización lazy HTTP del 2026-09-08:
  `:app:testMobileDebugUnitTest` y `:app:testTvDebugUnitTest` terminaron en
  `BUILD SUCCESSFUL`; la instalación del APK TV de QA terminó correctamente,
  pero el dispositivo Fire TV quedó offline durante la medición comparativa,
  por lo que no se declara una mejora de tiempo en hardware.

- Comprobación Fire TV del 2026-09-08: el dispositivo de la red local dejó de
  responder por ADB y la reconexión terminó en timeout 10060; no se declara
  validación de reproducción o Live TV en hardware en esta sesión.

- Revalidación completa posterior a esa alineación: `:app:testMobileDebugUnitTest`
  y `:app:testTvDebugUnitTest` terminaron en `BUILD SUCCESSFUL` el 2026-09-08
  (`1m05s`, 82 tareas; 7 ejecutadas, 75 en caché).

- La deduplicación Live TV conserva ahora la fila con guía, favorito y artwork
  más completos cuando Jellyfin repite el mismo `MediaSource`; Android y web
  tienen cobertura específica y la prueba focalizada Android (`3m30s`) y la
  suite web terminaron correctamente.

- Apple comparte también esa regla de deduplicación: al repetir una fuente,
  conserva la fila con metadatos Live TV más completos y mantiene una sola
  opción de reproducción. Se añadió prueba Swift; la ejecución queda
  pendiente de macOS/Xcode, que no está disponible en este host.

- Se añadió una prueba Swift con `URLProtocol` para la música de tema: verifica
  que Jellyfin recibe el token por cabecera, que la URL final apunta a
  `Audio/<id>/universal` y que no contiene credenciales. Su ejecución queda
  pendiente de macOS/Xcode junto con el resto de la suite Apple.

- Revalidación estática del 2026-09-08 sobre `main` (`e107ef7`): identidad
  pública, coherencia de versión, política offline mobile/tablet-only,
  contrato de autenticación `Pw`, workflows de releases, catálogos Android,
  Apple y web, y `web/scripts/test-platform.mjs` terminaron correctamente.

- Revalidación fresca posterior al commit `d6a7710`: `:app:testMobileDebugUnitTest`
  y `:app:testTvDebugUnitTest` terminaron en `BUILD SUCCESSFUL` en 6m10s.
  También pasaron `check-apple-locales`, `check-public-identity`,
  `check-offline-surface-policy`, `check-android-playback-policy` y
  `web/scripts/test-platform.mjs`.

- `:app:testMobileDebugUnitTest`: `BUILD SUCCESSFUL`.
- `:app:testTvDebugUnitTest`: `BUILD SUCCESSFUL`.
- Revalidación web posterior a la música de tema: `node --check` para la
  aplicación y el service worker, `web/scripts/test-platform.mjs`,
  `check-offline-surface-policy` y `git diff --check` terminaron correctamente.
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
- `:app:testTvDebugUnitTest`: `BUILD SUCCESSFUL` en 11m49s tras corregir el
  orden HTTP/HTTPS del descubrimiento local; se añadieron pruebas unitarias
  para IP local con puerto explícito y URL HTTP completa.
- `:app:testTvDebugUnitTest`: `BUILD SUCCESSFUL` en 4m42s tras localizar los
  mensajes de la pantalla de conexión y pasar el catálogo Android a 450 claves.
- `:app:testMobileDebugUnitTest`: `BUILD SUCCESSFUL` en 3m28s después del
  ajuste compartido del selector de fuentes Live TV.
- `:app:testMobileDebugUnitTest` y `:app:testTvDebugUnitTest`: `BUILD
  SUCCESSFUL` en 5m52s tras localizar el aviso de configuración de trailers.
- Verificación fresca sobre `main` (`56be881`): `:app:testMobileDebugUnitTest`
  y `:app:testTvDebugUnitTest` terminaron en `BUILD SUCCESSFUL` en 6m07s;
  la caché aislada de Gradle se creó correctamente.
- Verificación posterior al ajuste de accesibilidad: ambas suites terminaron
  en `BUILD SUCCESSFUL` en 11m30s.
- El empaquetado web se verificó en una salida aislada: el bundle Samsung se
  preparó sin declarar un WGT al no estar instalado Tizen Studio, webOS generó
  un IPK y VIDAA generó el bundle HTML5; el script acepta ahora una carpeta de
  salida explícita para evitar que artefactos bloqueados contaminen otro build.
- Se eliminaron del código de ejemplo y del dashboard las direcciones privadas
  y credenciales usadas durante pruebas locales; los ejemplos usan ahora
  dominios reservados para documentación. `ServerUrlValidatorTest` y
  `VeloraLocaleTest` terminaron en `BUILD SUCCESSFUL`.
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
- Compilación Android release focalizada del 2026-09-06: `:app:assembleMobileRelease`
  y `:app:assembleTvRelease` terminaron en `BUILD SUCCESSFUL`; la firma no se
  declara verificada en este host y, por tanto, no se publica ningún APK como
  release de distribución desde esta ejecución.
- `apksigner verify` confirmó explícitamente que ambos artefactos actuales son
  unsigned (`DOES NOT VERIFY`); quedan correctamente restringidos a QA.
- `git diff --check`: sin errores en los cambios publicados.
- `node scripts/check-version-consistency.mjs`: versiones coherentes en `1.4.0`.
- `node scripts/check-jellyfin-auth-contract.mjs`: contrato `Pw` coherente en
  Android, Apple, web y smoke test.
- `node scripts/check-android-locales.mjs`: 452 claves en los catálogos Android.
- `node scripts/check-apple-locales.mjs`: 58 claves coherentes en `en`, `es`,
  `fr` y `de`.
- `node scripts/check-web-locales.mjs`: 69 claves efectivas coherentes en los
  12 idiomas de la web, incluyendo los fallbacks y overrides traducidos.
- `:app:testMobileDebugUnitTest`, `:app:testTvDebugUnitTest`,
  `:app:compileMobileDebugKotlin` y `:app:compileTvDebugKotlin`: correctos tras
  blindar la ruta de subtítulos offline para superficies TV.
- Ejecución focalizada del 2026-09-06: `AspectPresentationTest` y
  `LiveTvChannelQueryTest` terminaron en `BUILD SUCCESSFUL`; cubren la
  presentación de aspecto en los distintos contenedores y la agrupación de
  variantes de un canal bajo un único elemento seleccionable.
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
- Web: la sesión y las preferencias toleran navegadores TV/modos privados que
  bloquean `localStorage`; los tokens solo se mantienen en `sessionStorage` y
  la prueba web cubre el fallo de almacenamiento sin impedir el arranque.
- Batería estática completa del 2026-09-06: identidad pública, versiones,
  locales Android/Apple, privacidad Apple, política offline, contrato de
  autenticación Jellyfin, releases y pruebas web terminaron correctamente.
- `node web/scripts/build-web.mjs all` y `node scripts/check-tv-packaging-output.mjs`
  terminaron correctamente; se generó el IPK webOS y los bundles Samsung y
  VIDAA. Tizen sigue marcado como no instalable hasta disponer de Tizen
  Studio/CLI y firma, sin presentarlo como paquete certificado.
- Apple: la cabecera de autenticación usa ahora el idioma seleccionado por el
  usuario; la compilación Swift queda pendiente de la ejecución macOS de CI.
- Apple: el inicio de sesión distingue y localiza credenciales rechazadas,
  servidor no válido y respuesta inesperada, sin exponer detalles de red ni
  secretos.
- Web/Smart TV: el inicio de sesión clasifica `401/403` como credenciales
  inválidas y separa dirección inválida de fallo de conexión; la prueba web
  cubre explícitamente esas ramas sin imprimir la respuesta del servidor.
- Apple: la carga incremental de bibliotecas grandes queda implementada en el
  cliente Swift; falta confirmar la compilación y las pruebas en macOS.
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
  evita publicar APKs por comodín. La ejecución manual queda limitada también a
  etiquetas públicas `vX.Y.0`, igual que el disparador automático.
- Smoke real contra el servidor Jellyfin local proporcionado el 2026-09-06: la
  información pública del servidor responde, pero la cuenta proporcionada
  devuelve `401` en autenticación; quedan sin certificar con ese servidor el
  catálogo, Live TV y `PlaybackInfo` hasta disponer de credenciales válidas.
- `node scripts/check-offline-surface-policy.mjs`: correcto; verifica el límite
  mobile/tablet-only en Android, Apple, navegador y Smart TV, incluida la
  frontera del reproductor Android.
- La validación de releases cubre también la limpieza correcta de artefactos
  obsoletos cuando se reconstruye una etiqueta mediante ejecución manual.
- `node scripts/check-public-identity.mjs`: correcto; no hay referencias
  heredadas de identidad en el contenido público revisado.
- `node web/scripts/test-platform.mjs` y `node web/scripts/build-web.mjs all`:
  correctos; el bundle web y el IPK de webOS se regeneraron. Tizen sigue
  pendiente por falta de Tizen Studio/CLI y firma.
- Revalidación del estado actual: `node web/scripts/build-web.mjs all` y
  `node scripts/check-tv-packaging-output.mjs` correctos; se regeneraron los
  metadatos de Samsung, webOS y VIDAA. La ausencia de Tizen Studio/CLI se
  mantiene como limitación explícita, no como paquete instalable simulado.
- Revalidación adicional del 2026-09-06: el bundle Samsung quedó marcado como
  `packaging: bundle`, webOS generó un IPK real con `installablePackage: true`
  y VIDAA quedó marcado como `hosted-html5`; ninguno se presenta como paquete
  firmado o certificado de tienda.
- Batería completa reejecutada en el estado publicado `514261e`: versiones,
  identidad, contrato Jellyfin, catálogos Android/Apple/web, privacidad Apple,
  workflows de release, política offline, seguridad web, tests de interacción
  y metadatos Smart TV correctos. El IPK webOS se regeneró; Tizen continúa
  pendiente de su SDK y firma.

- Tests Android reejecutados en `main`: `testMobileDebugUnitTest` y
  `testTvDebugUnitTest` correctos. El bundle web y el IPK webOS también se
  regeneraron correctamente; Samsung queda como bundle porque este host no
  tiene Tizen Studio/CLI.
- La prueba `LibraryContentQueryTest` cubre ahora bibliotecas sintéticas de
  1.000, 10.000 y 50.000 elementos en las variantes móvil y TV; valida que
  el filtrado de reproducción/género y el ordenado sigan siendo deterministas.
- `LiveTvChannelQueryTest` y `node web/scripts/test-platform.mjs` cubren que
  una variante con programa actual/favorito sea la fila principal sin perder
  las fuentes restantes.
- `MediaUrlSecurityTest` cubre el alcance por esquema, host, puerto y prefijo
  de ruta para impedir que una fuente externa reciba cabeceras Jellyfin.
- `PlaybackBackendTest` cubre la regla de reproductor: Media3/ExoPlayer es el
  backend inicial, MPV requiere elección explícita y el fallback solo se activa
  después de un error real de decodificación. Las suites móvil y TV volvieron a
  pasar el 2026-09-06.
- `node scripts/check-android-playback-policy.mjs` protege además por CI que la
  preferencia nueva no cambie accidentalmente el backend inicial.
- Revalidación de continuación del 2026-09-06: `:app:testMobileDebugUnitTest`
  y `:app:testTvDebugUnitTest` terminaron en `BUILD SUCCESSFUL` en 49 s;
  las comprobaciones web, identidad, versiones, locales, privacidad Apple,
  política offline, autenticación Jellyfin y workflows de release también
  terminaron correctamente.

## Artefactos Android locales

Son builds `unsigned` para QA, no releases de distribución:

Builds debug regeneradas tras la validación de entrega (QA, 2026-09-06):

- Móvil: `app/build/outputs/apk/mobile/debug/app-mobile-debug.apk`,
  SHA-256 `50D0DA51C614A42BAB37DB5D35866AACA6EBB4A65771A0CF57B621596FFACB47`.
- TV: `app/build/outputs/apk/tv/debug/app-tv-debug.apk`,
  SHA-256 `79679AF438E098176F1A44936A5E9FD348B8583376A99862618E2F7CCDDD510A`.

Regeneración posterior al ajuste de accesibilidad del reproductor (QA,
commit `adc1a5f`):

- Móvil: `app/build/outputs/apk/mobile/debug/app-mobile-debug.apk`,
  SHA-256 `535933CBBBBB9217FBEAEB450280E0415481E09C45FED2B2283FB070F5394E1C`.
- TV: `app/build/outputs/apk/tv/debug/app-tv-debug.apk`,
  SHA-256 `1529907A1ABF0A0EF1E06F2F6C42CE822EDBE71B9D279D609BFC44C61431564C`.

Build TV de QA posterior al ajuste de descubrimiento local (commit `74f70e7`):
`app/build/outputs/apk/tv/debug/app-tv-debug.apk`, SHA-256
`722D77BC445FF1232737CCCFD550BFFBAFA946577BE5795B59FD892252A02EA9`.

Build TV de QA posterior al ajuste de etiquetas de fuentes Live TV:
`app/build/outputs/apk/tv/debug/app-tv-debug.apk`, SHA-256
`409FCF24B6C218F67808D964483EA39340A23E5306D7847F30CCEF02C6CAE8C9`.
Se instaló correctamente en el mismo Fire TV AFTSS y la actividad principal
permaneció activa tras el arranque explícito.

Build TV de QA posterior a la localización del aviso de trailers:
`app/build/outputs/apk/tv/debug/app-tv-debug.apk`, SHA-256
`ECEF2930F02CD4AADF194C5D41B4CA726567ACBDC9BE89CD9DB2BC3ACBDAC714`.
Se instaló correctamente y el proceso de la actividad quedó activo; no se
detectaron excepciones fatales en el logcat de arranque.

La compilación conjunta `:app:assembleMobileDebug :app:assembleTvDebug`
terminó en `BUILD SUCCESSFUL` en 9m24s. Siguen siendo APK unsigned de QA.

Ambas variantes se generaron correctamente en QA el 2026-09-06. La variante TV
se instaló correctamente mediante ADB en un Fire TV AFTSS (1920×1080) y la
actividad principal arrancó; la pantalla de conexión, el campo de dirección y
los botones se verificaron visualmente. No se considera una certificación de
reproducción ni de Live TV hasta completar una sesión Jellyfin autenticada.

La compilación conjunta `:app:assembleMobileDebug :app:assembleTvDebug`
terminó en `BUILD SUCCESSFUL` el 2026-09-06. La regeneración web conjunta y
`check-tv-packaging-output.mjs` también terminaron correctamente: Samsung se
mantiene como bundle sin Tizen Studio, webOS genera un IPK y VIDAA un bundle
HTML5.

Comprobación adicional en el Fire TV AFTSS del 2026-09-06: el cliente
descubrió desde el dispositivo el endpoint HTTP local de Jellyfin
el servidor Jellyfin de la red local y llegó correctamente a la pantalla de inicio de
sesión. La llamada posterior a `Users/AuthenticateByName` terminó en
`HttpRequestTimeoutException` tras 30 segundos; por ello esta ejecución no
certifica catálogo, Live TV ni reproducción autenticada. El proceso de Velora
permaneció activo y no hubo excepción fatal.

La carga inicial de las pantallas de películas y series ya no recorre la
biblioteca completa mientras se muestran las recomendaciones. Se carga una
primera página para que la transición sea inmediata y el catálogo completo se
solicita solo al abrir la pestaña de biblioteca, manteniendo disponibles el
ordenado y los filtros completos.

La accesibilidad web/Smart TV también se ha reforzado: las tarjetas de
filmografía exponen su título a lectores de pantalla, conservan activación por
teclado/mando y Escape cierra los paneles modales o el reproductor activo.

Revalidación fresca posterior a la localización del error de descarga de
subtítulos (2026-09-06): `:app:testMobileDebugUnitTest` y
`:app:testTvDebugUnitTest` terminaron en `BUILD SUCCESSFUL` en 7m17s. El
mensaje de reserva ya no está escrito en inglés en las pantallas de películas
y episodios; queda cubierto por los cuatro catálogos Android garantizados, con
452 claves cada uno. La compilación emitió únicamente avisos de APIs
deprecadas, sin errores.

Build TV de QA posterior a esa corrección: `:app:assembleTvDebug` terminó en
`BUILD SUCCESSFUL` el 2026-09-06. APK
`app/build/outputs/apk/tv/debug/app-tv-debug.apk`, SHA-256
`183A45E568BBB3322C870698E8CB59D81F0E18D4A6B9382A4B02E49E791CE3CA`.
Se instaló correctamente por ADB en el Fire TV AFTSS y la actividad principal
quedó activa; el log de arranque no mostró `FATAL EXCEPTION` ni
`AndroidRuntime`.

- Móvil: `app/build/outputs/apk/mobile/release/velora-release-unsigned.apk`
  SHA-256 `06EF392D2A0FD6C09868D4E934BBF2B46672125235B5347A147FC052B93CB1EA`.
- TV: `app/build/outputs/apk/tv/release/velora-release-unsigned.apk`
  SHA-256 `AD1C8525CA7A867B2D71AEF1678D3176128C2F7F461B79B4B743AE69976FE857`.

Las variantes release se regeneraron correctamente después del selector
centralizado de backend el 2026-09-06. Siguen sin firma: `apksigner` las rechaza
con `Missing META-INF/MANIFEST.MF`, por lo que no se publican como release.

Las releases etiquetadas `vX.Y.0` exigen secretos de firma Android. Sin ellos,
el workflow falla deliberadamente en vez de publicar un APK no instalable como
release pública. El changelog de release está en castellano y agrupa cambios
relevantes.

## Plataformas

| Plataforma | Estado verificable |
| --- | --- |
| Android móvil/tablet | Compila y pasa tests unitarios locales |
| Android TV / Fire TV | Compila y pasa tests unitarios; pantalla de acceso y transporte de autenticación validados en Fire TV AFTSS; sesión Jellyfin válida, reproducción y Live TV aún pendientes |
| Web | Bundle y tests locales correctos |
| Samsung Tizen | Bundle preparado; falta Tizen Studio, firma y dispositivo/emulador |
| LG webOS | IPK generado con `ares-package`; falta dispositivo/emulador |
| Hisense VIDAA | Bundle HTML5 y metadatos preparados; falta portal/certificación |
| iPhone/iPad/tvOS | Base Swift Package y CI macOS configuradas; no hay Xcode en este host |

La validación Apple se ejecuta en `macos-14` mediante GitHub Actions. No se
declara certificación de tienda ni de hardware sin esa ejecución o dispositivo.

## Jellyfin y hardware conectado

- En la revalidación del 2026-09-06, la raíz del servidor configurado respondió
  la WebGUI de Unraid; el endpoint Jellyfin correcto quedó localizado en el
  puerto HTTP configurado y su información pública devuelve Jellyfin `10.11.11`.
  La autenticación en ese endpoint devuelve `HTTP 401` con las credenciales
  probadas, por lo que todavía no se certifican catálogo, Live TV,
  `PlaybackInfo` ni reproducción real. No se publica aquí la dirección privada.
- El smoke test identifica la etapa (`información pública`, `autenticación`,
  `listado Live TV` o `PlaybackInfo`), exige JSON Jellyfin en
  `/System/Info/Public`, elimina espacios accidentales de la URL y no registra
  credenciales ni tokens.
- El smoke test valida que `/System/Info/Public` sea JSON Jellyfin, elimina
  espacios accidentales de la URL y conserva el diagnóstico de respuestas sin
  código HTTP, sin registrar credenciales ni tokens.
- Comprobación ADB del 2026-09-06: Fire TV AFTSS conectado por ADB; instalación
  del APK TV de QA correcta, arranque explícito de `MainActivity` correcto y
  captura/UIAutomator confirmaron la pantalla de conexión a 1920×1080.
- En la misma sesión, el servidor indicado fue alcanzable por red y por el
  puerto configurado, pero la validación de `System/Info/Public` no devolvió un
  servidor Jellyfin aceptable para Velora. La app mostró el error de conexión
  sin cerrarse; no se continúa con credenciales ni se certifican catálogo,
  Live TV, `PlaybackInfo` o reproducción.
- Intento autenticado adicional del 2026-09-06 contra el servidor indicado:
  la primera llamada de login respondió `400` y el reintento con la cabecera
  oficial de cliente terminó por timeout; no se marca como válida ninguna
  prueba de catálogo, Live TV, `PlaybackInfo` o reproducción y no se guardaron
  credenciales ni tokens.
- Revalidación del endpoint LAN indicado en la misma fecha: la información
  pública de Jellyfin respondió correctamente y el puerto HTTP estaba abierto,
  pero la petición de autenticación volvió a agotar el tiempo de espera desde
  el host de QA. Por ello siguen sin certificarse catálogo, Live TV,
  `PlaybackInfo` o reproducción real.
- Prueba limpia en Fire TV AFTSS del 2026-09-08 con la build de QA
  `1C4B8469341E85A9B9181754D4EBFAFD687E1BD95A7ECCAC2CE910AB5327E3DD`:
  tras borrar los datos locales, descubrir el servidor y enviar el formulario
  de contraseña, el cliente recibió inmediatamente `HTTP 401` y mostró el
  error de credenciales en español. El cambio de autenticación a OkHttp evita
  el bloqueo/timeout observado con el transporte Android anterior; la sesión
  no fue aceptada por el servidor y por tanto siguen sin certificarse catálogo,
  Live TV, `PlaybackInfo` o reproducción. No se registraron credenciales ni
  tokens.
- Tras el ajuste de identidad de cliente de reproducción (`bf6db85`), la suite
  de compilación de las variantes móvil y TV y sus tests unitarios vuelven a
  pasar. Esto valida el contrato de código, pero no sustituye la prueba de
  reproducción con una sesión Jellyfin autenticada.
- La suite Android móvil y TV volvió a pasar después de esta mejora del
  selector Live TV: `:app:testMobileDebugUnitTest` y
  `:app:testTvDebugUnitTest` terminaron con `BUILD SUCCESSFUL` el 2026-09-08.
- Empaquetado web multiplataforma en QA aislado: el bundle Samsung/Tizen y el
  bundle VIDAA se generan correctamente, pero Tizen Studio/CLI no está
  instalado y por eso no se presenta un `.wgt`; `ares-package` sí generó el
  `.ipk` webOS `com.klortek.velora_1.4.0_all.ipk`. Ninguno de estos resultados
  certifica firma, tienda ni ejecución en hardware real.
- Apple Live TV conserva ahora `ChannelType`, `ServiceName` y `MediaSource.Name`
  para rotular el selector de variantes sin inspeccionar ni mostrar rutas de
  reproducción. Se añadió cobertura de decodificación; la ejecución Swift
  sigue pendiente de macOS/Xcode en CI.
- En Apple, tocar una fila con varias fuentes abre ahora directamente el
  selector; los canales de una sola fuente reproducen al instante. El selector
  usa un diálogo nativo con foco/remote y textos localizados, sin depender de
  un icono secundario.
- Build TV de QA instalada en Fire TV AFTSS el 2026-09-08 desde el estado
  `6ff83ef`, SHA-256 `5CACDBD89C02A94B4E854FF762CD9458EA13929836917E680C62388C573A72EE`.
  La actividad principal arrancó y el logcat no mostró `FATAL EXCEPTION`,
  `AndroidRuntime` ni `ANR`; no se interpreta como certificación de catálogo,
  Live TV o reproducción autenticada.
- La APK TV del commit `6c512be` se recompiló e instaló después con SHA-256
  `9F7E55ED62F3C9D334A56E0301CA4D483EAAC8F7ABF75D78DACA0DB396959050` y volvió
  a arrancar en `MainActivity` sin `FATAL EXCEPTION`, `AndroidRuntime` ni `ANR`.
- Revalidación autenticada del 2026-09-08 contra el endpoint Jellyfin activo:
  la información pública responde, pero el contrato `Pw` usado por Velora
  recibe `HTTP 400 Error processing request` desde el servidor. La APK TV
  instalada queda enfocada en `MainActivity` sin excepciones fatales; no se
  marca como válida ninguna prueba posterior de catálogo, Live TV o
  reproducción hasta que la autenticación sea aceptada.
- Diagnóstico adicional del mismo endpoint: las solicitudes controladas con el
  contrato estándar `Pw` y con el campo alternativo `Password` reciben ambas
  `HTTP 400`. Esto descarta un simple desajuste de nombre de campo en Velora;
  el rechazo queda atribuido al endpoint/proxy/configuración del servidor sin
  registrar credenciales, tokens ni cuerpos de respuesta.
- Build TV de QA del commit `2f6184f` instalada en el Fire TV AFTSS el
  2026-09-08: `app-tv-debug.apk`, SHA-256
  `CD2E8482B4817D91213987576A8A820EC216F75190A1D7E2C607AD4DB93608E8`.
  `MainActivity` quedó reanudada y los logs no muestran `FATAL EXCEPTION`,
  `AndroidRuntime` ni `ANR`; esto valida el arranque, no una sesión Jellyfin
  autenticada.

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
