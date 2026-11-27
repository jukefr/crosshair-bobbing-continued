# Crosshair Bobbing Reborn

**Make the Minecraft crosshair follow view bobbing — with reaction animations for sneaking, attacks, item switches, eating, bows and crossbows. Client-side only, for Fabric.**

[![Modrinth](https://img.shields.io/modrinth/dt/crosshair-bobbing-reborn?style=flat-square&logo=modrinth&label=Modrinth)](https://modrinth.com/mod/crosshair-bobbing-reborn)
[![GitHub release](https://img.shields.io/github/v/release/jukefr/crosshair-bobbing-reborn?style=flat-square&logo=github)](https://github.com/jukefr/crosshair-bobbing-reborn/releases)
[![License](https://img.shields.io/badge/license-GPL--3.0--only-blue?style=flat-square)](https://github.com/jukefr/crosshair-bobbing-reborn/blob/main/LICENSE)

Crosshair Bobbing Reborn is a maintained continuation of **Crosshair Bobbing** by Krash220.
Vanilla Minecraft keeps the crosshair pinned to the centre of the screen while the camera bobs,
so the crosshair no longer points where the camera is looking. This mod moves the crosshair with
the camera, and adds small animations so it reacts to what you are doing.

It is a **client-side** mod. You do not need it on the server, and it does not change gameplay.

## Features

- **Crosshair follows view bobbing** — the crosshair tracks the camera's walking bob instead of
  staying fixed at the centre of the screen.
- **Respects your video settings** — if View Bobbing is disabled in Minecraft's video settings,
  the crosshair stays centred automatically.
- **Sneaking animation** — the crosshair tilts while you crouch.
- **Attack animation** — the crosshair kicks on a hit and on a broken block.
- **Swing pulse** — a subtle pulse while your hand swings.
- **Item switch slide** — a short slide when you change hotbar slots or held items.
- **Eating and drinking** — the crosshair spins and settles while you eat.
- **Bow and trident aiming** — the crosshair narrows as you draw a bow or charge a trident.
- **Crossbow charging** — the crosshair spins while a crossbow winds up.
- **Trident spin attack** — the crosshair follows the riptide spin.
- **Toggle keybind** — press **B** in game to turn the effect on or off.
- **Every animation can be disabled** — edit one JSON file; no restart required.

## Installation

1. Install **Fabric Loader** for your Minecraft version.
2. Install [Fabric API](https://modrinth.com/mod/fabric-api).
3. Download the jar for your Minecraft version from [Modrinth](https://modrinth.com/mod/crosshair-bobbing-reborn)
   or [GitHub Releases](https://github.com/jukefr/crosshair-bobbing-reborn/releases).
4. Put the jar in your `mods/` folder and start the game.

> This is a client-side mod. Servers do not need it, and it works on any server.

## Supported versions

| Loader | Minecraft versions |
|---|---|
| Fabric | 1.21.1, 1.21.11, 26.1–26.1.2, 26.2–26.3 |

NeoForge is not supported yet. The config format and animations are loader-agnostic, so a NeoForge
port is planned.

These versions cover the large majority of active Fabric players. Older versions are not included
in the first release.

## Configuration

The config is created at `config/crosshair-bobbing-reborn.json` the first time the game starts:

```json
{
  "configVersion": 1,
  "enabled": true,
  "followBobbing": true,
  "sneakAnimation": true,
  "itemSwitchAnimation": true,
  "swingAnimation": true,
  "attackAnimation": true,
  "eatingAnimation": true,
  "crossbowAnimation": true,
  "bowAimAnimation": true,
  "tridentSpinAnimation": true
}
```

| Key | Default | What it does |
|---|---|---|
| `enabled` | `true` | Master switch for the whole mod. |
| `followBobbing` | `true` | Crosshair follows the walking camera bob. |
| `sneakAnimation` | `true` | Tilt while crouching. |
| `itemSwitchAnimation` | `true` | Slide when the held item changes. |
| `swingAnimation` | `true` | Pulse while the hand swings. |
| `attackAnimation` | `true` | Kick when hitting an entity or breaking a block. |
| `eatingAnimation` | `true` | Spin while eating or drinking. |
| `crossbowAnimation` | `true` | Spin while charging a crossbow. |
| `bowAimAnimation` | `true` | Narrow while drawing a bow or trident. |
| `tridentSpinAnimation` | `true` | Follow the riptide spin attack. |

Edit the file and restart the game to apply changes. The **B** key toggles `enabled` immediately
and saves the file for you.

## Why a Reborn version?

The original Crosshair Bobbing was last published for Minecraft 1.20.2 and has millions of
downloads. This project keeps the same behaviour and ideas, adds configuration, and ports the
effect to current Minecraft versions. It is not a drop-in jar replacement: the code was rebuilt
against modern Minecraft and does not use the original project's custom loader.

## Credits and license

- Original mod: [Crosshair Bobbing](https://modrinth.com/mod/xbob) by **Krash220**
  ([source](https://github.com/Krash220/CrosshairBobbingMod)).
- Reborn port and maintenance: **jukefr**.

Because the original is licensed under **GPL-3.0-only**, this project is also licensed under
**GPL-3.0-only**. The full license is in [LICENSE](LICENSE), and the source is published in this
repository. If you redistribute a modified build, you must keep the same license and publish the
source.
