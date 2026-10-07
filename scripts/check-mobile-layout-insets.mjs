import fs from 'node:fs';

const file = 'app/src/main/java/com/klortek/velora/screens/MobileDetailsLayouts.kt';
const source = fs.readFileSync(file, 'utf8');

function blockBetween(name, nextName) {
  const start = source.indexOf(`fun ${name}(`);
  if (start < 0) throw new Error(`${name} was not found in ${file}`);
  const end = source.indexOf(`fun ${nextName}(`, start + 1);
  return source.slice(start, end < 0 ? source.length : end);
}

for (const [name, nextName] of [
  ['MobileMovieDetailsLayout', 'MobileSeriesDetailsLayout'],
  ['MobileSeriesDetailsLayout', 'SeasonEpisodeSelectionDialog'],
]) {
  const block = blockBetween(name, nextName);
  for (const inset of ['statusBarsPadding()', 'navigationBarsPadding()']) {
    if (!block.includes(inset)) {
      throw new Error(`${name} is missing ${inset}`);
    }
  }
}

console.log('Mobile layout inset contract passed: details avoid system bars.');
