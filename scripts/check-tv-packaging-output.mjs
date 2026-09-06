import fs from 'node:fs';
import path from 'node:path';
import process from 'node:process';

const root = process.cwd();
const version = JSON.parse(fs.readFileSync(path.join(root, 'web', 'package.json'), 'utf8')).version;
const expected = {
  samsung: { platform: 'Samsung Tizen', requiresTizenStudio: true },
  webos: { platform: 'LG webOS', requiresAres: true },
  vidaa: { platform: 'Hisense VIDAA', installablePackage: false }
};
const errors = [];

for (const [directory, requirements] of Object.entries(expected)) {
  const file = path.join(root, 'outputs', 'web', directory, 'velora-build.json');
  if (!fs.existsSync(file)) {
    errors.push(`${directory}: falta outputs/web/${directory}/velora-build.json`);
    continue;
  }
  let metadata;
  try {
    metadata = JSON.parse(fs.readFileSync(file, 'utf8'));
  } catch {
    errors.push(`${directory}: metadatos JSON inválidos`);
    continue;
  }
  if (metadata.product !== 'Velora') errors.push(`${directory}: product no es Velora`);
  if (metadata.version !== version) errors.push(`${directory}: versión ${metadata.version} distinta de ${version}`);
  for (const [key, value] of Object.entries(requirements)) {
    if (metadata[key] !== value) errors.push(`${directory}: ${key} debe ser ${String(value)}`);
  }
  if (!['bundle', 'ipk', 'wgt', 'hosted-html5'].includes(metadata.packaging)) {
    errors.push(`${directory}: packaging inválido (${metadata.packaging})`);
  }
}

if (errors.length) {
  console.error('Metadatos de empaquetado Smart TV inválidos:');
  for (const error of errors) console.error(`- ${error}`);
  process.exit(1);
}

console.log('Metadatos Smart TV coherentes: Samsung, webOS y VIDAA.');
