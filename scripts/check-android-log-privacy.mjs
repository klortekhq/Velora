import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';

const roots = [
  'app/src/main/java/com/klortek/velora',
  'app/src/main/java/com/klortek/velora/MainActivity.kt'
];

const files = [];
for (const root of roots) {
  const stat = fs.statSync(root);
  if (stat.isFile()) files.push(root);
  else visit(root);
}

function visit(relative) {
  for (const entry of fs.readdirSync(relative, { withFileTypes: true })) {
    const child = path.join(relative, entry.name);
    if (entry.isDirectory()) visit(child);
    else if (entry.isFile() && entry.name.endsWith('.kt')) files.push(child);
  }
}

// Logcat is routinely collected during support and QA. Keep catalogue titles,
// user/item identifiers, playback positions and provider metadata out of it.
const privateLogPatterns = [
  /Log\.[diwe]\([^\n]*\$(?:\{)?(?:UserId|SeriesId|PositionTicks|LastPlayedDate|ProviderIds|Genres|MediaSources|libraryId|seriesId|itemId|tmdbId|genre|title|artistId|albumId|seasonId|currentEpisodeIndex|startIndex)(?:\}|\b)/,
  /Log\.[diwe]\([^\n]*\$\{[^\n]*(?:\.Name|\.Id|\.DisplayTitle|\.address|\.customAction|\.packageName)\b/,
  /Log\.[diwe]\([^\n]*(?:Episode clicked|Library from API|UserId:|Requesting .*TMDB ID|server with input|Trying .*for:)/i
];

const violations = [];
for (const file of files) {
  const lines = fs.readFileSync(file, 'utf8').split(/\r?\n/);
  lines.forEach((line, index) => {
    if (privateLogPatterns.some((pattern) => pattern.test(line))) {
      violations.push(`${file}:${index + 1}`);
    }
  });
}

assert.deepEqual(violations, [], `Android log privacy violations: ${violations.join(', ')}`);
console.log(`Android log privacy contract passed: ${files.length} Velora Android source files checked.`);
