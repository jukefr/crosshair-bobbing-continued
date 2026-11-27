# AGENTS.md

Instructions for future agents working on this repository.

## Project overview

Crosshair Bobbing Reborn (`crosshair-bobbing-reborn`) is a **client-side only** Fabric mod that
makes the crosshair follow Minecraft's view bobbing and adds optional action animations.

- Display name: Crosshair Bobbing Reborn
- Mod id: `crosshair-bobbing-reborn` (same on every branch)
- Main package: `fr.juke.crosshairbobbing`
- Client entrypoint: `fr.juke.crosshairbobbing.CrosshairBobbingReborn`
- Config file: `config/crosshair-bobbing-reborn.json`
- GitHub: https://github.com/jukefr/crosshair-bobbing-reborn
- Modrinth: https://modrinth.com/mod/crosshair-bobbing-reborn (project id: ask the user — a new
  Modrinth project cannot be created through the API)
- License: **GPL-3.0-only**

The mod is a continuation of **Crosshair Bobbing** by Krash220
(https://modrinth.com/mod/xbob, source https://github.com/Krash220/CrosshairBobbingMod). The
original is GPL-3.0-only, so this repository **must stay GPL-3.0-only and must publish its source**.
Keep the attribution in `README.md` and the Modrinth description.

Do not reuse the original project's name, icon, or jar as-is. This is a Reborn continuation under
the original license, with a new icon and a rebuilt, version-specific implementation.

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

## Supported versions and branch layout

`main` tracks the latest supported Minecraft version (currently 26.3). One branch per supported
line:

| Branch | Minecraft | Loader | Mappings | Java |
|---|---|---|---|---|
| `1.21.1` | 1.21.1 | Fabric | Mojang official | 21 |
| `1.21.11` | 1.21.11 | Fabric | Mojang official | 21 |
| `26.1.2` | 26.1, 26.1.1, 26.1.2 | Fabric | none (unobfuscated) | 25 |
| `main` | 26.2, 26.3 | Fabric | none (unobfuscated) | 25 |

NeoForge branches are not created yet. If they are added, the pattern from
`~/Projects/death-messages` applies: one branch per version line (`neoforge-1.21.1`,
`neoforge-1.21.11`, `neoforge-26.1.2`, `neoforge-26.3`). The shared animation code is loader
agnostic; only the entrypoint, config directory, and keybind registration need a NeoForge version.

## Toolchains and build commands

| Branch range | Gradle | Build JDK | Loom | Java target |
|---|---|---|---|---|
| 1.21.1 – 1.21.11 | 9.8.2 | 25 | Loom 1.19.4 (`fabric-loom-remap`), official Mojang mappings | 21 |
| 26.x | 9.8.2 | 25 | Loom 1.19.4 (`fabric-loom`), no mappings | 25 |

```bash
# Fabric 1.21.x and 26.x
JAVA_HOME=/usr/lib/jvm/java-25-openjdk \
PATH=/usr/lib/jvm/java-25-openjdk/bin:$PATH \
./gradlew build --no-daemon
```

Loom 1.19.4 requires **JDK 25 to run Gradle**, even on branches that compile to Java 21.

Mixin compatibility levels: `JAVA_21` on 1.21.x, `JAVA_25` on 26.x. Do not lower them.

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
mod uses the same 1000-block fallback as the original.

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
| Keybind helper | `KeyBindingHelper` (string category) | `KeyBindingHelper` + `KeyMapping.Category` | `KeyMappingHelper` + `KeyMapping.Category` | `KeyMappingHelper` + `KeyMapping.Category` |
| Action-bar message | `displayClientMessage(component, true)` | `displayClientMessage(component, true)` | `sendOverlayMessage(component)` | `sendOverlayMessage(component)` |
| GUI class / method | `Gui#renderCrosshair` | `Gui#renderCrosshair` | `Gui#extractCrosshair` | `Hud#extractCrosshair` |
| GUI pose type | `PoseStack` | `Matrix3x2fStack` | `Matrix3x2fStack` | `Matrix3x2fStack` |

26.3 renamed `Options#hideGui` (gone) and replaced GLFW with SDL, so use
`InputConstants.KEY_B`, not `GLFW.GLFW_KEY_B`.

## Testing

Build every branch. Then run a real client on each branch and confirm:

1. The log contains `Crosshair Bobbing Reborn loaded (enabled=true)`.
2. No `Mixin apply failed`, `InvalidInjectionException`, or `InjectionError` line appears.
3. An integrated world starts and the player joins (`Starting integrated minecraft server`
   followed by `joined the game`).

```bash
mkdir -p run
printf 'eula=true\n' > run/eula.txt
JAVA_HOME=/usr/lib/jvm/java-25-openjdk \
PATH=/usr/lib/jvm/java-25-openjdk/bin:$PATH \
timeout 150 ./gradlew runClient --no-daemon
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

1. Make animation/maths changes on `main` first.
2. Port the change to every branch, keeping the branch's version-specific code intact.
3. Build every branch with its era's JDK.
4. Bump `mod_version` in `gradle.properties` on every branch.
5. Update the Minecraft dependency in `fabric.mod.json` when a branch's version range changes.
6. Commit and push all branches.
7. Create grouped tags per version range and GitHub releases with jars renamed as
   `crosshair-bobbing-reborn-<version>+mc<range>.jar`.
8. Publish separate Modrinth versions per Minecraft range. Use the `fabric` loader and the
   `client` environment. `version_number` must be unique per project, for example
   `1.0.0+mc26.2`.
9. Archive superseded Modrinth versions; do not delete them.

Use temporary worktrees for isolated builds:

```bash
git worktree add -f /tmp/opencode/cbr-1.21.1 1.21.1
rm -rf /tmp/opencode/cbr-1.21.1/build /tmp/opencode/cbr-1.21.1/.gradle
git worktree remove --force /tmp/opencode/cbr-1.21.1
```

## Modrinth

- Slug: `crosshair-bobbing-reborn`
- Title: `Crosshair Bobbing Reborn`
- Environment: `client_only`
- Loaders: `["fabric"]`
- License: GPL-3.0-only
- Project body mirrors `README.md`
- The Modrinth project must be created in the dashboard by the user before versions can be
  uploaded. Ask for the project id/slug, then publish.
- Token handling: `.env` at the repo root, `chmod 600`, gitignored, never printed or committed.
  Load it with `set -a; . ./.env; set +a`.

Publishing follows the same `POST /v2/version` multipart flow documented in
`~/Projects/death-messages/AGENTS.md`. List every supported game version in `game_versions` and
use a unique `version_number` per loader/range.

## Common pitfalls

- **Do not apply the transform when `options.bobView()` is off.** The crosshair must stay centred.
- **Push/pop pairing.** The GUI mixin sets an `ACTIVE` thread-local and only pops when it pushed.
  Keep the HEAD/RETURN pair in sync; an unbalanced poses stack corrupts every later GUI draw.
- **26.x GUI is extracted, not rendered.** The method is on `Hud`/`Gui` and writes into
  `GuiGraphicsExtractor`; do not look for `GuiGraphics` drawing calls there.
- **1.21.11 already uses `Matrix3x2fStack`**, not `PoseStack`. Do not copy the 1.21.1 mixin as-is.
- **26.3 uses SDL, not GLFW.** Use `InputConstants.KEY_*`.
- **Mod id is `crosshair-bobbing-reborn`** on disk and in `fabric.mod.json`; do not rename it
  without a migration plan for config paths.
- **GPL.** Never relicense this repository or copy code from an incompatible mod into it.
- `.env` and `run/` are gitignored. Keep them out of commits.
