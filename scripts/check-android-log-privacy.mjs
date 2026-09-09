import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';

const roots = [
  'app/src/main/java/com/klortek/velora/jellyfin',
  'app/src/main/java/com/klortek/velora/MainActivity.kt'
];

const files = [];
for (const root of roots) {
  const stat = fs.statSync(root);
  if (stat.isFile()) files.push(root);
  else for (const entry of fs.readdirSync(root, { withFileTypes: true })) {
    if (entry.isFile() && entry.name.endsWith('.kt')) files.push(path.join(root, entry.name));
  }
}

// Logcat is routinely collected during support and QA. Keep catalogue titles,
// user/item identifiers, playback positions and provider metadata out of it.
const privateLogPatterns = [
  /Log\.[diwe]\([^\n]*\$\{[^\n]*(?:UserId|SeriesId|PositionTicks|LastPlayedDate|ProviderIds|Genres|MediaSources)\b/,
  /Log\.[diwe]\([^\n]*\$\{[^\n]*(?:\.Name|\.Id|libraryId|seriesId|itemId|tmdbId|genre|title)\b/,
  /Log\.[diwe]\([^\n]*(?:Episode clicked|Library from API|UserId:)/
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
console.log(`Android log privacy contract passed: ${files.length} network/auth files checked.`);
