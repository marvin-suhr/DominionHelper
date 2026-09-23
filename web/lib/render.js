// Server-rendered HTML for the landing and kingdom pages.
import { cardById, cardByName, cardSetNames } from './cards.js';

const esc = (s) => String(s)
  .replaceAll('&', '&amp;')
  .replaceAll('<', '&lt;')
  .replaceAll('>', '&gt;')
  .replaceAll('"', '&quot;')
  .replaceAll("'", '&#39;');

const titleCaseType = (t) => t.charAt(0).toUpperCase() + t.slice(1).toLowerCase();

const fmtDate = (ts) => new Date(ts).toLocaleDateString('en-US', {
  year: 'numeric', month: 'short', day: 'numeric',
});

function costBadge(card) {
  if (card.cost === null && !card.debt && !card.potion) return '';
  const parts = [];
  if (card.cost !== null) parts.push(`<span class="cost-coin">${card.cost}</span>`);
  if (card.potion) parts.push('<span class="cost-potion" title="Potion">P</span>');
  if (card.debt > 0) parts.push(`<span class="cost-debt" title="Debt">${card.debt}</span>`);
  if (card.overpay) parts.push('<span class="cost-overpay" title="May be overpaid">+</span>');
  return `<span class="cost-badge">${parts.join('')}</span>`;
}

function cardTile(card, count = 1) {
  const img = card ? `/images/${card.imageName}.webp` : '';
  const name = card ? card.name : 'Unknown card';
  const types = card ? card.types.map(titleCaseType).join(' - ') : '';
  const sets = card ? cardSetNames(card).join(', ') : '';
  return `
  <li class="card-tile">
    <div class="card-img-wrap">
      <img src="${esc(img)}" alt="${esc(name)}" loading="lazy"
           data-fallback="${esc(name.charAt(0))}">
    </div>
    <div class="card-info">
      <div class="card-title">${esc(name)}${costBadge(card ?? {})}${count > 1 ? `<span class="card-count">&times;${count}</span>` : ''}</div>
      <div class="card-types">${esc(types)}</div>
      <div class="card-sets">${esc(sets)}</div>
    </div>
  </li>`;
}

function cardSection(title, entries, { columns = 'cards', hint = '' } = {}) {
  if (!entries.length) return '';
  const items = entries.map(([card, count]) => cardTile(card, count)).join('');
  return `
  <section class="card-section">
    <h2>${esc(title)} <span class="section-count">${entries.length}</span></h2>
    ${hint ? `<p class="section-hint">${esc(hint)}</p>` : ''}
    <ul class="card-grid ${columns}">${items}</ul>
  </section>`;
}

function namedMapToEntries(map) {
  // map: { "Card Name": count } -> [ [card, count], ... ]
  return Object.entries(map)
    .map(([name, count]) => [cardByName(name), count])
    .filter(([card]) => card !== undefined);
}

function ratingSummary(rating) {
  if (!rating || rating.count === 0) {
    return '<span class="rating-summary" data-rating-summary>No ratings yet</span>';
  }
  const full = Math.round(rating.average);
  const stars = '★'.repeat(full) + '☆'.repeat(5 - full);
  return `<span class="rating-summary" data-rating-summary>
    <span class="stars">${stars}</span> ${rating.average.toFixed(1)} (${rating.count})</span>`;
}

export function kingdomPage(kingdom, baseUrl) {
  const randomEntries = kingdom.randomCards.map((id) => [cardById(id), 1]).filter(([c]) => c);
  const landscapeEntries = kingdom.landscapeCards.map((id) => [cardById(id), 1]).filter(([c]) => c);
  const basicEntries = namedMapToEntries(kingdom.basicCards);
  const dependentEntries = namedMapToEntries(kingdom.dependentCards);
  const startingEntries = namedMapToEntries(kingdom.startingCards);

  const intentUrl = `intent://${baseUrl.replace(/^https?:\/\//, '')}/${kingdom.id}` +
    `#Intent;scheme=https;package=dev.msuhr.dominionkingdoms;` +
    `S.browser_fallback_url=${encodeURIComponent(`${baseUrl}/${kingdom.id}`)};end`;

  return `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>${esc(kingdom.name)} - Dominion Kingdoms</title>
  <meta property="og:title" content="${esc(kingdom.name)}">
  <meta property="og:description" content="A shared Dominion kingdom (${randomEntries.length} cards). Open it in the Dominion Kingdoms app.">
  <link rel="stylesheet" href="/static/style.css">
  <link rel="icon" href="data:image/svg+xml,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 100 100'><text y='.9em' font-size='90'>🏰</text></svg>">
</head>
<body>
  <header class="page-header">
    <a class="brand" href="/">🏰 Dominion Kingdoms</a>
  </header>

  <main class="container">
    <div class="kingdom-header">
      <h1>${esc(kingdom.name)}</h1>
      <p class="kingdom-meta">
        Shared ${esc(fmtDate(kingdom.createdAt))}
        &middot; ${kingdom.playerCount} players
        &middot; ${ratingSummary(kingdom.rating)}
      </p>

      <div class="actions">
        <a id="open-in-app" class="btn btn-primary" href="${esc(`${baseUrl}/${kingdom.id}`)}" hidden>Open in the app</a>
        <button id="copy-link" class="btn" type="button">Copy link</button>
        <span id="non-android-hint" class="hint" hidden>Open this page on your Android device to launch the app.</span>
      </div>
    </div>

    <div class="rate-box" data-kingdom-id="${esc(kingdom.id)}">
      <span class="rate-label">Rate this kingdom:</span>
      <div class="rate-stars" role="radiogroup" aria-label="Rating">
        ${[1, 2, 3, 4, 5].map((n) => `<button class="rate-star" type="button" data-rating="${n}" aria-label="${n} star${n > 1 ? 's' : ''}">&#9733;</button>`).join('')}
      </div>
      <span class="rate-thanks" hidden>Thanks for rating!</span>
    </div>

    ${cardSection('Kingdom cards', randomEntries, { hint: 'The 10 kingdom stacks for this game.' })}
    ${cardSection('Landscape cards', landscapeEntries, { hint: 'Events, Ways, Projects, Landmarks, Traits - these sit next to the supply.' })}
    ${cardSection('Additional cards & tokens', dependentEntries, { hint: 'Needed because of the cards above (e.g. Trash Mat, Spoils, Curse).' })}
    ${cardSection('Basic supply', basicEntries)}
    ${cardSection('Starting decks', startingEntries, { columns: 'starting', hint: 'Each player starts the game with these cards.' })}
  </main>

  <footer class="page-footer">
    <p>Shared with <a href="/">Dominion Kingdoms</a> - companion app for the Dominion card game.</p>
  </footer>

  <script src="/static/app.js" defer></script>
</body>
</html>`;
}

export function landingPage(recent, baseUrl) {
  const list = recent.map((k) => `
    <li class="kingdom-list-item">
      <a href="/${esc(k.id)}">
        <span class="kl-name">${esc(k.name)}</span>
        <span class="kl-meta">${k.cardCount} cards${k.landscapeCount ? ` + ${k.landscapeCount} landscapes` : ''}
          ${k.ratingAverage !== null ? ` &middot; <span class="stars">${'★'.repeat(Math.round(k.ratingAverage))}</span> ${k.ratingAverage.toFixed(1)} (${k.ratingCount})` : ' &middot; unrated'}</span>
        <span class="kl-date">${esc(fmtDate(k.createdAt))}</span>
      </a>
    </li>`).join('');

  return `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Dominion Kingdoms - Share your kingdoms</title>
  <link rel="stylesheet" href="/static/style.css">
  <link rel="icon" href="data:image/svg+xml,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 100 100'><text y='.9em' font-size='90'>🏰</text></svg>">
</head>
<body>
  <header class="page-header">
    <span class="brand">🏰 Dominion Kingdoms</span>
  </header>
  <main class="container">
    <section class="hero">
      <h1>Share your Dominion kingdoms</h1>
      <p>
        Generate a random kingdom in the <strong>Dominion Kingdoms</strong> Android app,
        tap <em>Share</em>, and send the link to your friends.
        They can view every card here - and open the exact same kingdom in their app with one tap.
      </p>
    </section>

    <section class="card-section">
      <h2>Recently shared kingdoms</h2>
      ${recent.length ? `<ul class="kingdom-list">${list}</ul>` : '<p class="section-hint">Nothing here yet - share the first kingdom from the app!</p>'}
    </section>
  </main>
  <footer class="page-footer">
    <p>Fan-made companion tool for the Dominion card game by Rio Grande Games.</p>
  </footer>
</body>
</html>`;
}

export function notFoundPage(message = 'This kingdom does not exist (or was removed).') {
  return `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Not found - Dominion Kingdoms</title>
  <link rel="stylesheet" href="/static/style.css">
</head>
<body>
  <main class="container">
    <section class="hero">
      <h1>🏰 Nothing here</h1>
      <p>${esc(message)}</p>
      <p><a class="btn btn-primary" href="/">Go to the overview</a></p>
    </section>
  </main>
</body>
</html>`;
}
