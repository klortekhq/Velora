/* Velora browser media proxy.
 * Media elements cannot set Authorization headers themselves. This same-origin
 * worker adds the Jellyfin token only on the server request, so playback URLs
 * do not expose credentials to history, logs or referrers.
 */
'use strict';

var credentials = { server: '', token: '' };

self.addEventListener('install', function (event) {
  self.skipWaiting();
});

self.addEventListener('activate', function (event) {
  event.waitUntil(self.clients.claim());
});

self.addEventListener('message', function (event) {
  var data = event.data || {};
  if (data.type === 'velora-credentials' && typeof data.server === 'string' && typeof data.token === 'string') {
    credentials = { server: data.server.replace(/\/$/, ''), token: data.token };
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
      if (target.origin !== new URL(credentials.server).origin) {
        return new Response('Invalid Velora media target', { status: 403 });
      }
    } catch (error) {
      return new Response('Invalid Velora media target', { status: 400 });
    }
    var headers = new Headers(event.request.headers);
    headers.set('X-Emby-Token', credentials.token);
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
