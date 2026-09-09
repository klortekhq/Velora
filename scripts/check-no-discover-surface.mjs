import assert from 'node:assert/strict';
import fs from 'node:fs';

const read = (file) => fs.readFileSync(file, 'utf8');
const movies = read('app/src/main/java/com/klortek/velora/screens/MoviesLibraryScreen.kt');
const series = read('app/src/main/java/com/klortek/velora/screens/TvShowsLibraryScreen.kt');
const settings = read('app/src/main/java/com/klortek/velora/screens/SettingsScreen.kt');

for (const [name, source] of [['películas', movies], ['series', series]]) {
  assert.match(source, /LEGACY_DISCOVERY_ENABLED\s*=\s*false/, `${name}: la ruta legacy debe permanecer desactivada.`);
  assert.doesNotMatch(source, /to\s+"discover"/, `${name}: no debe registrar una pestaña Discover navegable.`);
}
assert.doesNotMatch(
  settings,
  /settings_jellyseerr_discover|jellyseerrEnabled\s*=\s*!jellyseerrEnabled/,
  'Ajustes no debe mostrar ni reactivar la pestaña Discover.'
);

console.log('Discover surface contract passed: Velora mantiene solo Recomendaciones/Biblioteca y no expone Discover.');
