(function () {
  'use strict';

  var root = document.querySelector('#app');
  var state = {
    server: localStorage.veloraServer || '',
    token: localStorage.veloraToken || '',
    userId: localStorage.veloraUserId || '',
    items: [],
    query: ''
  };

  function base() {
    return state.server.replace(/\/$/, '');
  }

  function esc(value) {
    return String(value == null ? '' : value).replace(/[&<>"']/g, function (character) {
      return { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[character];
    });
  }

  function api(path, options) {
    var request = options || {};
    var headers = {
      Accept: 'application/json',
      'Content-Type': 'application/json',
      'X-Emby-Token': state.token
    };
    Object.keys(request.headers || {}).forEach(function (key) {
      headers[key] = request.headers[key];
    });
    request.headers = headers;
    return fetch(base() + path, request).then(function (response) {
      if (!response.ok) throw Error('El servidor respondió ' + response.status);
      return response.status === 204 ? null : response.json();
    });
  }

  function image(id) {
    return base() + '/Items/' + encodeURIComponent(id) +
      '/Images/Primary?api_key=' + encodeURIComponent(state.token) + '&maxWidth=600';
  }

  function stream(id) {
    return base() + '/Videos/' + encodeURIComponent(id) +
      '/stream?static=true&api_key=' + encodeURIComponent(state.token);
  }

  function login() {
    root.innerHTML = '<section class="login" aria-labelledby="loginTitle">' +
      '<h1 id="loginTitle">Velora</h1>' +
      '<p class="muted">Conecta tu servidor Jellyfin</p>' +
      '<label for="server">Servidor</label>' +
      '<input id="server" value="' + esc(state.server) + '" placeholder="https://servidor:8096" autocomplete="url">' +
      '<label for="user">Usuario</label>' +
      '<input id="user" placeholder="Usuario" autocomplete="username">' +
      '<label for="password">Contraseña</label>' +
      '<input id="password" type="password" placeholder="Contraseña" autocomplete="current-password">' +
      '<button type="button" class="primary" id="signIn">Iniciar sesión</button>' +
      '<p id="loginError" class="error" role="alert"></p>' +
      '</section>';
    document.querySelector('#signIn').onclick = authenticate;
    document.querySelector('#password').onkeydown = function (event) {
      if (event.key === 'Enter') authenticate();
    };
  }

  function authenticate() {
    var error = document.querySelector('#loginError');
    state.server = document.querySelector('#server').value.trim();
    var username = document.querySelector('#user').value.trim();
    fetch(base() + '/Users/AuthenticateByName', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-Emby-Authorization': 'MediaBrowser Client="Velora Web", Device="Browser", DeviceId="velora-web", Version="1.2.1"'
      },
      body: JSON.stringify({ Username: username, Password: document.querySelector('#password').value })
    }).then(function (response) {
      if (!response.ok) throw Error('No se pudo iniciar sesión');
      return response.json();
    }).then(function (data) {
      state.token = data.AccessToken;
      state.userId = data.User && data.User.Id ? data.User.Id : '';
      localStorage.veloraServer = state.server;
      localStorage.veloraToken = state.token;
      localStorage.veloraUser = username;
      localStorage.veloraUserId = state.userId;
      return renderApp();
    }).catch(function (exception) {
      error.textContent = exception.message;
    });
  }

  function loadItems() {
    if (!state.userId) {
      return api('/Users/Me').then(function (me) {
        state.userId = me.Id;
        localStorage.veloraUserId = state.userId;
      }).then(loadItems);
    }
    var params = 'Recursive=true&IncludeItemTypes=Movie%2CSeries%2CLiveTvChannel&' +
      'SortBy=DateCreated&SortOrder=Descending&Limit=150&' +
      'Fields=Overview%2CProductionYear%2CPrimaryImageAspectRatio';
    return api('/Users/' + state.userId + '/Items?' + params).then(function (data) {
      state.items = data.Items || [];
    });
  }

  function section(title, items) {
    if (!items.length) return '';
    return '<section><h2>' + esc(title) + '</h2><div class="grid">' +
      items.map(function (item) {
        return '<article class="card" tabindex="0" role="button" data-id="' + esc(item.Id) + '">' +
          '<img loading="lazy" src="' + image(item.Id) + '" alt="">' +
          '<div class="label">' + esc(item.Name) + '</div></article>';
      }).join('') + '</div></section>';
  }

  function bindCards() {
    Array.prototype.forEach.call(document.querySelectorAll('.card'), function (card) {
      var open = function () { openItem(card.getAttribute('data-id')); };
      card.onclick = open;
      card.onkeydown = function (event) {
        if (event.key === 'Enter' || event.key === ' ') {
          event.preventDefault();
          open();
        }
      };
    });
  }

  function closeDetails() {
    var details = document.querySelector('#details');
    if (details) details.remove();
  }

  function openItem(id) {
    var item = state.items.find(function (candidate) { return candidate.Id === id; });
    if (!item) return;
    closeDetails();
    root.insertAdjacentHTML('beforeend', '<div class="modal" id="details" role="dialog" aria-modal="true" aria-labelledby="detailsTitle">' +
      '<div class="modal-card">' +
      '<button type="button" class="close" id="detailsClose">Atrás</button>' +
      '<img class="detail-image" src="' + image(item.Id) + '" alt="">' +
      '<h1 id="detailsTitle">' + esc(item.Name) + '</h1>' +
      '<p class="muted">' + esc(item.ProductionYear || '') + (item.Type === 'Series' ? ' · Serie' : '') + '</p>' +
      '<p>' + esc(item.Overview || 'Sin descripción disponible.') + '</p>' +
      '<button type="button" class="primary" id="playItem">Reproducir</button>' +
      '</div></div>');
    document.querySelector('#detailsClose').onclick = closeDetails;
    document.querySelector('#playItem').onclick = function () {
      closeDetails();
      play(item);
    };
  }

  function setFullscreen(player, video) {
    var target = player;
    var request = player.requestFullscreen || player.webkitRequestFullscreen;
    if (!request && video.requestFullscreen) {
      request = video.requestFullscreen;
      target = video;
    }
    if (request) {
      var result = request.call(target);
      if (result && result.catch) result.catch(function () {});
    } else if (video.webkitEnterFullscreen) {
      video.webkitEnterFullscreen();
    } else {
      player.classList.add('video-fullscreen');
    }
    if (window.screen && window.screen.orientation && window.screen.orientation.lock) {
      var orientation = window.screen.orientation.lock('landscape');
      if (orientation && orientation.catch) orientation.catch(function () {});
    }
  }

  function closePlayer() {
    var player = document.querySelector('#player');
    if (!player) return;
    if (document.fullscreenElement && document.exitFullscreen) document.exitFullscreen();
    player.remove();
  }

  function play(item) {
    root.insertAdjacentHTML('beforeend', '<div class="video-wrap" id="player" role="dialog" aria-label="Reproductor">' +
      '<video controls autoplay playsinline preload="metadata" src="' + stream(item.Id) + '"></video>' +
      '<div class="video-controls">' +
      '<button type="button" id="fullscreen">Pantalla completa</button>' +
      '<button type="button" id="playerClose">Cerrar</button>' +
      '</div></div>');
    var player = document.querySelector('#player');
    var video = player.querySelector('video');
    player.querySelector('#playerClose').onclick = closePlayer;
    player.querySelector('#fullscreen').onclick = function () { setFullscreen(player, video); };
    video.onerror = function () { toast('El dispositivo no puede reproducir este formato directamente.'); };
    video.onloadedmetadata = function () { player.querySelector('#fullscreen').focus(); };
  }

  function toast(message) {
    var previous = document.querySelector('.toast');
    if (previous) previous.remove();
    root.insertAdjacentHTML('beforeend', '<div class="toast" role="status">' + esc(message) + '</div>');
    window.setTimeout(function () {
      var current = document.querySelector('.toast');
      if (current) current.remove();
    }, 4000);
  }

  function renderHome() {
    var query = state.query.toLowerCase();
    var items = state.items.filter(function (item) {
      return !query || String(item.Name || '').toLowerCase().indexOf(query) !== -1;
    });
    var movies = items.filter(function (item) { return item.Type === 'Movie'; });
    var series = items.filter(function (item) { return item.Type === 'Series'; });
    var live = items.filter(function (item) { return item.Type === 'LiveTvChannel'; });
    document.querySelector('#content').innerHTML = '<div class="hero">' +
      '<h1>Tu biblioteca</h1><p class="muted">Películas, series y televisión en directo</p>' +
      '<div class="row"><input class="search" id="query" value="' + esc(state.query) + '" placeholder="Buscar películas y series">' +
      '<button type="button" class="primary" id="search">Buscar</button></div></div>' +
      '<nav class="tabs" aria-label="Biblioteca"><button type="button" class="active" data-tab="all">Todo</button>' +
      '<button type="button" data-tab="movies">Películas</button><button type="button" data-tab="series">Series</button>' +
      (live.length ? '<button type="button" data-tab="live">Televisión en directo</button>' : '') +
      '</nav><div id="results">' + section('Películas', movies) + section('Series', series) +
      section('Televisión en directo', live) + '</div>';

    var submit = function () {
      state.query = document.querySelector('#query').value;
      renderHome();
    };
    document.querySelector('#search').onclick = submit;
    document.querySelector('#query').onkeydown = function (event) {
      if (event.key === 'Enter') submit();
    };
    Array.prototype.forEach.call(document.querySelectorAll('[data-tab]'), function (tab) {
      tab.onclick = function () {
        Array.prototype.forEach.call(document.querySelectorAll('[data-tab]'), function (candidate) { candidate.classList.remove('active'); });
        tab.classList.add('active');
        var view = tab.getAttribute('data-tab');
        document.querySelector('#results').innerHTML = view === 'movies' ? section('Películas', movies) :
          view === 'series' ? section('Series', series) :
          view === 'live' ? section('Televisión en directo', live) :
          section('Películas', movies) + section('Series', series) + section('Televisión en directo', live);
        bindCards();
      };
    });
    bindCards();
  }

  function renderApp() {
    if (!state.token) {
      login();
      return Promise.resolve();
    }
    root.innerHTML = '<div class="shell"><header><div class="brand">Velora</div><div class="actions">' +
      '<button type="button" id="refresh">Actualizar</button><button type="button" id="logout">Salir</button>' +
      '</div></header><div id="content"><div class="empty">Cargando biblioteca…</div></div></div>';
    document.querySelector('#logout').onclick = function () {
      localStorage.removeItem('veloraToken');
      state.token = '';
      login();
    };
    document.querySelector('#refresh').onclick = function () {
      loadItems().then(renderHome).catch(function (exception) { toast(exception.message); });
    };
    return loadItems().then(renderHome).catch(function (exception) {
      document.querySelector('#content').innerHTML = '<div class="error">' + esc(exception.message) +
        '<br><button type="button" class="primary" id="retry">Reintentar</button></div>';
      document.querySelector('#retry').onclick = renderApp;
    });
  }

  document.addEventListener('velora:back', function (event) {
    var details = document.querySelector('#details');
    var player = document.querySelector('#player');
    if (player) {
      closePlayer();
      event.preventDefault();
    } else if (details) {
      closeDetails();
      event.preventDefault();
    }
  });

  renderApp();
}());
