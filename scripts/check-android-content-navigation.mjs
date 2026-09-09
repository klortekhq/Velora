import assert from 'node:assert/strict';
import fs from 'node:fs';

const read = (file) => fs.readFileSync(file, 'utf8');

const mobileDetails = read('app/src/main/java/com/klortek/velora/screens/MobileDetailsLayouts.kt');
const movieDetails = read('app/src/main/java/com/klortek/velora/screens/MovieDetailsScreen.kt');
const seriesDetails = read('app/src/main/java/com/klortek/velora/screens/SeriesDetailsScreen.kt');
const api = read('app/src/main/java/com/klortek/velora/jellyfin/JellyfinApi.kt');
const castActivity = read('app/src/main/java/com/klortek/velora/CastInfoActivity.kt');

assert.match(
  mobileDetails,
  /MobileCastMemberCard[\s\S]*?\.clickable\(enabled = person\.Name\.isNotBlank\(\)\)[\s\S]*?CastInfoActivity\.createIntent/,
  'La tarjeta móvil de reparto debe abrir la ficha del actor.'
);
for (const file of [movieDetails, seriesDetails]) {
  assert.match(file, /R\.string\.details_cast/, 'Las fichas deben etiquetar la sección como reparto.');
}
assert.match(
  api,
  /parameters\.append\("IncludeItemTypes", "Movie,Series"\)/,
  'La filmografía debe incluir películas y series.'
);
assert.match(castActivity, /CastInfoScreen\(/, 'La actividad debe renderizar la ficha de persona.');
assert.match(castActivity, /"Movie"\s*->\s*startActivity\(MovieDetailsActivity\.createIntent/, 'La filmografía debe abrir películas.');
assert.match(castActivity, /"Series"\s*->\s*startActivity\(SeriesDetailsActivity\.createIntent/, 'La filmografía debe abrir series.');

console.log('Android content navigation contract passed: cast cards open filmography with movies and series.');
