# Sandevistan — NeoForge 1.21.1

This branch contains the NeoForge 1.21.1 port of Sandevistan.

## Requirements

- Minecraft 1.21.1
- NeoForge 21.1.x
- Java 21

## Build

```bash
./gradlew build
```

The mod jar is written to `build/libs/`.

## Dependencies

- **Satin API (NeoForge port)** is bundled inside the jar (Jar-in-Jar). Its source lives in
  [watsbunny/Satin](https://github.com/watsbunny/Satin), branch `neoforge/1.21.1`.
- No separate Satin installation is required.

## Notes

- The original Fabric 1.21.1 port is on branch `1.21.1-fabric`.
- The 1.20.1 NeoForge port is on branch `1.20.1-neoforge`.
