import fs from 'node:fs';

const api = fs.readFileSync('app/src/main/java/com/klortek/velora/jellyfin/JellyfinApi.kt', 'utf8');
const appleApi = fs.readFileSync('apple/Sources/VeloraKit/JellyfinClient.swift', 'utf8');
const movie = fs.readFileSync('app/src/main/java/com/klortek/velora/screens/MovieDetailsScreen.kt', 'utf8');
const series = fs.readFileSync('app/src/main/java/com/klortek/velora/screens/SeriesDetailsScreen.kt', 'utf8');
const quickConnect = fs.readFileSync('app/src/main/java/com/klortek/velora/jellyfin/QuickConnectService.kt', 'utf8');
const androidLiveTv = fs.readFileSync('app/src/main/java/com/klortek/velora/JellyfinVideoPlayerActivity.kt', 'utf8');
const smoke = fs.readFileSync('scripts/qa/jellyfin-smoke.ps1', 'utf8');

if (!api.includes('IncludeItemTypes", "Trailer"')) {
  throw new Error('Jellyfin 12: el resolver Android no usa IncludeItemTypes=Trailer');
}
if (!api.includes('suspend fun getTrailers(itemId: String)')) {
  throw new Error('Jellyfin 12: falta el resolver único de trailers');
}
if (/getMediaItems\("Items\/\$itemId\/(?:Local|Remote)Trailers"/.test(api)) {
  throw new Error('Jellyfin 12: el cliente Android conserva una llamada a una ruta legacy de trailers');
}
if (!/suspend fun initiateQuickConnect[\s\S]*?client\.post\(url\)/.test(quickConnect)) {
  throw new Error('Jellyfin 12: Quick Connect debe iniciarse mediante POST');
}
if (!androidLiveTv.includes('Protocol?.equals("M3U", ignoreCase = true)') ||
    !androidLiveTv.includes('!isM3uSource && liveSource?.SupportsDirectPlay == true')) {
  throw new Error('Jellyfin 12: los canales M3U no deben saltarse el LiveStreamId/remux del servidor');
}
if (!smoke.includes('StartIndex=$startIndex') || !smoke.includes('TotalRecordCount') ||
    !/do \{[\s\S]*?\} while \(\$pageItems\.Count -gt 0/.test(smoke)) {
  throw new Error('Jellyfin Live TV: el smoke test debe recorrer todas las páginas de canales');
}
if (!appleApi.includes('public func trailers(for itemID: String, userID: String)')) {
  throw new Error('Jellyfin 12: falta el resolver de trailers Apple');
}
if (!appleApi.includes('includeTypes: ["Trailer"]')) {
  throw new Error('Jellyfin 12: Apple no consulta trailers mediante IncludeItemTypes=Trailer');
}
const appleLiveTv = appleApi.slice(appleApi.indexOf('public func liveTvChannels'));
if (!appleLiveTv.includes('StartIndex') || !appleLiveTv.includes('Limit') ||
    !appleLiveTv.includes('totalRecordCount') || !/repeat \{[\s\S]*?while totalRecordCount/.test(appleLiveTv)) {
  throw new Error('Jellyfin Live TV: Apple debe recorrer todas las páginas de canales');
}
for (const [name, source] of [['películas', movie], ['series', series]]) {
  if (!source.includes('getTrailers(')) throw new Error(`Trailers: la pantalla de ${name} no usa el resolver moderno`);
  if (source.includes('getLocalTrailers(') || source.includes('getRemoteTrailers(')) {
    throw new Error(`Trailers: la pantalla de ${name} conserva llamadas legacy directas`);
  }
}

console.log('Jellyfin API contract passed: trailers use GetItems/IncludeItemTypes=Trailer without removed legacy routes.');
