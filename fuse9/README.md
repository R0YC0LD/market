# FUSE9

A Sudoku × Minesweeper puzzle for Android, where every cell is both a Sudoku cell and part
of the minefield. Kotlin, Jetpack Compose, fully offline.

## The rules

1. Fill the 9×9 grid like Sudoku.
2. Nine cells are **seals** (mines): exactly one in every row, every column and every box.
3. An open cell shows its digit and, as dots, how many seals touch it (diagonals count).
4. **Each seal hides a different digit.** Find the 4-seal and every other 4 on the board is safe.

You open a cell by placing its digit, and you defuse a seal by holding the cell (or pressing
the seal key). Each move is checked as soon as you make it. A wrong move is a strike, and it
shows you the truth about that cell. Classic allows three strikes, Hardcore allows one, and Zen
has no limit. Opening a zero marks its neighbours safe in a short ripple.

Rule 4 links the two halves. Without it the seals would just decorate a Sudoku. With it:

- Sudoku tells you *what* a cell holds, and the seal logic tells you *whether* you can open it.
- Defusing a seal reveals a digit that the Sudoku needs.
- Knowing which digits are already sealed tells you which cells are safe.
  The tracker above the board shows those digits.

## Build & run

Requirements: JDK 17+, Android SDK with platform 36.

```bash
./gradlew :app:assembleDebug      # app/build/outputs/apk/debug/app-debug.apk (id com.fuse9.dev)
./gradlew :app:installDebug       # on a connected device
./gradlew :app:assembleRelease    # minified; signed with the debug key for playtesting
```

## Tests

```bash
./gradlew :app:testDebugUnitTest
```

| Area | What it checks |
|---|---|
| `SudokuTest` | valid / invalid grids, determinism, solution counting, candidates |
| `SealsTest` | exactly 9 seals, one per row/column/box/digit, adjacency counts, degenerate layouts |
| `FusionTechniqueTest` | each digit×seal deduction in isolation |
| `GeneratorTest` | every tier solvable by the human-style solver with its own techniques, no unsound step, seals required, determinism |
| `MoveEngineTest` | placing, strikes, trips, defusal, notes, undo (strikes survive it), win, ripple, timer |
| `HintEngineTest` | following hints alone solves every tier with zero strikes |
| `TutorialGuideTest` | the first-board guide points at real moves and reaches the seal lesson |
| `SaveRepositoryTest` | full save/load round trip, corrupt save, atomic write |
| `StatsRepositoryTest` | best/average times, daily streak rules |
| `SfxSynthTest` | every sound is short, unclipped, starts and ends silent, and renders the same every time |
| `AppFlowTest` (Robolectric) | real app: first launch → teaching board → defuse → place → menu offers Continue |

Robolectric downloads its Android runtime jar on first run. If Maven Central rate-limits you,
point it at a mirror: `-ProbolectricRepo=https://maven-central.storage-download.googleapis.com/maven2/`.

Opt-in tools (they are skipped unless their property is set):

```bash
# Render key screens to PNG on the JVM (no device needed)
./gradlew :app:testDebugUnitTest --tests '*ScreenshotTour*' -Dfuse9.screens=/abs/path/out
# Export every sound effect as WAV for listening
./gradlew :app:testDebugUnitTest --tests '*SfxSynthTest*' -Dfuse9.sfx=/abs/path/out
# Generator survey: givens, score, technique level, waves per tier
FUSE9_EXPERIMENT=1 ./gradlew :app:testDebugUnitTest --tests '*GeneratorExperiment*' -i
```

## Architecture

```
app/src/main/java/com/fuse9/
  puzzle/       pure Kotlin, no Android
    Grid, Rng            geometry, units, neighbours; seeded SplitMix64
    Sudoku               full-grid generation, brute-force counting
    Seals                seal layout generation and validation
    Knowledge, Truth     what a player knows vs. the hidden answer
    Technique(s)         human-style deductions (Sudoku, seal, fusion)
    FusionSolver         grades and validates puzzles the way a person solves them
    Difficulty           tiers, scoring, acceptance rules
    PuzzleGenerator      the generation pipeline
  game/         pure rules
    GameState            serialisable board state (cells, notes, strikes, history, trail)
    MoveEngine           (state, action) -> (state, events); every rule lives here
    HintEngine           next provable move from the visible board, with an explanation
    TutorialGuide        the first board's step-by-step guide
    Scoring, GameMode, PuzzleSource
  persistence/  atomic JSON save of the board in progress
  stats/        lifetime stats and daily streak
  settings/     SharedPreferences-backed settings flow
  audio/        SfxSynth (procedural synthesis), AudioManager (SoundPool + light mixer)
  haptics/      HapticManager (composition primitives with amplitude fallbacks)
  ui/           Compose: theme, icons, Canvas board + animation system, screens
```

The UI renders `GameState` and turns `GameEvent`s into animation, sound and haptics.
`GameViewModel` routes input, keeps the clock, and saves. The rules, hints and generation live
outside it. The board is a single Canvas. Its animations run a frame loop only while something
is moving, they redraw without recomposing, and digit layouts are measured once per cell size.

## Puzzle generation

1. Build a complete Sudoku from the seed.
2. Place seals by backtracking: one per row, column and box, and each on a different digit.
   Reject regular-looking layouts (diagonals, cyclic shifts, dense clumps).
3. Start with every safe cell given.
4. Remove givens in seeded random order. Keep a removal only if the **human-style solver**,
   limited to the tier's techniques, still finishes. Every box keeps at least one given.
5. Grade the result. Discard it if it misses the tier, if Sudoku alone can solve it (the seals
   would be decoration), or if it needs fewer than 3 fusion deductions.

The solver sees only what the player sees. It makes every move it can prove: placing a digit
reveals that cell's count, and defusing a seal reveals its digit. It uses harder techniques only
when it is stuck. If it finishes, every step has a logical reason and no guess was ever needed.
With `verify = true` it throws as soon as a deduction contradicts the answer, and the tests run
it that way across many seeds.

Each puzzle stores `seed`, `difficulty`, `generatorVersion`, and its full answer. Saved games
therefore survive later generator changes. The same seed and tier always give the same puzzle.
The daily puzzle is seeded from `fuse9-daily-YYYY-MM-DD`. Difficulty climbs through the week,
from Easy on Monday to Master on Sunday.

### Techniques and tiers

| Level | Sudoku | Seal | Fusion |
|---|---|---|---|
| 1 | naked / hidden single | unit already sealed, last cell of a unit, count satisfied, count needs every cell | sealed digit ⇒ safe; seals hide distinct digits |
| 2 | | | the only cell that can hide digit *d*'s seal |
| 3 | locked candidates | region subset (counts vs. units) | the *d*-seal is confined to a unit, so cells there that can't be *d* are safe |
| 4 | naked pair/triple, hidden pair | region overlap | |

Survey of 16 seeds per tier (`GeneratorExperiment`):

| Tier | Givens | Max level | Waves* | Typical score |
|---|---|---|---|---|
| Easy | 34 | 1 | 1–2 | 90–120 |
| Medium | 27 | 1–2 | 2–3 | 105–140 |
| Hard | 23 | 3 | 3–6 | 170–240 |
| Expert | 19 | 3–4 | 4–8 | 220–310 |
| Master | 14–20 | 4 | 5–8 | 260–390 |

\*A wave is a round where the player must make moves to gain new information before they can
continue. More waves mean the two halves feed each other more.

**Design note: cascades.** A classic Minesweeper zero-cascade would reveal the neighbours'
counts. The survey showed this gives away so much seal information that no Hard puzzle could be
generated, and Easy boards never needed the place-learn-deduce loop. So in FUSE9 a zero only
marks its neighbours *safe*. You still have to work out their digits. Cascades are still
supported by `Rules(zeroCascade = true)` for future modes.

## Difficulty score

`DifficultyAnalyzer.rate` adds up:

- technique weight × uses (capped per technique)
- pressure: rounds with only one or two moves available
- the number of waves
- how bare the opening board is

The player only sees Easy / Medium / Hard / Expert / Master.

## Audio

All sounds are synthesised at first launch by `SfxSynth` from sine partials, glides and
band-limited noise bursts. The results are written to the cache as WAV and played through
SoundPool. There are no sample files, so no sound is borrowed from anywhere else. The defuse
sound is built on the same timeline as its animation: click at 0 ms, lock tick at 70 ms, tonal
rise at 140 ms, settle at 280 ms. The mixer drops identical sounds that arrive within 30 ms, and
lowers gain when several sounds land at once. Volume, mute and haptics are in Settings. Bump
`SfxSynth.VERSION` after changing a sound so cached files are rebuilt.

## Debug options (debug build only)

The bug icon in the game header opens a panel:

- toggles for the solution, seal locations and the candidate map
- puzzle id, seed, generator version, score, max level, givens, solver steps, generation time,
  seal coordinates and how often each technique was used

The release build sets `DEBUG_TOOLS = false`, so the panel isn't there.

## Roadmap

V1 polish → V2 daily (seeded daily is in) → V3 advanced difficulty → V4 cosmetics →
V5 stats (basic stats are in) → V6 time modes (`GameMode` already has the slots) →
V7 custom → V8 sharing (puzzles are fully described by seed and generator version) →
V9 achievements → V10 seasonal themes.

## Licences

Manrope and Fraunces are under the SIL Open Font License. See `licenses/`.
