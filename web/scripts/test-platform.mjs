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
// Browser and Smart TV builds must not expose offline/download actions at all.
// This is stronger than merely disabling a button at runtime: the shared web
// surface must not accidentally reintroduce a mobile-only download affordance.
assert.doesNotMatch(appSource, /\b(download|downloads|offline|descargar|descargas)\b/i);

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
assert.match(appSource, /Object\.assign\(\{\}, TRANSLATIONS\.en, TRANSLATIONS\[code\]\)/);
assert.match(appSource, /X-Emby-Token/);
assert.match(appSource, /media-proxy-sw\.js/);
assert.match(appSource, /__velora_media/);
assert.match(appSource, /waitForMediaProxy/);
assert.match(appSource, /\/Items\/'.*PlaybackInfo\?UserId=/);
assert.match(appSource, /AutoOpenLiveStream=true/);
assert.match(appSource, /MediaSourceId=/);
assert.match(appSource, /\/Sessions\/Playing\/Stopped/);
assert.match(appSource, /PositionTicks/);
assert.match(appSource, /function sanitizeMediaTarget/);
assert.match(appSource, /function normalizeServerUrl/);
assert.match(appSource, /parsed\.username \|\| parsed\.password/);
assert.match(appSource, /parsed\.search \|\| parsed\.hash/);
assert.match(appSource, /if \(!state\.server\)/);
assert.match(appSource, /searchParams\.delete\('api_key'\)/);
assert.match(appSource, /function toggleFullscreen/);
assert.match(appSource, /function minimizePlayer/);
assert.match(appSource, /function restorePlayer/);
assert.match(appSource, /video-mini-player/);
assert.match(appSource, /id="playerMinimize"/);
assert.match(appSource, /restore\.id = 'playerRestore'/);
assert.match(appSource, /exitFullscreen/);
assert.match(appSource, /webkitExitFullscreen/);
assert.match(appSource, /fullscreenchange/);
assert.match(appSource, /velora-clear-credentials/);
assert.match(appSource, /\/LiveTv\/Channels\?UserId=/);
assert.match(appSource, /AddCurrentProgram=true/);
assert.match(appSource, /Fields=Overview%2CMediaSources/);
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
assert.match(appSource, /function groupLiveTvChannels/);
assert.match(appSource, /function showLiveSourcePicker/);
assert.match(appSource, /liveSourceLabel/);
assert.match(appSource, /Array\.isArray\(channel\.MediaSources\)/);
assert.match(appSource, /MediaSources: \[source\]/);
assert.match(appSource, /data-source-index/);
assert.match(appSource, /function filteredLiveChannels/);
assert.doesNotMatch(appSource, /data-velora-image-id[^>]+src=/);
const proxySource = fs.readFileSync(new URL('../media-proxy-sw.js', import.meta.url), 'utf8');
assert.match(proxySource, /function normalizeServer/);
assert.match(proxySource, /parsed\.username \|\| parsed\.password/);
assert.match(proxySource, /parsed\.search \|\| parsed\.hash/);
assert.match(proxySource, /credentials = server && data\.token/);
assert.match(proxySource, /target\.protocol !== server\.protocol/);
assert.match(proxySource, /function isAllowedMediaPath/);
assert.match(proxySource, /Videos\\\/\[\^\/\]\+\\\/stream/);
assert.match(proxySource, /LiveTv\\\/LiveStreamFiles/);
assert.doesNotMatch(proxySource, /server\.replace\(\\\/$/);
// Execute the real web grouping function instead of relying only on source
// pattern checks. This protects the one-row/multiple-source Live TV contract.
const testableAppSource = appSource.replace(/\r\n/g, '\n').replace(
  '  renderApp();\n}());',
  '  window.__veloraTest = { groupLiveTvChannels };\n}());'
);
assert.match(testableAppSource, /window\.__veloraTest = \{ groupLiveTvChannels \}/, 'web app test hook was not injected');
const testStorage = {
  getItem() { return null; },
  setItem() {},
  removeItem() {}
};
const testDocument = {
  querySelector() { return null; },
  addEventListener() {},
  documentElement: { setAttribute() {} }
};
const testWindow = {
  document: testDocument,
  navigator: { languages: ['es-ES'], language: 'es-ES' },
  localStorage: testStorage,
  sessionStorage: testStorage,
  setTimeout,
  clearTimeout,
  addEventListener() {},
  removeEventListener() {}
};
vm.runInNewContext(testableAppSource, {
  window: testWindow,
  document: testDocument,
  navigator: testWindow.navigator,
  localStorage: testStorage,
  sessionStorage: testStorage,
  setTimeout,
  clearTimeout,
  console
});
const groupedLiveTv = testWindow.__veloraTest.groupLiveTvChannels([
  {
    Id: 'same-channel',
    Name: 'DAZN F1',
    MediaSources: [
      { Id: 'source-main', Tags: ['Principal'] },
      { Id: 'source-iptv', ChannelType: 'IPTV' }
    ]
  },
  { Id: 'other-channel', Name: 'Noticias', MediaSources: [{ Id: 'source-news' }] }
]);
assert.equal(groupedLiveTv.length, 2);
assert.equal(groupedLiveTv[0].channelId, 'same-channel');
assert.equal(groupedLiveTv[0].channels.length, 2);
assert.deepEqual(
  Array.from(groupedLiveTv[0].channels, channel => channel.MediaSources[0].Id),
  ['source-main', 'source-iptv']
);
console.log('web security and library interaction tests passed');
