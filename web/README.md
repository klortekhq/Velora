# Velora Web

Cliente web responsive para móvil, tablet y televisores con navegador. Usa la
API de Jellyfin y el elemento HTML5 `video`, sin depender de otra aplicación.

Ejecutar localmente:

```text
python -m http.server 4173 --directory web
```

Los adaptadores de empaquetado para Samsung Tizen, LG webOS y Hisense VIDAA
están documentados en `web/platforms/`. Los SDK y certificados de fabricante
no se incluyen y el resultado debe validarse en hardware real.
