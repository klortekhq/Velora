import fs from 'node:fs';

const api = fs.readFileSync('app/src/main/java/com/klortek/velora/jellyfin/JellyfinApi.kt', 'utf8');
const appleApi = fs.readFileSync('apple/Sources/VeloraKit/JellyfinClient.swift', 'utf8');
const movie = fs.readFileSync('app/src/main/java/com/klortek/velora/screens/MovieDetailsScreen.kt', 'utf8');
const series = fs.readFileSync('app/src/main/java/com/klortek/velora/screens/SeriesDetailsScreen.kt', 'utf8');

if (!api.includes('IncludeItemTypes", "Trailer"')) {
  throw new Error('Jellyfin 12: el resolver Android no usa IncludeItemTypes=Trailer');
}
if (!api.includes('suspend fun getTrailers(itemId: String)')) {
  throw new Error('Jellyfin 12: falta el resolver único de trailers');
}
if (!appleApi.includes('public func trailers(for itemID: String, userID: String)')) {
  throw new Error('Jellyfin 12: falta el resolver de trailers Apple');
}
if (!appleApi.includes('includeTypes: ["Trailer"]')) {
  throw new Error('Jellyfin 12: Apple no consulta trailers mediante IncludeItemTypes=Trailer');
}
for (const [name, source] of [['películas', movie], ['series', series]]) {
  if (!source.includes('getTrailers(')) throw new Error(`Trailers: la pantalla de ${name} no usa el resolver moderno`);
  if (source.includes('getLocalTrailers(') || source.includes('getRemoteTrailers(')) {
    throw new Error(`Trailers: la pantalla de ${name} conserva llamadas legacy directas`);
  }
}

console.log('Jellyfin API contract passed: trailers use GetItems/IncludeItemTypes=Trailer with legacy fallback.');
