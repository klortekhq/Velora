import fs from 'node:fs';
import assert from 'node:assert/strict';

const workflows = [
  '.github/workflows/android-release.yml',
  '.github/workflows/web-release.yml'
];

for (const file of workflows) {
  const source = fs.readFileSync(file, 'utf8');
  assert.doesNotMatch(source, /^\s*pull_request\s*:/m, `${file}: no debe publicar desde pull requests`);
  assert.match(source, /^\s*workflow_dispatch\s*:/m, `${file}: falta ejecución manual`);
  assert.match(source, /tags:[\s\S]*?-\s*['"]v\*\.\*\.0['"]/, `${file}: falta el filtro de tags de versión`);
  assert.match(source, /permissions:\s*\n\s*contents:\s*write/, `${file}: falta permiso de publicación explícito`);
  assert.match(source, /softprops\/action-gh-release@v2/, `${file}: falta la acción de release`);
}

const android = fs.readFileSync(workflows[0], 'utf8');
assert.match(android, /dist\/Velora-\$\{\{ matrix\.target \}\}-release\.apk/, 'Android: la release debe seleccionar solo APK release firmado');
assert.doesNotMatch(android, /files:\s*\n\s+dist\/\*\.apk/, 'Android: no debe publicar APKs por comodín');

console.log('Release workflow policy passed: manual/version tag only, no pull requests, no APK wildcard.');
