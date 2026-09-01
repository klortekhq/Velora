# Velora para Apple

Este directorio contiene la base nativa compartida para iOS, iPadOS y tvOS.
Usa Swift/SwiftUI y deja la reproducción a AVFoundation/AVPlayer en las apps
finales. `VeloraPlatform` mantiene la decisión de producto: las descargas sin
conexión solo están disponibles en iPhone y iPad; tvOS es streaming.

`VeloraKit` no mete tokens en URLs: las peticiones autenticadas usan
`X-Emby-Token`. Incluye autenticación Jellyfin, sesión tipada, preferencias
locales de idioma/audio/subtítulos/rendimiento, requests autenticadas para
artwork o AVPlayer y componentes SwiftUI adaptativos para bibliotecas y ajustes.
Las futuras apps iOS/iPadOS y tvOS deben consumir este paquete sin copiar la
interfaz Android; el target de tvOS debe omitir descargas mediante
`VeloraPlatform.tvOS.supportsOfflineDownloads`. La compilación y las pruebas
requieren macOS con Xcode; este host Windows no certifica todavía esos targets.
