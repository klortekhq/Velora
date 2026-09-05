# Velora Web

Cliente web responsive para móvil, tablet y televisores con navegador. Usa la
API de Jellyfin y el elemento HTML5 `video`, sin depender de otra aplicación.
El cliente web no ofrece descargas ni almacenamiento sin conexión: esa función
está reservada a las aplicaciones móviles nativas.

Ejecutar localmente:

```text
python -m http.server 4173 --directory web
```

Los adaptadores de empaquetado para Samsung Tizen, LG webOS y Hisense VIDAA
están documentados en `web/platforms/`. Todos comparten el cliente HTML5 y la
capa de mando/teclas de Velora. Los SDK y certificados de fabricante no se
incluyen y el resultado debe validarse en hardware real.

Comandos de empaquetado:

```text
npm run build:samsung
npm run build:tizen
npm run build:webos
npm run build:vidaa
npm run build:all
```

`build:tizen` es el alias explícito del empaquetado Samsung Tizen; por
compatibilidad, ambos comandos dejan la preparación en `outputs/web/samsung`.

VIDAA genera un bundle HTML5 y su ficha de envío; la publicación oficial no
usa un `.vpk` universal y debe tramitarse mediante el portal VIDAA para los
modelos y regiones aprobados.
