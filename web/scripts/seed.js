// Seeds a few demo kingdoms so the landing page and ratings have content.
// Usage: node scripts/seed.js [--force]
// (skips seeding if kingdoms already exist, unless --force)
import { countKingdoms, insertKingdom } from '../lib/db.js';
import { cardByName, resolveDependencies } from '../lib/cards.js';

const DEMO_KINGDOMS = [
  {
    name: 'First Game Favourites',
    playerCount: 2,
    randomNames: ['Village', 'Festival', 'Market', 'Smithy', 'Laboratory', 'Woodcutter', 'Council Room', 'Cellar', 'Chapel', 'Moat'],
    landscapeNames: [],
    opts: {},
  },
  {
    name: 'Dark Ages Graveyard',
    playerCount: 4,
    randomNames: ['Fortress', 'Ironmonger', 'Rats', 'Scavenger', 'Armory', 'Forager', 'Catacombs', 'Count', 'Graverobber', 'Wandering Minstrel'],
    landscapeNames: [],
    opts: { darkAgesMode: 'IF_PRESENT' },
  },
  {
    name: 'Adventures Ahoy',
    playerCount: 3,
    randomNames: ['Page', 'Giant', 'Ranger', 'Hireling', 'Amulet', 'Duplicate', 'Magpie', 'Port', 'Dungeon', 'Guide'],
    landscapeNames: ['Ferry', 'Training'],
    opts: {},
  },
  {
    name: 'Coffers & Coin',
    playerCount: 2,
    randomNames: ['Baker', 'Candlestick Maker', 'Butcher', 'Doctor', 'Journeyman', 'Plaza', 'Champion', 'Merchant Guild', 'Masterpiece', 'Stonemason'],
    landscapeNames: [],
    opts: { prosperityMode: 'IF_PRESENT' },
  },
];

const existing = countKingdoms();
if (existing > 0 && !process.argv.includes('--force')) {
  console.log(`Database already has ${existing} kingdoms - skipping seed (use --force to add anyway).`);
  process.exit(0);
}

for (const demo of DEMO_KINGDOMS) {
  const portraits = demo.randomNames.map((n) => {
    const card = cardByName(n);
    if (!card) throw new Error(`Seed error: card "${n}" not found in cards.json`);
    return card;
  });
  const landscapes = demo.landscapeNames.map((n) => {
    const card = cardByName(n);
    if (!card) throw new Error(`Seed error: landscape "${n}" not found in cards.json`);
    return card;
  });

  // Simulate what the app will POST once sharing is integrated:
  // fully resolved basic / dependent / starting cards.
  const { basic, dependent, starting } = resolveDependencies(
    demo.randomNames,
    { prosperityMode: demo.opts.prosperityMode ?? 'IF_PRESENT', darkAgesMode: demo.opts.darkAgesMode ?? 'NEVER' },
  );

  const id = insertKingdom({
    name: demo.name,
    playerCount: demo.playerCount,
    randomCards: portraits.map((c) => c.id),
    landscapeCards: landscapes.map((c) => c.id),
    basicCards: Object.fromEntries(basic),
    dependentCards: Object.fromEntries(dependent),
    startingCards: Object.fromEntries(starting),
  });

  console.log(`Seeded "${demo.name}" -> /${id}`);
}
