import fs from 'node:fs';
import path from 'node:path';
import assert from 'node:assert/strict';

const root = path.resolve(import.meta.dirname, '..');
const source = fs.readFileSync(
  path.join(root, 'app', 'src', 'main', 'java', 'com', 'klortek', 'velora', 'MainActivity.kt'),
  'utf8'
);

assert.match(source, /window\.setBackgroundDrawableResource\(android\.R\.color\.transparent\)/,
  'MainActivity debe retirar el fondo de splash al entregar el primer frame.');
assert.match(source, /if \(isFirstLaunch\)\s*\{\s*settings\.isFirstLaunch\s*=\s*false/s,
  'El estado de primera ejecución debe seguir persistiendo sin bloquear el arranque.');
assert.doesNotMatch(source, /if \(!isFirstLaunch\)[\s\S]{0,240}setBackgroundDrawableResource/,
  'El fondo de splash no debe retirarse solo en lanzamientos posteriores.');

console.log('Android splash handoff contract passed: first launch cannot retain the splash drawable.');
