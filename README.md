# Roller Rush – base version (`com.voxcom.rollerrush`)

Kotlin / Android, landscape 2D endless roller-skating game on a custom `SurfaceView` engine.
No third-party dependencies except `androidx.core:core-ktx`.

## Open & run
1. Android Studio (Koala / 2024.1+ recommended) → **Open** this folder → let Gradle sync
   (AGP 8.5.2, Kotlin 1.9.24, Gradle 8.7, JDK 17, compile/target SDK 34, min SDK 24).
2. If Studio reports a missing Gradle wrapper jar, use *File ▸ Settings ▸ Build ▸ Gradle* (use the
   bundled Gradle 8.7) or run `gradle wrapper` once.
3. Run on a device/emulator. All screens are locked to landscape.

## Architecture
```
RollerRushApp            app-scoped holder: prefs, playerData, inventory, gameAssets
game/    GameView (SurfaceView + touch) · GameLoop (thread, sub-stepped dt) · GameWorld (all simulation)
         GameRenderer (all drawing) · Camera (world<->screen) · GameState
player/  Player (rig + physics state) · BodyPart (scene-graph node) · PlayerAnimator (pose -> rig, FK ground contact)
         PlayerController (gravity/jump, reads SkateStats) · Rig (dimensions/hitboxes) · Skate
animation/ SkatingAnimation (procedural cycle, air pose, crash pose) · Pose · AnimationState
entities/  GameEntity (pooled) · Obstacle (GROUND/TALL/MOVING) · Coin · PowerUp (stub)
systems/   Collision · Spawn · Score · Difficulty · Coin
data/      GamePreferences (only SharedPreferences user) · PlayerData · InventoryData/ShopCatalog
           CharacterData/SkinCatalog · SkateStats
ui/        Splash, MainMenu, Character, Shop, Game, Pause (translucent), GameOver, Settings (programmatic UI via UiKit)
utils/     Constants (ALL tunables) · AssetManager · ScreenUtils
```
(`MainActivity` from the suggested layout was left out: `SplashActivity` is the launcher.)

## Key design points
* **World units + Camera**: world is always 360 units tall, ground at y=300, skater at x=100. Scale = min(h/360, w/560),
  anchored to the bottom, so proportions hold on any aspect ratio. Wider screens just show more world.
* **Skating cycle** (`SkatingAnimation`): L push → L recover → R push → R recover → glide, per-leg curves with
  smoothstep/sin² so the loop is seamless; right leg = left leg phase-shifted by 0.42. Knee flex, skate pitch,
  torso lean, hip shift, counter-swinging arms, head stabilisation. The hip is placed by forward kinematics so the
  planted skate always touches the ground (`PlayerAnimator.apply`).
* **Jump**: airborne pose is blended in (legs tucked, arms counterbalance, torso tilts with vertical speed);
  landing adds a short knee-absorb dip. A tap is buffered for ~0.1 s before landing; no air jumps.
* **Performance**: pooled entities, indexed loops, reused Paints/Rects/StringBuilder, bitmaps created once,
  fixed sub-steps (≤ 20 ms) so physics is frame-rate independent.
* **Pause/resume**: world state is frozen (no update/spawn/physics) and resumes exactly; also auto-pauses on
  `onPause` and shows the pause screen when returning.
* **Game over**: crash tumble (0.9 s) → coins banked once into `PlayerData` → Game Over screen.

## Tuning
Everything is in `utils/Constants.kt` (BASE_SPEED, MAX_SPEED, SPEED_INCREASE, SPAWN_INTERVAL_*, jump height,
gravity, unlock distances, score weights, pool sizes). Skate upgrades: `data/SkateStats.kt`.
Settings ▸ "Show hitboxes" draws the collision shapes.

## Replacing art
Drop PNGs into `app/src/main/assets/...` (see `assets/README.txt`); missing files fall back to generated placeholders.
Sprite sizes must match `player/Rig.kt` (pixels = units × `BITMAP_SCALE`, or they are auto-scaled).
Skins: add ids to `SkinCatalog` and presets/shop items.
