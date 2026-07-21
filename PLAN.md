# FewsBox — Plan

Living document for where the game is going. Ideas land here first, get
built when their time comes. Lore is still unwritten; this captures the
shape of it.

## The color system (the core identity)

Color belongs to the heroes. Enemies are grayscale, always. That contrast
is the visual language of the whole game: the world drained of color versus
the party carrying it.

### The rainbow roster (playable now)

Red, Orange, Yellow, Green, Blue, Violet. Each has its own stats and its
own restricted equipment pools (five of the six weapons, five of the six
offhands) so every color plays differently and no build is universal.
Party cap is 3, room designed in for 4 later.

### Hidden characters (unlockable later)

- **Pink** - unlockable hero. Powers TBD. Pink is the logo color, so this
  one should feel special.
- **Black** and **White** - unlockable heroes with their own kits. How and
  when they unlock is undecided.

### The grayscale bosses

Boss-fight characters are grayscale like all enemies, but these ones can
be won over: after their boss fight, they somehow switch to your side and
become playable. Grayscale party members fighting for the colorful side is
the long-game payoff of the visual language.

- Exact roster of defectable bosses: TBD
- **Gray** is the exception. Gray never joins. Gray is the true mastermind
  behind everything and stays the final enemy of the game, forever.

## Lore

Not written yet. Known fixed points:

- The enemy world is grayscale; the heroes are the color in it
- Defeated grayscale bosses can defect to the party
- Gray is the mastermind and the one permanent enemy

## Campaign and progression (shipped)

The campaign is 100 levels on a swipeable level-select board (25 per
page): beat a level to unlock the next, replay anything, stars (1-3, by
survivors) saved per level. Every 10th level is a gauntlet of 3-5 stages
where survivors, statuses and the ult meter carry between waves. Every
25th level is a boss set piece: Ash at 25, Silver at 50, both together
at 75, and Gray himself at 100 - a three-stage finale, and the only boss
who never defects. Beating level 100 opens endless mode: procedurally
scaled levels that never stop, gauntlets up to 8 stages deep, progress
measured in distance instead of stars.

Heroes earn XP from performance - damage dealt that level plus 5 for
surviving - and level 1-5; levels unlock the kit itself (one weapon and
two offhands at level 1, everything by level 5) plus small HP/ATK growth.

## Kit overhaul (shipped in v2.0)

All of it is live: five weapons and five offhands per color, team-only
offhands, the Red-only start with the unlock ladder, hero levels capped
at 50 as pure stat growth, and the fews economy - 15 a star (20 for the
third on a first clear), an armory and workshop in the shop, and battle
consumables. The wardrobe and archive pages stand as under-construction
storefronts. Decisions that shaped it:

- **Unlock ladder** - you start with Red alone and earn the roster in
  order: Orange, Yellow, Green and Blue through the early campaign
  (about levels 2, 5, 8, 12), then Violet is FREED from the level 25
  boss fight (the grayscale captured her - the first boss is a rescue),
  Ash defects at level 50, Silver at level 100 as the door to endless
- **Five weapons and five offhands per hero**, all sidegrades: nothing
  strictly better or worse than anything else, pure customization, no
  gear power ladder
- **Offhands never hit enemies** - they only affect your own team, and
  every offhand can be dragged onto any teammate
- **Red's weapons**: Sword (damage, 3-turn bleed, can crit), Crimson
  Maul (heavy, sometimes stuns 1 turn), Cleaver (3 hits, 2-turn bleed),
  Axe (decent damage, target deals 25% less for 3 turns), Pulse Cutter
  (big sword, +1 damage per 1% of stored ult charge)
- **Red's offhands**: Jagged Buckler (shield 8 + thorns), Adrenaline
  Vial (below half HP: extra turn, otherwise heal 8), Wound Salt
  (consume bleed on an ally? - pending redesign since offhands are
  ally-only now), Annoying Pest (any ally taunts everything for 3 turns
  and takes 15% less damage), War Cry (team deals +25% for 3 turns)
- **Orange's weapons**: Ember Blade (starter, 2 burn for 3 turns, can
  crit), Twin Torches (2 hits, 1 burn each), Flame Whip (hits the
  target and the enemies adjacent to them; no neighbors means all 3
  lashes land on the target), Cinder Fan (burn over time, crits if the
  target is already burning), The Blazeborn Blade (heavy damage, applies
  burn, crits against burning targets)
- **Orange's offhands**: rework in progress - team-only, no enemy
  damage; Ember Drain (strip burn off an ally, heal per stack) shipped
  early as the Detonator replacement
- **New statuses needed**: bleed (DoT distinct from burn and poison)

## Systems backlog

- **Fourth party slot** - Party.MAX_SIZE is 3; the code iterates the party
  list everywhere, so this is a constant bump plus balance work
- **Modifier slots** - pure functions that rewrite an ability's effect list
  ("+1 hit, -20% per hit", "hits also apply Weaken"). Deepest mixing axis,
  deferred until the base loop is proven fun
- **Boss fights** - three grayscale bosses live: Ash (level 25, an AI
  boss that smothers the party in burn and poison), Silver (level 50, the
  telegraphing storm), and Gray (level 100, three escalating forms built
  around Null Wave). Ash and Silver defect when their battles fall - the
  unlock system is generalized, so future bosses are one map entry each.
  Gray never joins, by design
- **Ultimate balance pass** - the shared meter fills 5% per landed
  attack, 3% per hit taken, 15% when one hit costs over half a hero's max
  HP; fire at 100% by dragging the bar onto a hero, reset to zero. The
  rates and all eight ults need playtesting
- **Weapon balance pass** - every hero has three signature weapons of
  their own now, 21 total; numbers are first drafts
- **Sound** - hooks exist via the combat event stream, nothing wired yet
- **Real art** - swap the color blocks and glyph chips for actual
  characters. Everything renders off iconId lookups, so art lands without
  touching game logic

## Release rhythm

Versions run v1.0, v1.1, ... with v2.0 reserved for something big. Every
push to main cuts a release; CI attaches the APK automatically. Release
notes stay simple: what the app is, what it does.
