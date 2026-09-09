import fs from 'node:fs';
import path from 'node:path';
import assert from 'node:assert/strict';

const root = path.resolve(import.meta.dirname, '..');
const sourcePath = path.join(
  root,
  'app',
  'src',
  'main',
  'java',
  'com',
  'klortek',
  'velora',
  'screens',
  'JellyfinLoginScreen.kt'
);
const source = fs.readFileSync(sourcePath, 'utf8');

assert.match(source, /\.systemBarsPadding\(\)/,
  'El acceso debe respetar las barras del sistema.');
assert.match(source, /\.imePadding\(\)/,
  'El acceso debe dejar espacio al teclado en móvil.');
assert.match(source, /\.verticalScroll\(rememberScrollState\(\)\)/,
  'El formulario debe poder desplazarse en pantallas bajas.');
assert.match(source, /fieldHeight\s*=\s*if \(isTv\) 72\.dp else 48\.dp/,
  'Los campos deben adaptar su tamaño al modo TV.');
assert.match(source, /\.height\(72\.dp\)[\s\S]{0,900}\.focusRequester\(loginButtonFocusRequester\)/,
  'El botón de acceso TV debe tener un tamaño estable y foco de mando.');
assert.match(source, /maxLines\s*=\s*1[\s\S]{0,180}softWrap\s*=\s*false/,
  'El texto del botón de acceso TV no debe recortarse ni partirse.');
assert.match(source, /onClick\s*=\s*onLogin/,
  'El botón visible debe ejecutar el inicio de sesión real.');

console.log('Android login layout contract passed: safe area, scroll, sizing and TV focus are protected.');
