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

## Systems backlog

- **Fourth party slot** - Party.MAX_SIZE is 3; the code iterates the party
  list everywhere, so this is a constant bump plus balance work
- **Modifier slots** - pure functions that rewrite an ability's effect list
  ("+1 hit, -20% per hit", "hits also apply Weaken"). Deepest mixing axis,
  deferred until the base loop is proven fun
- **Boss fights** - two grayscale bosses live: Ash (battle 8, an AI boss
  that smothers the party in burn and poison) and Silver (battle 10, the
  telegraphing finale). Both defect when their battle falls - the unlock
  system is generalized, so future bosses are one map entry each. Gray
  remains unbuilt and unbeatable by design
- **Ultimate balance pass** - ultimates are meter-based now (fill by
  dealing damage at 2%/point and taking damage at 3%/point, fire at 100%,
  reset to zero); the gain rates and all seven ults need playtesting
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
