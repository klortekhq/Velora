import fs from 'node:fs';
import path from 'node:path';

const root = process.cwd();
const read = (relative) => fs.readFileSync(path.join(root, relative), 'utf8');
const settings = read('app/src/main/java/com/klortek/velora/jellyfin/AppSettings.kt');
const activity = read('app/src/main/java/com/klortek/velora/JellyfinVideoPlayerActivity.kt');
const policy = read('app/src/main/java/com/klortek/velora/playback/PlaybackBackend.kt');
const tests = read('app/src/test/java/com/klortek/velora/playback/PlaybackBackendTest.kt');

const failures = [];
if (!/get\(\)\s*=\s*prefs\.getBoolean\(KEY_MPV_ENABLED,\s*false\)/.test(settings)) {
  failures.push('AppSettings must default MPV to disabled.');
}
if (!activity.includes('PlaybackBackendSelector.initialBackend')) {
  failures.push('The Android activity must use the canonical backend selector.');
}
if (!policy.includes('PlaybackBackend.MEDIA3') || !policy.includes('mpvExplicitlyEnabled')) {
  failures.push('The canonical backend policy must make Media3 the explicit default.');
}
if (!tests.includes('freshPreferencesUseMedia3') || !tests.includes('mpvFallbackRequiresARealDecoderFailure')) {
  failures.push('Backend default and fallback regressions are missing.');
}

if (failures.length) {
  console.error(failures.join('\n'));
  process.exitCode = 1;
} else {
  console.log('Android playback policy passed: Media3 default, MPV explicit/fallback only.');
}
