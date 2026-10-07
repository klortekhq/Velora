import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const read = relative => fs.readFileSync(path.join(root, relative), 'utf8');
const errors = [];

const webVersion = JSON.parse(read('web/package.json')).version;
const androidVersion = read('app/build.gradle.kts').match(/versionName\s*=\s*"([^"]+)"/)?.[1];
const webOSVersion = JSON.parse(read('web/platforms/webos/appinfo.json')).version;
const vidaaVersion = JSON.parse(read('web/platforms/vidaa/submission.json')).version;
const samsungVersion = read('web/platforms/samsung/config.xml').match(/<widget[^>]*version="([^"]+)"/)?.[1];
const readmeVersion = read('README.md').match(/Versión de código:\s*\*\*([^*]+)\*\*/)?.[1];

const versions = { android: androidVersion, web: webVersion, webOS: webOSVersion, VIDAA: vidaaVersion, Samsung: samsungVersion, README: readmeVersion };
for (const [name, version] of Object.entries(versions)) {
  if (!version) errors.push(`${name}: no se pudo leer la versión`);
  else if (version !== webVersion) errors.push(`${name}: ${version} (se esperaba ${webVersion})`);
}
if (!/^\d+\.\d+\.\d+$/.test(webVersion)) errors.push(`versión no semver: ${webVersion}`);

if (errors.length) {
  console.error('Inconsistencias de versión detectadas:');
  for (const error of errors) console.error(`- ${error}`);
  process.exit(1);
}
console.log(`Versiones coherentes: ${webVersion}`);
