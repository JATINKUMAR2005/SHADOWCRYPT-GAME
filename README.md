# ShadowCrypt — 2D Dungeon Roguelike (100% Pure Java)

A **B.Tech Semester 5 Java College Project** built **exclusively in Pure Java 21** using only standard JDK libraries (`javax.swing`, `java.awt`, `javax.sound.sampled`, `java.nio.file`, `java.io`, and `java.util`) — zero external JARs, game engines, or third-party dependencies.

---

## Key Features

- **Procedural Dungeon Generation (`DungeonGenerator`)**: Every floor generates unique interconnected rooms, 2-tile corridors, flickering wall torches, spike traps, weapon pedestals, and stairs verified by **Breadth-First Search (BFS)** connectivity checks.
- **Real-Time Raycast Fog of War (`CollisionSystem`)**: Unexplored areas remain hidden in darkness; visited rooms stay dimly mapped on screen and on the live minimap while live enemies remain concealed outside line of sight.
- **Polymorphic Enemy AI & Boss Battles (`Enemy` hierarchy)**:
  - **Mire Slime (`SlimeEnemy`)**: Bouncy melee stalker with windup lunges.
  - **Bone Marksman (`SkeletonEnemy`)**: Ranged skeleton archer that maintains distance and fires bolts.
  - **Crypt Stalker (`ShadowStalkerEnemy`)**: High-speed flanker that telegraphs and dashes.
  - **Crypt Warden (`CryptBossEnemy`)**: Multi-phase Floor Boss every 3rd floor (`Floor 3, 6, 9...`) featuring radial shadow novas, aimed volleys, warden charges, and Phase-II minion summons. Stairs stay sealed until the boss falls.
- **Combat, Progression & Relics**:
  - Aimed melee sword cleaves (`Left Click` / `Space`) + ranged Arcane Bolts (`Right Click` / `F`) + i-frame Dodge Dash (`Shift`).
  - 4-tier weapon progression (`Iron Shortsword` → `Runed Scimitar` → `Sunfire Blade` → `Shadowbane Edge`) from Runic Chests.
  - XP & Level-Up scaling, Crimson Health Flasks (`Q`), Fury Relics, Windstride Charms, and Aegis Shield Wards.
- **Pure Java Synthesized Retro Audio (`SoundEngine`)**: Pre-synthesizes 16-bit PCM sound effects (sword slashes, bolts, hits, crits, potions, level-up arpeggios, boss roars) directly in Java via `javax.sound.sampled` with instant mute toggle (`M`).
- **Atomic Save/Resume & Top-10 Hall of Fame (`StorageService`)**: Quick-save mid-run (`F5`) or save on exit to `data/savegame.dat` and resume anytime; high scores persist atomically in `data/leaderboard.dat`.

---

## Controls

| Input | Action |
| :--- | :--- |
| `W A S D` / `Arrow Keys` | Move in 8 directions |
| `Mouse Aim + Left Click` (or `Space` / `J`) | Melee Sword Cleave |
| `Right Click` (or `F` / `K`) | Cast Arcane Bolt (Ranged) |
| `Left Shift` | Dodge Dash (grants brief invulnerability frames) |
| `Q` / `H` / `1` | Drink Crimson Health Flask (+45 HP) |
| `E` (when standing on Stairs) | Descend to next dungeon floor |
| `F5` | Quick Save current run |
| `Esc` / `P` | Pause Menu |
| `M` | Toggle Sound Effects |

---

## OOP & Architecture Highlights

```text
src/main/java/com/college/shadowcrypt/
├── Main.java                       # Swing EDT entry point
├── model/                          # Pure domain entities (zero UI coupling)
│   ├── Entity.java                 # Abstract base actor (Encapsulation & Inheritance)
│   ├── Player.java                 # Hero stats, XP/levels, buffs, weapon tiers
│   ├── Enemy.java                  # Abstract polymorphic enemy base
│   ├── SlimeEnemy.java             # Melee lunge AI
│   ├── SkeletonEnemy.java          # Ranged spacing & projectile AI
│   ├── ShadowStalkerEnemy.java     # Telegraphed dash AI
│   ├── CryptBossEnemy.java         # Multi-phase boss AI
│   ├── DungeonCombatContext.java   # Decoupled combat callback interface
│   ├── DungeonLevel.java           # Tile map, visibility & exploration state
│   ├── Room.java                   # Java record for room geometry
│   ├── Difficulty.java             # Enum with scaling multipliers
│   ├── WeaponType.java             # Enum progression tiers
│   ├── ItemType.java               # Loot & relic metadata
│   ├── Projectile.java / PickupItem.java / Particle.java / FloatingText.java
│   └── LeaderboardEntry.java / SaveGameData.java
├── engine/                         # Simulation, algorithms & synthesized audio
│   ├── DungeonGenerator.java       # Procedural dungeon & BFS connectivity check
│   ├── CollisionSystem.java        # Smooth axis-sliding collision & raycast FOV
│   ├── Pathfinder.java             # Bounded reverse-BFS enemy routing around walls
│   ├── SoundEngine.java            # Pure Java PCM waveform audio synthesizer
│   └── GameEngine.java             # Central 60 FPS update loop & combat logic
├── persistence/
│   └── StorageService.java         # Atomic NIO file writes & Object Serialization
└── ui/                             # Custom Java2D rendering & Swing canvas
    ├── Theme.java                  # Dark-fantasy palette & vector primitives
    ├── SpriteRenderer.java         # Tiles, torch lighting, characters & FX
    ├── HudRenderer.java            # Status card, boss HP bar & live minimap
    ├── OverlayRenderer.java        # Interactive Menu, Leaderboard, Guide, Pause & Game Over
    ├── GameCanvasPanel.java        # Swing Timer loop & keyboard/mouse dispatch
    └── GameFrame.java              # Desktop window host
```

---

## Quick Start (Windows)

### Run Automated Tests
Double-click `test.bat` or run in terminal:
```bat
test.bat
```

### Compile & Launch the Game
Double-click `run.bat` or run in terminal:
```bat
run.bat
```

### Package Standalone JAR, Windows EXE & Browser Bundle
Run `build.bat` to produce:
- `dist\ShadowCrypt.jar` (runnable JAR)
- `dist\ShadowCrypt\ShadowCrypt.exe` (self-contained Windows app bundle, no JDK needed)
- `web\index.html` + `web\ShadowCrypt.jar` (CheerpJ 5.0 WebAssembly browser edition)

### Launch Live Browser Edition Locally
Double-click `serve-web.bat` (or open `http://127.0.0.1:8765/` while the server is running) to play ShadowCrypt directly in Chrome/Edge, or deploy the `web/` folder to any static host (Netlify, Cloudflare Pages, or GitHub Pages).
