import fs from 'node:fs';
import path from 'node:path';

const root = process.cwd();
const activity = fs.readFileSync(path.join(root, 'app/src/main/java/com/klortek/velora/LiveTvActivity.kt'), 'utf8');
const query = fs.readFileSync(path.join(root, 'app/src/main/java/com/klortek/velora/livetv/LiveTvChannelQuery.kt'), 'utf8');

function assertMatch(source, pattern, message) {
  if (!pattern.test(source)) throw new Error(message);
}

assertMatch(query, /fun groupLiveTvChannels\(channels: List<LiveTvChannel>\)/, 'Android Live TV debe agrupar canales por identidad visible.');
assertMatch(query, /data class LiveTvChannelGroup\(/, 'Android Live TV debe exponer un modelo de fila agrupada.');
assertMatch(query, /fun liveTvPlaybackChannelList\(/, 'El zapping debe conservar la fuente seleccionada dentro de la fila agrupada.');
assertMatch(activity, /remember\(channels\)\s*\{\s*groupLiveTvChannels\(channels\)\s*\}/, 'La pantalla Android debe renderizar grupos derivados de la respuesta real.');
assertMatch(activity, /if \(channelGroup\.channels\.size > 1\) sourceSelection = channelGroup/, 'Una fila con varias fuentes debe abrir el selector.');
assertMatch(activity, /onPlay\(selected, liveTvPlaybackChannelList\(visibleGroups, group, selected\)\)/, 'El selector debe reproducir la fuente elegida y conservar el zapping.');
assertMatch(activity, /LiveTvSourceDialog\(/, 'Android debe tener un diálogo interactivo para las fuentes agrupadas.');

console.log('Android Live TV grouping contract passed: grouped rows, source picker, and selected-source playback are wired.');
