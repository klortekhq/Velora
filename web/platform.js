(function () {
  'use strict';

  var userAgent = (navigator.userAgent || '').toLowerCase();
  var platform = 'web';
  if (userAgent.indexOf('vidaa') !== -1 ||
      (userAgent.indexOf('hisense') !== -1 && userAgent.indexOf('smarttv') !== -1)) {
    platform = 'vidaa';
  } else if (userAgent.indexOf('tizen') !== -1) {
    platform = 'tizen';
  } else if (userAgent.indexOf('web0s') !== -1 || userAgent.indexOf('webos') !== -1) {
    platform = 'webos';
  }

  var keyCodes = {
    left: 37,
    up: 38,
    right: 39,
    down: 40,
    enter: 13,
    back: [8, 27, 461, 10009]
  };

  function isTextControl(element) {
    if (!element || !element.tagName) return false;
    var tag = element.tagName.toLowerCase();
    return tag === 'input' || tag === 'textarea' || tag === 'select' || element.isContentEditable;
  }

  function isVisible(element) {
    if (!element || element.disabled) return false;
    var style = window.getComputedStyle(element);
    var rect = element.getBoundingClientRect();
    return style.display !== 'none' && style.visibility !== 'hidden' &&
      rect.width > 0 && rect.height > 0;
  }

  function focusables() {
    return Array.prototype.slice.call(document.querySelectorAll(
      'button, input, select, textarea, a[href], [tabindex="0"]'
    )).filter(isVisible);
  }

  function focusFirst() {
    var elements = focusables();
    if (elements.length && (!document.activeElement || document.activeElement === document.body)) {
      elements[0].focus();
    }
  }

  function moveFocus(direction) {
    var elements = focusables();
    if (!elements.length) return;
    var current = document.activeElement;
    if (elements.indexOf(current) === -1) {
      elements[0].focus();
      return;
    }

    var currentRect = current.getBoundingClientRect();
    var currentX = currentRect.left + currentRect.width / 2;
    var currentY = currentRect.top + currentRect.height / 2;
    var best = null;
    var bestScore = Infinity;

    elements.forEach(function (candidate) {
      if (candidate === current) return;
      var rect = candidate.getBoundingClientRect();
      var x = rect.left + rect.width / 2;
      var y = rect.top + rect.height / 2;
      var dx = x - currentX;
      var dy = y - currentY;
      var primary = direction === 'left' ? -dx : direction === 'right' ? dx : direction === 'up' ? -dy : dy;
      var secondary = direction === 'left' || direction === 'right' ? Math.abs(dy) : Math.abs(dx);
      if (primary <= 1) return;
      var score = primary * primary + secondary * secondary * 2;
      if (score < bestScore) {
        bestScore = score;
        best = candidate;
      }
    });

    if (best) best.focus();
  }

  function closeApplication() {
    try {
      if (platform === 'tizen' && window.tizen && window.tizen.application &&
          window.tizen.application.getCurrentApplication) {
        window.tizen.application.getCurrentApplication().exit();
        return;
      }
    } catch (error) {
      // Continue with the generic platform-independent fallback.
    }

    try {
      if (window.webOS && typeof window.webOS.platformBack === 'function') {
        window.webOS.platformBack();
        return;
      }
    } catch (error) {
      // Continue with the generic platform-independent fallback.
    }

    try {
      window.close();
    } catch (error) {
      // Some TV browsers do not expose window.close().
    }

    if (window.history.length > 1) window.history.back();
  }

  function isBackKey(code) {
    return keyCodes.back.indexOf(code) !== -1;
  }

  function init() {
    document.documentElement.setAttribute('data-velora-platform', platform);
    document.addEventListener('keydown', function (event) {
      var code = event.keyCode || event.which;
      var active = document.activeElement;

      if (isBackKey(code)) {
        if (isTextControl(active) && active.value) return;
        var backEvent;
        try {
          backEvent = new CustomEvent('velora:back', { cancelable: true });
        } catch (error) {
          backEvent = document.createEvent('Event');
          backEvent.initEvent('velora:back', true, true);
        }
        document.dispatchEvent(backEvent);
        if (backEvent.defaultPrevented) {
          event.preventDefault();
          return;
        }
        event.preventDefault();
        closeApplication();
        return;
      }

      if (isTextControl(active)) return;
      if (code === keyCodes.left || code === keyCodes.right ||
          code === keyCodes.up || code === keyCodes.down) {
        event.preventDefault();
        moveFocus(code === keyCodes.left ? 'left' : code === keyCodes.right ? 'right' :
          code === keyCodes.up ? 'up' : 'down');
      }
    }, false);

    window.setTimeout(focusFirst, 0);
  }

  window.VeloraPlatform = {
    name: platform,
    isTv: platform === 'vidaa' || platform === 'tizen' || platform === 'webos',
    // Offline media is intentionally mobile-app-only. Browser and Smart TV
    // builds must not grow download controls even if their UI is reused.
    capabilities: Object.freeze({
      supportsOfflineDownloads: false
    }),
    close: closeApplication,
    init: init
  };

  init();
}());
