# Velora para Apple

Este directorio contiene la base nativa compartida para iOS, iPadOS y tvOS.
El paquete expone también los entrypoints `VeloraMobile` y `VeloraTV`, que
embeben el shell existente como aplicaciones separadas para que móvil/tablet
y televisión puedan evolucionar con ciclos de vida y navegación propios.
Usa Swift/SwiftUI y deja la reproducción a AVFoundation/AVPlayer en las apps
finales. `VeloraPlatform` mantiene la decisión de producto: las descargas sin
conexión solo están disponibles en iPhone y iPad; tvOS es streaming.

`VeloraKit` no mete tokens en URLs: las peticiones autenticadas usan
`X-Emby-Token` y la autenticación inicial usa el contrato estándar de Jellyfin
(`Pw` y `X-Emby-Authorization`). Incluye autenticación Jellyfin, sesión tipada, preferencias
locales de idioma/audio/subtítulos/rendimiento, requests autenticadas para
artwork o AVPlayer y componentes SwiftUI adaptativos para bibliotecas y ajustes.
También incluye `VeloraAppShell`, una superficie SwiftUI nativa que conecta
inicio de sesión, biblioteca, detalle, ajustes, AVPlayer y Live TV; en tvOS no
muestra acciones de descarga. Las apps iOS/iPadOS y tvOS deben consumir este
paquete sin copiar la interfaz Android; el target de tvOS debe omitir descargas
mediante `VeloraPlatform.tvOS.supportsOfflineDownloads`. La base compartida,
el shell y los entrypoints SwiftPM ya están implementados, pero todavía no hay
proyectos `.app` firmados ni un pipeline de firma.
La compilación de Apple y la validación en hardware requieren macOS con Xcode;
este host Windows no las certifica.
Los entrypoints incluyen una ruta mínima de enlace exclusiva para macOS CI;
no es una aplicación macOS distribuible ni sustituye los targets nativos de
iOS/iPadOS/tvOS.
Los textos del shell se sirven mediante recursos nativos `Localizable.strings`
en español, inglés, francés y alemán; la opción automática sigue el idioma del
dispositivo y una selección explícita se aplica al entorno SwiftUI.

`JellyfinClient` también expone canales y programación de Live TV con consultas
acotadas por usuario y ventana temporal. La reproducción de canal usa una ruta
AVPlayer específica y el shell no presenta controles de Live TV sin acción real.
La ruta `liveTvPlaybackURL` abre el tuner mediante `PlaybackInfo`, valida que la
URL devuelta pertenezca al servidor configurado y entrega un `AVPlayer` con la
cabecera autenticada; tvOS no ofrece descargas.
El shell muestra Live TV solo cuando Jellyfin devuelve canales y cada fila
reproduce en el reproductor integrado; las cargas se cancelan al cambiar rápido
de canal y al salir se notifica `Sessions/Playing/Stopped` para liberar el tuner.

Las tarjetas de biblioteca cargan el artwork directamente desde Jellyfin con
`X-Emby-Token` en la petición; el token no se incluye en la URL. Los estados de
carga y error muestran un placeholder estable para que la navegación siga
siendo usable aunque una imagen no esté disponible.

Los ajustes del shell se persisten de forma local mediante `VeloraSettingsStore`
usando datos Codable. La sesión autenticada se guarda mediante
`VeloraCredentialStore`: en Apple usa Keychain con accesibilidad
`AfterFirstUnlockThisDeviceOnly`; el adaptador de `UserDefaults` solo se usa en
entornos de pruebas que no disponen de Security. Cerrar sesión elimina la
credencial. Ninguno de estos datos se sincroniza con Jellyfin ni entre
dispositivos. Las sesiones antiguas sin servidor asociado no se restauran para
evitar enviar accidentalmente un token a otra instancia; basta con iniciar
sesión una vez de nuevo.

Los entrypoints no apuntan a `localhost`: muestran un servidor inicial
editable (`jellyfin.local:8096`) para evitar intentar conectar con el propio
dispositivo. El servidor real se introduce en el formulario de inicio de
sesión y queda asociado a la sesión guardada.

El cliente valida también el host, puerto y componentes de la URL del servidor
antes de guardar o usar la sesión: no acepta credenciales, query ni fragmentos
embebidos.
