import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const source = fs.readFileSync(path.join(root, 'web', 'app.js'), 'utf8');

function skipSpace(source, index) {
  while (index < source.length) {
    if (/\s/.test(source[index])) {
      index += 1;
      continue;
    }
    if (source.startsWith('//', index)) {
      const end = source.indexOf('\n', index + 2);
      index = end === -1 ? source.length : end + 1;
      continue;
    }
    if (source.startsWith('/*', index)) {
      const end = source.indexOf('*/', index + 2);
      index = end === -1 ? source.length : end + 2;
      continue;
    }
    break;
  }
  return index;
}

function readBalanced(source, openingIndex) {
  const opening = source[openingIndex];
  const closing = opening === '{' ? '}' : opening === '[' ? ']' : ')';
  let depth = 0;
  let quote = null;
  let escaped = false;
  for (let index = openingIndex; index < source.length; index += 1) {
    const char = source[index];
    const next = source[index + 1];
    if (quote) {
      if (escaped) escaped = false;
      else if (char === '\\') escaped = true;
      else if (char === quote) quote = null;
      continue;
    }
    if (char === "'" || char === '"' || char === '`') {
      quote = char;
      continue;
    }
    if (char === '/' && next === '/') {
      const end = source.indexOf('\n', index + 2);
      index = end === -1 ? source.length : end;
      continue;
    }
    if (char === '/' && next === '*') {
      const end = source.indexOf('*/', index + 2);
      index = end === -1 ? source.length : end + 1;
      continue;
    }
    if (char === opening) depth += 1;
    else if (char === closing) {
      depth -= 1;
      if (depth === 0) return source.slice(openingIndex, index + 1);
    }
  }
  throw new Error(`No se pudo cerrar el objeto en ${openingIndex}`);
}

function findObject(name) {
  const marker = `var ${name} =`;
  const markerIndex = source.indexOf(marker);
  if (markerIndex === -1) throw new Error(`No se encontró ${name}`);
  const openingIndex = source.indexOf('{', markerIndex + marker.length);
  return readBalanced(source, openingIndex);
}

function topLevelObjectEntries(objectSource) {
  const entries = new Map();
  let index = 1;
  while (index < objectSource.length - 1) {
    index = skipSpace(objectSource, index);
    const match = /^[A-Za-z_$][\w$]*/.exec(objectSource.slice(index));
    if (!match) {
      index += 1;
      continue;
    }
    const key = match[0];
    const keyEnd = index + key.length;
    const colonIndex = skipSpace(objectSource, keyEnd);
    if (objectSource[colonIndex] !== ':') {
      index = keyEnd;
      continue;
    }
    const valueIndex = skipSpace(objectSource, colonIndex + 1);
    if (objectSource[valueIndex] !== '{') {
      index = keyEnd;
      continue;
    }
    const value = readBalanced(objectSource, valueIndex);
    entries.set(key, value);
    index = valueIndex + value.length;
  }
  return entries;
}

function propertyKeys(objectSource) {
  const keys = new Set();
  let index = 1;
  let depth = 1;
  let quote = null;
  let escaped = false;
  while (index < objectSource.length - 1) {
    const char = objectSource[index];
    const next = objectSource[index + 1];
    if (quote) {
      if (escaped) escaped = false;
      else if (char === '\\') escaped = true;
      else if (char === quote) quote = null;
      index += 1;
      continue;
    }
    if (char === "'" || char === '"' || char === '`') {
      quote = char;
      index += 1;
      continue;
    }
    if (char === '/' && next === '/') {
      index = objectSource.indexOf('\n', index + 2);
      if (index === -1) break;
      continue;
    }
    if (char === '/' && next === '*') {
      index = objectSource.indexOf('*/', index + 2);
      index = index === -1 ? objectSource.length : index + 2;
      continue;
    }
    if (char === '{' || char === '[' || char === '(') depth += 1;
    else if (char === '}' || char === ']' || char === ')') depth -= 1;
    if (depth === 1) {
      const match = /^[A-Za-z_$][\w$]*/.exec(objectSource.slice(index));
      if (match) {
        const keyEnd = index + match[0].length;
        const colonIndex = skipSpace(objectSource, keyEnd);
        if (objectSource[colonIndex] === ':') {
          if (keys.has(match[0])) throw new Error(`Clave duplicada: ${match[0]}`);
          keys.add(match[0]);
          index = keyEnd;
          continue;
        }
      }
    }
    index += 1;
  }
  return keys;
}

function differences(expected, actual) {
  return [
    ...[...expected].filter(key => !actual.has(key)).map(key => `falta "${key}"`),
    ...[...actual].filter(key => !expected.has(key)).map(key => `clave no presente en el catálogo base "${key}"`)
  ];
}

const translations = topLevelObjectEntries(findObject('TRANSLATIONS'));
const overrides = topLevelObjectEntries(findObject('TRANSLATION_OVERRIDES'));
const localeCodes = [...translations.keys()];
const errors = [];
const baseline = propertyKeys(translations.get('en'));

if (!translations.has('es') || !translations.has('en')) errors.push('Deben existir los catálogos es y en.');
if (baseline.size === 0) errors.push('El catálogo en no contiene claves.');

for (const locale of localeCodes) {
  const effective = new Set([...baseline, ...propertyKeys(translations.get(locale))]);
  if (overrides.has(locale)) for (const key of propertyKeys(overrides.get(locale))) effective.add(key);
  const localeErrors = differences(baseline, effective);
  for (const error of localeErrors) errors.push(`${locale}: ${error}`);
}

const languageOptionBlock = source.slice(source.indexOf('var LANGUAGE_OPTIONS ='), source.indexOf('var TRANSLATIONS ='));
const optionCodes = [...languageOptionBlock.matchAll(/value:\s*'([a-z]{2})'/g)].map(match => match[1]);
for (const locale of localeCodes) if (!optionCodes.includes(locale)) errors.push(`${locale}: falta en LANGUAGE_OPTIONS.`);
for (const locale of optionCodes) if (!translations.has(locale)) errors.push(`${locale}: LANGUAGE_OPTIONS no tiene catálogo.`);

if (errors.length) {
  console.error('Catálogos web inconsistentes:');
  for (const error of errors) console.error(`- ${error}`);
  process.exit(1);
}

console.log(`Catálogos web coherentes: ${baseline.size} claves efectivas en ${localeCodes.join(', ')}`);
