import fs from 'node:fs';

const file = 'app/src/main/java/com/klortek/velora/screens/MobileDetailsLayouts.kt';
const source = fs.readFileSync(file, 'utf8');
const start = source.indexOf('private fun MobileFileDetails(');
if (start < 0) {
  throw new Error(`MobileFileDetails was not found in ${file}`);
}

// Keep the check scoped to the composable, not the rest of the file where
// local subtitle paths are legitimately used for playback plumbing.
const end = source.indexOf('\n@Composable', start + 1);
const block = source.slice(start, end < 0 ? source.length : end);

const forbidden = [
  /\.Path\b/,
  /\.Name\b/,
  /fileName/i,
  /filePath/i,
  /localPath/i,
  /absolutePath/i,
  /stringResource\([^)]*file_name/i,
  /stringResource\([^)]*file_path/i,
];

const violations = forbidden
  .filter((pattern) => pattern.test(block))
  .map((pattern) => pattern.toString());

if (violations.length > 0) {
  throw new Error(
    `MobileFileDetails must expose media format only; forbidden file identity fields: ${violations.join(', ')}`,
  );
}

for (const required of ['mobile_video', 'mobile_audio', 'mobile_format']) {
  if (!block.includes(required)) {
    throw new Error(`MobileFileDetails is missing the technical field ${required}`);
  }
}

console.log('Mobile file information contract passed: technical media fields only.');
