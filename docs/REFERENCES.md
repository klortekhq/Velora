# Referencias técnicas de Velora

Revisado: **2026-10-06**.

Estas referencias sirven para contrastar arquitectura, comportamiento de APIs,
reproducción y experiencia de usuario. Incluir un proyecto aquí no convierte su
código en dependencia de Velora ni autoriza copiar componentes sin revisar su
licencia y procedencia.

## Plezy

- Repositorio: https://github.com/edde746/plezy
- Revisión fijada: `75e33983a17e4e511ebd7feba1997f39e99992bc`
- Licencia observada: GPL-3.0
- Velora: GPL-3.0-or-later

Plezy es un cliente multimedia Flutter multiplataforma con soporte para Plex,
Jellyfin y Emby. Es una referencia especialmente útil para Velora en áreas donde
ambos proyectos resuelven problemas de cliente multimedia reales, aunque con
arquitecturas distintas.

### Áreas útiles

- **Jellyfin / MediaBrowser HTTP API**: rutas, dialectos Jellyfin/Emby, identidad de
  servidor/usuario y manejo de capacidades.
- **WebSocket de sesión**: conexión `/socket`, autenticación y eventos como
  `LibraryChanged`. La revisión fijada incluye una corrección reciente para enviar
  el mismo encabezado MediaBrowser en el upgrade WebSocket y evitar sesiones
  duplicadas por diferencias de identidad de cliente.
- **Reproducción**: separación entre lógica de servidor y backend de reproducción,
  integración MPV en varias plataformas y gestión de capacidades.
- **Descargas/offline**: resolución de descarga independiente del backend, cola,
  metadatos, artwork y estados de progreso.
- **TV y navegación por foco**: patrones útiles para revisar UX de mando, sin copiar
  la interfaz.
- **Pruebas multiplataforma**: tests de sockets, reproducción y empaquetado que pueden
  inspirar casos equivalentes en Velora.

### Regla de uso

Plezy es una **referencia técnica**, no una dependencia automática. Antes de reutilizar
código concreto hay que comprobar licencia por archivo, dependencias y compatibilidad
con la arquitectura nativa de Velora. Cuando solo necesitamos comportamiento o
contratos de Jellyfin, preferimos una implementación independiente apoyada en la API
oficial y pruebas propias.

### Prioridad práctica para Velora

1. contrastar autenticación y ciclo de vida del WebSocket Jellyfin;
2. revisar coherencia entre identidad de cliente HTTP y socket;
3. comparar selección de fuente, Direct Play/Direct Stream/transcoding y reporting
   de sesión;
4. revisar robustez del gestor de descargas y metadatos offline;
5. extraer ideas de tests, no UI ni arquitectura Flutter.
