// Card data access + a port of the app's CardDependencyResolver logic.
//
// The Android app is the source of truth:
//   - app/src/main/assets/cards.json          (card metadata, sparse JSON, defaults applied on load)
//   - CardDependencyResolver.kt               (dependent / basic / starting card resolution)
//   - CardNames.kt / CardDependencies.kt      (trigger lists)
// Keep this file in sync when those change. Long-term, share this as generated data.

import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import path from 'node:path';

const CARDS_JSON_PATH = process.env.CARDS_JSON_PATH
  ?? fileURLToPath(new URL('../data/cards.json', import.meta.url));

// The Kotlin serializers uppercase enum names before matching ("Base_1E" -> BASE_1E),
// and cards.json uses inconsistent casing, so we do the same here.
const norm = (s) => String(s).trim().toUpperCase();

function applyDefaults(raw) {
  return {
    id: raw.id,
    name: raw.name,
    imageName: raw.image_name,
    sets: (raw.sets ?? []).map(norm),
    types: (raw.types ?? []).map(norm),
    categories: (raw.categories ?? []).map(norm),
    cost: raw.cost ?? null,
    overpay: raw.overpay ?? false,
    specialCost: raw.special_cost ?? false,
    potion: raw.potion ?? false,
    debt: raw.debt ?? 0,
    supply: raw.supply ?? true,
    landscape: raw.landscape ?? false,
    basic: raw.basic ?? false,
  };
}

let _cards = null;
let _byId = null;
let _byName = null;

function loadCards() {
  if (_cards) return _cards;
  const raw = JSON.parse(readFileSync(CARDS_JSON_PATH, 'utf8'));
  _cards = raw.map(applyDefaults);
  _byId = new Map(_cards.map((c) => [c.id, c]));
  _byName = new Map(_cards.map((c) => [c.name, c]));
  return _cards;
}

export function allCards() {
  return loadCards();
}

export function cardById(id) {
  loadCards();
  return _byId.get(id) ?? null;
}

export function cardByName(name) {
  loadCards();
  return _byName.get(name) ?? null;
}

export function cardsByIds(ids) {
  loadCards();
  return ids.map((id) => _byId.get(id)).filter(Boolean);
}

export function validateCardIds(ids) {
  loadCards();
  const invalid = ids.filter((id) => !_byId.has(id));
  return { valid: invalid.length === 0, invalid };
}

// ---------------------------------------------------------------------------
// Set display names (mirrors model/CardEnums.kt Set.displayName)
// ---------------------------------------------------------------------------
export const SET_DISPLAY_NAMES = {
  BASE_1E: 'Dominion',
  BASE_2E: 'Dominion',
  INTRIGUE_1E: 'Intrigue',
  INTRIGUE_2E: 'Intrigue',
  SEASIDE_1E: 'Seaside',
  SEASIDE_2E: 'Seaside',
  ALCHEMY: 'Alchemy',
  PROSPERITY_1E: 'Prosperity',
  PROSPERITY_2E: 'Prosperity',
  CORNUCOPIA_1E: 'Cornucopia',
  CORNUCOPIA_GUILDS_2E: 'Cornucopia',
  HINTERLANDS_1E: 'Hinterlands',
  HINTERLANDS_2E: 'Hinterlands',
  DARK_AGES: 'Dark Ages',
  GUILDS_1E: 'Guilds',
  ADVENTURES: 'Adventures',
  EMPIRES: 'Empires',
  NOCTURNE: 'Nocturne',
  RENAISSANCE: 'Renaissance',
  MENAGERIE: 'Menagerie',
  ALLIES: 'Allies',
  PLUNDER: 'Plunder',
  RISING_SUN: 'Rising Sun',
  PROMO: 'Promo Cards',
};

export function cardSetNames(card) {
  const names = [...new Set(card.sets.map((s) => SET_DISPLAY_NAMES[s] ?? s))];
  return names;
}

// ---------------------------------------------------------------------------
// Port of CardNames.kt
// ---------------------------------------------------------------------------
const NAMES = {
  POTION: 'Potion',
  CURSE: 'Curse',

  // Piles
  BOON_PILE: 'Boon Pile',
  HEX_PILE: 'Hex Pile',
  LOOT_PILE: 'Loot Pile',
  RUINS_PILE: 'Ruins Pile',
  PRIZE_PILE: 'Prize Pile',
  REWARD_PILE: 'Reward Pile',

  // Mats
  TRASH_MAT: 'Trash Mat',
  ISLAND_MAT: 'Island Mat',
  PIRATE_SHIP_MAT: 'Pirate Ship Mat',
  NATIVE_VILLAGE_MAT: 'Native Village Mat',
  TRADE_ROUTE_MAT: 'Trade Route Mat',
  VICTORY_TOKEN_MAT: 'Victory Token Mat',
  TAVERN_MAT: 'Tavern Mat',
  COFFERS_MAT: 'Coffers Mat',
  VILLAGERS_MAT: 'Villagers Mat',
  EXILE_MAT: 'Exile Mat',
  FAVORS_MAT: 'Favors Mat',

  // Tokens
  VICTORY_TOKENS: 'Victory Tokens',
  COIN_TOKENS: 'Coin Tokens',
  EMBARGO_TOKENS: 'Embargo Tokens',
  ADVENTURES_TOKENS: 'Adventures Tokens',
  DEBT_TOKENS: 'Debt Tokens',
  WOODEN_CUBES: 'Wooden Cubes',
  SUN_TOKENS: 'Sun Tokens',
};

const lootProviders = [
  'Jewelled Egg', 'Peril', 'Search', 'Foray', 'Pickaxe', 'Wealthy Village',
  'Cutthroat', 'Looting', 'Sack of Loot', 'Invasion', 'Prosper', 'Cursed',
];

const horseCards = [
  'Sleigh', 'Supplies', 'Scrap', 'Cavalry', 'Groom', 'Hostelry', 'Livery',
  'Paddock', 'Ride', 'Bargain', 'Demand', 'Stampede',
];

const spoilsProviders = ['Bandit Camp', 'Marauder', 'Pillage'];

const coffersCards = [
  'Baker', 'Butcher', 'Candlestick Maker', 'Footpad', 'Joust', 'Merchant Guild',
  'Plaza', 'Ducat', 'Patron', 'Silk Merchant', 'Spices', 'Swashbuckler',
  'Villain', 'Exploration', 'Guildhall', 'Pageant',
];

const villagersCards = [
  'Acting Troupe', 'Lackeys', 'Patron', 'Recruiter', 'Sculptor',
  'Silk Merchant', 'Academy', 'Exploration',
];

const altVpCards = [
  'Triumph', 'Chariot Race', "Farmers' Market", 'Bishop', 'Crumbling Castle',
  'Investment', 'Monument', 'Ritual', 'Sacrifice', 'Salt the Earth', 'Temple',
  'Wedding', 'Collection', 'Emporium', 'Groundskeeper', 'Plunder', 'Wild Hunt',
  'Conquest', 'Goons', 'Grand Castle', 'Dominate', 'Aqueduct', 'Arena',
  'Basilica', 'Baths', 'Battlefield', 'Colonnade', 'Defiled Shrine',
  'Labyrinth', 'Mountain Pass', 'Tomb',
];

const coinCards = ['Pirate Ship', 'Trade Route', 'Sinister Plot', 'Garrison',
  ...coffersCards, ...villagersCards];

const adventureTokenCards = [
  'Teacher', 'Lost Arts', 'Seaway', 'Pathfinding', 'Training', 'Ferry', 'Plan',
  'Relic', 'Borrow', 'Raid', 'Bridge Troll', 'Ball', 'Ranger', 'Giant',
  'Pilgrimage', 'Inheritance',
];

const debtCards = [
  'Engineer', 'Mountain Shrine', 'Triumph', 'Daimyo', 'Annex', 'Artist',
  'City Quarter', 'Continue', 'Donate', 'Overlord', 'Royal Blacksmith',
  'Wedding', 'Fortune', 'Change', 'Craftsman', 'Gold Mine', 'Imperial Envoy',
  'Litter', 'Root Cellar', 'Capital', 'Credit', 'Tax', 'Harsh Winter',
  'Mountain Pass',
];

const heirloomPairs = [
  ['Fool', 'Lucky Coin'],
  ['Cemetery', 'Haunted Mirror'],
  ['Secret Cave', 'Magic Lamp'],
  ['Pixie', 'Goat'],
  ['Shepherd', 'Pasture'],
  ['Tracker', 'Pouch'],
  ['Pooka', 'Cursed Gold'],
];

// ---------------------------------------------------------------------------
// Port of CardDependencies (CardDependencyResolver.kt)
// ---------------------------------------------------------------------------
const typeTriggers = [
  ['FATE', [NAMES.BOON_PILE, "Will-o'-Wisp"]],
  ['DOOM', [NAMES.HEX_PILE, NAMES.CURSE, 'Deluded', 'Envious', 'Miserable', 'Twice Miserable']],
  ['LOOTER', [NAMES.RUINS_PILE]],
  ['RESERVE', [NAMES.TAVERN_MAT]],
  ['LIAISON', [NAMES.FAVORS_MAT]],
  ['PROJECT', [NAMES.WOODEN_CUBES]],
  ['OMEN', [NAMES.SUN_TOKENS]],
];

const categoryTriggers = [
  ['CURSER', [NAMES.CURSE]],
  ['TRASHER', [NAMES.TRASH_MAT]],
  ['TRASH_FOR_BENEFIT', [NAMES.TRASH_MAT]],
  ['EXILE', [NAMES.EXILE_MAT]],
];

const nameTriggers = [
  ['Tournament', [NAMES.PRIZE_PILE]],
  ['Joust', [NAMES.REWARD_PILE]],
  ['Border Guard', ['Lantern', 'Horn']],
  ['Flag Bearer', ['Flag']],
  ['Swashbuckler', ['Treasure Chest']],
  ['Treasurer', ['Key']],
  ['Page', ['Treasure Hunter', 'Warrior', 'Hero', 'Champion']],
  ['Peasant', ['Soldier', 'Fugitive', 'Disciple', 'Teacher']],
  ['Exorcist', ["Will-o'-Wisp", 'Imp', 'Ghost']],
  ['Fool', ['Lost in the Woods']],
  ['Necromancer', ['Zombie Apprentice', 'Zombie Mason', 'Zombie Spy']],
  ['Vampire', ['Bat']],
  ['Secret Cave', ['Wish']],
  ['Leprechaun', ['Wish']],
  ['Hermit', ['Madman']],
  ['Urchin', ['Mercenary']],
  ["Devil's Workshop", ['Imp']],
  ['Tormentor', ['Imp']],
  ['Island', [NAMES.ISLAND_MAT]],
  ['Pirate Ship', [NAMES.PIRATE_SHIP_MAT]],
  ['Native Village', [NAMES.NATIVE_VILLAGE_MAT]],
  ['Trade Route', [NAMES.TRADE_ROUTE_MAT]],
  ['Embargo', [NAMES.EMBARGO_TOKENS]],
];

// ---------------------------------------------------------------------------
// Port of CardDependencyResolver.addDependentCards / getStartingCards.
// Modes match the app's settings enums; the API fallback uses IF_PRESENT so the
// result is deterministic (the app always sends fully resolved kingdoms).
// ---------------------------------------------------------------------------
export function resolveDependencies(randomCardNames, {
  prosperityMode = 'IF_PRESENT',   // NEVER | TEN_PERCENT_PER_CARD | IF_PRESENT
  darkAgesMode = 'IF_PRESENT',     // NEVER | TEN_PERCENT_PER_CARD | IF_PRESENT
  random = Math.random,
} = {}) {
  loadCards();

  const cards = randomCardNames.map((n) => _byName.get(n)).filter(Boolean);
  const typesInKingdom = new Set(cards.flatMap((c) => c.types));
  const categoriesInKingdom = new Set(cards.flatMap((c) => c.categories));
  const namesInKingdom = new Set(cards.map((c) => c.name));

  const dependent = new Map(); // name -> count, insertion-ordered

  const addDep = (name, count = 1) => dependent.set(name, (dependent.get(name) ?? 0) + count);

  for (const [type, deps] of typeTriggers) {
    if (typesInKingdom.has(type)) deps.forEach((d) => addDep(d));
  }
  for (const [cat, deps] of categoryTriggers) {
    if (categoriesInKingdom.has(cat)) deps.forEach((d) => addDep(d));
  }
  for (const [name, deps] of nameTriggers) {
    if (namesInKingdom.has(name)) deps.forEach((d) => addDep(d));
  }
  if (cards.some((c) => c.potion)) addDep(NAMES.POTION);
  if (cards.some((c) => lootProviders.includes(c.name))) addDep(NAMES.LOOT_PILE);
  if (cards.some((c) => horseCards.includes(c.name))) addDep('Horse');
  if (cards.some((c) => spoilsProviders.includes(c.name))) addDep('Spoils');
  if (cards.some((c) => coffersCards.includes(c.name))) addDep(NAMES.COFFERS_MAT);
  if (cards.some((c) => villagersCards.includes(c.name))) addDep(NAMES.VILLAGERS_MAT);
  if (cards.some((c) => altVpCards.includes(c.name))) {
    addDep(NAMES.VICTORY_TOKEN_MAT);
    addDep(NAMES.VICTORY_TOKENS);
  }
  if (cards.some((c) => coinCards.includes(c.name))) addDep(NAMES.COIN_TOKENS);
  if (cards.some((c) => adventureTokenCards.includes(c.name))) addDep(NAMES.ADVENTURES_TOKENS);
  if (cards.some((c) => debtCards.includes(c.name))) addDep(NAMES.DEBT_TOKENS);

  // Prosperity (Platinum + Colony)
  const prosperityCount = cards.filter((c) =>
    c.sets.includes('PROSPERITY_1E') || c.sets.includes('PROSPERITY_2E')).length;
  const addProsperity = () => { addDep('Platinum'); addDep('Colony'); };
  if (prosperityMode === 'IF_PRESENT') {
    if (prosperityCount > 0) addProsperity();
  } else if (prosperityMode === 'TEN_PERCENT_PER_CARD') {
    if (prosperityCount > 0 && random() * 100 < Math.min(prosperityCount * 10, 100)) addProsperity();
  }

  // Starting cards
  const starting = new Map();
  const darkAgesCount = cards.filter((c) => c.sets.includes('DARK_AGES')).length;
  const addShelters = () => {
    starting.set('Overgrown Estate', 1);
    starting.set('Hovel', 1);
    starting.set('Necropolis', 1);
  };
  if (darkAgesMode === 'IF_PRESENT') {
    if (darkAgesCount > 0) addShelters(); else starting.set('Estate', 3);
  } else if (darkAgesMode === 'TEN_PERCENT_PER_CARD') {
    if (darkAgesCount > 0 && random() * 100 < Math.min(darkAgesCount * 10, 100)) addShelters();
    else starting.set('Estate', 3);
  } else {
    starting.set('Estate', 3);
  }

  let heirloomCount = 0;
  for (const [cardName, heirloom] of heirloomPairs) {
    if (namesInKingdom.has(cardName)) {
      starting.set(heirloom, 1);
      heirloomCount++;
    }
  }
  starting.set('Copper', 7 - heirloomCount);

  // Basic supply (always: Copper, Silver, Gold, Estate, Duchy, Province)
  const basic = new Map(['Copper', 'Silver', 'Gold', 'Estate', 'Duchy', 'Province']
    .map((n) => [n, 1]));

  return { basic, dependent, starting };
}
