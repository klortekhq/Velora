import fs from 'node:fs';
import path from 'node:path';
import assert from 'node:assert/strict';

const root = path.resolve(import.meta.dirname, '..');
const source = fs.readFileSync(
  path.join(root, 'app', 'src', 'main', 'java', 'com', 'klortek', 'velora', 'screens', 'ServerEntryScreen.kt'),
  'utf8'
);

assert.match(source, /\.statusBarsPadding\(\)/, 'La entrada de servidor debe respetar la barra de estado.');
assert.match(source, /\.navigationBarsPadding\(\)/, 'La entrada de servidor debe respetar la navegación del sistema.');
assert.match(source, /\.then\(if \(isTv\) Modifier\.fillMaxSize\(\) else Modifier\.wrapContentHeight\(\)\)/,
  'El formulario móvil no debe ocupar toda la altura y quedar pegado arriba.');
assert.match(source, /fillMaxWidth\(widthFraction\)/, 'El formulario debe conservar el ancho adaptativo TV/móvil.');

console.log('Server entry layout contract passed: safe areas and adaptive content height are protected.');
