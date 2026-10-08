---
name: minecraft-building
description: Build, edit, analyze or repair the Zaruba game maps (capture points, destroy objectives) on the local Docker server - placing structures over RCON, reading region files, migrating missing/renamed mod blocks, restoring chunks, setting respawn points and capture areas. Use for any request to construct, remove or change something in a Minecraft world, to find out what's in a world, or to fix blocks that disappear.
---

# Building in the Zaruba worlds

You can read every block from the region files and place blocks over RCON. To see the result, use the screenshot
camera (below); without it, compensate with numbers (summaries, probes) and ask the user for screenshots.

## Screenshot camera

A second client, `ClaudeCam` (Prism instance `X:\Games\mc_modpacks\Instances\ZarubaCamera`), runs in the background
as a spectator. DEV mode only, map open (`/openmap`). Check it's online with `rcon.sh list`; if it isn't, ask the user to
launch it.

```bash
rcon.sh "camera look -160 50 -20 -150 34 10 pump_overview"      # from xyz, target xyz, name [fov]
rcon.sh "camera shot -150 45 10 0 90 pump_top 70"                # xyz yaw pitch name [fov]; pitch 90 = straight down
rcon.sh "camera status"                                          # repeat until "<name>: saved ..." (usually 2-10 s)
```

Then Read `X:\Games\mc_modpacks\Instances\ZarubaCamera\minecraft\screenshots\claude\<name>.png`. Yaw: 0 = south (+z),
90 = west, 180 = north, -90 = east. Take an overview from ~20 blocks up at 45° plus ground-level shots from each
approach. Coordinates are game_dim's; the command always shoots in the game map.

## The setup

- Server: Docker container `zaruba`, data at `X:\Projects\UnrealFrontiers\zaruba-server\data` (bind mount, `/data` inside).
- RCON: `X:\Projects\UnrealFrontiers\zaruba-server\tools\rcon.sh "<command>"` (one command per call).
- Map templates: `data/game_worlds/<gamemode>/dimensions/unrealzaruba/game_dim/{region,entities,poi,data}`
  (`capturepointsgamemode`, `destroyobjectivesgamemode`). Matches in GAME mode run on a throwaway copy in
  `data/game_worlds_runtime/` that never saves.
- Map config: `data/unrealzaruba/gamemodedata/<Gamemode>/objectives.json` (capture areas, `respawnPosition`),
  `teamData.json` (team bases), `economy.json`, `barriervolumes.json`. Read once per match start.
- Mode: `data/unrealzaruba/mainConfig.json` `"mode": "DEV" | "GAME"`; restart to switch.
- Source maps (user's own): `X:\Projects\UnrealZaruba\Maps\<Map>\game_dim.vN`.
- World is 1.21.1 (DataVersion 3955). Much of the map is still 1.18.2 (2975) / 1.20.1 (3465) chunks that upgrade on load.
- Ground in the capture points map is y=32 (top ground block), players stand at y=33. Dimension min y is -64.

## Rules that are not optional

1. **Every world command goes through `execute in unrealzaruba:game_dim run ...`.** RCON runs in the overworld,
   which is the LOBBY and it saves. A build without the prefix lands in the lobby (it happened once; it cost a restore).
2. **Edit maps in DEV mode.** In DEV, `/openmap capturepoints` opens the template itself with saving on;
   `save-all flush` writes it. In GAME mode nothing you build survives the match.
3. **Never edit region files of a map that is open in the server.** It will overwrite them on save. Either edit live
   over RCON, or have the map closed (server stopped) before offline edits (Transplant, Migrate).
4. **Ask before restarting** if anyone is online (`rcon.sh list`). Restarts kick players. Tell the user what moves them.
5. **Backups:** the user keeps their own. Don't make extra copies unless the operation is destructive and offline;
   then one copy of just the files you rewrite.
6. **Check block ids exist** before building: vanilla ids are 1.21 names (`short_grass`, not `grass`), mod ids from
   the jars (see "known ids" below). An unknown id fails the whole command.

## Tools (tools/worldkit, Java 21)

Compile once per session into the scratchpad:

```bash
GS=$(find /x/Projects/UnrealFrontiers/zaruba-server/data/libraries -name "gson-2*.jar" | head -1)
javac -d "$SCRATCH/wk" -cp "$GS" /x/Projects/Unreal-Zaruba-Gamemode/tools/worldkit/*.java
java -cp "$SCRATCH/wk" <Tool> ...      # StateProps needs ";$GS" on the classpath too
```

All read region files offline and print summaries. Region dir = `.../game_dim/region`.

| Tool | Use |
|---|---|
| `ScanWorld <world dir> minecraft` | Every block and block entity id with counts and an example position (skips `minecraft:`). Basis for "what's missing" |
| `Terrain <region> <cx> <cz> <radius> <half>` | Ground height / water / missing chunks around a point, rated on a circle (site finding) |
| `Built <region> x0 z0 x1 z1 <step> <minY>` | Non-natural blocks with bounding boxes + top-down map (`#` built, `w` water, `t` trees) + ground heights. Main site-analysis tool. Use minY = ground+3 to see obstacles only |
| `Surface <region> x0 z0 x1 z1 <step> <aboveY>` | Top block per cell as a letter map with legend; quick look at what a build or terrain looks like from above |
| `Probe <region> x y z [x y z ...]` | Block at a position plus the two above. Verify spawn spots, key blocks |
| `Palette <world dir> ns1,ns2` | Blocks of given namespaces with property sets, and references to them inside block entity NBT |
| `BeCheck <world dir> [idPrefix]` | Block entities by the block they sit on ("beId @ blockId") with an example |
| `DumpBe <region> x y z` / `DumpNbt <file> <depth>` | Full NBT of one block entity / any gzip NBT file |
| `MultiParts <region>` | Copycats+ multi-material part layouts (for copycat migrations) |
| `StateProps <mods dir> id...` | Property names/values of target blocks from blockstate JSON in jars |
| `Transplant <src.mca> <dst.mca> cx0 cz0 cx1 cz1` | Copy a chunk rectangle between region files (offline). Undo, or moving a build into a template |
| `Restore <backup region> x0 y0 z0 x1 y1 z1` | Commands that rebuild a box from a backup (live undo while the map is open) |
| `Migrate <rules> <region dir> <out dir>` | Rename blocks / block entity ids / copycat materials in region files (offline). Rule file syntax in its header; example in `examples/decor-migration-mapping.txt`. Add the line `copycatfix` to convert old `create:copycat` entities on Copycats+ blocks |
| `generators/Base.java` | Respawn base: `compound <cx> <groundY> <cz> <R> <padLen> <tents yes/no>` or `outpost <cx> <groundY> <cz> <padLen>` |
| `generators/Complex.java` | The lakeside pumping station (capture point). Template for capture-point complexes |

Known mod block ids: index every `assets/<ns>/blockstates/*.json` in `data/mods/*.jar` (and their `META-INF/jarjar/*.jar`)
into a `known.txt` of `ns:path`; check ids with `grep -qxF`.

## Running many commands

Generators print one `execute in unrealzaruba:game_dim run ...` command per line. Run them inside the container in one go:

```bash
tr -d '\r' < cmds.txt > cmds_lf.txt                      # Windows line endings break every command
docker cp cmds_lf.txt zaruba:/tmp/run.txt
docker cp tools/worldkit/runcmds.sh zaruba:/tmp/runcmds.sh
docker exec zaruba sh -c 'sh /tmp/runcmds.sh' > out.txt  # sh -c: Git Bash rewrites bare /tmp paths
grep -viE "\|(Successfully filled|Changed the block|Marked|Unmarked)" out.txt   # what didn't apply
```

`out.txt` lines are `<n>|<reply>`; map `n` back to the command to see what failed.
- "No blocks were filled" / "Could not set the block" = nothing to change (already that block). Harmless.
- "That position is not loaded" = missing `forceload add` (wrap builds in forceload add/remove, max 256 chunks).
- "Expected whitespace ... trailing data" = CRLF.
- `fill` is limited to 32768 blocks; split big clears into layers.

## Workflow for a build

1. **Locate.** Coordinates from the user, or calibrate their map screenshot: pick 3+ known points (team bases from
   `teamData.json`, capture points from `objectives.json`, a lake edge) and fit `x = a*px + b`, `z = c*py + d`. The
   Xaero screenshot is NOT 1 block/pixel (last fit: x = 1.025 px - 1020, z = 1.054 py - 521). Check the fit against a
   terrain feature (`Built` along a line through a lake) before building.
2. **Analyze the site** with `Built` (obstacles above ground, trees, water, ground heights) and `Surface`. Note
   existing buildings to keep and pick the footprint around them.
3. **Match the style.** Run `Built` over a building the user likes (e.g. the RED base around -938 449) and use its
   top blocks as the palette. Capture points map palette: cut granite / cut dripstone bricks, bricks, polished
   andesite, smooth stone, stone bricks, dndecor containers, create metal girders, andesite bars, grey glass panes.
4. **Write a generator** (copy `Base.java` / `Complex.java`): relative coordinates around a centre, ground y as a
   parameter, `site()` that clears trees (air above floor), fills holes (`fill ... dirt replace air|water`) and lays the
   floor; then features. Keep gameplay structures mirrored between the two team directions.
5. **Check ids** in the generated file against `known.txt`, then run (above).
6. **Verify**: `save-all flush`, then `Probe` key spots (respawn block + 2 air above, spawner present) and a `Surface`
   top-down of the whole build.
7. **Wire up config**: `respawnPosition` (feet position) on the capture point; new capture points need
   `captureArea {minPos, maxPos, center}`, `position`, `name`, `type: "capturepoint"`, `progress`, `isCompleted`.
   Edit `objectives.json` textually (keep formatting), validate with PowerShell `ConvertFrom-Json`.
8. **Report** what was built where, what was kept/cleared, then ask for screenshots of specific spots
   (give tp commands: `execute in unrealzaruba:game_dim run tp <player> x y z`). Remind: `/mapscan` refreshes the
   pre-rendered Xaero map; GAME mode to play.

## Gameplay design rules (capture points)

- Teams: RED base (-938 32 449, south-west), BLUE base (943 34 -426, north-east). Mirror cover and paths along the
  axis between their approach directions.
- At least 3-5 approaches to a capture area, none much shorter for one team. Mid-path cover on each.
- Capture yard: mix low cover (walls, 1 high: crouch) and 2-high cover (crates, valve stations); keep a clear centre
  line of sight broken by one central object.
- High ground only if neutral (same distance from both teams) or mirrored.
- Respawn bases: walled compound, gates on all sides, lodestone at the respawn spot with 2 air above, vehicle
  spawner (`unrealzaruba:vehicle_purchase_block[facing=west]`) in a gate with an open pad behind it
  (vehicles spawn opposite the facing, `gap + width/2` away). Shop/team storage work within 20 blocks of the respawn.

## Pitfalls seen before

- Vanilla 1.18/1.20 ids in old data: `grass` is now `short_grass`. Two-block plants need `[half=lower]`/`[half=upper]`.
- Blocks of uninstalled mods turn to air when a chunk is saved in 1.21 format (any chunk loaded in DEV mode).
- Block entities that 1.21 rejects are skipped silently on load (log: "Invalid block entity ... state at", "Skipping
  BlockEntity with id"). Grep the server log after a match: `docker logs zaruba --since <ts>`.
- Removed mods on this modpack: s_a_b, combatgear, trackwork, cbcmodernwarfare, Valkyrien Skies (vs_*), old
  `design_decor` (now `dndecor`, migrated on capture points), Copies Cats. createdeco `cast_iron_*` is now `industrial_iron_*`.
- `/openmap` needs op level 3; the game level exists only after it (or a match) loads the map.
- Players standing on a build you clear will fall: teleport them aside first.
- Use the scratchpad for generated command files; keep reusable code in `tools/worldkit`.
