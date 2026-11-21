# Primitive (Fabric 1.21.11, client-side)

Record your inputs, save them, replay them. Default keys (all changeable):

- Right Shift - open / close the Primitive GUI
- R - start recording, press again to stop and save
- G - play the selected macro, press again to stop

Optional 2-key combos: set "RECORD +" / "PLAY +" in the GUI (or in Controls). When set, hold that key
and press the main key.

Saves live in `.minecraft/config/primitive/` (macros.json + settings.json).

## Build
1. Install JDK 21.
2. Open this folder in IntelliJ IDEA (or run `gradle wrapper --gradle-version 9.2.0` then `./gradlew build`).
3. Run the `build` Gradle task. The mod is `build/libs/primitive-1.0.0.jar`.
4. Put the jar in `.minecraft/mods/` together with Fabric API for 1.21.11.

If a version in `gradle.properties` doesn't resolve, copy the 1.21.11 values from https://fabricmc.net/develop.
