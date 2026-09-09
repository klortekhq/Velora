import assert from 'node:assert/strict';
import fs from 'node:fs';

const read = (file) => fs.readFileSync(file, 'utf8');

const mobileDetails = read('app/src/main/java/com/klortek/velora/screens/MobileDetailsLayouts.kt');
const movieDetails = read('app/src/main/java/com/klortek/velora/screens/MovieDetailsScreen.kt');
const seriesDetails = read('app/src/main/java/com/klortek/velora/screens/SeriesDetailsScreen.kt');
const mobileHome = read('app/src/main/java/com/klortek/velora/screens/MobileHomeScreen.kt');
const tvHome = read('app/src/main/java/com/klortek/velora/screens/JellyfinHomeScreen.kt');
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
const mpvPlayer = read('app/src/main/java/com/klortek/velora/player/mpv/MpvTvPlayerActivity.kt');
assert.match(
  mpvPlayer,
  /Card\([\s\S]*?CastInfoActivity\.createIntent\(context, person\)/,
  'El reparto visible en la pantalla MPV debe abrir la ficha de la persona con mando o toque.'
);
assert.match(castActivity, /"Movie"\s*->\s*startActivity\(MovieDetailsActivity\.createIntent/, 'La filmografía debe abrir películas.');
assert.match(castActivity, /"Series"\s*->\s*startActivity\(SeriesDetailsActivity\.createIntent/, 'La filmografía debe abrir series.');
const videoPlayer = read('app/src/main/java/com/klortek/velora/screens/JellyfinVideoPlayerScreen.kt');
assert.match(
  videoPlayer,
  /if \(isMobile\) \{[\s\S]*?R\.string\.player_fullscreen[\s\S]*?R\.string\.player_exit_fullscreen[\s\S]*?toggleMobileFullscreen\(\)/,
  'El reproductor móvil debe ofrecer entrada y salida de pantalla completa desde los controles táctiles.'
);

assert.match(
  mobileHome,
  /queryLibraryItems\(items, mobileSortMode, sortDescending, favoritesOnly, playbackFilter, selectedGenre\)/,
  'La biblioteca móvil debe aplicar la consulta real a películas y series.'
);
assert.match(
  mobileHome,
  /queryLibraryItems\(recommendations, mobileSortMode, sortDescending, favoritesOnly, playbackFilter, selectedGenre\)/,
  'La pestaña de recomendaciones móvil debe respetar los mismos filtros que Todo.'
);
for (const token of ['settings\.setSortType', 'settings\.libraryFavoritesOnly', 'settings\.libraryPlaybackFilter', 'settings\.libraryGenreFilter']) {
  assert.match(mobileHome, new RegExp(token), `La biblioteca móvil debe persistir ${token}.`);
}
assert.match(
  tvHome,
  /onSortSelected\(SortType\.(Alphabetically|DateAdded|DateReleased|Runtime|CriticRating|CommunityRating)\)/,
  'La biblioteca TV debe conectar sus opciones de ordenación con el estado de la consulta.'
);
assert.match(tvHome, /onGenreSelected\(/, 'La biblioteca TV debe conectar el filtro de género.');
assert.match(tvHome, /onPlaybackFilterSelected\(/, 'La biblioteca TV debe conectar el filtro de reproducción.');

console.log('Android content navigation contract passed: cast cards and movie/series library filters are wired to real queries.');
