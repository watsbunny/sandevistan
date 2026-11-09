<div align="center">

<img src="src/main/resources/assets/sandevistan/icon_sandevistan.png" width="140" alt="Sandevistan">

# Sandevistan

**The Militech "Apogee" Sandevistan from Cyberpunk: Edgerunners for Minecraft 1.21.1 (Fabric)**

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-62B47A?style=for-the-badge&logo=minecraft&logoColor=white)](https://www.minecraft.net/)
[![Fabric](https://img.shields.io/badge/Fabric-0.15.11+-DBB69B?style=for-the-badge&logo=fabric&logoColor=white)](https://fabricmc.net/)
[![Java](https://img.shields.io/badge/Java-21-E76F00?style=for-the-badge&logo=openjdk&logoColor=white)](https://adoptium.net/)
[![Discord](https://img.shields.io/badge/Discord-Community-5865F2?style=for-the-badge&logo=discord&logoColor=white)](https://discord.gg/ghxC6H2htE)

[🇷🇺 Русский](README.md) · [**🇬🇧 English**](README.en.md) · [🇯🇵 日本語](README.ja.md)

</div>

---

> **"Time is a luxury. The Sandevistan makes it affordable."**
>
> Install the cyberware into your spine, press **V**, and the world around you
> slows to a crawl while you leave a trail of afterimages. But beware: overuse
> leads to **cyberpsychosis**.

---

## ✨ Features

| | |
|---|---|
| ⏳ **Time dilation** | The world around you slows to **15%** speed within a **64-block** radius. Activation lasts up to **9 seconds**; cooldown is up to **15 seconds** and scales with the charge spent. |
| 👤 **You stay fast** | The wearer keeps normal movement speed. Other players can be slowed too (gamerule). |
| 🌀 **Visual effects** | Ghost trails, shock rings, particles, `sandy`/`psycho` post-processing shaders and subtle camera shake. |
| 🧠 **Cyberpsychosis** | Overuse causes screen distortion, static, ringing, debuffs and damage. Fill the meter and things end badly. |
| 🖥️ **HUD** | Charge bar, cooldown indicator and installation status. |
| 🔊 **Sound** | Custom activation, loop, warning and flatline sounds. |
| 🌐 **Localization** | Russian, English and Japanese. |
| 🔌 **Compatibility** | Soft integration with **Capri**; nothing breaks when it is absent. |

## 📦 Installation

### Requirements

| Component | Version |
|---|---|
| Minecraft | **1.21.1** |
| Fabric Loader | **0.16.10+** |
| Fabric API | **0.116.5+1.21.1** |
| Java | **21+** |

### Option 1 — prebuilt JAR

1. Download the latest `sandevistan-*.jar` from [Releases](https://github.com/watsbunny/sandevistan/releases).
2. Drop it into the `mods` folder of your Minecraft profile.
3. Make sure **Fabric Loader** and **Fabric API** are installed.
4. Launch the game.

### Option 2 — build from source

```bash
git clone https://github.com/watsbunny/sandevistan.git
cd sandevistan
./gradlew build
```

The finished jar appears in `build/libs/sandevistan-1.0.0+1.21.1.jar`.

## 🎮 Controls

| Key | Action |
|---|---|
| **Right-click** the item | Install the Sandevistan into your spine |
| **V** | Toggle time dilation |

The key can be rebound in **Options → Controls → Sandevistan**.

## 💬 Commands

Operator permission level (**2**) is required.

| Command | Description |
|---|---|
| `/sandevistan install [players]` | Give and install the Sandevistan |
| `/sandevistan remove [players]` | Remove the Sandevistan |
| `/sandevistan toggle` | Toggle time dilation |
| `/sandevistan psycho <players> <0–1000>` | Set the cyberpsychosis level (debug) |

## ⚙️ Gamerules

| Gamerule | Default | Description |
|---|---|---|
| `sandevistanSlowsPlayers` | `true` | Slows other players near an active Sandevistan |
| `sandevistanCyberpsychosis` | `true` | Enables cyberpsychosis buildup from overuse |

Example:

```mcfunction
/gamerule sandevistanCyberpsychosis false
```

## 🧩 Compatibility

- **Fabric API** — required.
- **Capri** — optional. Detected via `Class.forName`; the integration silently disables itself when Capri is absent.
- **Satin** — shader library bundled inside the jar (JiJ); no separate installation needed.

## 🗂️ Project layout

```
sandevistan/
├─ src/main/java/com/watsbunny/sandy/
│  ├─ Sandevistan.java         entrypoint, item, gamerules
│  ├─ SandyServer.java         server-side time dilation
│  ├─ SlowClock.java           time-rate calculation
│  ├─ SandyCommand.java        /sandevistan commands
│  ├─ cyber/                   item, spine, cyberpsychosis
│  └─ client/                  HUD, shaders, trails, effects, mixins
├─ src/main/resources/
│  ├─ assets/sandevistan/      textures, sounds, shaders, languages
│  ├─ data/sandevistan/        recipes, tags, damage types
│  └─ *.mixins.json            mixin configs
├─ build.gradle
└─ gradle.properties
```

## 🛠️ Building

Built with **JDK 21**, targeting **Java 21**, using **Gradle 8.9**.

```bash
./gradlew build       # build the mod
./gradlew runClient   # launch the client for debugging
```

## 💬 Community

The developer's official Discord is the place to ask questions, report bugs
and get news about new releases first:

<div align="center">

[![Join Discord](https://img.shields.io/badge/Join_Discord-official_server-5865F2?style=for-the-badge&logo=discord&logoColor=white)](https://discord.gg/ghxC6H2htE)

**https://discord.gg/ghxC6H2htE**

</div>

## 📜 License

**All Rights Reserved.** The source code is provided for personal study only.
Publishing, republishing or distributing modified builds without the author's
permission is prohibited.

---

<div align="center">

Made with ❤️ by **WatsBunny**

[Discord](https://discord.gg/ghxC6H2htE) · [GitHub](https://github.com/watsbunny)

<sub>Unofficial fan project based on the Cyberpunk: Edgerunners universe. Minecraft is a trademark of Mojang Studios.</sub>

</div>
