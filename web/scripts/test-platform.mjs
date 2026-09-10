import assert from 'node:assert/strict';
import fs from 'node:fs';
import vm from 'node:vm';

const source = fs.readFileSync(new URL('../platform.js', import.meta.url), 'utf8');
const appSource = fs.readFileSync(new URL('../app.js', import.meta.url), 'utf8');
const indexSource = fs.readFileSync(new URL('../index.html', import.meta.url), 'utf8');
const buildSource = fs.readFileSync(new URL('./build-web.mjs', import.meta.url), 'utf8');
const packageJson = JSON.parse(fs.readFileSync(new URL('../package.json', import.meta.url), 'utf8'));

assert.doesNotMatch(appSource, /\/Images\/Primary\?api_key=/);
assert.doesNotMatch(appSource, /[?&]api_key=/);
assert.doesNotMatch(appSource, /(?:exception|error)\.message/);
assert.match(appSource, /data-velora-image-id/);
assert.match(appSource, /function mediaBrowserAuthorization\(\)/);
assert.match(appSource, /function ensureUserId\(\)/, 'web debe resolver el usuario antes de consultar catálogos dependientes');
assert.match(appSource, /function loadLiveTvChannels\(\)\s*\{[\s\S]*?return ensureUserId\(\)\.then/, 'Live TV no debe competir con la resolución inicial del usuario');
assert.match(appSource, /api\('\/Users\/\' \+ encodeURIComponent\(state\.userId\) \+ '\/Items\?'/, 'las rutas de biblioteca web deben codificar el usuario');
assert.doesNotMatch(appSource, /X-Emby-(?:Authorization|Token)/, 'web no debe usar cabeceras legacy de Jellyfin');
assert.match(appSource, /URL\.createObjectURL\(blob\)/);
assert.match(appSource, /function applyAspectMode\(player, mode\)/);
assert.match(appSource, /function applyPerformanceMode\(mode\)/);
assert.match(appSource, /veloraPerformanceMode/);
assert.match(appSource, /mode === 'quality' \? 900 : mode === 'performance' \? 320 : 600/);
assert.match(appSource, /function cycleAspectMode\(player\)/);
assert.match(appSource, /id=\"playerAspect\"/);
assert.match(appSource, /savePreference\('veloraAspectMode', next\)/);
assert.match(appSource, /video-aspect-' \+ candidate/);
// Browser and Smart TV builds must not expose offline/download actions at all.
// This is stronger than merely disabling a button at runtime: the shared web
// surface must not accidentally reintroduce a mobile-only download affordance.
assert.doesNotMatch(appSource, /\b(download|downloads|offline|descargar|descargas)\b/i);
assert.match(indexSource, /Content-Security-Policy/);
assert.match(indexSource, /default-src 'self'/);
assert.match(indexSource, /frame-ancestors 'none'/);
assert.match(indexSource, /object-src 'none'/);
assert.match(indexSource, /referrer.*no-referrer/);
assert.doesNotMatch(buildSource, /shell:\s*true/, 'el empaquetador no debe activar un shell implícito');
assert.match(buildSource, /shell:\s*false/, 'las herramientas del empaquetador deben ejecutarse sin shell implícito');
assert.match(buildSource, /quoteWindowsArg/, 'los CLIs .cmd deben invocarse con argumentos escapados');
assert.match(buildSource, /powershell\.exe/, 'los shims PowerShell deben invocarse explícitamente');

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
assert.match(appSource, /saveSessionValue\('veloraToken', state\.token\)/);
assert.match(appSource, /function legacyStorageValue\(key\)/);
assert.match(appSource, /function removeLegacyStorageValue\(key\)/);
assert.match(appSource, /function durableStorageValue\(key, fallback\)/);
assert.match(appSource, /function saveDurableStorageValue\(key, value\)/);
assert.match(appSource, /removeLegacyStorageValue\('veloraToken'\)/);
assert.match(appSource, /removeLegacyStorageValue\('veloraUserId'\)/);
assert.doesNotMatch(appSource, /delete localStorage\.veloraToken/);
assert.doesNotMatch(appSource, /localStorage\.veloraServer/);
assert.doesNotMatch(appSource, /localStorage\.veloraUser/);
assert.doesNotMatch(appSource, /localStorage\.veloraToken\s*=/);
assert.doesNotMatch(appSource, /localStorage\.veloraUserId\s*=/);
assert.match(appSource, new RegExp(`var APP_VERSION = '${packageJson.version.replace(/[.*+?^${}()|[\\]\\\\]/g, '\\$&')}'`));
assert.match(appSource, /selected !== 'auto' && TRANSLATIONS\[selected\]/);
assert.match(appSource, /Object\.assign\(\{\}, TRANSLATIONS\.en, TRANSLATIONS\[code\]\)/);
assert.match(appSource, /Client=\"Velora Web\".*Token=/);
assert.match(appSource, /media-proxy-sw\.js/);
assert.match(appSource, /__velora_media/);
assert.match(appSource, /waitForMediaProxy/);
assert.match(appSource, /\/Items\/'.*PlaybackInfo\?UserId=/);
assert.match(appSource, /JSON\.stringify\(\{ Username: username, Pw:/);
assert.doesNotMatch(appSource, /JSON\.stringify\(\{ Username: username, Password:/);
assert.match(appSource, /AutoOpenLiveStream=true/);
assert.match(appSource, /MediaSourceId=/);
assert.match(appSource, /\/Sessions\/Playing\/Stopped/);
assert.match(appSource, /PositionTicks/);
assert.match(appSource, /function sanitizeMediaTarget/);
assert.match(appSource, /function normalizeServerUrl/);
assert.match(appSource, /parsed\.username \|\| parsed\.password/);
assert.match(appSource, /parsed\.search \|\| parsed\.hash/);
assert.match(appSource, /if \(!state\.server\)/);
assert.match(appSource, /response\.status === 401 \|\| response\.status === 403/);
assert.match(appSource, /failure\.code =/);
assert.match(appSource, /t\(failure && failure\.code \? failure\.code : 'connectionError'\)/);
assert.match(appSource, /sensitiveNames = \['api_key', 'apikey', 'access_token', 'token', 'x-emby-token', 'authorization'\]/);
assert.match(appSource, /sensitiveNames\.indexOf\(String\(name\)\.toLowerCase\(\)\)/);
assert.match(appSource, /function toggleFullscreen/);
assert.match(appSource, /function minimizePlayer/);
assert.match(appSource, /function rememberFocus\(\)/);
assert.match(appSource, /function restoreFocus\(\)/);
assert.match(appSource, /document\.querySelector\('#detailsClose'\)\.focus\(\)/);
assert.match(appSource, /document\.querySelector\('#settingsClose'\)\.focus\(\)/);
assert.match(appSource, /function closeDetails\(\)[\s\S]*restoreFocus\(\)/);
assert.match(appSource, /function closeSettings\(\)[\s\S]*restoreFocus\(\)/);
assert.match(appSource, /function startThemeMusic/);
assert.match(appSource, /function stopThemeMusic/);
assert.match(appSource, /ThemeSongs\?UserId=/);
assert.match(appSource, /veloraThemeMusicVolume/);
assert.match(appSource, /card\.onfocus/);
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
assert.match(appSource, /StartIndex=' \+ encodeURIComponent\(startIndex\)/);
assert.match(appSource, /Limit=' \+ pageSize/);
assert.match(appSource, /response\.TotalRecordCount/);
assert.match(appSource, /function loadChannelPage\(startIndex, channels, totalCount\)/);
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
assert.match(appSource, /state\.returnFocus\s*=\s*opener/);
assert.match(appSource, /var closePicker\s*=\s*function/);
assert.match(appSource, /document\.querySelector\('#liveSourcePicker'\)/);
assert.match(appSource, /function filteredLiveChannels/);
assert.match(appSource, /StartIndex=' \+ encodeURIComponent\(startIndex\)/);
assert.match(appSource, /function loadMoreItems/);
assert.match(appSource, /id="loadMore"/);
assert.match(appSource, /var LOAD_MORE_TRANSLATIONS/);
assert.match(appSource, /class="card" tabindex="0" role="button" aria-label=/);
assert.match(appSource, /role="button" aria-label="' \+ esc\(work\.Name \|\| ''\)/);
assert.match(appSource, /class="live-row" tabindex="0" role="button" aria-label=/);
assert.match(appSource, /role="tablist"/);
assert.match(appSource, /role="tab" aria-selected="true" aria-controls="results"/);
assert.match(appSource, /role="tabpanel" tabindex="0"/);
assert.match(appSource, /setAttribute\('aria-selected', candidate === tab \? 'true' : 'false'\)/);
assert.match(appSource, /event\.key !== 'Escape'/);
assert.match(appSource, /var liveSourcePicker = document\.querySelector\('#liveSourcePicker'\)/);
assert.doesNotMatch(appSource, /data-velora-image-id[^>]+src=/);
const proxySource = fs.readFileSync(new URL('../media-proxy-sw.js', import.meta.url), 'utf8');
assert.match(proxySource, /function normalizeServer/);
assert.match(proxySource, /parsed\.username \|\| parsed\.password/);
assert.match(proxySource, /parsed\.search \|\| parsed\.hash/);
assert.match(proxySource, /credentials = server && data\.token/);
assert.match(proxySource, /target\.protocol !== server\.protocol/);
assert.match(proxySource, /function isAllowedMediaPath/);
assert.match(proxySource, /Videos\\\/\[\^\/\]\+\\\/stream/);
assert.match(proxySource, /Audio\\\/\[\^\/\]\+\\\/universal/);
assert.match(proxySource, /LiveTv\\\/LiveStreamFiles/);
assert.match(proxySource, /access_token/);
assert.doesNotMatch(proxySource, /X-Emby-(?:Authorization|Token)/, 'proxy no debe usar cabeceras legacy de Jellyfin');
assert.match(proxySource, /target\.searchParams\.delete/);
assert.match(proxySource, /sensitiveNames\.indexOf\(String\(name\)\.toLowerCase\(\)\)/);
assert.doesNotMatch(proxySource, /server\.replace\(\\\/$/);
// Execute the real web grouping function instead of relying only on source
// pattern checks. This protects the one-row/multiple-source Live TV contract.
const testableAppSource = appSource.replace(/\r\n/g, '\n').replace(
  '  renderApp();\n}());',
  '  window.__veloraTest = { groupLiveTvChannels, filteredLiveChannelGroups, liveRowAction, liveSourceLabel, selectLiveTvPlaybackSource, liveTvSourceIdentifier, selectSubtitleStream, durableStorageValue, saveDurableStorageValue, applyPerformanceMode, image };\n}());'
);
assert.match(testableAppSource, /window\.__veloraTest = \{ groupLiveTvChannels, filteredLiveChannelGroups, liveRowAction, liveSourceLabel, selectLiveTvPlaybackSource, liveTvSourceIdentifier, selectSubtitleStream, durableStorageValue, saveDurableStorageValue, applyPerformanceMode, image \}/, 'web app test hook was not injected');
const preferenceValues = Object.create(null);
const testLocalStorage = {
  getItem(key) { return preferenceValues[key] || null; },
  setItem(key, value) { preferenceValues[key] = String(value); },
  removeItem() {}
};
const testSessionStorage = {
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
  localStorage: testLocalStorage,
  sessionStorage: testSessionStorage,
  setTimeout,
  clearTimeout,
  addEventListener() {},
  removeEventListener() {}
};
vm.runInNewContext(testableAppSource, {
  window: testWindow,
  document: testDocument,
  navigator: testWindow.navigator,
  localStorage: testLocalStorage,
  sessionStorage: testSessionStorage,
  setTimeout,
  clearTimeout,
  console
});
const storageBeforeFailureTest = testWindow.localStorage;
testWindow.localStorage = {
  getItem() { throw new Error('storage blocked'); },
  setItem() { throw new Error('storage blocked'); },
  removeItem() { throw new Error('storage blocked'); }
};
assert.equal(testWindow.__veloraTest.durableStorageValue('unavailable', 'fallback'), 'fallback');
assert.doesNotThrow(() => testWindow.__veloraTest.saveDurableStorageValue('unavailable', 'value'));
testWindow.localStorage = storageBeforeFailureTest;
testWindow.__veloraTest.applyPerformanceMode('performance');
assert.equal(testDocument.documentElement.dataset.veloraPerformance, 'performance');
testWindow.__veloraTest.applyPerformanceMode('unknown');
assert.equal(testDocument.documentElement.dataset.veloraPerformance, 'automatic');
assert.equal(testWindow.__veloraTest.image('poster'), '/Items/poster/Images/Primary?maxWidth=600');
testWindow.localStorage.setItem('veloraPerformanceMode', 'performance');
assert.equal(testWindow.__veloraTest.image('poster'), '/Items/poster/Images/Primary?maxWidth=320');
testWindow.localStorage.setItem('veloraPerformanceMode', 'quality');
assert.equal(testWindow.__veloraTest.image('poster'), '/Items/poster/Images/Primary?maxWidth=900');
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
assert.equal(testWindow.__veloraTest.liveRowAction(groupedLiveTv[0]), 'source-picker');
assert.deepEqual(
  Array.from(groupedLiveTv[0].channels, channel => channel.MediaSources[0].Id),
  ['source-main', 'source-iptv']
);
const aliasedSourceGroup = testWindow.__veloraTest.groupLiveTvChannels([
  { Id: 'tuner-channel', ChannelNumber: '42', Name: 'DAZN F1', MediaSources: [{ Id: 'source-tuner' }] },
  { Id: 'iptv-channel', ChannelNumber: '42', Name: '  DAZN   F1 ', MediaSources: [{ Id: 'source-iptv' }] }
]);
assert.equal(aliasedSourceGroup.length, 1);
assert.equal(aliasedSourceGroup[0].channels.length, 2);
assert.equal(testWindow.__veloraTest.liveRowAction(aliasedSourceGroup[0]), 'source-picker');
const mergedAliasGroups = testWindow.__veloraTest.groupLiveTvChannels([
  { Id: 'shared-id', ChannelNumber: '10', Name: 'Canal A', MediaSources: [{ Id: 'source-a' }] },
  { Id: 'other-id', ChannelNumber: '20', Name: 'Canal B', MediaSources: [{ Id: 'source-b' }] },
  { Id: 'shared-id', ChannelNumber: '20', Name: 'Canal B', MediaSources: [{ Id: 'source-c' }] }
]);
assert.equal(mergedAliasGroups.length, 1);
assert.equal(mergedAliasGroups[0].channels.length, 3);
assert.equal(
  testWindow.__veloraTest.liveSourceLabel({ MediaSources: [{ Name: 'Fuente IPTV' }] }, 2),
  'Fuente IPTV'
);
assert.equal(
  testWindow.__veloraTest.liveTvSourceIdentifier({ MediaSources: [{ LiveStreamId: 'stream-iptv' }] }),
  'stream-iptv'
);
assert.equal(
  testWindow.__veloraTest.selectLiveTvPlaybackSource([
    { Id: 'source-main', DirectStreamUrl: '/main.m3u8' },
    { LiveStreamId: 'stream-iptv', DirectStreamUrl: '/iptv.m3u8' }
  ], 'stream-iptv').DirectStreamUrl,
  '/iptv.m3u8'
);
const duplicateGroup = testWindow.__veloraTest.groupLiveTvChannels([
  { Id: 'duplicate-channel', Name: 'Noticias' },
  { Id: 'duplicate-channel', Name: 'Noticias' }
]);
assert.equal(duplicateGroup.length, 1);
assert.equal(duplicateGroup[0].channels.length, 1);
const enrichedDuplicateGroup = testWindow.__veloraTest.groupLiveTvChannels([
  { Id: 'enriched-channel', Name: 'Noticias', MediaSources: [{ Id: 'source-main' }] },
  {
    Id: 'enriched-channel',
    Name: 'Noticias',
    CurrentProgram: { Name: 'Ahora' },
    UserData: { IsFavorite: true },
    MediaSources: [{ Id: 'source-main' }]
  }
]);
assert.equal(enrichedDuplicateGroup[0].channels.length, 1);
assert.equal(enrichedDuplicateGroup[0].channels[0].CurrentProgram.Name, 'Ahora');
assert.equal(testWindow.__veloraTest.liveRowAction(duplicateGroup[0]), 'open-item');
const unnamedSourceGroup = testWindow.__veloraTest.groupLiveTvChannels([
  {
    Id: 'unnamed-source-channel',
    Name: 'DAZN F1',
    MediaSources: [{ Name: 'Principal', LiveStreamId: 'stream-main', Protocol: 'hls' }]
  },
  {
    Id: 'unnamed-source-channel',
    Name: 'DAZN F1',
    MediaSources: [{ Name: 'IPTV', LiveStreamId: 'stream-iptv', Protocol: 'hls' }]
  }
]);
assert.equal(unnamedSourceGroup.length, 1);
assert.equal(unnamedSourceGroup[0].channels.length, 2);
const metadataGroup = testWindow.__veloraTest.groupLiveTvChannels([
  { Id: 'metadata-channel', Name: 'Canal antiguo', MediaSources: [{ Id: 'source-a' }] },
  { Id: 'metadata-channel', Name: 'Canal actual', CurrentProgram: { Name: 'Ahora' }, UserData: { IsFavorite: true }, MediaSources: [{ Id: 'source-b' }] }
]);
assert.equal(metadataGroup[0].primary.Name, 'Canal actual');
assert.equal(metadataGroup[0].primary.CurrentProgram.Name, 'Ahora');
const filterInput = [
  {
    Id: 'filtered-channel',
    Name: 'Deportes',
    Tags: ['Deportes'],
    MediaSources: [{ Id: 'source-main' }]
  },
  {
    Id: 'filtered-channel',
    Name: 'Deportes',
    UserData: { IsFavorite: true },
    MediaSources: [{ Id: 'source-iptv' }]
  }
];
preferenceValues.veloraLiveFavorites = 'true';
preferenceValues.veloraLiveGroup = 'all';
const favoriteGroups = testWindow.__veloraTest.filteredLiveChannelGroups(filterInput);
assert.equal(favoriteGroups.length, 1);
assert.equal(favoriteGroups[0].channels.length, 2);
preferenceValues.veloraLiveFavorites = 'false';
preferenceValues.veloraLiveGroup = 'Deportes';
const taggedGroups = testWindow.__veloraTest.filteredLiveChannelGroups(filterInput);
assert.equal(taggedGroups.length, 1);
assert.equal(taggedGroups[0].channels.length, 2);
const prototypeNamedGroup = testWindow.__veloraTest.groupLiveTvChannels([
  { Id: '__proto__', Name: 'Canal especial' }
]);
assert.equal(prototypeNamedGroup.length, 1);
assert.equal(prototypeNamedGroup[0].channelId, '__proto__');
console.log('web security and library interaction tests passed');

const selectSubtitleStream = testWindow.__veloraTest.selectSubtitleStream;
const subtitleTracks = [
  { Type: 'Subtitle', Index: 1, Language: 'es', IsDefault: true, IsForced: false },
  { Type: 'Subtitle', Index: 2, Language: 'en', IsDefault: false, IsForced: true },
  { Type: 'Subtitle', Index: 3, Language: 'es', IsDefault: false, IsForced: true }
];
assert.equal(selectSubtitleStream(subtitleTracks, 'forced', 'auto').Index, 2);
assert.equal(selectSubtitleStream(subtitleTracks, 'forced', 'es').Index, 3);
assert.equal(selectSubtitleStream(subtitleTracks, 'preferred', 'auto').Index, 1);
assert.equal(selectSubtitleStream(subtitleTracks, 'off', 'auto').Index, 1);
console.log('web subtitle preference tests passed');
