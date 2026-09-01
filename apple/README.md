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
inicio de sesión, biblioteca, detalle, ajustes y AVPlayer; en tvOS no muestra
acciones de descarga. Las futuras apps iOS/iPadOS y tvOS deben consumir este
paquete sin copiar la
interfaz Android; el target de tvOS debe omitir descargas mediante
`VeloraPlatform.tvOS.supportsOfflineDownloads`. La compilación y las pruebas
requieren macOS con Xcode; este host Windows no certifica todavía esos targets.
Los textos del shell se sirven mediante recursos nativos `Localizable.strings`
en español, inglés, francés y alemán; la opción automática sigue el idioma del
dispositivo y una selección explícita se aplica al entorno SwiftUI.

`JellyfinClient` también expone canales y programación de Live TV con consultas
acotadas por usuario y ventana temporal. La reproducción de canal se mantiene
separada de la reproducción VOD hasta disponer de su ruta AVPlayer específica;
no se presentan controles de Live TV que aún no tengan acción real.

Los ajustes del shell se persisten de forma local mediante `VeloraSettingsStore`
usando datos Codable. La sesión autenticada se guarda mediante
`VeloraCredentialStore`: en Apple usa Keychain con accesibilidad
`AfterFirstUnlockThisDeviceOnly`; el adaptador de `UserDefaults` solo se usa en
entornos de pruebas que no disponen de Security. Cerrar sesión elimina la
credencial. Ninguno de estos datos se sincroniza con Jellyfin ni entre
dispositivos. Las sesiones antiguas sin servidor asociado no se restauran para
evitar enviar accidentalmente un token a otra instancia; basta con iniciar
sesión una vez de nuevo.
