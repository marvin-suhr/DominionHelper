// SQLite storage via node:sqlite (no external dependencies).
import { DatabaseSync } from 'node:sqlite';
import { mkdirSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import path from 'node:path';
import crypto from 'node:crypto';

const DATA_DIR = process.env.DATA_DIR
  ?? fileURLToPath(new URL('../data/', import.meta.url));

mkdirSync(DATA_DIR, { recursive: true });

const db = new DatabaseSync(path.join(DATA_DIR, 'kingdoms.db'));

db.exec(`
  CREATE TABLE IF NOT EXISTS kingdoms (
    id               TEXT PRIMARY KEY,
    name             TEXT NOT NULL,
    player_count     INTEGER NOT NULL DEFAULT 2,
    random_cards     TEXT NOT NULL,   -- JSON [cardId, ...] (display order)
    landscape_cards  TEXT NOT NULL,   -- JSON [cardId, ...]
    basic_cards      TEXT NOT NULL,   -- JSON { "Card Name": count, ... }
    dependent_cards  TEXT NOT NULL,   -- JSON { "Card Name": count, ... }
    starting_cards   TEXT NOT NULL,   -- JSON { "Card Name": count, ... }
    created_at       INTEGER NOT NULL
  );

  CREATE TABLE IF NOT EXISTS ratings (
    kingdom_id  TEXT NOT NULL,
    rating      INTEGER NOT NULL CHECK (rating BETWEEN 1 AND 5),
    created_at  INTEGER NOT NULL
  );

  CREATE INDEX IF NOT EXISTS idx_ratings_kingdom ON ratings(kingdom_id);
  CREATE INDEX IF NOT EXISTS idx_kingdoms_created ON kingdoms(created_at);
`);

const ID_ALPHABET = 'abcdefghijkmnopqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789';
const ID_LENGTH = 8;

function generateId() {
  const bytes = crypto.randomBytes(ID_LENGTH);
  let id = '';
  for (const b of bytes) id += ID_ALPHABET[b % ID_ALPHABET.length];
  return id;
}

const jsonOrEmpty = (value, fallback) => {
  if (value === undefined || value === null) return JSON.stringify(fallback);
  return JSON.stringify(value);
};

export function insertKingdom({ name, playerCount, randomCards, landscapeCards, basicCards, dependentCards, startingCards }) {
  if (!Array.isArray(randomCards) || randomCards.length === 0) {
    throw new Error('insertKingdom: randomCards must be a non-empty array of card ids');
  }
  for (let attempt = 0; attempt < 5; attempt++) {
    const id = generateId();
    try {
      db.prepare(`
        INSERT INTO kingdoms (id, name, player_count, random_cards, landscape_cards,
                              basic_cards, dependent_cards, starting_cards, created_at)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
      `).run(
        id,
        name,
        playerCount,
        jsonOrEmpty(randomCards, []),
        jsonOrEmpty(landscapeCards, []),
        jsonOrEmpty(basicCards, {}),
        jsonOrEmpty(dependentCards, {}),
        jsonOrEmpty(startingCards, {}),
        Date.now(),
      );
      return id;
    } catch (err) {
      if (!String(err.message).includes('UNIQUE')) throw err; // id collision -> retry
    }
  }
  throw new Error('Could not generate a unique kingdom id');
}

export function getKingdom(id) {
  const row = db.prepare('SELECT * FROM kingdoms WHERE id = ?').get(id);
  if (!row) return null;
  return {
    id: row.id,
    name: row.name,
    playerCount: row.player_count,
    randomCards: JSON.parse(row.random_cards),
    landscapeCards: JSON.parse(row.landscape_cards),
    basicCards: JSON.parse(row.basic_cards),
    dependentCards: JSON.parse(row.dependent_cards),
    startingCards: JSON.parse(row.starting_cards),
    createdAt: row.created_at,
    rating: getRating(row.id),
  };
}

export function getRating(kingdomId) {
  const row = db.prepare('SELECT AVG(rating) AS avg, COUNT(*) AS count FROM ratings WHERE kingdom_id = ?')
    .get(kingdomId);
  return {
    average: row.count > 0 ? Math.round(row.avg * 10) / 10 : null,
    count: row.count,
  };
}

export function addRating(kingdomId, rating) {
  db.prepare('INSERT INTO ratings (kingdom_id, rating, created_at) VALUES (?, ?, ?)')
    .run(kingdomId, rating, Date.now());
  return getRating(kingdomId);
}

export function listRecentKingdoms(limit = 20) {
  return db.prepare(`
    SELECT k.id, k.name, k.player_count, k.random_cards, k.landscape_cards, k.created_at,
           (SELECT AVG(rating) FROM ratings r WHERE r.kingdom_id = k.id) AS rating_avg,
           (SELECT COUNT(*) FROM ratings r WHERE r.kingdom_id = k.id)    AS rating_count
    FROM kingdoms k
    ORDER BY k.created_at DESC
    LIMIT ?
  `).all(limit).map((row) => ({
    id: row.id,
    name: row.name,
    playerCount: row.player_count,
    cardCount: JSON.parse(row.random_cards).length,
    landscapeCount: JSON.parse(row.landscape_cards).length,
    createdAt: row.created_at,
    ratingAverage: row.rating_count > 0 ? Math.round(row.rating_avg * 10) / 10 : null,
    ratingCount: row.rating_count,
  }));
}

export function countKingdoms() {
  return db.prepare('SELECT COUNT(*) AS n FROM kingdoms').get().n;
}
