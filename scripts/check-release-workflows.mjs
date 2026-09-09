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
  assert.match(source, /RELEASE_TAG.*\^v\[0-9\]\+\\\.\[0-9\]\+\\\.0\$/, `${file}: la ejecución manual debe aceptar solo etiquetas vX.Y.0`);
  assert.match(source, /permissions:\s*\n\s*contents:\s*write/, `${file}: falta permiso de publicación explícito`);
  assert.match(source, /softprops\/action-gh-release@v2/, `${file}: falta la acción de release`);
}

const android = fs.readFileSync(workflows[0], 'utf8');
assert.match(android, /dist\/Velora-\$\{\{ matrix\.target \}\}-release\.apk/, 'Android: la release debe seleccionar solo APK release firmado');
assert.doesNotMatch(android, /files:\s*\n\s+dist\/\*\.apk/, 'Android: no debe publicar APKs por comodín');

const updater = fs.readFileSync('app/src/main/java/com/klortek/velora/updater/UpdateService.kt', 'utf8');
assert.match(updater, /Velora-tv-release\.apk/, 'Actualizador: falta el nombre del APK firmado de TV');
assert.match(updater, /Velora-mobile-release\.apk/, 'Actualizador: falta el nombre del APK firmado móvil');
assert.doesNotMatch(updater, /expected\s*=.*release-unsigned\.apk/, 'Actualizador: no debe seleccionar APK unsigned');

const web = fs.readFileSync(workflows[1], 'utf8');
assert.match(web, /releases\/tags\/\$\{RELEASE_TAG\}/, 'Web: la limpieza manual debe resolver la etiqueta solicitada');
assert.doesNotMatch(web, /releases\/tags\/\$\{GITHUB_REF_NAME\}/, 'Web: no debe usar la rama de ejecución para localizar la release');
assert.match(web, /outputs\/web\/samsung\/\*\.wgt/, 'Web: debe publicar el WGT de Tizen cuando el SDK lo genere');
assert.match(web, /Velora-tizen-\$\{version\}\.wgt/, 'Web: falta el nombre estable del paquete Tizen');
assert.match(web, /Velora-samsung-bundle-\$\{version\}\.zip/, 'Web: falta el fallback bundle de Samsung');

const apple = fs.readFileSync('.github/workflows/apple.yml', 'utf8');
assert.match(apple, /tags:\s*\['v\*\.\*\.0'\]/, 'Apple: falta el filtro de tags de versión');
assert.match(apple, /workflow_dispatch:/, 'Apple: falta ejecución manual');
assert.match(apple, /RELEASE_TAG.*\^v\[0-9\]\+\\\.\[0-9\]\+\\\.0\$/, 'Apple: la ejecución manual debe aceptar solo etiquetas vX.Y.0');
assert.match(apple, /permissions:\s*\n\s*contents:\s*write/, 'Apple: falta permiso de publicación explícito');
assert.match(apple, /swift test/, 'Apple: falta la suite Swift');
assert.match(apple, /softprops\/action-gh-release@v2/, 'Apple: falta la publicación en la release común');
assert.match(apple, /Velora-Apple-source-\$\{version\}\.tar\.gz/, 'Apple: falta el paquete fuente verificable');
assert.match(apple, /SHA256SUMS-apple\.txt/, 'Apple: falta el checksum del paquete');

console.log('Release workflow policy passed: Android, Apple y web publican en una release versionada común; sin APK unsigned ni comodines peligrosos.');
