# Dominion Kingdoms — Share Service (Prototype)

Web service behind `kingdoms.msuhr.dev/<id>`: share kingdoms generated in the
Android app, view them on any browser, rate them, and open them back in the app
with one tap.

**Zero dependencies.** Requires Node.js >= 22.13 (uses built-in `node:sqlite`
and `node:http`). No `npm install` needed.

```
web/
├── server.js          # HTTP server, routing, API, static files
├── lib/
│   ├── db.js          # SQLite storage (kingdoms + ratings)
│   ├── cards.js       # cards.json loader + port of CardDependencyResolver
│   └── render.js      # server-rendered HTML pages
├── public/            # style.css, app.js (rating, open-in-app button)
├── scripts/seed.js    # demo kingdoms
└── data/              # kingdoms.db (created at runtime), cards.json copy
```

## Run locally

```bash
# 1. Sync the card data from the app (only needed when cards.json changed):
cp ../app/src/main/assets/cards.json data/cards.json

# 2. (Optional) seed demo kingdoms:
node scripts/seed.js          # add --force to seed into a non-empty db

# 3. Start:
node server.js                # -> http://localhost:8080
```

Environment variables:

| Variable         | Default                        | Purpose                              |
|------------------|--------------------------------|--------------------------------------|
| `PORT`           | `8080`                         | Listen port                          |
| `BASE_URL`       | `http://localhost:$PORT`       | Public URL used in share links / OG tags. Set to `https://kingdoms.msuhr.dev` in production. |
| `IMAGES_DIR`     | `../app/src/main/res/drawable` | Card image folder (webp). On the server, copy that folder somewhere and point this at it. |
| `CARDS_JSON_PATH`| `data/cards.json`              | Card metadata                        |
| `DATA_DIR`       | `data/`                        | SQLite db + optional assetlinks.json |

## API

| Method | Path                        | Description                                |
|--------|-----------------------------|--------------------------------------------|
| `POST` | `/api/kingdoms`             | Create a share link, returns `{id, url}`   |
| `GET`  | `/api/kingdoms/:id`         | Full kingdom JSON (this is what the app will call when opening a shared link) |
| `POST` | `/api/kingdoms/:id/rate`    | `{rating: 1..5}` -> `{average, count}`     |
| `GET`  | `/.well-known/assetlinks.json` | App Links verification (placeholder until you add fingerprints) |
| `GET`  | `/:id`                      | HTML page for a kingdom                    |

### Create payload

```json
{
  "name": "Silly Engines",
  "playerCount": 4,
  "randomCards": [29, 10, 15, 24, 13, 31, 8, 5, 7, 19],
  "landscapeCards": [333],
  "basicCards":      { "Copper": 1, "Silver": 1, "...": 1 },
  "dependentCards":  { "Trash Mat": 1 },
  "startingCards":   { "Copper": 7, "Estate": 3 }
}
```

- `name` (optional, max 80 chars) and `playerCount` (2-6, default 2).
- `randomCards` (1-20 ids) is required; `landscapeCards` (max 10) is optional.
- The three `*Cards` maps (`"Card Name": count`) are **optional**: if any is
  missing, the server resolves all three itself, mirroring the app's
  `CardDependencyResolver` (prosperity/shelters default to "if present").
  The Android app should send fully resolved maps so both sides always agree.
- Card ids are the hardcoded ids from `cards.json` — the same values the app
  stores in `KingdomEntity.randomCardIds` / `landscapeCardIds`.

## Deploying to kingdoms.msuhr.dev

1. Copy the `web/` folder (plus `data/cards.json`) and the app's
   `app/src/main/res/drawable/*.webp` images to the server
   (e.g. `/opt/dominion-web/images`).
2. Run behind a TLS-terminating reverse proxy (Caddy / nginx + certbot):
   `BASE_URL=https://kingdoms.msuhr.dev PORT=8080 node server.js`
   and proxy `kingdoms.msuhr.dev` -> `127.0.0.1:8080`.
3. Run `node scripts/seed.js` once if you want demo content.
4. Process supervision: `systemd` unit or `pm2` both work; there are no deps
   to install.

### Enabling "Open in app" (Android App Links)

The kingdom page shows an **Open in the app** button on Android devices. For
the plain `https://kingdoms.msuhr.dev/<id>` link to open the app directly
(no chooser), the app must declare the intent-filter (not yet implemented -
see roadmap) **and** the server must prove the association:

1. Get your signing cert fingerprints:
   ```bash
   keytool -list -printcert -jarfile app-release.apk          # release
   keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey  # debug
   ```
2. Create a real `assetlinks.json` (statement list, `namespace: android_app`,
   one entry per package/fingerprint — release is `dev.msuhr.dominionkingdoms`,
   debug builds use `dev.msuhr.dominionkingdoms.debug`).
3. Put it at `web/data/assetlinks.json` — the server serves it at
   `/.well-known/assetlinks.json` (replacing the placeholder).
4. Test with: https://developers.google.com/digital-asset-links/tools/generator

## Prototype limitations (by design)

- **No auth / anti-abuse**: anyone can create kingdoms or spam ratings
  (per-browser only via localStorage). Add rate limiting + moderation before
  public launch.
- **One shared SQLite file**, no backups/migrations yet.
- **Card artwork**: the server serves the app's webp files. Hosting published
  card artwork publicly is a copyright gray area — fine for a personal fan
  prototype, be aware for a public launch.
- **No delete/edit** of shared kingdoms yet; IDs are not guessable but are
  public once shared.
- Node prints an "experimental SQLite" warning on some versions — harmless.
