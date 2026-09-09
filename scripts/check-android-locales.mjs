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

// Downloading a season is a complete user journey, so the extra catalogues
// must not silently fall back to English for its primary actions.
const extraLocaleDirectories = ['values-ar', 'values-it', 'values-ja', 'values-ko', 'values-pt', 'values-ru', 'values-tr', 'values-zh'];
const translatedDownloadKeys = ['download_season', 'download_season_queued', 'select_episodes', 'continue_label', 'download_quality_title', 'download_quality_description'];
const englishSource = fs.readFileSync(path.join(resources, 'values-en', 'strings.xml'), 'utf8');
const englishValues = new Map([...englishSource.matchAll(/<string name="([^"]+)">([^<]*)<\/string>/g)].map((match) => [match[1], match[2]]));
for (const directory of extraLocaleDirectories) {
  const file = path.join(resources, directory, 'strings.xml');
  if (!fs.existsSync(file)) continue;
  const source = fs.readFileSync(file, 'utf8');
  for (const key of translatedDownloadKeys) {
    const match = source.match(new RegExp(`<string name="${key}">([^<]*)</string>`));
    if (match && match[1] === englishValues.get(key)) {
      failures.push(`${directory}: ${key} todavía usa el texto inglés`);
    }
  }
}

if (failures.length) {
  console.error(failures.join('\n'));
  process.exitCode = 1;
} else {
  console.log(`Android locale catalogs complete (${localeDirectories.length} locales, ${base.size} keys)`);
}
