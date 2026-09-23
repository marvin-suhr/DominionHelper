// Client-side behaviour for the kingdom share page.
(function () {
  'use strict';

  var isAndroid = /Android/i.test(navigator.userAgent);

  // --- Open in app -----------------------------------------------------------
  // Chrome on Android understands the intent:// scheme. The https fallback URL
  // keeps the button working for browsers without intent support, and once the
  // app ships App Links (autoVerify intent-filter + assetlinks.json), the plain
  // https link itself will open the app directly.
  var openBtn = document.getElementById('open-in-app');
  var hint = document.getElementById('non-android-hint');
  var kingdomPath = window.location.pathname.replace(/\/+$/, '');

  if (openBtn && isAndroid) {
    var fallback = encodeURIComponent(window.location.href);
    var host = window.location.host;
    openBtn.href =
      'intent://' + host + kingdomPath +
      '#Intent;scheme=https;package=dev.msuhr.dominionkingdoms;S.browser_fallback_url=' + fallback + ';end';
    openBtn.hidden = false;
  } else if (hint) {
    hint.hidden = false;
  }

  // --- Copy link -------------------------------------------------------------
  var copyBtn = document.getElementById('copy-link');
  if (copyBtn) {
    copyBtn.addEventListener('click', function () {
      var url = window.location.href;
      var done = function () {
        copyBtn.textContent = 'Copied!';
        setTimeout(function () { copyBtn.textContent = 'Copy link'; }, 1500);
      };
      if (navigator.clipboard && navigator.clipboard.writeText) {
        navigator.clipboard.writeText(url).then(done, function () { fallbackCopy(url, done); });
      } else {
        fallbackCopy(url, done);
      }
    });
  }

  function fallbackCopy(text, done) {
    var ta = document.createElement('textarea');
    ta.value = text;
    document.body.appendChild(ta);
    ta.select();
    try { document.execCommand('copy'); done(); } catch (e) { /* ignore */ }
    document.body.removeChild(ta);
  }

  // --- Rating -----------------------------------------------------------------
  var rateBox = document.querySelector('.rate-box');
  if (rateBox) {
    var kingdomId = rateBox.getAttribute('data-kingdom-id');
    var stars = rateBox.querySelectorAll('.rate-star');
    var thanks = rateBox.querySelector('.rate-thanks');
    var storageKey = 'dk-rated-' + kingdomId;

    var lightStars = function (upTo) {
      stars.forEach(function (s) {
        s.classList.toggle('lit', Number(s.getAttribute('data-rating')) <= upTo);
      });
    };

    var alreadyRated = Number(localStorage.getItem(storageKey) || 0);
    if (alreadyRated) {
      rateBox.classList.add('rated');
      lightStars(alreadyRated);
      if (thanks) thanks.textContent = 'You rated this kingdom ' + alreadyRated + '/5.';
      if (thanks) thanks.hidden = false;
      stars.forEach(function (s) { s.disabled = true; });
    } else {
      stars.forEach(function (s) {
        s.addEventListener('mouseenter', function () { lightStars(Number(s.getAttribute('data-rating'))); });
        s.addEventListener('mouseleave', function () { lightStars(0); });
        s.addEventListener('click', function () {
          var rating = Number(s.getAttribute('data-rating'));
          stars.forEach(function (x) { x.disabled = true; });
          rateBox.classList.add('rated');
          lightStars(rating);
          fetch('/api/kingdoms/' + kingdomId + '/rate', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ rating: rating }),
          })
            .then(function (r) { return r.json(); })
            .then(function (data) {
              localStorage.setItem(storageKey, String(rating));
              var summary = document.querySelector('[data-rating-summary]');
              if (summary && data.count > 0) {
                var full = Math.round(data.average);
                summary.innerHTML =
                  '<span class="stars">' + '★'.repeat(full) + '☆'.repeat(5 - full) + '</span> ' +
                  data.average.toFixed(1) + ' (' + data.count + ')';
              }
              if (thanks) {
                thanks.textContent = 'Thanks! Average is now ' +
                  (data.average !== null && data.average !== undefined ? data.average.toFixed(1) : '?') +
                  ' (' + data.count + ' rating' + (data.count === 1 ? '' : 's') + ').';
                thanks.hidden = false;
              }
            })
            .catch(function () {
              if (thanks) {
                thanks.textContent = 'Could not save your rating - please try again.';
                thanks.hidden = false;
              }
              stars.forEach(function (x) { x.disabled = false; });
              rateBox.classList.remove('rated');
            });
        });
      });
    }
  }

  // --- Image fallback (server may not have the image folder) -------------------
  window.addEventListener('error', function (event) {
    var img = event.target;
    if (img && img.tagName === 'IMG' && !img.classList.contains('missing')) {
      img.classList.add('missing');
      var wrap = img.parentElement;
      if (wrap && !wrap.querySelector('.img-fallback')) {
        var span = document.createElement('span');
        span.className = 'img-fallback';
        span.textContent = img.getAttribute('data-fallback') || '?';
        wrap.appendChild(span);
      }
    }
  }, true);
})();
