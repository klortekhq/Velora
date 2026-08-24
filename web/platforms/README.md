# Paquetes Smart TV

`web/` es el cliente común. Se empaqueta por separado para Samsung Tizen,
LG webOS y Hisense VIDAA, conservando el mismo cliente de Velora y una capa
de control común para mando, OK y Back.

Samsung necesita un proyecto web Tizen con `config.xml` y genera `.wgt` con
`tizen build-web` y `tizen package -t wgt`. LG usa `appinfo.json` y genera
`.ipk` con `ares-package`. VIDAA utiliza aplicaciones HTML5 alojadas y su
publicación oficial se tramita mediante el portal/tienda VIDAA; no hay un
formato universal `.vpk` que pueda generarse sin el SDK y certificado del
fabricante.

Los SDK y certificados no se incluyen. Los paquetes de tienda deben firmarse
con las herramientas oficiales y validarse en hardware real. El cliente evita
pop-ups, funciona a pantalla completa y gestiona las teclas direccionales,
OK y Back exigidas por VIDAA.
