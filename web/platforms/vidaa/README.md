# VIDAA

Velora para VIDAA es una aplicación HTML5 alojada. El bundle de esta carpeta
se sirve desde `index.html` y se presenta a pantalla completa, sin barra del
navegador ni ventanas emergentes.

La capa `platform.js` gestiona las teclas obligatorias del mando: izquierda,
arriba, derecha, abajo, OK y Back. En la página principal, Back solicita el
cierre de la aplicación; dentro de un diálogo o reproductor vuelve a la
pantalla anterior.

La plataforma VIDAA varía por generación, modelo, chipset, región y firmware.
La distribución final se realiza a través del portal/tienda VIDAA y requiere
la validación y firma del fabricante. Este bundle no pretende ser un paquete
`.vpk` universal: es el artefacto HTML5 que se entrega para la publicación.

## Publicación

1. Servir o alojar el contenido generado en `web/vidaa/`.
2. Usar `submission.json` como ficha técnica inicial del envío.
3. Registrar el modelo Hisense y la región de destino en el portal VIDAA.
4. Validar mando, salida con Back, reproducción HTML5, HLS/DASH, audio,
   subtítulos y memoria en cada familia aprobada.

El firmware de referencia del televisor aportado para Velora es
`V0000.09.090.P0930`; la fotografía no contiene el número de modelo, así que
no se declara como certificación automática de toda la gama.
