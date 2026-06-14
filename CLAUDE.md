# Rally Dash

An original Android clone of "New Rally-X" (maze chase, collect flags, dodge
AI cars, drop smoke to stun chasers). No network functionality; high scores
are saved locally. Licensed under GPLv3 (see LICENSE).

## Stack

- Kotlin 1.9.24, Jetpack Compose UI (Canvas-based rendering)
- AGP 8.5.2, Gradle 8.7, compileSdk/targetSdk 35, minSdk 24
- `android.suppressUnsupportedCompileSdkWarning=true` is set in
  gradle.properties to silence the compileSdk 35 / AGP 8.5.2 warning.

## Build

```
./gradlew assembleDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`

## Package layout (`com.sillymobile.rallyx`)

- `game/` — core engine: `GameEngine`, `Maze` (recursive-backtracker +
  braiding generation), `Pathfinder` (BFS for enemy AI), `Entities`
  (`Car`/`EnemyCar`/`Flag`/`SmokeCloud`), `Direction`, `GameConstants`
  (all tunables), `GameEvents` (`GamePhase`, `GameEvent`).
- `audio/` — procedural chiptune music and SFX via raw `AudioTrack` PCM
  streaming (square/triangle/noise waveforms). No audio asset files —
  everything is synthesized at runtime to avoid any copyright issues.
  `Compositions.kt` holds the original `NORMAL_THEME` and
  `CHALLENGE_THEME` tracks; `SfxPlayer` renders one-shot effects.
- `ui/` — Compose screens: `MainMenuScreen`, `GameScreen`,
  `HighScoreScreen`, `RallyXApp` (navigation), `GameRenderer` (Canvas
  drawing), `Controls` (on-screen D-pad/smoke button).
- `data/HighScoreRepository` — local persistence via SharedPreferences +
  org.json (no network).

## Controls

Touch: on-screen D-pad + smoke button.
Keyboard (emulator/desktop): arrow keys steer, Space drops smoke. This is
wired up via `Modifier.onKeyEvent` + `FocusRequester` in `GameScreen.kt`.

## Emulator notes

AVD "pixel" (Android 15, google_apis/x86_64). Launch with:

```
emulator -avd pixel -gpu swiftshader_indirect
```

`hw.keyboard=yes` must be set in the AVD's `config.ini` for host keyboard
input (arrow keys/space) to reach the emulator at all — without it, key
events never arrive regardless of app code.

## Known build gotchas

- `Car` must be `open` (it's subclassed by `EnemyCar`).
- Use `android.media.AudioManager.AUDIO_SESSION_ID_GENERATE`, not
  `AudioTrack.AUDIO_SESSION_ID_GENERATE` (doesn't exist).
- Vector drawables (`ic_launcher_foreground.xml`) must use `<path>` with
  arc `pathData`, not `<circle>` elements (invalid for AAPT).
