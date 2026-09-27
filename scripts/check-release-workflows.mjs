import fs from 'node:fs';
import assert from 'node:assert/strict';

const workflows = [
  '.github/workflows/android-release.yml',
  '.github/workflows/web-release.yml'
];

const ciSources = [
  '.github/workflows/android-qa-reconcile.yml',
  '.github/workflows/android-release.yml',
  '.github/workflows/apple.yml',
  '.github/workflows/ci.yml',
  '.github/workflows/web-release.yml'
].map((file) => [file, fs.readFileSync(file, 'utf8')]);

for (const [file, source] of ciSources) {
  assert.match(source, /actions\/checkout@v6/, `${file}: debe usar checkout v6`);
  assert.match(source, /actions\/setup-node@v5/, `${file}: debe usar setup-node v5`);
}

for (const file of ['.github/workflows/android-qa-reconcile.yml', '.github/workflows/android-release.yml', '.github/workflows/ci.yml']) {
  const source = fs.readFileSync(file, 'utf8');
  assert.match(source, /actions\/setup-java@v5/, `${file}: debe usar setup-java v5`);
  assert.match(source, /gradle\/actions\/setup-gradle@v6/, `${file}: debe usar setup-gradle v6`);
}

// The executable bit is verified by Git in CI (Windows worktrees expose the
// wrapper as 0644 even when the index correctly stores 100755).

const androidQa = fs.readFileSync('.github/workflows/android-qa-reconcile.yml', 'utf8');
assert.match(androidQa, /cancel-in-progress:\s*true/, 'Android QA: una ejecución obsoleta no debe bloquear main');
assert.match(androidQa, /timeout[^\n]*5m[\s\S]*sdkmanager.*--licenses/, 'Android QA: las licencias SDK deben tener timeout');
assert.match(androidQa, /timeout[^\n]*5m\s+sdkmanager/, 'Android QA: la instalación del SDK debe tener timeout');

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
assert.match(android, /Limpiar APK Android obsoletos de la release/, 'Android: debe limpiar APKs antiguos al reconstruir una release');
assert.match(android, /\^Velora-\(mobile\|tv\)-\.\*\\\\\.apk\$/, 'Android: la limpieza debe limitarse a assets APK de Velora');
assert.match(android, /releases\/assets\/\$\{asset_id\}/, 'Android: la limpieza debe borrar assets por ID');

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
assert.match(web, /Velora-webos-\$\{version\}\.ipk/, 'Web: debe publicar el IPK de webOS cuando ares-package lo genere');
assert.match(web, /Velora-webos-bundle-\$\{version\}\.zip/, 'Web: falta el fallback bundle de webOS');
assert.match(web, /Velora-vidaa-bundle-\$\{version\}\.zip/, 'Web: falta el bundle de VIDAA');
assert.match(web, /SOURCE_DATE_EPOCH=\$\(git log -1 --format=%ct\)/, 'Web: la release debe fijar SOURCE_DATE_EPOCH al commit etiquetado');
assert.match(web, /wait-for-android-release-assets\.sh/, 'Web: debe esperar a los APK Android firmados antes de publicar');
assert.match(web, /RELEASE_SHA:\s*\$\{\{\s*github\.sha\s*\}\}/, 'Web: debe pasar el commit al gate Android');

const apple = fs.readFileSync('.github/workflows/apple.yml', 'utf8');
assert.match(apple, /tags:\s*\['v\*\.\*\.0'\]/, 'Apple: falta el filtro de tags de versión');
assert.match(apple, /workflow_dispatch:/, 'Apple: falta ejecución manual');
assert.match(apple, /RELEASE_TAG.*\^v\[0-9\]\+\\\.\[0-9\]\+\\\.0\$/, 'Apple: la ejecución manual debe aceptar solo etiquetas vX.Y.0');
assert.match(apple, /permissions:\s*\n\s*contents:\s*write/, 'Apple: falta permiso de publicación explícito');
assert.match(apple, /swift test/, 'Apple: falta la suite Swift');
assert.match(apple, /softprops\/action-gh-release@v2/, 'Apple: falta la publicación en la release común');
assert.match(apple, /Velora-Apple-source-\$\{version\}\.tar\.gz/, 'Apple: falta el paquete fuente verificable');
assert.match(apple, /SHA256SUMS-apple\.txt/, 'Apple: falta el checksum del paquete');
assert.match(apple, /wait-for-android-release-assets\.sh/, 'Apple: debe esperar a los APK Android firmados antes de publicar');
assert.match(apple, /RELEASE_SHA:\s*\$\{\{\s*github\.sha\s*\}\}/, 'Apple: debe pasar el commit al gate Android');

const releaseGate = fs.readFileSync('scripts/ci/wait-for-android-release-assets.sh', 'utf8');
assert.match(releaseGate, /Velora-mobile-release\.apk/, 'Release gate: falta el APK móvil firmado');
assert.match(releaseGate, /Velora-tv-release\.apk/, 'Release gate: falta el APK TV firmado');
assert.match(releaseGate, /RELEASE_SHA/, 'Release gate: debe fijar el commit que produjo los APK');
assert.match(releaseGate, /headSha/, 'Release gate: debe comprobar la ejecución Android del mismo commit');
assert.match(releaseGate, /refusing a partial release/i, 'Release gate: debe rechazar releases parciales');

console.log('Release workflow policy passed: Android, Apple y web publican en una release versionada común; sin APK unsigned ni comodines peligrosos.');
