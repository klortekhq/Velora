import fs from 'node:fs';
import path from 'node:path';

const resources = path.resolve('app/src/main/res');
const keyPattern = /<(?:string|plurals|string-array)\s+name="([^"]+)"/g;

function keys(file) {
  const source = fs.readFileSync(file, 'utf8');
  return new Set([...source.matchAll(keyPattern)].map((match) => match[1]));
}

const base = keys(path.join(resources, 'values', 'strings.xml'));
// These are the product's guaranteed complete catalogues. The additional
// locales may still fall back to the Spanish base until their translation
// coverage is complete; they are never allowed to introduce an unknown key.
const localeDirectories = ['values-en', 'values-es', 'values-de', 'values-fr'];

const failures = [];
for (const directory of localeDirectories) {
  const file = path.join(resources, directory, 'strings.xml');
  if (!fs.existsSync(file)) {
    failures.push(`${directory}: strings.xml not found`);
    continue;
  }
  const localeKeys = keys(file);
  const missing = [...base].filter((key) => !localeKeys.has(key));
  const extra = [...localeKeys].filter((key) => !base.has(key));
  if (missing.length || extra.length) {
    failures.push(`${directory}: missing=${missing.join(',') || '-'} extra=${extra.join(',') || '-'}`);
  }
}

if (failures.length) {
  console.error(failures.join('\n'));
  process.exitCode = 1;
} else {
  console.log(`Android locale catalogs complete (${localeDirectories.length} locales, ${base.size} keys)`);
}
