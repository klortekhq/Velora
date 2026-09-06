import assert from 'node:assert/strict';
import fs from 'node:fs';

const read = (file) => fs.readFileSync(file, 'utf8');

const platform = read('app/src/main/java/com/klortek/velora/platform/PlatformCapabilities.kt');
assert.match(platform, /PlatformSurface\.MOBILE_TABLET, PlatformSurface\.IOS_MOBILE -> true/);
for (const surface of ['TV', 'BROWSER', 'TVOS', 'TIZEN', 'WEBOS', 'VIDAA']) {
  assert.match(
    platform,
    new RegExp(`PlatformSurface\\.${surface}`),
    `Android capability surface missing: ${surface}`
  );
}

const offlineActivity = read('app/src/main/java/com/klortek/velora/OfflineDownloadsActivity.kt');
assert.match(offlineActivity, /!PlatformCapabilities\.supportsOfflineDownloads/,
  'Android offline activity must enforce the capability boundary');

const player = read('app/src/main/java/com/klortek/velora/JellyfinVideoPlayerActivity.kt');
assert.match(player, /if \(!PlatformCapabilities\.supportsOfflineDownloads\)/,
  'Android player must reject local/offline intents on TV builds');

const webPlatform = read('web/platform.js');
assert.match(webPlatform, /supportsOfflineDownloads:\s*false/,
  'Browser and Smart TV capabilities must disable offline downloads');
for (const file of ['web/app.js', 'web/index.html', 'web/styles.css']) {
  assert.doesNotMatch(read(file), /download|offline|descargar|descargas/i,
    `${file} must not expose offline/download UI`);
}

const models = read('apple/Sources/VeloraKit/VeloraModels.swift');
assert.match(models, /case \.tvOS: return false/,
  'tvOS must not support offline downloads');
const shell = read('apple/Sources/VeloraKit/VeloraAppShell.swift');
assert.match(shell, /if model\.platform\.supportsOfflineDownloads/,
  'Apple detail UI must gate download controls by platform capability');

console.log('Offline surface policy passed: downloads are mobile/tablet-only across Android, Apple, browser and Smart TV.');
