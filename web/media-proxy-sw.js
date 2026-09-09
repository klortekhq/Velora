/* Velora browser media proxy.
 * Media elements cannot set Authorization headers themselves. This same-origin
 * worker adds the Jellyfin token only on the server request, so playback URLs
 * do not expose credentials to history, logs or referrers.
 */
'use strict';

var credentials = { server: '', token: '' };

function normalizeServer(value) {
  try {
    var parsed = new URL(String(value || '').trim());
    if ((parsed.protocol !== 'http:' && parsed.protocol !== 'https:') ||
        !parsed.hostname || parsed.username || parsed.password ||
        parsed.search || parsed.hash ||
        (parsed.port && (Number(parsed.port) < 1 || Number(parsed.port) > 65535))) {
      return '';
    }
    parsed.pathname = parsed.pathname.replace(/\/+$/, '');
    return parsed.toString().replace(/\/$/, '');
  } catch (error) {
    return '';
  }
}

function isAllowedMediaPath(pathname) {
  // The proxy exists only because media elements cannot attach Jellyfin
  // headers. Never turn it into a generic authenticated same-origin proxy.
  return /^\/Videos\/[^/]+\/stream(?:\.[^/]+)?$/i.test(pathname) ||
    /^\/Audio\/[^/]+\/universal$/i.test(pathname) ||
    /^\/LiveTv\/LiveStreamFiles\//i.test(pathname);
}

self.addEventListener('install', function (event) {
  self.skipWaiting();
});

self.addEventListener('activate', function (event) {
  event.waitUntil(self.clients.claim());
});

self.addEventListener('message', function (event) {
  var data = event.data || {};
  if (data.type === 'velora-credentials' && typeof data.server === 'string' && typeof data.token === 'string') {
    var server = normalizeServer(data.server);
    credentials = server && data.token ? { server: server, token: data.token } : { server: '', token: '' };
  }
  if (data.type === 'velora-clear-credentials') credentials = { server: '', token: '' };
});

self.addEventListener('fetch', function (event) {
  var requestUrl = new URL(event.request.url);
  if (requestUrl.pathname !== '/__velora_media') return;

  event.respondWith((async function () {
    var targetValue = requestUrl.searchParams.get('url');
    if (!targetValue || !credentials.server || !credentials.token) {
      return new Response('Velora media session unavailable', { status: 401 });
    }
    var target;
    try {
      target = new URL(targetValue);
      var server = new URL(credentials.server);
      if (target.origin !== server.origin || target.protocol !== server.protocol ||
          !isAllowedMediaPath(target.pathname)) {
        return new Response('Invalid Velora media target', { status: 403 });
      }
    } catch (error) {
      return new Response('Invalid Velora media target', { status: 400 });
    }
    // Defense in depth: media targets are generated without credentials, but
    // scrub credential-like query parameters again before forwarding any
    // request that reaches this worker.
      var sensitiveNames = ['api_key', 'apikey', 'access_token', 'token', 'x-emby-token', 'authorization'];
      Array.from(target.searchParams.keys()).forEach(function (name) {
        if (sensitiveNames.indexOf(String(name).toLowerCase()) !== -1) target.searchParams.delete(name);
      });
    var headers = new Headers(event.request.headers);
      headers.set('Authorization', 'MediaBrowser Token="' + credentials.token + '"');
    headers.delete('Cookie');
    return fetch(new Request(target.href, {
      method: event.request.method,
      headers: headers,
      mode: 'cors',
      credentials: 'omit',
      cache: 'no-store'
    }));
  })());
});
