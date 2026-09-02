(function () {
  'use strict';

  var APP_VERSION = '1.3.0';

  var LANGUAGE_OPTIONS = [
    { value: 'auto', label: 'Automático', native: 'Automático' },
    { value: 'es', label: 'Español', native: 'Español' },
    { value: 'en', label: 'Inglés', native: 'English' },
    { value: 'pt', label: 'Portugués', native: 'Português' },
    { value: 'fr', label: 'Francés', native: 'Français' },
    { value: 'de', label: 'Alemán', native: 'Deutsch' },
    { value: 'it', label: 'Italiano', native: 'Italiano' },
    { value: 'ja', label: 'Japonés', native: '日本語' },
    { value: 'ko', label: 'Coreano', native: '한국어' },
    { value: 'zh', label: 'Chino', native: '中文' },
    { value: 'ru', label: 'Ruso', native: 'Русский' },
    { value: 'ar', label: 'Árabe', native: 'العربية' },
    { value: 'tr', label: 'Turco', native: 'Türkçe' }
  ];

  var TRANSLATIONS = {
    es: {
      server: 'Servidor', user: 'Usuario', password: 'Contraseña', signIn: 'Iniciar sesión',
      connectServer: 'Conecta tu servidor Jellyfin', serverPlaceholder: 'https://servidor:8096',
      library: 'Tu biblioteca', libraryDescription: 'Películas, series y televisión en directo',
      searchPlaceholder: 'Buscar películas y series', search: 'Buscar', all: 'Todo', movies: 'Películas',
      series: 'Series', live: 'Televisión en directo', back: 'Atrás', play: 'Reproducir',
      close: 'Cerrar', noDescription: 'Sin descripción disponible.', refresh: 'Actualizar', logout: 'Salir',
      loading: 'Cargando biblioteca…', retry: 'Reintentar', player: 'Reproductor', fullscreen: 'Pantalla completa', exitFullscreen: 'Salir de pantalla completa',
      settings: 'Ajustes', languageSettings: 'Idioma y reproducción', appLanguage: 'Idioma de la aplicación',
      automatic: 'Automático (idioma del dispositivo)', preferredAudio: 'Audio preferido', audioAuto: 'Automático / servidor',
      subtitles: 'Subtítulos', subtitleOff: 'Desactivados', subtitlePreferred: 'Preferidos', subtitleForced: 'Forzados',
      subtitleAuto: 'Automáticos', subtitleLanguage: 'Idioma de subtítulos', save: 'Guardar', cancel: 'Cancelar',
      saved: 'Preferencias guardadas', settingDescription: 'Se aplican al próximo contenido y se guardan en este dispositivo.',
      cast: 'Reparto', actorWorks: 'Películas y series de este actor', noActorWorks: 'No hay otros títulos disponibles.', personError: 'No se pudo cargar la filmografía',
      sortAndFilter: 'Ordenar y filtrar', sortName: 'Nombre', sortDateAdded: 'Fecha de incorporación', sortPremiere: 'Fecha de estreno', sortRuntime: 'Duración', sortRating: 'Valoración de la comunidad', favorites: 'Favoritos', playbackState: 'Estado de reproducción', playbackAll: 'Todos', playbackWatched: 'Vistos', playbackUnwatched: 'No vistos',
      liveAll: 'Todos los canales', liveFavorites: 'Solo favoritos', liveGroup: 'Grupo de canales', liveNoChannels: 'No hay canales disponibles', liveSources: 'fuentes', liveSourceOption: 'Opción',
      loginError: 'No se pudo iniciar sesión', playbackError: 'El dispositivo no puede reproducir este formato directamente.'
    },
    en: {
      server: 'Server', user: 'User', password: 'Password', signIn: 'Sign in', connectServer: 'Connect your Jellyfin server',
      serverPlaceholder: 'https://server:8096', library: 'Your library', libraryDescription: 'Movies, series and live television',
      searchPlaceholder: 'Search movies and series', search: 'Search', all: 'All', movies: 'Movies', series: 'Series',
      live: 'Live TV', back: 'Back', play: 'Play', close: 'Close', noDescription: 'No description available.',
      refresh: 'Refresh', logout: 'Sign out', loading: 'Loading library…', retry: 'Retry', player: 'Player',
      fullscreen: 'Fullscreen', exitFullscreen: 'Exit fullscreen', settings: 'Settings', languageSettings: 'Language and playback', appLanguage: 'App language',
      automatic: 'Automatic (device language)', preferredAudio: 'Preferred audio', audioAuto: 'Automatic / server',
      subtitles: 'Subtitles', subtitleOff: 'Disabled', subtitlePreferred: 'Preferred', subtitleForced: 'Forced',
      subtitleAuto: 'Automatic', subtitleLanguage: 'Subtitle language', save: 'Save', cancel: 'Cancel',
      saved: 'Preferences saved', settingDescription: 'Applied to new playback and saved on this device.', cast: 'Cast',
      actorWorks: 'Movies and series with this actor', noActorWorks: 'No other titles available.', personError: 'Could not load filmography',
      sortAndFilter: 'Sort and filter', sortName: 'Name', sortDateAdded: 'Date added', sortPremiere: 'Premiere date', sortRuntime: 'Runtime', sortRating: 'Community rating', favorites: 'Favorites', playbackState: 'Playback state', playbackAll: 'All', playbackWatched: 'Watched', playbackUnwatched: 'Unwatched',
      liveAll: 'All channels', liveFavorites: 'Favorites only', liveGroup: 'Channel group', liveNoChannels: 'No channels available', liveSources: 'sources', liveSourceOption: 'Option',
      loginError: 'Sign-in failed', playbackError: 'This device cannot play this format directly.'
    },
    pt: {
      server: 'Servidor', user: 'Utilizador', password: 'Palavra-passe', signIn: 'Iniciar sessão', connectServer: 'Ligue o seu servidor Jellyfin',
      serverPlaceholder: 'https://servidor:8096', library: 'A sua biblioteca', libraryDescription: 'Filmes, séries e televisão em direto',
      searchPlaceholder: 'Pesquisar filmes e séries', search: 'Pesquisar', all: 'Tudo', movies: 'Filmes', series: 'Séries',
      live: 'TV em direto', back: 'Voltar', play: 'Reproduzir', close: 'Fechar', noDescription: 'Sem descrição disponível.',
      refresh: 'Atualizar', logout: 'Sair', loading: 'A carregar biblioteca…', retry: 'Tentar novamente', player: 'Reprodutor',
      fullscreen: 'Ecrã inteiro', exitFullscreen: 'Sair do ecrã inteiro', settings: 'Definições', languageSettings: 'Idioma e reprodução', appLanguage: 'Idioma da aplicação',
      automatic: 'Automático (idioma do dispositivo)', preferredAudio: 'Áudio preferido', audioAuto: 'Automático / servidor',
      subtitles: 'Legendas', subtitleOff: 'Desativadas', subtitlePreferred: 'Preferidas', subtitleForced: 'Forçadas',
      subtitleAuto: 'Automáticas', subtitleLanguage: 'Idioma das legendas', save: 'Guardar', cancel: 'Cancelar',
      saved: 'Preferências guardadas', settingDescription: 'Aplicadas à próxima reprodução e guardadas neste dispositivo.',
      loginError: 'Não foi possível iniciar sessão', playbackError: 'Este dispositivo não consegue reproduzir este formato diretamente.'
    },
    fr: {
      server: 'Serveur', user: 'Utilisateur', password: 'Mot de passe', signIn: 'Se connecter', connectServer: 'Connectez votre serveur Jellyfin',
      serverPlaceholder: 'https://serveur:8096', library: 'Votre bibliothèque', libraryDescription: 'Films, séries et télévision en direct',
      searchPlaceholder: 'Rechercher des films et séries', search: 'Rechercher', all: 'Tout', movies: 'Films', series: 'Séries',
      live: 'TV en direct', back: 'Retour', play: 'Lire', close: 'Fermer', noDescription: 'Aucune description disponible.',
      refresh: 'Actualiser', logout: 'Se déconnecter', loading: 'Chargement de la bibliothèque…', retry: 'Réessayer', player: 'Lecteur',
      fullscreen: 'Plein écran', exitFullscreen: 'Quitter le plein écran', settings: 'Réglages', languageSettings: 'Langue et lecture', appLanguage: 'Langue de l’application',
      automatic: 'Automatique (langue de l’appareil)', preferredAudio: 'Audio préféré', audioAuto: 'Automatique / serveur',
      subtitles: 'Sous-titres', subtitleOff: 'Désactivés', subtitlePreferred: 'Préférés', subtitleForced: 'Forcés',
      subtitleAuto: 'Automatiques', subtitleLanguage: 'Langue des sous-titres', save: 'Enregistrer', cancel: 'Annuler',
      saved: 'Préférences enregistrées', settingDescription: 'Appliquées à la prochaine lecture et enregistrées sur cet appareil.',
      loginError: 'Échec de la connexion', playbackError: 'Cet appareil ne peut pas lire ce format directement.'
    },
    de: {
      server: 'Server', user: 'Benutzer', password: 'Passwort', signIn: 'Anmelden', connectServer: 'Jellyfin-Server verbinden',
      serverPlaceholder: 'https://server:8096', library: 'Deine Bibliothek', libraryDescription: 'Filme, Serien und Live-Fernsehen',
      searchPlaceholder: 'Filme und Serien suchen', search: 'Suchen', all: 'Alle', movies: 'Filme', series: 'Serien',
      live: 'Live-TV', back: 'Zurück', play: 'Wiedergeben', close: 'Schließen', noDescription: 'Keine Beschreibung verfügbar.',
      refresh: 'Aktualisieren', logout: 'Abmelden', loading: 'Bibliothek wird geladen…', retry: 'Erneut versuchen', player: 'Player',
      fullscreen: 'Vollbild', exitFullscreen: 'Vollbild beenden', settings: 'Einstellungen', languageSettings: 'Sprache und Wiedergabe', appLanguage: 'App-Sprache',
      automatic: 'Automatisch (Gerätesprache)', preferredAudio: 'Bevorzugte Audiosprache', audioAuto: 'Automatisch / Server',
      subtitles: 'Untertitel', subtitleOff: 'Deaktiviert', subtitlePreferred: 'Bevorzugt', subtitleForced: 'Erzwungen',
      subtitleAuto: 'Automatisch', subtitleLanguage: 'Untertitelsprache', save: 'Speichern', cancel: 'Abbrechen',
      saved: 'Einstellungen gespeichert', settingDescription: 'Für die nächste Wiedergabe angewendet und auf diesem Gerät gespeichert.',
      loginError: 'Anmeldung fehlgeschlagen', playbackError: 'Dieses Gerät kann dieses Format nicht direkt wiedergeben.'
    },
    it: {
      server: 'Server', user: 'Utente', password: 'Password', signIn: 'Accedi', connectServer: 'Collega il server Jellyfin',
      serverPlaceholder: 'https://server:8096', library: 'La tua libreria', libraryDescription: 'Film, serie e televisione in diretta',
      searchPlaceholder: 'Cerca film e serie', search: 'Cerca', all: 'Tutto', movies: 'Film', series: 'Serie', live: 'TV in diretta',
      back: 'Indietro', play: 'Riproduci', close: 'Chiudi', noDescription: 'Nessuna descrizione disponibile.', refresh: 'Aggiorna',
      logout: 'Esci', loading: 'Caricamento libreria…', retry: 'Riprova', player: 'Lettore', fullscreen: 'Schermo intero',
      settings: 'Impostazioni', languageSettings: 'Lingua e riproduzione', appLanguage: 'Lingua dell’app', automatic: 'Automatico (lingua del dispositivo)',
      preferredAudio: 'Audio preferito', audioAuto: 'Automatico / server', subtitles: 'Sottotitoli', subtitleOff: 'Disattivati', subtitlePreferred: 'Preferiti',
      subtitleForced: 'Forzati', subtitleAuto: 'Automatici', subtitleLanguage: 'Lingua dei sottotitoli', save: 'Salva', cancel: 'Annulla',
      saved: 'Preferenze salvate', settingDescription: 'Applicate alla prossima riproduzione e salvate su questo dispositivo.', loginError: 'Accesso non riuscito',
      playbackError: 'Questo dispositivo non può riprodurre direttamente questo formato.'
    },
    ja: { server: 'サーバー', user: 'ユーザー', password: 'パスワード', signIn: 'ログイン', connectServer: 'Jellyfinサーバーに接続', serverPlaceholder: 'https://server:8096', library: 'ライブラリ', libraryDescription: '映画、シリーズ、ライブテレビ', searchPlaceholder: '映画とシリーズを検索', search: '検索', all: 'すべて', movies: '映画', series: 'シリーズ', live: 'ライブTV', back: '戻る', play: '再生', close: '閉じる', noDescription: '説明はありません。', refresh: '更新', logout: 'ログアウト', loading: 'ライブラリを読み込み中…', retry: '再試行', player: 'プレーヤー', fullscreen: '全画面', settings: '設定', languageSettings: '言語と再生', appLanguage: 'アプリの言語', automatic: '自動（端末の言語）', preferredAudio: '優先音声', audioAuto: '自動 / サーバー', subtitles: '字幕', subtitleOff: '無効', subtitlePreferred: '優先', subtitleForced: '強制', subtitleAuto: '自動', subtitleLanguage: '字幕の言語', save: '保存', cancel: 'キャンセル', saved: '設定を保存しました', settingDescription: '次の再生に適用され、この端末に保存されます。', loginError: 'ログインに失敗しました', playbackError: 'この端末はこの形式を直接再生できません。' },
    ko: { server: '서버', user: '사용자', password: '비밀번호', signIn: '로그인', connectServer: 'Jellyfin 서버 연결', serverPlaceholder: 'https://server:8096', library: '라이브러리', libraryDescription: '영화, 시리즈 및 실시간 TV', searchPlaceholder: '영화 및 시리즈 검색', search: '검색', all: '전체', movies: '영화', series: '시리즈', live: '실시간 TV', back: '뒤로', play: '재생', close: '닫기', noDescription: '설명이 없습니다.', refresh: '새로고침', logout: '로그아웃', loading: '라이브러리 로드 중…', retry: '다시 시도', player: '플레이어', fullscreen: '전체 화면', settings: '설정', languageSettings: '언어 및 재생', appLanguage: '앱 언어', automatic: '자동 (기기 언어)', preferredAudio: '선호 오디오', audioAuto: '자동 / 서버', subtitles: '자막', subtitleOff: '사용 안 함', subtitlePreferred: '선호', subtitleForced: '강제', subtitleAuto: '자동', subtitleLanguage: '자막 언어', save: '저장', cancel: '취소', saved: '환경설정 저장됨', settingDescription: '다음 재생에 적용되며 이 기기에 저장됩니다.', loginError: '로그인하지 못했습니다', playbackError: '이 기기에서 이 형식을 직접 재생할 수 없습니다.' },
    zh: { server: '服务器', user: '用户', password: '密码', signIn: '登录', connectServer: '连接 Jellyfin 服务器', serverPlaceholder: 'https://server:8096', library: '媒体库', libraryDescription: '电影、剧集和直播电视', searchPlaceholder: '搜索电影和剧集', search: '搜索', all: '全部', movies: '电影', series: '剧集', live: '直播电视', back: '返回', play: '播放', close: '关闭', noDescription: '暂无描述。', refresh: '刷新', logout: '退出登录', loading: '正在加载媒体库…', retry: '重试', player: '播放器', fullscreen: '全屏', settings: '设置', languageSettings: '语言与播放', appLanguage: '应用语言', automatic: '自动（设备语言）', preferredAudio: '首选音频', audioAuto: '自动 / 服务器', subtitles: '字幕', subtitleOff: '关闭', subtitlePreferred: '首选', subtitleForced: '强制', subtitleAuto: '自动', subtitleLanguage: '字幕语言', save: '保存', cancel: '取消', saved: '偏好已保存', settingDescription: '应用于下一次播放并保存在此设备上。', loginError: '登录失败', playbackError: '此设备无法直接播放此格式。' },
    ru: { server: 'Сервер', user: 'Пользователь', password: 'Пароль', signIn: 'Войти', connectServer: 'Подключите сервер Jellyfin', serverPlaceholder: 'https://server:8096', library: 'Медиатека', libraryDescription: 'Фильмы, сериалы и прямой эфир', searchPlaceholder: 'Поиск фильмов и сериалов', search: 'Поиск', all: 'Все', movies: 'Фильмы', series: 'Сериалы', live: 'Прямой эфир', back: 'Назад', play: 'Воспроизвести', close: 'Закрыть', noDescription: 'Описание отсутствует.', refresh: 'Обновить', logout: 'Выйти', loading: 'Загрузка медиатеки…', retry: 'Повторить', player: 'Плеер', fullscreen: 'Полный экран', settings: 'Настройки', languageSettings: 'Язык и воспроизведение', appLanguage: 'Язык приложения', automatic: 'Автоматически (язык устройства)', preferredAudio: 'Предпочитаемое аудио', audioAuto: 'Автоматически / сервер', subtitles: 'Субтитры', subtitleOff: 'Отключены', subtitlePreferred: 'Предпочитаемые', subtitleForced: 'Принудительные', subtitleAuto: 'Автоматически', subtitleLanguage: 'Язык субтитров', save: 'Сохранить', cancel: 'Отмена', saved: 'Настройки сохранены', settingDescription: 'Применяются к следующему воспроизведению и сохраняются на устройстве.', loginError: 'Не удалось войти', playbackError: 'Устройство не может напрямую воспроизвести этот формат.' },
    ar: { server: 'الخادم', user: 'المستخدم', password: 'كلمة المرور', signIn: 'تسجيل الدخول', connectServer: 'الاتصال بخادم Jellyfin', serverPlaceholder: 'https://server:8096', library: 'مكتبتك', libraryDescription: 'أفلام ومسلسلات وتلفزيون مباشر', searchPlaceholder: 'البحث عن الأفلام والمسلسلات', search: 'بحث', all: 'الكل', movies: 'أفلام', series: 'مسلسلات', live: 'تلفزيون مباشر', back: 'رجوع', play: 'تشغيل', close: 'إغلاق', noDescription: 'لا يوجد وصف.', refresh: 'تحديث', logout: 'تسجيل الخروج', loading: 'جارٍ تحميل المكتبة…', retry: 'إعادة المحاولة', player: 'المشغل', fullscreen: 'ملء الشاشة', settings: 'الإعدادات', languageSettings: 'اللغة والتشغيل', appLanguage: 'لغة التطبيق', automatic: 'تلقائي (لغة الجهاز)', preferredAudio: 'الصوت المفضل', audioAuto: 'تلقائي / الخادم', subtitles: 'الترجمات', subtitleOff: 'معطلة', subtitlePreferred: 'مفضلة', subtitleForced: 'إجبارية', subtitleAuto: 'تلقائية', subtitleLanguage: 'لغة الترجمة', save: 'حفظ', cancel: 'إلغاء', saved: 'تم حفظ التفضيلات', settingDescription: 'تُطبق على التشغيل التالي وتُحفظ على هذا الجهاز.', loginError: 'فشل تسجيل الدخول', playbackError: 'لا يمكن لهذا الجهاز تشغيل هذا التنسيق مباشرة.' },
    tr: { server: 'Sunucu', user: 'Kullanıcı', password: 'Şifre', signIn: 'Giriş yap', connectServer: 'Jellyfin sunucunuzu bağlayın', serverPlaceholder: 'https://sunucu:8096', library: 'Kitaplığınız', libraryDescription: 'Filmler, diziler ve canlı televizyon', searchPlaceholder: 'Film ve dizi ara', search: 'Ara', all: 'Tümü', movies: 'Filmler', series: 'Diziler', live: 'Canlı TV', back: 'Geri', play: 'Oynat', close: 'Kapat', noDescription: 'Açıklama yok.', refresh: 'Yenile', logout: 'Çıkış yap', loading: 'Kitaplık yükleniyor…', retry: 'Tekrar dene', player: 'Oynatıcı', fullscreen: 'Tam ekran', settings: 'Ayarlar', languageSettings: 'Dil ve oynatma', appLanguage: 'Uygulama dili', automatic: 'Otomatik (cihaz dili)', preferredAudio: 'Tercih edilen ses', audioAuto: 'Otomatik / sunucu', subtitles: 'Altyazılar', subtitleOff: 'Kapalı', subtitlePreferred: 'Tercih edilen', subtitleForced: 'Zorunlu', subtitleAuto: 'Otomatik', subtitleLanguage: 'Altyazı dili', save: 'Kaydet', cancel: 'İptal', saved: 'Tercihler kaydedildi', settingDescription: 'Bir sonraki oynatmaya uygulanır ve bu cihaza kaydedilir.', loginError: 'Giriş yapılamadı', playbackError: 'Bu cihaz bu biçimi doğrudan oynatamıyor.' }
  };

  function languageCode() {
    var selected = localStorage.veloraLanguage || 'auto';
    if (selected !== 'auto' && TRANSLATIONS[selected]) return selected;
    var candidates = navigator.languages || [navigator.language || 'es'];
    for (var i = 0; i < candidates.length; i += 1) {
      var code = String(candidates[i] || '').toLowerCase().split('-')[0];
      if (TRANSLATIONS[code]) return code;
    }
    return 'es';
  }

  var LIVE_SOURCE_TRANSLATIONS = {
    es: { liveSources: 'fuentes', liveSourceOption: 'Opción' },
    en: { liveSources: 'sources', liveSourceOption: 'Option' },
    pt: { liveSources: 'fontes', liveSourceOption: 'Opção' },
    fr: { liveSources: 'sources', liveSourceOption: 'Option' },
    de: { liveSources: 'Quellen', liveSourceOption: 'Option' },
    it: { liveSources: 'sorgenti', liveSourceOption: 'Opzione' },
    ja: { liveSources: 'ソース', liveSourceOption: 'オプション' },
    ko: { liveSources: '소스', liveSourceOption: '옵션' },
    zh: { liveSources: '来源', liveSourceOption: '选项' },
    ru: { liveSources: 'источника', liveSourceOption: 'Вариант' },
    ar: { liveSources: 'مصادر', liveSourceOption: 'خيار' },
    tr: { liveSources: 'kaynak', liveSourceOption: 'Seçenek' }
  };

  function t(key) {
    var code = languageCode();
    if (LIVE_SOURCE_TRANSLATIONS[code] && LIVE_SOURCE_TRANSLATIONS[code][key]) {
      return LIVE_SOURCE_TRANSLATIONS[code][key];
    }
    var current = TRANSLATIONS[code] || TRANSLATIONS.es;
    return current[key] || TRANSLATIONS.es[key] || key;
  }

  function preference(key, fallback) {
    return localStorage[key] || fallback;
  }

  function savePreference(key, value) {
    localStorage[key] = value;
  }

  // Authentication state is intentionally scoped to the browser session. The
  // server URL and UI preferences may persist, but a Jellyfin access token
  // must not survive as durable localStorage data. Migrate the legacy token
  // once so existing users are not unexpectedly signed out.
  function sessionValue(key) {
    try { return window.sessionStorage.getItem(key) || ''; } catch (error) { return ''; }
  }

  function saveSessionValue(key, value) {
    try { window.sessionStorage.setItem(key, value); } catch (error) { /* session storage may be unavailable */ }
  }

  function removeSessionValue(key) {
    try { window.sessionStorage.removeItem(key); } catch (error) { /* best effort */ }
  }

  var legacyToken = localStorage.veloraToken || '';
  if (legacyToken && !sessionValue('veloraToken')) saveSessionValue('veloraToken', legacyToken);
  if (legacyToken) delete localStorage.veloraToken;
  var legacyUserId = localStorage.veloraUserId || '';
  if (legacyUserId && !sessionValue('veloraUserId')) saveSessionValue('veloraUserId', legacyUserId);
  if (legacyUserId) delete localStorage.veloraUserId;

  var root = document.querySelector('#app');
  var state = {
    server: localStorage.veloraServer || '',
    token: sessionValue('veloraToken'),
    userId: sessionValue('veloraUserId'),
    items: [],
    liveChannels: [],
    query: '',
    settingsOpen: false,
    playingItem: null,
    liveTvPlaySessionId: ''
  };

  function syncMediaProxyCredentials() {
    if (!navigator.serviceWorker || !state.token || !state.server) return;
    navigator.serviceWorker.ready.then(function (registration) {
      var worker = navigator.serviceWorker.controller || registration.active;
      if (worker) worker.postMessage({ type: 'velora-credentials', server: state.server, token: state.token });
    }).catch(function () { /* playback will report that the secure proxy is unavailable */ });
  }

  function clearMediaProxyCredentials() {
    if (!navigator.serviceWorker) return;
    navigator.serviceWorker.ready.then(function (registration) {
      var worker = navigator.serviceWorker.controller || registration.active;
      if (worker) worker.postMessage({ type: 'velora-clear-credentials' });
    }).catch(function () { /* worker may not be available */ });
  }

  if (navigator.serviceWorker) {
    navigator.serviceWorker.register('media-proxy-sw.js').then(syncMediaProxyCredentials).catch(function () {
      /* Older TV browsers can still use Jellyfin's browser-compatible URL fallback. */
    });
  }

  function base() {
    return state.server.replace(/\/$/, '');
  }

  function normalizeServerUrl(value) {
    try {
      var parsed = new URL(String(value || '').trim());
      if ((parsed.protocol !== 'http:' && parsed.protocol !== 'https:') ||
          !parsed.hostname || parsed.username || parsed.password || parsed.search || parsed.hash ||
          (parsed.port && (Number(parsed.port) < 1 || Number(parsed.port) > 65535))) return '';
      parsed.pathname = parsed.pathname.replace(/\/+$/, '');
      return parsed.toString().replace(/\/$/, '');
    } catch (error) {
      return '';
    }
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
      'X-Emby-Token': state.token,
      'Accept-Language': languageCode()
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
      '/Images/Primary?maxWidth=600';
  }

  function hydrateProtectedImages(scope) {
    var container = scope || document;
    Array.prototype.forEach.call(container.querySelectorAll('[data-velora-image-id]'), function (element) {
      if (element.dataset.veloraImageState) return;
      element.dataset.veloraImageState = 'loading';
      var id = element.getAttribute('data-velora-image-id');
      fetch(image(id), {
        headers: {
          Accept: 'image/*',
          'X-Emby-Token': state.token
        }
      }).then(function (response) {
        if (!response.ok) throw Error('No se pudo cargar la imagen');
        return response.blob();
      }).then(function (blob) {
        element.src = URL.createObjectURL(blob);
        element.dataset.veloraImageState = 'ready';
      }).catch(function () {
        element.dataset.veloraImageState = 'error';
        element.removeAttribute('src');
      });
    });
  }

  function streamTarget(item) {
    var params = [
      'static=true',
      'mediasourceid=' + encodeURIComponent(item.MediaSources && item.MediaSources[0] ? item.MediaSources[0].Id : '')
    ];
    var streams = item.MediaSources && item.MediaSources[0] ? (item.MediaSources[0].MediaStreams || []) : [];
    var audio = preference('veloraAudioLanguage', 'auto');
    var audioStream = audio !== 'auto' && streams.find(function (candidate) {
      return candidate.Type === 'Audio' && String(candidate.Language || '').toLowerCase().split('-')[0] === audio;
    });
    if (audioStream && audioStream.Index != null) params.push('AudioStreamIndex=' + encodeURIComponent(audioStream.Index));
    var subtitleMode = preference('veloraSubtitleMode', 'off');
    if (subtitleMode !== 'off') {
      var subtitleLanguage = preference('veloraSubtitleLanguage', 'auto');
      var subtitleStream = streams.find(function (candidate) {
        if (candidate.Type !== 'Subtitle') return false;
        if (subtitleLanguage === 'auto') return !!candidate.IsDefault || !!candidate.IsForced;
        return String(candidate.Language || '').toLowerCase().split('-')[0] === subtitleLanguage;
      });
      if (subtitleStream && subtitleStream.Index != null) {
        params.push('SubtitleStreamIndex=' + encodeURIComponent(subtitleStream.Index));
        params.push('SubtitleMethod=Encode');
      }
    }
    return base() + '/Videos/' + encodeURIComponent(item.Id) + '/stream?' + params.join('&');
  }

  function stream(item) {
    return protectedMediaUrl(streamTarget(item));
  }

  function protectedMediaUrl(target) {
    // A native <video> element cannot attach Authorization headers. The
    // same-origin service worker proxies this request and adds X-Emby-Token,
    // keeping the token out of the address bar, history and referrers.
    return '/__velora_media?url=' + encodeURIComponent(target);
  }

  function sanitizeMediaTarget(value) {
    try {
      var target = new URL(value, base());
      var server = new URL(base());
      if (target.origin !== server.origin) return '';
      target.searchParams.delete('api_key');
      target.searchParams.delete('ApiKey');
      return target.href;
    } catch (error) {
      return '';
    }
  }

  function liveTvStreamTarget(channel) {
    var sourceId = channel.MediaSources && channel.MediaSources[0] && channel.MediaSources[0].Id;
    var sourceParam = sourceId ? '&MediaSourceId=' + encodeURIComponent(sourceId) : '';
    return api('/Items/' + encodeURIComponent(channel.Id) + '/PlaybackInfo?UserId=' +
      encodeURIComponent(state.userId) + '&StartTimeTicks=0&IsPlayback=true&AutoOpenLiveStream=true' + sourceParam, {
        // Jellyfin accepts MediaSourceId for providers exposing alternatives
        // under one channel identity. Omit it when unavailable for backwards
        // compatibility with standard Live TV channel responses.
        method: 'POST',
        body: JSON.stringify({})
      }).then(function (data) {
        var source = data && data.MediaSources && data.MediaSources[0];
        if (!source) return '';
        state.liveTvPlaySessionId = source.PlaySessionId || data.PlaySessionId || '';
        // PlaybackInfo returns a playable server URL for Live TV. A source
        // Path can be a filesystem path, not a browser media endpoint, so it
        // must never be promoted into a URL fallback.
        return sanitizeMediaTarget(source.TranscodingUrl || source.DirectStreamUrl || '');
      });
  }

  function waitForMediaProxy() {
    if (!navigator.serviceWorker) return Promise.resolve(false);
    if (navigator.serviceWorker.controller) {
      syncMediaProxyCredentials();
      return new Promise(function (resolve) { window.setTimeout(function () { resolve(true); }, 30); });
    }
    return new Promise(function (resolve) {
      var settled = false;
      var finish = function (available) {
        if (settled) return;
        settled = true;
        navigator.serviceWorker.removeEventListener('controllerchange', onControllerChange);
        resolve(available);
      };
      var onControllerChange = function () {
        syncMediaProxyCredentials();
        window.setTimeout(function () { finish(!!navigator.serviceWorker.controller); }, 30);
      };
      navigator.serviceWorker.addEventListener('controllerchange', onControllerChange);
      navigator.serviceWorker.ready.then(function () {
        if (navigator.serviceWorker.controller) {
          onControllerChange();
        } else {
          window.setTimeout(function () { finish(false); }, 1500);
        }
      }).catch(function () { finish(false); });
    });
  }

  function login() {
    root.innerHTML = '<section class="login" aria-labelledby="loginTitle">' +
      '<h1 id="loginTitle">Velora</h1>' +
      '<p class="muted">' + esc(t('connectServer')) + '</p>' +
      '<label for="server">' + esc(t('server')) + '</label>' +
      '<input id="server" value="' + esc(state.server) + '" placeholder="' + esc(t('serverPlaceholder')) + '" autocomplete="url">' +
      '<label for="user">' + esc(t('user')) + '</label>' +
      '<input id="user" placeholder="' + esc(t('user')) + '" autocomplete="username">' +
      '<label for="password">' + esc(t('password')) + '</label>' +
      '<input id="password" type="password" placeholder="' + esc(t('password')) + '" autocomplete="current-password">' +
      '<button type="button" class="primary" id="signIn">' + esc(t('signIn')) + '</button>' +
      '<p id="loginError" class="error" role="alert"></p>' +
      '</section>';
    document.querySelector('#signIn').onclick = authenticate;
    document.querySelector('#password').onkeydown = function (event) {
      if (event.key === 'Enter') authenticate();
    };
  }

  function authenticate() {
    var error = document.querySelector('#loginError');
    state.server = normalizeServerUrl(document.querySelector('#server').value);
    if (!state.server) {
      error.textContent = t('loginError');
      return;
    }
    var username = document.querySelector('#user').value.trim();
    fetch(base() + '/Users/AuthenticateByName', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Accept-Language': languageCode(),
      'X-Emby-Authorization': 'MediaBrowser Client="Velora Web", Device="Browser", DeviceId="velora-web", Version="' + APP_VERSION + '", Language="' + languageCode() + '"'
      },
      body: JSON.stringify({ Username: username, Password: document.querySelector('#password').value })
    }).then(function (response) {
      if (!response.ok) throw Error(t('loginError'));
      return response.json();
    }).then(function (data) {
      state.token = data.AccessToken;
      state.userId = data.User && data.User.Id ? data.User.Id : '';
      localStorage.veloraServer = state.server;
      localStorage.veloraUser = username;
      saveSessionValue('veloraToken', state.token);
      saveSessionValue('veloraUserId', state.userId);
      syncMediaProxyCredentials();
      return renderApp();
    }).catch(function () {
      error.textContent = t('loginError');
    });
  }

  function loadItems() {
    if (!state.userId) {
      return api('/Users/Me').then(function (me) {
        state.userId = me.Id;
        saveSessionValue('veloraUserId', state.userId);
      }).then(loadItems);
    }
    var params = 'Recursive=true&IncludeItemTypes=Movie%2CSeries%2CLiveTvChannel&' +
      'SortBy=DateCreated&SortOrder=Descending&Limit=150&' +
      'Fields=Overview%2CProductionYear%2CDateCreated%2CPremiereDate%2CRunTimeTicks%2CCommunityRating%2CCriticRating%2CPrimaryImageAspectRatio%2CMediaSources%2CUserData%2CPeople%2CSeriesName%2CSeriesId%2CIndexNumber%2CParentIndexNumber';
    return Promise.all([
      api('/Users/' + state.userId + '/Items?' + params),
      loadLiveTvChannels()
    ]).then(function (responses) {
      state.items = responses[0].Items || [];
      state.liveChannels = responses[1].Items || [];
    });
  }

  function loadLiveTvChannels() {
    var channelPath = '/LiveTv/Channels?UserId=' + encodeURIComponent(state.userId) +
      '&AddCurrentProgram=true&EnableUserData=true&EnableImages=true&Fields=Overview';
    return api(channelPath).then(function (response) {
      var channels = response.Items || [];
      if (!channels.length) return channels;
      var now = new Date();
      var until = new Date(now.getTime() + 6 * 60 * 60 * 1000);
      var query = '/LiveTv/Programs?UserId=' + encodeURIComponent(state.userId) +
        '&ChannelIds=' + channels.map(function (channel) { return encodeURIComponent(channel.Id); }).join('%2C') +
        '&MinStartDate=' + encodeURIComponent(now.toISOString()) +
        '&MaxEndDate=' + encodeURIComponent(until.toISOString()) + '&Limit=500';
      return api(query).then(function (programResponse) {
        var programs = programResponse.Items || [];
        return channels.map(function (channel) {
          var channelPrograms = programs.filter(function (program) { return program.ChannelId === channel.Id; })
            .sort(function (left, right) { return String(left.StartDate || '').localeCompare(String(right.StartDate || '')); });
          return Object.assign({}, channel, {
            UpcomingProgram: channelPrograms.find(function (program) { return new Date(program.EndDate || 0) > now; }) || null
          });
        });
      });
    }).catch(function () { return []; });
  }

  function section(title, items) {
    if (!items.length) return '';
    return '<section><h2>' + esc(title) + '</h2><div class="grid">' +
      items.map(function (item) {
        return '<article class="card" tabindex="0" role="button" data-id="' + esc(item.Id) + '">' +
          '<img loading="lazy" data-velora-image-id="' + esc(item.Id) + '" alt="">' +
          '<div class="label">' + esc(item.Name) + '</div></article>';
      }).join('') + '</div></section>';
  }

  function liveProgramTime(program) {
    if (!program || !program.StartDate || !program.EndDate) return '';
    var start = new Date(program.StartDate);
    var end = new Date(program.EndDate);
    if (Number.isNaN(start.getTime()) || Number.isNaN(end.getTime())) return '';
    return start.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) +
      ' – ' + end.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  }

  function liveProgramProgress(program) {
    if (!program || !program.StartDate || !program.EndDate) return 0;
    var start = new Date(program.StartDate).getTime();
    var end = new Date(program.EndDate).getTime();
    if (!Number.isFinite(start) || !Number.isFinite(end) || end <= start) return 0;
    return Math.max(0, Math.min(1, (Date.now() - start) / (end - start)));
  }

  function liveSection(title, channels) {
    if (!channels.length) return '';
    channels = groupLiveTvChannels(channels);
    return '<section><h2>' + esc(title) + '</h2><div class="live-grid">' +
      channels.map(function (group) {
        var channel = group.primary;
        var current = channel.CurrentProgram || null;
        var upcoming = channel.UpcomingProgram || null;
        var progress = Math.round(liveProgramProgress(current) * 100);
        return '<article class="live-row" tabindex="0" role="button" data-id="' + esc(channel.Id) + '">' +
          '<img loading="lazy" data-velora-image-id="' + esc(channel.Id) + '" alt="">' +
          '<div class="live-copy"><strong>' + esc(channel.Name || '') + '</strong>' +
          (group.channels.length > 1 ? '<small class="live-sources">' + group.channels.length + ' ' + esc(t('liveSources')) + '</small>' : '') +
          (current ? '<span>' + esc(current.Name || '') + '</span><small>' + esc(liveProgramTime(current)) + '</small>' +
            '<div class="progress" aria-label="' + progress + '%"><i style="width:' + progress + '%"></i></div>' :
            '<span class="muted">' + esc(t('noDescription')) + '</span>') +
          (upcoming ? '<small class="live-next">' + esc(upcoming.Name || '') + ' · ' + esc(liveProgramTime(upcoming)) + '</small>' : '') +
          '</div></article>';
      }).join('') + '</div></section>';
  }

  function groupLiveTvChannels(channels) {
    var groups = {};
    var order = [];
    channels.forEach(function (channel) {
      var key = String(channel.Id || '').trim() ||
        ('fallback:' + String(channel.ChannelNumber || '') + '|' + String(channel.Name || '').trim().toLocaleLowerCase(languageCode()));
      if (!groups[key]) {
        groups[key] = { channelId: key, primary: channel, channels: [] };
        order.push(groups[key]);
      }
      groups[key].channels.push(channel);
    });
    return order;
  }

  function liveSourceLabel(channel, index) {
    var tags = Array.isArray(channel.Tags) ? channel.Tags.filter(Boolean) : [];
    return String(tags[0] || channel.ChannelType || channel.ServiceName ||
      (t('liveSourceOption') + ' ' + index));
  }

  function showLiveSourcePicker(group) {
    closeDetails();
    root.insertAdjacentHTML('beforeend', '<div class="modal" id="liveSourcePicker" role="dialog" aria-modal="true" aria-labelledby="liveSourceTitle">' +
      '<div class="modal-card"><button type="button" class="close" id="liveSourceClose">' + esc(t('back')) + '</button>' +
      '<h2 id="liveSourceTitle">' + esc(group.primary.Name || '') + '</h2>' +
      '<p class="muted">' + group.channels.length + ' ' + esc(t('liveSources')) + '</p>' +
      '<div class="source-options">' + group.channels.map(function (channel, index) {
        return '<button type="button" class="source-option" data-source-index="' + index + '">' +
          esc(liveSourceLabel(channel, index + 1)) + '</button>';
      }).join('') + '</div></div></div>');
    document.querySelector('#liveSourceClose').onclick = function () { document.querySelector('#liveSourcePicker').remove(); };
    Array.prototype.forEach.call(document.querySelectorAll('#liveSourcePicker [data-source-index]'), function (button) {
      button.onclick = function () {
        var selected = group.channels[Number(button.getAttribute('data-source-index'))];
        document.querySelector('#liveSourcePicker').remove();
        play(selected);
      };
    });
    var first = document.querySelector('#liveSourcePicker [data-source-index]');
    if (first) first.focus();
  }

  function liveChannelGroups(channels) {
    var groups = {};
    channels.forEach(function (channel) {
      var tags = Array.isArray(channel.Tags) ? channel.Tags : [];
      var names = tags.filter(Boolean).map(String);
      if (channel.ChannelType && names.indexOf(channel.ChannelType) === -1) names.push(channel.ChannelType);
      if (channel.ServiceName && names.indexOf(channel.ServiceName) === -1) names.push(channel.ServiceName);
      if (!names.length) names.push('Sin grupo');
      names.forEach(function (name) { groups[name] = true; });
    });
    return Object.keys(groups).sort(function (left, right) { return left.localeCompare(right); });
  }

  function filteredLiveChannels(channels) {
    var favoriteOnly = preference('veloraLiveFavorites', 'false') === 'true';
    var group = preference('veloraLiveGroup', 'all');
    return channels.filter(function (channel) {
      if (favoriteOnly && !(channel.UserData && channel.UserData.IsFavorite)) return false;
      if (group === 'all') return true;
      var tags = Array.isArray(channel.Tags) ? channel.Tags.map(String) : [];
      return tags.indexOf(group) !== -1 || channel.ChannelType === group || channel.ServiceName === group ||
        (group === 'Sin grupo' && !tags.length && !channel.ChannelType && !channel.ServiceName);
    });
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
    Array.prototype.forEach.call(document.querySelectorAll('.live-row'), function (row) {
      var open = function () { openItem(row.getAttribute('data-id')); };
      row.onclick = open;
      row.onkeydown = function (event) {
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

  function sortedLibraryItems(items) {
    var favoritesOnly = preference('veloraLibraryFavorites', 'false') === 'true';
    var playback = preference('veloraLibraryPlayback', 'all');
    var sort = preference('veloraLibrarySort', 'name');
    var filtered = items.filter(function (item) {
      if (favoritesOnly && !(item.UserData && item.UserData.IsFavorite)) return false;
      if (playback === 'watched' && !(item.UserData && item.UserData.Played)) return false;
      if (playback === 'unwatched' && item.UserData && item.UserData.Played) return false;
      return true;
    });
    return filtered.sort(function (left, right) {
      var a;
      var b;
      if (sort === 'dateAdded') { a = left.DateCreated || ''; b = right.DateCreated || ''; }
      else if (sort === 'premiere') { a = left.PremiereDate || ''; b = right.PremiereDate || ''; }
      else if (sort === 'runtime') { a = left.RunTimeTicks || 0; b = right.RunTimeTicks || 0; }
      else if (sort === 'rating') { a = left.CommunityRating == null ? -1 : left.CommunityRating; b = right.CommunityRating == null ? -1 : right.CommunityRating; }
      else { a = String(left.Name || '').toLocaleLowerCase(languageCode()); b = String(right.Name || '').toLocaleLowerCase(languageCode()); }
      if (a < b) return -1;
      if (a > b) return 1;
      return String(left.Id).localeCompare(String(right.Id));
    });
  }

  function showPersonFilmography(personId, personName) {
    var details = document.querySelector('#details');
    if (!details) return;
    details.querySelector('.person-results').innerHTML = '<p class="muted">' + esc(t('loading')) + '</p>';
    api('/Users/' + encodeURIComponent(state.userId) + '/Items?Recursive=true&PersonIds=' + encodeURIComponent(personId) +
      '&IncludeItemTypes=Movie%2CSeries&Fields=Overview%2CProductionYear%2CPrimaryImageAspectRatio%2CMediaSources%2CUserData&Limit=100&SortBy=DateCreated&SortOrder=Descending')
      .then(function (data) {
        var works = data.Items || [];
        var html = '<h2>' + esc(t('actorWorks')) + '</h2>';
        if (!works.length) html += '<p class="muted">' + esc(t('noActorWorks')) + '</p>';
        else html += '<div class="grid">' + works.map(function (work) {
          return '<article class="card" tabindex="0" role="button" data-id="' + esc(work.Id) + '">' +
            '<img loading="lazy" data-velora-image-id="' + esc(work.Id) + '" alt=""><div class="label">' + esc(work.Name) + '</div></article>';
        }).join('') + '</div>';
        details.querySelector('.person-results').innerHTML = html;
        details.querySelector('.person-results').setAttribute('aria-label', personName);
        hydrateProtectedImages(details.querySelector('.person-results'));
        bindCards();
      })
      .catch(function () {
        details.querySelector('.person-results').innerHTML = '<p class="error">' + esc(t('personError')) + '</p>';
      });
  }

  function openItem(id) {
    var item = state.items.concat(state.liveChannels || []).find(function (candidate) { return candidate.Id === id; });
    if (!item) return;
    if (item.Type === 'LiveTvChannel') {
      var group = groupLiveTvChannels(state.liveChannels || []).find(function (candidate) { return candidate.channelId === id; });
      if (group && group.channels.length > 1) {
        showLiveSourcePicker(group);
        return;
      }
    }
    closeDetails();
    root.insertAdjacentHTML('beforeend', '<div class="modal" id="details" role="dialog" aria-modal="true" aria-labelledby="detailsTitle">' +
      '<div class="modal-card">' +
      '<button type="button" class="close" id="detailsClose">' + esc(t('back')) + '</button>' +
      '<img class="detail-image" data-velora-image-id="' + esc(item.Id) + '" alt="">' +
      '<h1 id="detailsTitle">' + esc(item.Name) + '</h1>' +
      '<p class="muted">' + esc(item.ProductionYear || '') + (item.Type === 'Series' ? ' · ' + esc(t('series')) : '') + '</p>' +
      '<p>' + esc(item.Overview || t('noDescription')) + '</p>' +
      '<section class="cast"><h2>' + esc(t('cast')) + '</h2><div class="cast-list">' +
      (item.People || []).filter(function (person) { return person.Type === 'Actor' || person.Type === 'GuestStar'; }).slice(0, 12).map(function (person) {
        if (!person.Id) return '<span class="cast-person">' + esc(person.Name || '') + '</span>';
        return '<button type="button" class="cast-person" data-person-id="' + esc(person.Id) + '" data-person-name="' + esc(person.Name || '') + '">' + esc(person.Name || '') + '</button>';
      }).join('') + '</div></section>' +
      '<div class="person-results"></div>' +
      '<button type="button" class="primary" id="playItem">' + esc(t('play')) + '</button>' +
      '</div></div>');
    document.querySelector('#detailsClose').onclick = closeDetails;
    hydrateProtectedImages(document.querySelector('#details'));
    Array.prototype.forEach.call(document.querySelectorAll('#details [data-person-id]'), function (personButton) {
      personButton.onclick = function () {
        showPersonFilmography(personButton.getAttribute('data-person-id'), personButton.getAttribute('data-person-name') || '');
      };
    });
    document.querySelector('#playItem').onclick = function () {
      closeDetails();
      play(item);
    };
  }

  function updateFullscreenButton(player) {
    var button = player && player.querySelector('#fullscreen');
    if (!button) return;
    var active = Boolean(document.fullscreenElement || document.webkitFullscreenElement || player.classList.contains('video-fullscreen'));
    button.textContent = t(active ? 'exitFullscreen' : 'fullscreen');
    button.setAttribute('aria-label', button.textContent);
  }

  function toggleFullscreen(player, video) {
    var active = Boolean(document.fullscreenElement || document.webkitFullscreenElement || player.classList.contains('video-fullscreen'));
    if (active) {
      var exit = document.exitFullscreen || document.webkitExitFullscreen;
      if (exit) {
        var exitResult = exit.call(document);
        if (exitResult && exitResult.catch) exitResult.catch(function () {});
      } else {
        player.classList.remove('video-fullscreen');
      }
      updateFullscreenButton(player);
      return;
    }
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
    updateFullscreenButton(player);
    if (window.screen && window.screen.orientation && window.screen.orientation.lock) {
      var orientation = window.screen.orientation.lock('landscape');
      if (orientation && orientation.catch) orientation.catch(function () {});
    }
  }

  function closePlayer() {
    var player = document.querySelector('#player');
    if (!player) return;
    var item = state.playingItem;
    if (item && item.Type === 'LiveTvChannel') {
      var positionTicks = Math.max(0, Math.round((player.querySelector('video').currentTime || 0) * 10000000));
      var stopped = { ItemId: item.Id, PositionTicks: positionTicks };
      if (state.liveTvPlaySessionId) stopped.PlaySessionId = state.liveTvPlaySessionId;
      // Do not delay closing the UI on a server-side cleanup request. Jellyfin
      // can release the tuner asynchronously while the next view is opening.
      api('/Sessions/Playing/Stopped', { method: 'POST', body: JSON.stringify(stopped) }).catch(function () {});
    }
    if (player._veloraAbortController) player._veloraAbortController.abort();
    if (player._veloraObjectUrl) URL.revokeObjectURL(player._veloraObjectUrl);
    if (player._veloraFullscreenCleanup) player._veloraFullscreenCleanup();
    var exit = document.exitFullscreen || document.webkitExitFullscreen;
    if ((document.fullscreenElement || document.webkitFullscreenElement) && exit) exit.call(document);
    player.classList.remove('video-fullscreen');
    player.remove();
    state.playingItem = null;
    state.liveTvPlaySessionId = '';
  }

  function play(item) {
    waitForMediaProxy().then(function (available) {
      // Live streams must remain behind the authenticated same-origin proxy;
      // buffering them as a Blob would never complete. VOD gets a secure
      // fetch/Blob fallback so a static host without a service worker still
      // plays without putting the Jellyfin token in the media URL.
      if (!available && item.Type === 'LiveTvChannel') {
        toast(t('playbackError'));
        return;
      }
      var targetPromise = item.Type === 'LiveTvChannel' ? liveTvStreamTarget(item) : Promise.resolve('');
      targetPromise.then(function (liveTarget) {
        if (item.Type === 'LiveTvChannel' && !liveTarget) {
          toast(t('playbackError'));
          return;
        }
        state.playingItem = item;
        root.insertAdjacentHTML('beforeend', '<div class="video-wrap" id="player" role="dialog" aria-label="' + esc(t('player')) + '">' +
        '<video controls autoplay playsinline preload="metadata"></video>' +
        '<div class="video-controls">' +
        '<button type="button" id="fullscreen">' + esc(t('fullscreen')) + '</button>' +
        '<button type="button" id="playerSettings">' + esc(t('settings')) + '</button>' +
        '<button type="button" id="playerClose">' + esc(t('close')) + '</button>' +
        '</div></div>');
        var player = document.querySelector('#player');
        var video = player.querySelector('video');
        var sourceTarget = item.Type === 'LiveTvChannel' ? liveTarget : streamTarget(item);
        var sourceUrl = available ? protectedMediaUrl(sourceTarget) : sourceTarget;
        if (!available) player._veloraAbortController = new AbortController();
        var update = function () { updateFullscreenButton(player); };
        document.addEventListener('fullscreenchange', update);
        document.addEventListener('webkitfullscreenchange', update);
        player._veloraFullscreenCleanup = function () {
          document.removeEventListener('fullscreenchange', update);
          document.removeEventListener('webkitfullscreenchange', update);
        };
        player.querySelector('#playerClose').onclick = closePlayer;
        player.querySelector('#fullscreen').onclick = function () { toggleFullscreen(player, video); };
        player.querySelector('#playerSettings').onclick = showSettings;
        video.onerror = function () { toast(t('playbackError')); };
        video.onloadedmetadata = function () { player.querySelector('#fullscreen').focus(); };
        updateFullscreenButton(player);
        if (available) {
          video.src = sourceUrl;
        } else {
          fetch(sourceUrl, {
          headers: { 'X-Emby-Token': state.token, Accept: 'video/*' },
          cache: 'no-store',
          signal: player._veloraAbortController.signal
          }).then(function (response) {
            if (!response.ok) throw Error(t('playbackError'));
            return response.blob();
          }).then(function (blob) {
            if (!document.body.contains(player)) return;
            player._veloraObjectUrl = URL.createObjectURL(blob);
            video.src = player._veloraObjectUrl;
          }).catch(function (error) {
            if (error.name !== 'AbortError') toast(t('playbackError'));
          });
        }
      }).catch(function () {
        toast(t('playbackError'));
      });
    });
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
    var movies = sortedLibraryItems(items.filter(function (item) { return item.Type === 'Movie'; }));
    var series = sortedLibraryItems(items.filter(function (item) { return item.Type === 'Series'; }));
    var live = state.liveChannels && state.liveChannels.length ? state.liveChannels : items.filter(function (item) { return item.Type === 'LiveTvChannel'; });
    var liveGroups = liveChannelGroups(live);
    var visibleLive = filteredLiveChannels(live);
    var sort = preference('veloraLibrarySort', 'name');
    var playback = preference('veloraLibraryPlayback', 'all');
    var favoritesOnly = preference('veloraLibraryFavorites', 'false') === 'true';
    document.querySelector('#content').innerHTML = '<div class="hero">' +
      '<h1>' + esc(t('library')) + '</h1><p class="muted">' + esc(t('libraryDescription')) + '</p>' +
      '<div class="row"><input class="search" id="query" value="' + esc(state.query) + '" placeholder="' + esc(t('searchPlaceholder')) + '">' +
      '<button type="button" class="primary" id="search">' + esc(t('search')) + '</button></div>' +
      '<div class="library-filters" aria-label="' + esc(t('sortAndFilter')) + '">' +
      '<label>' + esc(t('sortAndFilter')) + '<select id="librarySort">' +
      '<option value="name"' + (sort === 'name' ? ' selected' : '') + '>' + esc(t('sortName')) + '</option>' +
      '<option value="dateAdded"' + (sort === 'dateAdded' ? ' selected' : '') + '>' + esc(t('sortDateAdded')) + '</option>' +
      '<option value="premiere"' + (sort === 'premiere' ? ' selected' : '') + '>' + esc(t('sortPremiere')) + '</option>' +
      '<option value="runtime"' + (sort === 'runtime' ? ' selected' : '') + '>' + esc(t('sortRuntime')) + '</option>' +
      '<option value="rating"' + (sort === 'rating' ? ' selected' : '') + '>' + esc(t('sortRating')) + '</option></select></label>' +
      '<label class="checkbox"><input type="checkbox" id="libraryFavorites"' + (favoritesOnly ? ' checked' : '') + '>' + esc(t('favorites')) + '</label>' +
      '<label>' + esc(t('playbackState')) + '<select id="libraryPlayback">' +
      '<option value="all"' + (playback === 'all' ? ' selected' : '') + '>' + esc(t('playbackAll')) + '</option>' +
      '<option value="watched"' + (playback === 'watched' ? ' selected' : '') + '>' + esc(t('playbackWatched')) + '</option>' +
      '<option value="unwatched"' + (playback === 'unwatched' ? ' selected' : '') + '>' + esc(t('playbackUnwatched')) + '</option></select></label></div></div>' +
      (live.length ? '<div class="live-filters" aria-label="' + esc(t('live')) + '"><label class="checkbox"><input type="checkbox" id="liveFavorites"' + (preference('veloraLiveFavorites', 'false') === 'true' ? ' checked' : '') + '>' + esc(t('liveFavorites')) + '</label>' +
      (liveGroups.length ? '<label>' + esc(t('liveGroup')) + '<select id="liveGroup"><option value="all">' + esc(t('all')) + '</option>' + liveGroups.map(function (group) { return '<option value="' + esc(group) + '"' + (preference('veloraLiveGroup', 'all') === group ? ' selected' : '') + '>' + esc(group) + '</option>'; }).join('') + '</select></label>' : '') + '</div>' : '') +
      '<nav class="tabs" aria-label="' + esc(t('library')) + '"><button type="button" class="active" data-tab="all">' + esc(t('all')) + '</button>' +
      '<button type="button" data-tab="movies">' + esc(t('movies')) + '</button><button type="button" data-tab="series">' + esc(t('series')) + '</button>' +
      (live.length ? '<button type="button" data-tab="live">' + esc(t('live')) + '</button>' : '') +
      '</nav><div id="results">' + section(t('movies'), movies) + section(t('series'), series) +
       liveSection(t('live'), visibleLive) + '</div>';

    var submit = function () {
      state.query = document.querySelector('#query').value;
      renderHome();
    };
    document.querySelector('#search').onclick = submit;
    document.querySelector('#query').onkeydown = function (event) {
      if (event.key === 'Enter') submit();
    };
    document.querySelector('#librarySort').onchange = function (event) {
      savePreference('veloraLibrarySort', event.target.value);
      renderHome();
    };
    document.querySelector('#libraryFavorites').onchange = function (event) {
      savePreference('veloraLibraryFavorites', event.target.checked ? 'true' : 'false');
      renderHome();
    };
    document.querySelector('#libraryPlayback').onchange = function (event) {
      savePreference('veloraLibraryPlayback', event.target.value);
      renderHome();
    };
    if (live.length) {
      document.querySelector('#liveFavorites').onchange = function (event) {
        savePreference('veloraLiveFavorites', event.target.checked ? 'true' : 'false');
        renderHome();
      };
      var liveGroupSelect = document.querySelector('#liveGroup');
      if (liveGroupSelect) liveGroupSelect.onchange = function (event) {
        savePreference('veloraLiveGroup', event.target.value);
        renderHome();
      };
    }
    Array.prototype.forEach.call(document.querySelectorAll('[data-tab]'), function (tab) {
      tab.onclick = function () {
        Array.prototype.forEach.call(document.querySelectorAll('[data-tab]'), function (candidate) { candidate.classList.remove('active'); });
        tab.classList.add('active');
        var view = tab.getAttribute('data-tab');
        document.querySelector('#results').innerHTML = view === 'movies' ? section(t('movies'), movies) :
          view === 'series' ? section(t('series'), series) :
          view === 'live' ? liveSection(t('live'), filteredLiveChannels(live)) :
          section(t('movies'), movies) + section(t('series'), series) + liveSection(t('live'), filteredLiveChannels(live));
        bindCards();
        hydrateProtectedImages(document.querySelector('#results'));
      };
    });
    bindCards();
    hydrateProtectedImages(document.querySelector('#results'));
  }

  function languageOptionMarkup(selected, includeAuto) {
    return LANGUAGE_OPTIONS.filter(function (option) { return includeAuto || option.value !== 'auto'; }).map(function (option) {
      return '<option value="' + esc(option.value) + '"' + (selected === option.value ? ' selected' : '') + '>' + esc(option.native) + '</option>';
    }).join('');
  }

  function showSettings() {
    if (document.querySelector('#settings')) return;
    var selectedLanguage = preference('veloraLanguage', 'auto');
    var audioLanguage = preference('veloraAudioLanguage', 'auto');
    var subtitleMode = preference('veloraSubtitleMode', 'off');
    var subtitleLanguage = preference('veloraSubtitleLanguage', 'auto');
    root.insertAdjacentHTML('beforeend', '<div class="modal" id="settings" role="dialog" aria-modal="true" aria-labelledby="settingsTitle">' +
      '<div class="modal-card settings-card">' +
      '<div class="settings-heading"><h2 id="settingsTitle">' + esc(t('settings')) + '</h2><button type="button" class="close" id="settingsClose">' + esc(t('close')) + '</button></div>' +
      '<p class="muted">' + esc(t('settingDescription')) + '</p>' +
      '<label for="appLanguage">' + esc(t('appLanguage')) + '</label>' +
      '<select id="appLanguage">' + languageOptionMarkup(selectedLanguage, true) + '</select>' +
      '<label for="audioLanguage">' + esc(t('preferredAudio')) + '</label>' +
      '<select id="audioLanguage"><option value="auto"' + (audioLanguage === 'auto' ? ' selected' : '') + '>' + esc(t('audioAuto')) + '</option>' + languageOptionMarkup(audioLanguage, false) + '</select>' +
      '<label for="subtitleMode">' + esc(t('subtitles')) + '</label>' +
      '<select id="subtitleMode">' +
      '<option value="off"' + (subtitleMode === 'off' ? ' selected' : '') + '>' + esc(t('subtitleOff')) + '</option>' +
      '<option value="preferred"' + (subtitleMode === 'preferred' ? ' selected' : '') + '>' + esc(t('subtitlePreferred')) + '</option>' +
      '<option value="forced"' + (subtitleMode === 'forced' ? ' selected' : '') + '>' + esc(t('subtitleForced')) + '</option>' +
      '<option value="auto"' + (subtitleMode === 'auto' ? ' selected' : '') + '>' + esc(t('subtitleAuto')) + '</option>' +
      '</select>' +
      '<label for="subtitleLanguage">' + esc(t('subtitleLanguage')) + '</label>' +
      '<select id="subtitleLanguage">' + languageOptionMarkup(subtitleLanguage, true) + '</select>' +
      '<div class="settings-actions"><button type="button" class="primary" id="settingsSave">' + esc(t('save')) + '</button><button type="button" id="settingsCancel">' + esc(t('cancel')) + '</button></div>' +
      '</div></div>');
    document.querySelector('#settingsClose').onclick = closeSettings;
    document.querySelector('#settingsCancel').onclick = closeSettings;
    document.querySelector('#settingsSave').onclick = function () {
      savePreference('veloraLanguage', document.querySelector('#appLanguage').value);
      savePreference('veloraAudioLanguage', document.querySelector('#audioLanguage').value);
      savePreference('veloraSubtitleMode', document.querySelector('#subtitleMode').value);
      savePreference('veloraSubtitleLanguage', document.querySelector('#subtitleLanguage').value);
      document.documentElement.lang = languageCode();
      closeSettings();
      var video = document.querySelector('#player video');
      if (video && state.playingItem) {
        var position = video.currentTime || 0;
        video.src = stream(state.playingItem);
        video.addEventListener('loadedmetadata', function resumePlayback() {
          video.removeEventListener('loadedmetadata', resumePlayback);
          try { video.currentTime = position; } catch (error) { /* stream may not seek yet */ }
          video.play().catch(function () {});
        });
        video.load();
      } else {
        renderApp();
      }
    };
  }

  function closeSettings() {
    var settings = document.querySelector('#settings');
    if (settings) settings.remove();
    state.settingsOpen = false;
  }

  function renderApp() {
    if (!state.token) {
      login();
      return Promise.resolve();
    }
    syncMediaProxyCredentials();
    document.documentElement.lang = languageCode();
    root.innerHTML = '<div class="shell"><header><div class="brand">Velora</div><div class="actions">' +
      '<button type="button" id="refresh">' + esc(t('refresh')) + '</button><button type="button" id="settingsButton">' + esc(t('settings')) + '</button><button type="button" id="logout">' + esc(t('logout')) + '</button>' +
      '</div></header><div id="content"><div class="empty">' + esc(t('loading')) + '</div></div></div>';
    document.querySelector('#settingsButton').onclick = showSettings;
    document.querySelector('#logout').onclick = function () {
      clearMediaProxyCredentials();
      removeSessionValue('veloraToken');
      removeSessionValue('veloraUserId');
      state.token = '';
      state.userId = '';
      login();
    };
    document.querySelector('#refresh').onclick = function () {
      loadItems().then(renderHome).catch(function () { toast(t('retry')); });
    };
    return loadItems().then(renderHome).catch(function () {
      document.querySelector('#content').innerHTML = '<div class="error">' + esc(t('loginError')) +
        '<br><button type="button" class="primary" id="retry">' + esc(t('retry')) + '</button></div>';
      document.querySelector('#retry').onclick = renderApp;
    });
  }

  document.addEventListener('velora:back', function (event) {
    var settings = document.querySelector('#settings');
    var details = document.querySelector('#details');
    var player = document.querySelector('#player');
    if (settings) {
      closeSettings();
      event.preventDefault();
    } else if (player) {
      closePlayer();
      event.preventDefault();
    } else if (details) {
      closeDetails();
      event.preventDefault();
    }
  });

  renderApp();
}());
