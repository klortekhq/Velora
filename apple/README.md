# Velora para Apple

Este directorio contiene la base nativa compartida para iOS, iPadOS y tvOS.
Usa Swift/SwiftUI y deja la reproducción a AVFoundation/AVPlayer en las apps
finales. `VeloraPlatform` mantiene la decisión de producto: las descargas sin
conexión solo están disponibles en iPhone y iPad; tvOS es streaming.

`VeloraKit` no mete tokens en URLs: las peticiones autenticadas usan
`X-Emby-Token` y la autenticación inicial usa el contrato estándar de Jellyfin
(`Password` y `X-Emby-Authorization`). Incluye autenticación Jellyfin, sesión tipada, preferencias
locales de idioma/audio/subtítulos/rendimiento, requests autenticadas para
artwork o AVPlayer y componentes SwiftUI adaptativos para bibliotecas y ajustes.
También incluye `VeloraAppShell`, una superficie SwiftUI nativa que conecta
inicio de sesión, biblioteca, detalle, ajustes, AVPlayer y Live TV; en tvOS no
muestra acciones de descarga. Las apps iOS/iPadOS y tvOS deben consumir este
paquete sin copiar la interfaz Android; el target de tvOS debe omitir descargas
mediante `VeloraPlatform.tvOS.supportsOfflineDownloads`. La base compartida y
el shell ya están implementados, pero todavía no hay proyectos `.app` firmados.
La compilación de Apple y la validación en hardware requieren macOS con Xcode;
este host Windows no las certifica.
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

Los ajustes del shell se persisten de forma local mediante `VeloraSettingsStore`
usando datos Codable. La sesión autenticada se guarda mediante
`VeloraCredentialStore`: en Apple usa Keychain con accesibilidad
`AfterFirstUnlockThisDeviceOnly`; el adaptador de `UserDefaults` solo se usa en
entornos de pruebas que no disponen de Security. Cerrar sesión elimina la
credencial. Ninguno de estos datos se sincroniza con Jellyfin ni entre
dispositivos. Las sesiones antiguas sin servidor asociado no se restauran para
evitar enviar accidentalmente un token a otra instancia; basta con iniciar
sesión una vez de nuevo.
