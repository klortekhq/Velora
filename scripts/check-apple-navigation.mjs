import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const shellPath = path.join(root, 'apple', 'Sources', 'VeloraKit', 'VeloraAppShell.swift');
const shell = fs.readFileSync(shellPath, 'utf8');

const required = [
  'private struct VeloraAuthenticatedContent: View',
  'case home, movies, series, liveTV',
  'Label("Home", systemImage: "house")',
  'Label("Movies", systemImage: "film")',
  'Label("Series", systemImage: "rectangle.stack")',
  'if !model.liveTvChannels.isEmpty',
  'Label("Live TV", systemImage: "tv")',
  'VeloraSettingsView(settings: $model.settings)',
  'if selectedSection == .home { Task { await model.loadMoreItems() } }',
];

for (const marker of required) {
  if (!shell.includes(marker)) {
    throw new Error(`Apple navigation: falta el contrato ${marker}`);
  }
}

for (const locale of ['en', 'es', 'fr', 'de']) {
  const file = path.join(root, 'apple', 'Sources', 'VeloraKit', 'Resources', `${locale}.lproj`, 'Localizable.strings');
  const source = fs.readFileSync(file, 'utf8');
  for (const key of ['"Home"', '"Movies"', '"Series"']) {
    if (!source.includes(`${key} =`)) {
      throw new Error(`Apple navigation: falta ${key} en ${locale}`);
    }
  }
}

console.log('Apple navigation contract passed: tabs adapt to library sections and gate Live TV by availability.');
