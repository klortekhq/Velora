import assert from 'node:assert/strict';
import fs from 'node:fs';
import vm from 'node:vm';

const source = fs.readFileSync(new URL('../platform.js', import.meta.url), 'utf8');
const appSource = fs.readFileSync(new URL('../app.js', import.meta.url), 'utf8');
const packageJson = JSON.parse(fs.readFileSync(new URL('../package.json', import.meta.url), 'utf8'));

assert.doesNotMatch(appSource, /\/Images\/Primary\?api_key=/);
assert.doesNotMatch(appSource, /[?&]api_key=/);
assert.doesNotMatch(appSource, /(?:exception|error)\.message/);
assert.match(appSource, /data-velora-image-id/);
assert.match(appSource, /'X-Emby-Token': state\.token/);
assert.match(appSource, /URL\.createObjectURL\(blob\)/);

function detect(userAgent) {
  const listeners = {};
  const document = {
    documentElement: { setAttribute(name, value) { this[name] = value; } },
    addEventListener(name, handler) { listeners[name] = handler; },
    querySelectorAll() { return []; },
    body: {}
  };
  const window = {
    document,
    navigator: { userAgent },
    setTimeout(callback) { callback(); },
    close() {},
    history: { length: 1 },
    CustomEvent: class CustomEvent { constructor(type, options) { this.type = type; this.defaultPrevented = false; } }
  };
  vm.runInNewContext(source, { window, document, navigator: window.navigator });
  return window.VeloraPlatform;
}

for (const [userAgent, expected] of [
  ['Mozilla/5.0', 'web'],
  ['Mozilla/5.0 Tizen 6.0 SmartTV', 'tizen'],
  ['Mozilla/5.0 (Web0S; Linux/SmartTV)', 'webos'],
  ['Mozilla/5.0 VIDAA SmartTV', 'vidaa']
]) {
  const platform = detect(userAgent);
  assert.equal(platform.name, expected);
  assert.equal(platform.capabilities.supportsOfflineDownloads, false);
}

console.log('platform capability tests passed');

assert.match(appSource, /sessionStorage/);
assert.match(appSource, new RegExp(`var APP_VERSION = '${packageJson.version.replace(/[.*+?^${}()|[\\]\\\\]/g, '\\$&')}'`));
assert.match(appSource, /selected !== 'auto' && TRANSLATIONS\[selected\]/);
assert.match(appSource, /X-Emby-Token/);
assert.match(appSource, /media-proxy-sw\.js/);
assert.match(appSource, /__velora_media/);
assert.match(appSource, /waitForMediaProxy/);
assert.match(appSource, /\/Items\/'.*PlaybackInfo\?UserId=/);
assert.match(appSource, /AutoOpenLiveStream=true/);
assert.match(appSource, /\/Sessions\/Playing\/Stopped/);
assert.match(appSource, /PositionTicks/);
assert.match(appSource, /function sanitizeMediaTarget/);
assert.match(appSource, /function normalizeServerUrl/);
assert.match(appSource, /parsed\.username \|\| parsed\.password/);
assert.match(appSource, /parsed\.search \|\| parsed\.hash/);
assert.match(appSource, /if \(!state\.server\)/);
assert.match(appSource, /searchParams\.delete\('api_key'\)/);
assert.match(appSource, /function toggleFullscreen/);
assert.match(appSource, /exitFullscreen/);
assert.match(appSource, /webkitExitFullscreen/);
assert.match(appSource, /fullscreenchange/);
assert.match(appSource, /velora-clear-credentials/);
assert.match(appSource, /\/LiveTv\/Channels\?UserId=/);
assert.match(appSource, /AddCurrentProgram=true/);
assert.match(appSource, /live-row/);
assert.match(appSource, /liveProgramProgress/);
assert.match(appSource, /People/);
assert.match(appSource, /PersonIds/);
assert.match(appSource, /veloraLibrarySort/);
assert.match(appSource, /veloraLibraryPlayback/);
assert.match(appSource, /veloraLibraryFavorites/);
assert.match(appSource, /veloraLiveFavorites/);
assert.match(appSource, /veloraLiveGroup/);
assert.match(appSource, /function liveChannelGroups/);
assert.match(appSource, /function filteredLiveChannels/);
assert.doesNotMatch(appSource, /data-velora-image-id[^>]+src=/);
const proxySource = fs.readFileSync(new URL('../media-proxy-sw.js', import.meta.url), 'utf8');
assert.match(proxySource, /function normalizeServer/);
assert.match(proxySource, /parsed\.username \|\| parsed\.password/);
assert.match(proxySource, /parsed\.search \|\| parsed\.hash/);
assert.match(proxySource, /credentials = server && data\.token/);
assert.match(proxySource, /target\.protocol !== server\.protocol/);
assert.doesNotMatch(proxySource, /server\.replace\(\\\/$/);
console.log('web security and library interaction tests passed');
