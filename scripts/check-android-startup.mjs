import fs from 'node:fs';
import assert from 'node:assert/strict';

const manifest = fs.readFileSync('app/src/main/AndroidManifest.xml', 'utf8');
const manager = fs.readFileSync(
  'app/src/main/java/com/klortek/velora/offline/OfflineDownloadManager.kt',
  'utf8',
);

assert.match(
  manifest,
  /android:name="androidx\.startup\.InitializationProvider"[\s\S]*tools:node="remove"/,
  'Android must remove the automatic Startup provider from the critical launch path',
);
assert.match(
  manager,
  /WorkManager\.initialize\([\s\S]*context\.applicationContext/,
  'offline transfers must initialize WorkManager explicitly when needed',
);
assert.match(
  manager,
  /return androidx\.work\.WorkManager\.getInstance\(context\.applicationContext\)/,
  'offline transfers must use the explicitly initialized WorkManager instance',
);

console.log('Android startup contract passed: WorkManager is lazy and TV launch does not initialize it automatically.');
