# Sandevistan — NeoForge 1.20.1

This branch contains the NeoForge 1.20.1 port of Sandevistan.

## Requirements

- Minecraft 1.20.1
- NeoForge 47.1.x (the `net.neoforged:forge` 1.20.1 line)
- Java 17

## Build

```bash
./gradlew build
```

The mod jar is written to `build/libs/`.

## Dependencies

- **Satin API (NeoForge port)** is bundled inside the jar (Jar-in-Jar). Its source lives in
  [watsbunny/Satin](https://github.com/watsbunny/Satin), branch `neoforge/1.20.1`.
- **MixinExtras** is bundled inside the jar for the `@Local` mixin sugar used by the renderer.
- No separate Satin or MixinExtras installation is required.

## Notes

- 1.20.1 Forge/NeoForge does not support namespaced core shader names in the vanilla loader,
  so the ghost shader is also provided under `assets/minecraft/shaders/core/`.
- The 1.21.1 NeoForge port is on branch `1.21.1-neoforge`.
