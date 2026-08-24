# Paquetes Smart TV

`web/` es el cliente común. Se empaqueta por separado para Samsung Tizen,
LG webOS y Hisense VIDAA, conservando el mismo código Jellyfin. Los SDK y
certificados de cada fabricante no se incluyen; el paquete final debe firmarse
con las herramientas oficiales y probarse en hardware real.

Samsung necesita un proyecto web Tizen con `config.xml` y genera `.wgt` con
`tizen build-web` y `tizen package -t wgt`. LG usa `appinfo.json` y genera
`.ipk` con `ares-package`. VIDAA se mantiene parametrizado por modelo porque
el formato y el SDK dependen de la generación del televisor.
