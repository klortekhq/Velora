import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const localeRoot = path.join(root, 'apple', 'Sources', 'VeloraKit', 'Resources');
const locales = ['en', 'es', 'fr', 'de'];

function keys(locale) {
  const file = path.join(localeRoot, `${locale}.lproj`, 'Localizable.strings');
  const source = fs.readFileSync(file, 'utf8');
  return new Set([...source.matchAll(/^\s*"((?:\\.|[^"\\])*)"\s*=\s*"/gm)].map(match => match[1]));
}

const catalogs = Object.fromEntries(locales.map(locale => [locale, keys(locale)]));
const baseline = catalogs.en;
const errors = [];
for (const locale of locales.slice(1)) {
  for (const key of baseline) if (!catalogs[locale].has(key)) errors.push(`${locale}: falta "${key}"`);
  for (const key of catalogs[locale]) if (!baseline.has(key)) errors.push(`${locale}: clave no presente en en "${key}"`);
}

if (errors.length) {
  console.error('Catálogos Apple inconsistentes:');
  for (const error of errors) console.error(`- ${error}`);
  process.exit(1);
}
console.log(`Catálogos Apple coherentes: ${baseline.size} claves en ${locales.join(', ')}`);
