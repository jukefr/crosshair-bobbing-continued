# AGENTS.md

Instructions for future agents working on this repository.

## Project overview

Crosshair Bobbing Continued (`crosshair-bobbing-continued`) is a **client-side only** mod for
**Fabric and NeoForge** that makes the crosshair follow Minecraft's view bobbing and adds optional
action animations.

- Display name: Crosshair Bobbing Continued
- Fabric mod id: `crosshair-bobbing-continued`
- NeoForge mod id: `crosshair_bobbing_continued` (NeoForge ids cannot contain `-`)
- Main package: `fr.juke.crosshairbobbing`
- Entrypoint class: `fr.juke.crosshairbobbing.CrosshairBobbingContinued`
- Config file: `config/crosshair-bobbing-continued.json` (same file on both loaders)
- GitHub: https://github.com/jukefr/crosshair-bobbing-continued
- Modrinth: https://modrinth.com/mod/crosshair-bobbing-continued (project id `YLreMsL2`)
- License: **GPL-3.0-only**

The mod is a continuation of **Crosshair Bobbing** by Krash220
(https://modrinth.com/mod/xbob, source https://github.com/Krash220/CrosshairBobbingMod). The
original is GPL-3.0-only, so this repository **must stay GPL-3.0-only and must publish its source**.
Keep the attribution in `README.md` and the Modrinth description.

Do not reuse the original project's name, icon, or jar as-is. This is a continuation under the
original license, with a new icon and a rebuilt, version-specific implementation.

## The most important rule

**Do not change the config keys without a migration.** Users edit the JSON by hand and the README
documents every key.

Current config is version 1:

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

If a schema change is ever needed, bump `configVersion`, keep old keys readable, and migrate in
`Config.sanitize()`.

Second rule: **the crosshair only moves when Minecraft itself bobs.** If the player disables View
Bobbing, `followBobbing` must be a no-op so the crosshair stays centred. Do not apply the bob
transform unconditionally.

Third rule: **keep the two loaders' behaviour identical.** The shared classes must not import
Fabric or NeoForge APIs. Put loader differences in the entrypoint and in `Config.file()` only.

## Supported versions and branch layout

`main` tracks the latest supported Minecraft version (currently 26.3). One branch per supported
line and loader:

| Branch | Minecraft | Loader | Mappings | Java |
|---|---|---|---|---|
| `1.21.1` | 1.21.1 | Fabric | Mojang official | 21 |
| `1.21.11` | 1.21.11 | Fabric | Mojang official | 21 |
| `26.1.2` | 26.1, 26.1.1, 26.1.2 | Fabric | none (unobfuscated) | 25 |
| `main` | 26.2, 26.3 | Fabric | none (unobfuscated) | 25 |
| `neoforge-1.21.1` | 1.21.1 | NeoForge 21.1.268 | Mojang official | 21 |
| `neoforge-1.21.11` | 1.21.11 | NeoForge 21.11.48 | Mojang official | 21 |
| `neoforge-26.1.2` | 26.1.2 | NeoForge 26.1.2.130 | none | 25 |
| `neoforge-26.3` | 26.2, 26.3 | NeoForge 26.3.0.118 | none | 25 |

NeoForge publishes 26.1.2, not 26.1 or 26.1.1, so the 26.1 NeoForge branch is built from 26.1.2.

## Toolchains and build commands

| Branch range | Gradle | Build JDK | Plugin | Java target |
|---|---|---|---|---|
| Fabric 1.21.x | 9.8.2 | 25 | Loom 1.19.4 (`fabric-loom-remap`), official Mojang mappings | 21 |
| Fabric 26.x | 9.8.2 | 25 | Loom 1.19.4 (`fabric-loom`), no mappings | 25 |
| NeoForge 1.21.x | 9.2.1 | 21 | ModDevGradle 2.0.148 | 21 |
| NeoForge 26.x | 9.2.1 | 25 | ModDevGradle 2.0.148 | 25 |

Loom 1.19.4 requires **JDK 25 to run Gradle**, even on branches that compile to Java 21.

```bash
# Fabric 1.21.x and 26.x
JAVA_HOME=/usr/lib/jvm/java-25-openjdk \
PATH=/usr/lib/jvm/java-25-openjdk/bin:$PATH \
./gradlew build --no-daemon

# NeoForge 1.21.x
JAVA_HOME=/usr/lib/jvm/java-21-openjdk \
PATH=/usr/lib/jvm/java-21-openjdk/bin:$PATH \
./gradlew build --no-daemon

# NeoForge 26.x
JAVA_HOME=/usr/lib/jvm/java-25-openjdk \
PATH=/usr/lib/jvm/java-25-openjdk/bin:$PATH \
./gradlew build --no-daemon
```

Mixin compatibility levels: `JAVA_21` on 1.21.x, `JAVA_25` on 26.x. Do not lower them.

NeoForge builds take longer on the first run because ModDevGradle decompiles and patches
Minecraft for each NeoForge version.

## How the effect works

1. `CrosshairAnimation.compute(partialTick, width, height)` builds the same transform Minecraft
   uses in `GameRenderer#bobView`, applies the optional action animations, and returns a
   screen-space `Transform` (offset, angle, scale).
2. The per-version GUI mixin wraps only the crosshair draw call:
   - 1.21.1: `Gui#renderCrosshair` with a `PoseStack`.
   - 1.21.11: `Gui#renderCrosshair` with a `Matrix3x2fStack`.
   - 26.1.2: `Gui#extractCrosshair` with a `GuiGraphicsExtractor` and `Matrix3x2fStack`.
   - 26.2–26.3: `Hud#extractCrosshair` with a `GuiGraphicsExtractor` and `Matrix3x2fStack`.
3. `GameModeMixin` triggers the hit animation from `MultiPlayerGameMode#attack` and
   `#destroyBlock`.
4. `LivingEntityAccessor` exposes `autoSpinAttackTicks` for the riptide animation.
5. `GameRendererAccessor` exposes `getFov` on 1.21.x. On 26.x the public
   `Camera#getFov()` is used instead.

The depth used for the projection is the distance to `Minecraft#hitResult`; if nothing is hit, the
mod uses the same 1000-block fallback as the original. The GUI mixin pushes and pops around the
single crosshair call and uses an `ACTIVE` thread-local to keep the pair balanced.

### Version differences live in `PlayerState`

Keep `CrosshairAnimation` free of Minecraft-version APIs. Add a helper to `PlayerState` instead:

| Concern | 1.21.1 | 1.21.11 | 26.1.2 | 26.3 (main) |
|---|---|---|---|---|
| Bob amount | `Mth.lerp(pt, player.oBob, player.bob)` | `player.avatarState().getInterpolatedBob(pt)` | same as 1.21.11 | same as 1.21.11 |
| Walk distance | `player.walkDist` / `walkDistO` | `avatarState().getBackwardsInterpolatedWalkDistance(pt)` | same as 1.21.11 | same as 1.21.11 |
| Swing progress | `getAttackAnim(pt)` | `getAttackAnim(pt)` | `getAttackAnim(pt)` | `getSwingAnimation(pt)` |
| Use animation enum | `UseAnim` | `ItemUseAnimation` | `ItemUseAnimation` | `ItemUseAnimation` |
| Selected slot | `inventory.selected`, `getSelected()` | `getSelectedSlot()`, `getSelectedItem()` | same as 1.21.11 | same as 1.21.11 |
| FOV | `GameRendererAccessor#getFov` (returns `double`) | `GameRendererAccessor#getFov` (returns `float`) | `getMainCamera().getFov()` | `mainCamera().getFov()` |
| GUI class / method | `Gui#renderCrosshair` | `Gui#renderCrosshair` | `Gui#extractCrosshair` | `Hud#extractCrosshair` |
| GUI pose type | `PoseStack` | `Matrix3x2fStack` | `Matrix3x2fStack` | `Matrix3x2fStack` |

26.3 renamed `Options#hideGui` (gone) and replaced GLFW with SDL, so use
`InputConstants.KEY_B`, not `GLFW.GLFW_KEY_B`.

### Loader differences

Fabric branches:

- `CrosshairBobbingContinued` implements `ClientModInitializer`.
- Config directory: `FabricLoader.getInstance().getConfigDir()`.
- Keybind: `KeyBindingHelper.registerKeyBinding`; toggle handled in
  `ClientTickEvents.END_CLIENT_TICK`.
- Key category: `"key.categories.crosshair-bobbing-continued"` on 1.21.1; `KeyMapping.Category`
  on 1.21.11/26.x.
- Metadata: `fabric.mod.json`; `fabric-api` is required.

NeoForge branches:

- `@Mod(value = CrosshairBobbingContinued.MOD_ID, dist = Dist.CLIENT)` — the class is only loaded
  on the physical client, so there is no server-side guard needed.
- Config directory: `FMLPaths.CONFIGDIR.get()`.
- Keybind: `RegisterKeyMappingsEvent` on the mod event bus. On 1.21.11/26.x call
  `event.registerCategory(CATEGORY)` first; on 1.21.1 use the string category.
- Toggle handled in `NeoForge.EVENT_BUS`'s `ClientTickEvent.Post`.
- Metadata: `src/main/templates/META-INF/neoforge.mods.toml`, expanded by the
  `generateModMetadata` task.
- Mixins are registered in `neoforge.mods.toml` with `[[mixins]] config="..."`.
- No Fabric API, no `fabric.mod.json`.

**`logoFile` deprecation:** NeoForge 26.x replaced `logoFile` with `bannerFile` + `iconFile`.
Use `iconFile` for the square icon on `neoforge-26.1.2` and `neoforge-26.3`. NeoForge 1.21.x has
no `iconFile`; keep `logoFile` there. Using `logoFile` on 26.x triggers a mod-loading warning.

## Testing

Build every branch. Then run a real client on each branch and confirm:

1. The log contains `Crosshair Bobbing Continued loaded (enabled=true)`.
2. No `Mixin apply failed`, `InvalidInjectionException`, or `InjectionError` line appears.
3. No deprecated `logoFile` warning appears on NeoForge 26.x.
4. An integrated world starts and the player joins (`Starting integrated minecraft server`
   followed by `joined the game`).

Fabric:

```bash
mkdir -p run
printf 'eula=true\n' > run/eula.txt
JAVA_HOME=/usr/lib/jvm/java-25-openjdk \
PATH=/usr/lib/jvm/java-25-openjdk/bin:$PATH \
timeout 150 ./gradlew runClient --no-daemon
```

NeoForge uses the same `runClient` task with the branch's era JDK:

```bash
# 1.21.x
JAVA_HOME=/usr/lib/jvm/java-21-openjdk PATH=/usr/lib/jvm/java-21-openjdk/bin:$PATH ./gradlew runClient --no-daemon
# 26.x
JAVA_HOME=/usr/lib/jvm/java-25-openjdk PATH=/usr/lib/jvm/java-25-openjdk/bin:$PATH ./gradlew runClient --no-daemon
```

`com.mojang.text2speech` and Realms warnings about `libflite.so` are harmless in headless CI and
are not caused by this mod.

Manual visual checks (do these before a release when changing the maths):

- Walk forward and confirm the crosshair moves with the camera bob, not against it.
- Press **B** and confirm the crosshair returns to the centre immediately.
- Disable View Bobbing in Options → Video Settings and confirm the crosshair stays centred.
- Crouch, hit a mob, break a block, switch hotbar slots, eat, draw a bow, charge a crossbow, and
  use a riptide trident.

## Git and release workflow

1. Make animation/maths changes on `main` first, then on the Fabric branches.
2. Port every change to the matching NeoForge branch. Keep the branch's loader glue intact.
3. Build every branch with its era's JDK.
4. Bump `mod_version` in `gradle.properties` on every branch (Fabric and NeoForge both use
   `mod_version`).
5. Update the Minecraft dependency in `fabric.mod.json` / `neoforge.mods.toml` when a branch's
   version range changes.
6. Commit and push all eight branches.
7. Create grouped tags and GitHub releases. Attach both loaders to the matching release:
   `crosshair-bobbing-continued-<version>+mc<range>.jar` and
   `crosshair-bobbing-continued-<version>+mc<version>-neoforge.jar`.
8. Publish separate Modrinth versions per loader and Minecraft range. `loaders` is `["fabric"]`
   or `["neoforge"]`; `environment` is `client_only`. `version_number` must be unique per
   project, for example `1.0.0+mc26.2` and `1.0.0+mc26.2-neoforge`.
9. Archive superseded Modrinth versions; do not delete them.

Use temporary worktrees for isolated builds:

```bash
git worktree add -f /tmp/opencode/cbr-neoforge-26.3 neoforge-26.3
rm -rf /tmp/opencode/cbr-neoforge-26.3/build /tmp/opencode/cbr-neoforge-26.3/.gradle
git worktree remove --force /tmp/opencode/cbr-neoforge-26.3
```

## Modrinth

- Project id: `YLreMsL2`
- Slug: `crosshair-bobbing-continued` (do not change)
- Title: `Crosshair Bobbing Continued`
- Environment: `client_only`
- Loaders: `fabric` and `neoforge` (one version per loader/range)
- License: GPL-3.0-only
- Project body mirrors `README.md`
- Token handling: `.env` at the repo root, `chmod 600`, gitignored, never printed or committed.
  Load it with `set -a; . ./.env; set +a`.

Publishing follows the same `POST /v2/version` multipart flow documented in
`~/Projects/Grindset/mods/death-messages/AGENTS.md`. List every supported game version in `game_versions`.
NeoForge publishes exact versions: 1.21.1, 1.21.11, 26.1.2, 26.2, 26.3.

## Icons and Modrinth art

All project icons follow the shared family documented in [ICON.md](ICON.md):

- 512 × 512 PNG rendered from `resources/icon.svg`; rounded 96 px corners, dark tinted gradient,
  one bold white glyph, one accent colour, soft glow, no text.
- This project's accent is **amber `#FFC857`** on a `#3D2E1C` → `#17110A` background. Do not
  change it without updating ICON.md and the other projects' palettes.
- Keep `resources/icon.png` and `src/main/resources/assets/crosshair-bobbing-continued/icon.png`
  identical. The SVG is the source of truth.
- Upload with `PATCH /v2/project/YLreMsL2/icon?ext=png`; success is `204 No Content`.
- When adding or refreshing art, update ICON.md rather than inventing a separate style.

## Common pitfalls

- **Do not apply the transform when `options.bobView()` is off.** The crosshair must stay centred.
- **Push/pop pairing.** The GUI mixin sets an `ACTIVE` thread-local and only pops when it pushed.
  Keep the HEAD/RETURN pair in sync; an unbalanced pose stack corrupts every later GUI draw.
- **26.x GUI is extracted, not rendered.** The method is on `Hud`/`Gui` and writes into
  `GuiGraphicsExtractor`; do not look for `GuiGraphics` drawing calls there.
- **1.21.11 already uses `Matrix3x2fStack`**, not `PoseStack`. Do not copy the 1.21.1 mixin as-is.
- **26.3 uses SDL, not GLFW.** Use `InputConstants.KEY_*`.
- **NeoForge mod id is `crosshair_bobbing_continued`**, not the hyphenated Fabric id. The config
  file and resource namespace keep hyphens.
- **NeoForge `logoFile` is deprecated on 26.x.** Use `iconFile` there.
- **NeoForge `@Mod` must use `dist = Dist.CLIENT`.** Do not load client classes on a dedicated
  server.
- **GPL.** Never relicense this repository or copy code from an incompatible mod into it.
- `.env` and `run/` are gitignored. Keep them out of commits.
