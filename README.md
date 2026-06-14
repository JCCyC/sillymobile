# Rally Dash

An original Android game inspired by the classic top-down maze-chase racing arcade games of
the early 1980s (e.g. Namco's *New Rally-X*). **This project is an independent, original
work** - it does not use any of Namco's code, art, audio, names, or level data. All mazes,
artwork (drawn procedurally with Jetpack Compose `Canvas`), and music are generated/composed
from scratch for this project.

## Gameplay

- Drive your car around a procedurally generated maze, collecting every flag to clear the
  stage.
- Rival "rally cars" patrol the maze and chase you using pathfinding - touching one costs you
  a life.
- Drop a **smoke screen** behind your car to temporarily stun chasers that drive through it.
  Smoke charges slowly recharge over time.
- Watch your **fuel gauge** - it drains over time, and is topped up by collecting flags.
  Running out of fuel costs you a life.
- One flag per stage is a special **gold flag** worth a big score and fuel bonus.
- Every few stages is a **Challenge Stage**: no rival cars, just a timed flag-collecting bonus
  round with its own music.
- Your top 10 high scores are saved locally on the device (no network/account needed).

## Controls

- On-screen D-pad: drive (4-directional, like the original's joystick).
- **SMOKE** button: lay a smoke screen behind your car (limited charges, recharges over time).

## Audio

All music is **synthesized procedurally at runtime** (square/triangle/noise wave chiptune
synthesis via `AudioTrack`) - there are no bundled audio files. Two original compositions are
included:

- A bouncy major-key theme for normal stages (`Compositions.NORMAL_THEME`).
- A faster, driving minor-key theme for Challenge Stages (`Compositions.CHALLENGE_THEME`).

Sound effects (flag pickup, smoke, crashes, stuns, fanfares) are likewise synthesized on the
fly - see `app/src/main/java/com/sillymobile/rallyx/audio/`.

## Project structure

```
app/src/main/java/com/sillymobile/rallyx/
  game/    - Pure-Kotlin game engine: maze generation, vehicle movement, AI, rules
  audio/   - Procedural chiptune music & SFX synthesis
  ui/      - Jetpack Compose screens, rendering, controls
  data/    - Local high-score persistence (SharedPreferences/JSON)
```

## Building

This is a standard Gradle/Android project.

```sh
./gradlew assembleDebug
```

Requires JDK 17 and the Android SDK (API 35). Open the project in Android Studio, or run the
Gradle wrapper from the command line, then install `app/build/outputs/apk/debug/app-debug.apk`
on a device or emulator (minimum Android 7.0 / API 24).

## License

Licensed under the GNU General Public License v3.0 - see [LICENSE](LICENSE).
