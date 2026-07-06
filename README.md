<p align="center">
  <img src="assets/logo.png" alt="FewsBox logo" width="200">
</p>

<h1 align="center">FewsBox</h1>

<p align="center">A turn-based combat RPG for Android where mixing abilities is the whole point.</p>

---

## What is FewsBox?

FewsBox is a tactical RPG built around one idea: weapons, offhand items and status effects combine into far more possibilities than any single piece suggests. The depth comes from how abilities interact with each other, not from a giant pile of samey moves.

Combat is turn-based and deliberate. You commit to a move and live with it. Enemies play fair too: trash mobs are weak but come in numbers, while elites wind up their big attacks over several turns, giving you time to burst them down, stun them, or shield up before the hit lands.

The game is meant to be understood by looking, not reading. Icons, numbers, colors and meters carry the meaning. No tutorial walls, no text dumps. It should feel fun and accessible from the first battle, never like a chore.

## How the mixing works

Every unit carries a weapon (its attack) and an offhand (its defense or utility). Six weapons and six offhands already give you 36 loadouts before status effects multiply things further.

A few combos to give you the idea:

- **Burn Bomb** - Ember Blade stacks Burn on a target, then the Detonator consumes every stack for a burst of damage. Great against elites you can't out-damage raw.
- **Execute Chain** - chip an enemy below 30% health with multi-hit attacks, then finish with the Reaper's execute bonus.
- **Lockdown** - stun an elite the turn before its charged attack fires and deny the hit entirely.
- **Bruiser Loop** - lifesteal plus a tower shield makes a front-line unit that never needs a healer.

None of these are scripted. They fall out of how the pieces interact, and finding your own is the game.

## Core design rules

1. Ability mixing is the whole point
2. Turn-based combat that makes you think
3. Wordless clarity - the game explains itself visually
4. Asymmetric enemies - weak mobs in numbers, strong elites that telegraph
5. Fair RNG - enemies pick moves from readable odds, never rigged

## Tech

- **Kotlin** with **Jetpack Compose**
- **Dark mode first** - the app is designed for dark theme from day one
- Pure Kotlin game engine with zero Android dependencies, fully unit-testable
- Compose presentation layer driven by an event stream, so animations and art can be added later without touching game logic
- Min SDK 26 (Android 8.0)

## Project structure

```
engine/    pure Kotlin game logic (state, abilities, statuses, AI, events)
data/      content definitions (weapons, offhands, statuses, enemies)
ui/        Compose screens, components and theme
```

The one rule that never breaks: game logic never knows what anything looks like. The engine emits events, the UI listens. That seam is what lets the art land later without a rewrite.

## Roadmap

- [ ] Engine skeleton - damage, shields, core data models
- [ ] Full ability resolution - multi-hit, execute, lifesteal, statuses, combos
- [ ] Status system and the full turn loop
- [ ] Enemy AI with weighted moves and charge telegraphs
- [ ] Playable Compose battle screen
- [ ] Loadout screen - the mixing goes player-facing
- [ ] Juice pass - hit flashes, floating numbers, screen shake
- [ ] Persistence and progression
- [ ] Art and animation, one piece at a time

## License

MIT - see [LICENSE](LICENSE) for details.
