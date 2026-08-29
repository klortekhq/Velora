import assert from 'node:assert/strict';
import fs from 'node:fs';
import vm from 'node:vm';

const source = fs.readFileSync(new URL('../platform.js', import.meta.url), 'utf8');
const appSource = fs.readFileSync(new URL('../app.js', import.meta.url), 'utf8');

assert.doesNotMatch(appSource, /\/Images\/Primary\?api_key=/);
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
