// Dominion Kingdoms - kingdom sharing prototype
// Zero-dependency Node server (node:http + node:sqlite, Node >= 22.13).
//
// Run:        node server.js
// Env:        PORT (default 8080), BASE_URL (default http://localhost:$PORT),
//             IMAGES_DIR (default: the app's res/drawable folder),
//             CARDS_JSON_PATH, DATA_DIR
import http from 'node:http';
import { createReadStream, existsSync, statSync, readFileSync } from 'node:fs';
import { extname, join, normalize, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { insertKingdom, getKingdom, addRating, listRecentKingdoms } from './lib/db.js';
import { validateCardIds, cardById, resolveDependencies } from './lib/cards.js';
import { kingdomPage, landingPage, notFoundPage } from './lib/render.js';

const PORT = Number(process.env.PORT) || 8080;
const BASE_URL = (process.env.BASE_URL ?? `http://localhost:${PORT}`).replace(/\/$/, '');
const PUBLIC_DIR = fileURLToPath(new URL('./public/', import.meta.url));
const DATA_DIR = process.env.DATA_DIR ?? fileURLToPath(new URL('./data/', import.meta.url));
const IMAGES_DIR = process.env.IMAGES_DIR
  ?? resolve(fileURLToPath(new URL('../app/src/main/res/drawable/', import.meta.url)));

const MIME = {
  '.css': 'text/css; charset=utf-8',
  '.js': 'text/javascript; charset=utf-8',
  '.webp': 'image/webp',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.svg': 'image/svg+xml',
  '.json': 'application/json; charset=utf-8',
  '.ico': 'image/x-icon',
};

// ---------------------------------------------------------------------------
// API payload handling
// ---------------------------------------------------------------------------
const MAX_RANDOM_CARDS = 20;
const MAX_LANDSCAPE_CARDS = 10;
const MAX_NAME_LENGTH = 80; // matches Constants.KINGDOM_NAME_MAX_LENGTH in the app

class ApiError extends Error {
  constructor(status, message) {
    super(message);
    this.status = status;
  }
}

function parseIdCountMap(raw) {
  if (raw === undefined || raw === null) return null;
  if (typeof raw !== 'object' || Array.isArray(raw)) {
    throw new ApiError(400, 'basicCards/dependentCards/startingCards must be objects of { "Card Name": count }');
  }
  const result = {};
  for (const [name, count] of Object.entries(raw)) {
    const n = Number(count);
    if (!Number.isInteger(n) || n < 1 || n > 100) {
      throw new ApiError(400, `Invalid count for card "${name}": ${count}`);
    }
    result[name] = n;
  }
  return result;
}

function parseCreatePayload(body) {
  if (typeof body !== 'object' || body === null) throw new ApiError(400, 'Body must be a JSON object');

  const randomCards = body.randomCards;
  if (!Array.isArray(randomCards) || randomCards.length < 1) {
    throw new ApiError(400, 'randomCards must be a non-empty array of card ids');
  }
  if (randomCards.length > MAX_RANDOM_CARDS) {
    throw new ApiError(400, `randomCards must contain at most ${MAX_RANDOM_CARDS} cards`);
  }
  const landscapeCards = Array.isArray(body.landscapeCards) ? body.landscapeCards : [];
  if (landscapeCards.length > MAX_LANDSCAPE_CARDS) {
    throw new ApiError(400, `landscapeCards must contain at most ${MAX_LANDSCAPE_CARDS} cards`);
  }

  const dedupe = (ids) => [...new Set(ids)];
  const randomIds = dedupe(randomCards.map((id) => Number(id)));
  const landscapeIds = dedupe(landscapeCards.map((id) => Number(id)));
  if (randomIds.some((id) => !Number.isInteger(id) || id < 0) || landscapeIds.some((id) => !Number.isInteger(id) || id < 0)) {
    throw new ApiError(400, 'Card ids must be non-negative integers');
  }

  const randomCheck = validateCardIds(randomIds);
  if (!randomCheck.valid) throw new ApiError(400, `Unknown card ids in randomCards: ${randomCheck.invalid.join(', ')}`);
  const landscapeCheck = validateCardIds(landscapeIds);
  if (!landscapeCheck.valid) throw new ApiError(400, `Unknown card ids in landscapeCards: ${landscapeCheck.invalid.join(', ')}`);

  const name = typeof body.name === 'string' && body.name.trim().length > 0
    ? body.name.trim().slice(0, MAX_NAME_LENGTH)
    : 'Unnamed Kingdom';
  const playerCount = Number.isInteger(body.playerCount) && body.playerCount >= 2 && body.playerCount <= 6
    ? body.playerCount
    : 2;

  // The app normally sends fully resolved kingdoms. If the optional maps are
  // missing, resolve them server-side so minimal payloads still work.
  const providedBasic = parseIdCountMap(body.basicCards);
  const providedDependent = parseIdCountMap(body.dependentCards);
  const providedStarting = parseIdCountMap(body.startingCards);

  let basicCards = providedBasic;
  let dependentCards = providedDependent;
  let startingCards = providedStarting;
  if (basicCards === null || dependentCards === null || startingCards === null) {
    const resolved = resolveDependencies(randomIds.map((id) => cardById(id).name));
    basicCards ??= Object.fromEntries(resolved.basic);
    dependentCards ??= Object.fromEntries(resolved.dependent);
    startingCards ??= Object.fromEntries(resolved.starting);
  }

  return { name, playerCount, randomCards: randomIds, landscapeCards: landscapeIds, basicCards, dependentCards, startingCards };
}

// ---------------------------------------------------------------------------
// HTTP helpers
// ---------------------------------------------------------------------------
function sendHtml(res, status, html) {
  res.writeHead(status, { 'Content-Type': 'text/html; charset=utf-8' });
  if (res.req?.method === 'HEAD') return res.end();
  res.end(html);
}

function sendJson(res, status, data) {
  res.writeHead(status, {
    'Content-Type': 'application/json; charset=utf-8',
    'Access-Control-Allow-Origin': '*',
  });
  res.end(JSON.stringify(data));
}

function readBody(req, limit = 64 * 1024) {
  return new Promise((resolvePromise, reject) => {
    let size = 0;
    const chunks = [];
    req.on('data', (chunk) => {
      size += chunk.length;
      if (size > limit) {
        reject(new ApiError(413, 'Request body too large'));
        req.destroy();
        return;
      }
      chunks.push(chunk);
    });
    req.on('end', () => {
      const raw = Buffer.concat(chunks).toString('utf8');
      if (!raw) return resolvePromise(undefined);
      try {
        resolvePromise(JSON.parse(raw));
      } catch {
        reject(new ApiError(400, 'Body is not valid JSON'));
      }
    });
    req.on('error', reject);
  });
}

function serveFile(res, filePath, { cacheable = false } = {}) {
  const type = MIME[extname(filePath).toLowerCase()] ?? 'application/octet-stream';
  const cacheControl = cacheable ? 'public, max-age=86400' : 'no-cache';
  res.writeHead(200, { 'Content-Type': type, 'Cache-Control': cacheControl });
  if (res.req?.method === 'HEAD') return res.end();
  createReadStream(filePath).pipe(res);
}

// Must match ID_ALPHABET in lib/db.js (excludes l, I, O, 0, 1 to avoid confusion)
const ID_PATTERN = /^[a-km-zA-HJ-NP-Z2-9]{8}$/;

const server = http.createServer(async (req, res) => {
  const url = new URL(req.url, BASE_URL);
  const pathName = decodeURIComponent(url.pathname);

  // Access log - to debug reachability from devices
  const requestStart = Date.now();
  res.on('finish', () => {
    console.log(`${new Date().toISOString()} ${req.method} ${pathName} -> ${res.statusCode} (${Date.now() - requestStart}ms) from ${req.socket.remoteAddress}`);
  });

  try {
    // --- API -------------------------------------------------------------
    if (pathName === '/api/kingdoms' && req.method === 'POST') {
      const body = await readBody(req);
      const payload = parseCreatePayload(body);
      const id = insertKingdom(payload);
      return sendJson(res, 201, { id, url: `${BASE_URL}/${id}` });
    }

    let match = pathName.match(/^\/api\/kingdoms\/([a-zA-Z0-9]+)$/);
    if (match && req.method === 'GET') {
      const kingdom = getKingdom(match[1]);
      if (!kingdom) return sendJson(res, 404, { error: 'Kingdom not found' });
      return sendJson(res, 200, kingdom);
    }

    match = pathName.match(/^\/api\/kingdoms\/([a-zA-Z0-9]+)\/rate$/);
    if (match && req.method === 'POST') {
      const kingdom = getKingdom(match[1]);
      if (!kingdom) return sendJson(res, 404, { error: 'Kingdom not found' });
      const body = await readBody(req);
      const rating = Number(body?.rating);
      if (!Number.isInteger(rating) || rating < 1 || rating > 5) {
        return sendJson(res, 400, { error: 'rating must be an integer from 1 to 5' });
      }
      return sendJson(res, 200, addRating(kingdom.id, rating));
    }

    // --- App Links verification -------------------------------------------
    if (pathName === '/.well-known/assetlinks.json') {
      // Drop your real assetlinks.json into web/data/ to replace the placeholder.
      const custom = join(DATA_DIR, 'assetlinks.json');
      if (existsSync(custom)) return sendJson(res, 200, JSON.parse(readFileSync(custom, 'utf8')));
      return sendJson(res, 200, {
        _comment: 'PLACEHOLDER - replace the sha256 fingerprints with yours (see web/README.md). You can also put your real file at web/data/assetlinks.json.',
        statement: [
          {
            '@context': 'https://verifiedclowd.example/placeholder',
            target: {
              namespace: 'android_app',
              package_name: 'dev.msuhr.dominionkingdoms',
              sha256_cert_fingerprints: ['AA:BB:CC:...:YOUR_RELEASE_CERT_SHA256'],
            },
          },
          {
            '@context': 'https://verifiedclowd.example/placeholder',
            target: {
              namespace: 'android_app',
              package_name: 'dev.msuhr.dominionkingdoms.debug',
              sha256_cert_fingerprints: ['AA:BB:CC:...:YOUR_DEBUG_CERT_SHA256'],
            },
          },
        ],
      });
    }

    // --- Static assets ------------------------------------------------------
    if (pathName.startsWith('/static/')) {
      const filePath = normalize(join(PUBLIC_DIR, pathName.slice('/static/'.length)));
      if (!filePath.startsWith(normalize(PUBLIC_DIR)) || !existsSync(filePath) || !statSync(filePath).isFile()) {
        return sendHtml(res, 404, notFoundPage('File not found.'));
      }
      return serveFile(res, filePath); // no-cache: pick up edits immediately
    }

    if (pathName.startsWith('/images/')) {
      const filePath = normalize(join(IMAGES_DIR, pathName.slice('/images/'.length)));
      if (!filePath.startsWith(normalize(IMAGES_DIR)) || !existsSync(filePath) || !statSync(filePath).isFile()) {
        res.writeHead(404, { 'Content-Type': 'text/plain' });
        return res.end('image not found');
      }
      return serveFile(res, filePath, { cacheable: true }); // images rarely change
    }

    // --- Pages --------------------------------------------------------------
    if (pathName === '/' && (req.method === 'GET' || req.method === 'HEAD')) {
      return sendHtml(res, 200, landingPage(listRecentKingdoms(20), BASE_URL));
    }

    if ((req.method === 'GET' || req.method === 'HEAD') && ID_PATTERN.test(pathName.slice(1))) {
      const kingdom = getKingdom(pathName.slice(1));
      if (!kingdom) return sendHtml(res, 404, notFoundPage());
      return sendHtml(res, 200, kingdomPage(kingdom, BASE_URL));
    }

    if (pathName === '/favicon.ico') {
      res.writeHead(204);
      return res.end();
    }

    return sendHtml(res, 404, notFoundPage());
  } catch (err) {
    if (err instanceof ApiError) {
      return req.url.startsWith('/api/')
        ? sendJson(res, err.status, { error: err.message })
        : sendHtml(res, err.status, notFoundPage(err.message));
    }
    console.error('Unhandled error:', err);
    return req.url.startsWith('/api/')
      ? sendJson(res, 500, { error: 'Internal server error' })
      : sendHtml(res, 500, notFoundPage('Something went wrong on our side.'));
  }
});

server.listen(PORT, () => {
  console.log(`Dominion Kingdoms share service`);
  console.log(`  listening on  http://localhost:${PORT}`);
  console.log(`  base url      ${BASE_URL}`);
  console.log(`  images dir    ${IMAGES_DIR} ${existsSync(IMAGES_DIR) ? '(found)' : '(MISSING - card images will 404)'}`);
  console.log(`  data dir      ${DATA_DIR}`);
});
