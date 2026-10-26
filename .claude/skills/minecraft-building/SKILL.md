---
name: minecraft-building
description: Build, edit, analyze or repair the Zaruba game maps (capture points, destroy objectives) on the local Docker server - placing structures over RCON, reading region files, migrating missing/renamed mod blocks, restoring chunks, setting respawn points and capture areas. Use for any request to construct, remove or change something in a Minecraft world, to find out what's in a world, or to fix blocks that disappear.
---

# Building in the Zaruba worlds

You can read every block from the region files and place blocks over RCON. To see the result, use the screenshot
camera (below); without it, compensate with numbers (summaries, probes) and ask the user for screenshots.

**Read [builds.md](builds.md) first**: what has been built where (respawn bases A–E, the pumping station and its
deck), with generator arguments, layouts and known issues. Add every new build or change to it.

## Screenshot camera

A second client, `ClaudeCam`, runs in the background as a spectator. The user starts it from the UF launcher ("Unreal
Zaruba" pack, signed in as ClaudeCam); the launcher passes the login in `UF_ACCESS_TOKEN`. The old Prism instance
`X:\Games\mc_modpacks\Instances\ZarubaCamera` no longer authenticates. Any client logged in as ClaudeCam with the Zaruba
mod works: the camera tag is in ClaudeCam's player data on the server. DEV mode only, map open (`/openmap`). Check it's online with `rcon.sh list`; if it isn't, ask the user to
launch it.

```bash
rcon.sh "camera look -160 50 -20 -150 34 10 pump_overview"      # from xyz, target xyz, name [fov]
rcon.sh "camera shot -150 45 10 0 90 pump_top 70"                # xyz yaw pitch name [fov]; pitch 90 = straight down
rcon.sh "camera status"                                          # repeat until "<name>: saved ..." (usually 2-10 s)
```

Then Read the PNG at the path `camera status` prints (UF launcher client:
`%LOCALAPPDATA%\UnrealFrontiers.Local\Instances\Unreal Zaruba\screenshots\claude\<name>.png`). Yaw: 0 = south (+z),
90 = west, 180 = north, -90 = east. Take an overview from ~20 blocks up at 45° plus ground-level shots from each
approach. Coordinates are game_dim's; the command always shoots in the game map.

- Put the camera in open air: inside a building or tank the shot is just a dark wall (happened with a camera inside a
  storage tank). `look` from outside the build, 10-30 blocks away; ground-level shots 1-2 blocks above the floor.
- A server restart disconnects ClaudeCam: ask the user to reconnect it before taking shots.
- Wait for `camera status` to say `saved` (usually 3-8 s), then Read the PNG. 2560x1411 when the window is maximised.

## The setup

- Server: Docker container `zaruba`, data at `X:\Projects\UnrealFrontiers\zaruba-server\data` (bind mount, `/data` inside).
- RCON: `X:\Projects\UnrealFrontiers\zaruba-server\tools\rcon.sh "<command>"` (one command per call).
- Maps: several per mode. A map is `data/game_worlds/<map>/dimensions/unrealzaruba/game_dim/{region,entities,poi,data}`;
  each mode's first map is its old template (`capturepointsgamemode`, `destroyobjectivesgamemode`), others are made
  with `/newmap <key> <mode> [from]` and described in `data/unrealzaruba/maps/<map>/map.json`. `/mapinfo` lists them,
  `/openmap <map>` opens one. Matches in GAME mode run on a throwaway copy in `data/game_worlds_runtime/` that never saves.
- Map config (per map): `objectives.json` (capture areas, `respawnPosition`), `teamData.json` (team bases),
  `barriervolumes.json`, `supplydroparea.json`. For a mode's first map in `data/unrealzaruba/gamemodedata/<Gamemode>/`
  (the admin panel edits those), for other maps in `data/unrealzaruba/maps/<map>/`. Per mode, in
  `gamemodedata/<Gamemode>/`: `economy.json`, `scoring.json`. Read once per match start.
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
6. **Build additively.** Generators draw into `generators/Canvas` and never replace what's already in the world:
   blocks go only into air, water and replaceable plants; floors (`ground()`, and layer G in the generators) may also
   replace natural ground. Removing anything (trees, an opening in an existing wall) is a `C.clear(...)` that only runs
   with `/buildkit apply <name> clear`; use it only when the user agrees to those listed clears. Never write
   `fill ... air` by hand into a generator.
7. **Check block ids exist** before building: vanilla ids are 1.21 names (`short_grass`, not `grass`), mod ids from
   the jars (see "known ids" below). An unknown id fails the whole command.

## Tools (tools/worldkit, Java 21)

Compile once per session into the scratchpad:

```bash
GS=$(find /x/Projects/UnrealFrontiers/zaruba-server/data/libraries -name "gson-2*.jar" | head -1)
W=/x/Projects/Unreal-Zaruba-Gamemode/tools/worldkit
javac -d "$SCRATCH/wk" -cp "$GS" $W/*.java $W/generators/*.java
java -cp "$SCRATCH/wk" <Tool> ...      # StateProps needs ";$(cygpath -m $GS)" on the classpath too
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
| `generators/Canvas.java` | Additive canvas the generators draw into (see rule 6). Writes a build file; lists its clears on stderr |
| `Skipped <cmds_lf.txt> <out.txt>` | Only for the `--commands` fallback: what an RCON-command build couldn't place |
| `Base compound <cx> <groundY> <cz> <R> <padLen> <tents yes/no>` / `Base outpost <cx> <groundY> <cz> <padLen>` | Respawn base generator |
| `Complex <cx> <groundY> <cz>` | The lakeside pumping station (capture point). Template for capture-point complexes |
| `PumpDeck <cx> <groundY> <cz>` | Catwalk second floor of the pumping station (after Complex) |
| `Reground <region> x0 z0 x1 z1 <groundY> <block>` | Re-covers an artificial floor (e.g. coarse dirt under a site) with the natural surface pattern learned from a 24-block band around it, plants included. Only cells whose top block is `<block>` change. Read the region after `save-all flush` |
| `Pathway <region> <halfWidth> x z x z ... [--stumps=1|-1]` | Natural dirt trail (coarse dirt / podzol / rooted dirt in 2-block clumps, ~68% dense middle, soft edges, painted grass near the middle turned to moss/grass) along a spline through waypoints, in the style of the trail at 0,0; optional oak log stumps along one edge. Uses swaps; runs under tree canopies, skips farmland, roads and builds. Half width 8 matches the 0,0 trail |
| `LevelMounds <region> <groundY> <halfWidth> x z ...` | Levels whole raised mounds (rock/wheat bumps) that touch a road corridor, same waypoints as Pathway; road texture inside, grass outside. Clears: apply with `clear` |
| `Pathway ... --density=0.75 --clear-trees` | Vehicle road variant: denser, and removes whole trees whose wood within 8 of the ground stands in the corridor (+1 clearance), with their own leaves; neighbours keep full crowns. Apply with `clear` |
| `LaneRoad <region> xWest xEast z0 z1 y xRise` | Straight paved road along x in the main road's style (brown edges, yellow lines, grey lanes, white dashed bone-block centre line from 9 wide); one higher west of xRise with a slab band. Apply with `clear` |
| `Ramp <region> x0 x1 zStart dir y run rows [taperW taperE]` | Driveable ramp up a hillside (1 per `run` rows) with side banks; cuts earth above, fills dips. Apply with `clear` |
| `Tidy <region> x0 z0 x1 z1 slope smoothDist treeClear ref...` | Levels leftovers around new surfaces (ref = `x0 z0 x1 z1 y` or `auto` for a ramp): walls become banks rising `slope` per block, ditches by the road filled, small bumps/pits median-smoothed, broken/floating trees and leaf clumps removed (fence/copycat trunks count as trees). Run after a road or ramp; apply with `clear` |
| `Forest <region> <trees dir> x0 z0 x1 z1 [--track hw x z ...] [--hill x z r h] [--swamp x z r] [--avoid ...] [--part k n]` | Forest with relief in open land: hills, small swamps flush with the ground, corridors kept clear for tracks (then lay them with Pathway, same points), trees from a blueprint folder anchored by their trunk on level ground, forest floor and undergrowth. Keeps away from builds, roads, water, old trunks |
| `Empty <region> x0 z0 x1 z1 out.png [minR] [max]` | Where the map is bare: PNG (bright = far from any tree/build/road/water) and the largest bare disks (centre, radius, ground y). Start of any "fill the map" job |
| `ImportNbt <file.nbt> <out.bp> <name> --known known.txt [--replace old=new\|keep,props+k:v,...]` | Imports a structure-block file (downloads) into the library: ground layer detected, Create bearing contraptions (sails) added as static blocks, newer-version blocks replaced or listed. known.txt = vanilla ids from the server jar's block loot tables + wall variants + mod blockstates |
| `Props place <region> <type\|file.bp> x z q ...` | Puts library blueprints on the ground (highest ground under the footprint, earth/cobblestone under lower columns; skipped if the footprint isn't open). Its drawn designs are placeholders; use imported ones |
| `Farmstead <region> <rural dir> cx cz q [izbaA izbaB] [--no-mill]` | Khutor: imported windmill, two izbas facing a yard, well, haystacks, log pile, fenced field; prints the gate for a Pathway trail |
| `ClearTrees <region> x0 z0 x1 z1` | Removes whole trees whose trunk base is in the box (wood incl. fence/copycat branches, their own leaves; neighbours keep their crowns), forest floor back to grass. Apply with `clear` |
| `Meadow <region> x0 z0 x1 z1 [--avoid ...] [--density d]` | Grass, flower patches, bushes on open natural ground; ferns/berries at forest edges, verges by roads. Plants only, into air. Run last, in strips of 256 |
| `Entities list <entities dir>` / `Entities strip <dir> <out> item,hostile,...` | Saved entities by id; writes the region files without the given ids (dropped items, mobs saved while the map was open). Offline, map closed |
| `HallLights <region> <layout> [--sim-only]` | Lights for a closed hall: createdeco cage lamps (lit without redstone) on chains from the ceiling or under catwalks, planned against a block light simulation of the hall that prints the light map per standing level (digit = light, + = 10+). Layouts are code in the generator (`power_station`) |
| `PowerHall <region> units,walls,consoles,hazard` | The Электростанция hall dressing: turbine-generator sets from dndecor boilers (large boiler = 3x3 cylinder with its own invisible structure ring), wall channels with pipes and cable tubes (clears), platform consoles, hazard decals. Template for machinery in other buildings |
| `RestoreLost <old region> <current region> x0 y0 z0 x1 y1 z1 [--no-signs]` | Puts back blocks of mods the pack lost (s_a_b steel and sandbags, cbcmodernwarfare shells and thrusters, create_connected silos, design_decor signs) from an old map version (`MapsCity Capture Pointsgame_dim.v2`, 1.20.1) where the map now has air, as the nearest current blocks (mapping in the generator). Additive |
| `Dragon <cx> <waterY> <cz>` | Organic test statue (ender dragon on an end-stone island). Template for organic shapes: spline tubes with varying radius, bone + membrane wings, noise-mixed materials |

Known mod block ids: index every `assets/<ns>/blockstates/*.json` in `data/mods/*.jar` (and their `META-INF/jarjar/*.jar`)
into a `known.txt` of `ns:path`; check ids with `grep -qxF`.

## Applying a build (BuildKit, DEV mode, map open)

Generators print a build file (`clear|place|ground x0 y0 z0 x1 y1 z1 reason|block`, absolute coordinates). The mod's
`/buildkit` command (op 3, DEV mode, applies to the open game map) places it in one tick, additively, with undo:

```bash
java -cp "$SCRATCH/wk" PumpDeck -150 32 10 > pumpdeck.build      # stderr: the clears it contains
cp pumpdeck.build /x/Projects/UnrealFrontiers/zaruba-server/data/unrealzaruba/builds/
rcon.sh "buildkit check pumpdeck"          # dry run: would place / already there / skipped, what's in the way
rcon.sh "buildkit apply pumpdeck"          # place; "apply pumpdeck clear" also runs the clears (ask first)
rcon.sh "buildkit undo pumpdeck"           # put back exactly what the last apply changed (journal in builds/undo/)
```

- Fourth op, `swap x0 y0 z0 x1 y1 z1 <from> <block>` (Canvas `swap()`): replaces only where the world has exactly
  `<from>`. For surface edits on blocks the ground rule doesn't allow (this map paints its grass with
  `lime_terracotta` and `green_concrete_powder`): the generator reads the region, writes what it saw as `<from>`, and
  anything changed since is skipped. Read the region after `save-all flush`.
- Reports with every skipped position and the block in the way: `data/unrealzaruba/builds/reports/<name>.txt`.
- "already there" = same block with the properties the file names (re-applying a build is a no-op).
- Undo journals stack: each `undo` reverts the latest not-yet-undone apply of that name (the journal is then renamed
  `.undone`). After an undo, `check` should report the whole build as placeable again; that's the proof it's gone.
- Block entities with NBT (`oak_sign[...]{...}`) work; replaced containers are emptied first (no item drops).
- Tested 2026-10-08: 9,339 blocks applied in ~0.5 s, undone exactly (lake water back). `save-all flush` afterwards.
- `skipped` isn't always a problem: a deck re-check skips the rack girders that pass through it, by design. Read the
  report's "in the way" list before deciding.
- Files copied from Windows into `data/unrealzaruba/builds/` may need `docker exec zaruba sh -c 'chown -R 1000:1000
  /data/unrealzaruba/builds'` so the mod can write `undo/` and `reports/`.
- New mod code needs the jar swapped with the server **stopped** (overwriting a jar under a running JVM can crash it):
  `save-all flush`, `docker compose stop`, copy, `docker compose start`, wait for "Done (", `/openmap capturepoints`.
  Ask first if anyone is online.

## Organic shapes

For non-boxy builds (statues, rocks, terrain features) see `generators/Dragon.java`: sweep spheres along Catmull-Rom
splines (`spline` + `tube`, radius per control point), rasterise thin surfaces as dense barycentric samples of
triangles (`membrane`), classify voxels by priority (eye > claw > spike > body > bone > membrane > base) and only then
pick materials, with a hash noise for speckled mixes and a "belly" flag for the underside. Keep it in one generator
run so one `undo` removes all of it.

## Fallback: RCON commands

With `--commands` (and `--clear` to include the clears) generators print `execute in unrealzaruba:game_dim run ...`
fill commands instead. Use only if the mod's BuildKit is unavailable. Run them inside the container in one go:

```bash
tr -d '\r' < cmds.txt > cmds_lf.txt                      # Windows line endings break every command
docker cp cmds_lf.txt zaruba:/tmp/run.txt
docker cp tools/worldkit/runcmds.sh zaruba:/tmp/runcmds.sh
docker exec zaruba sh -c 'sh /tmp/runcmds.sh' > out.txt  # sh -c: Git Bash rewrites bare /tmp paths
grep -viE "\|(Successfully filled|No blocks were filled|Changed the block|Marked|Unmarked)" out.txt   # errors
java -cp "$SCRATCH/wk" Skipped cmds_lf.txt out.txt       # what an additive build couldn't place
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
4. **Write a generator** (copy `Base.java` / `Complex.java`, draw into a `Canvas`): relative coordinates around a
   centre, ground y as a parameter, `site()` that fills holes under the floor and lays the floor at G, trees only as a
   `C.clear`; then features. Openings in your own walls: draw "air" over them (erases from the canvas, safe). Keep
   gameplay structures mirrored between the two team directions.
5. **Check ids** in the generated file against `known.txt`, read the clears it lists on stderr, `buildkit check`, then
   `buildkit apply`. If trees or old builds block important parts (the report lists them), tell the user what and
   where, and ask before `apply <name> clear` or before moving the build. Wrong result: `buildkit undo`, fix, re-apply.
6. **Verify**: camera shots (overview + each approach), then `save-all flush`; `Probe` key spots (respawn block + 2 air
   above, spawner present) if needed.
7. **Wire up config** (in game the user can do it with the map editor, `/mapeditor`, DEV mode: capture areas, owners, respawns, targets, team bases): `respawnPosition` (feet position) on the capture point; new capture points need
   `captureArea {minPos, maxPos, center}`, `position`, `name`, `type: "capturepoint"`, `progress`, `isCompleted`.
   Edit `objectives.json` textually (keep formatting), validate with PowerShell `ConvertFrom-Json`.
8. **Record it in builds.md**: location, generator and arguments, layout, gameplay intent, config wiring, leftovers.
9. **Report** what was built where, what was kept/cleared, then ask for screenshots of specific spots
   (give tp commands: `execute in unrealzaruba:game_dim run tp <player> x y z`). Remind: `/mapscan <name>` refreshes the
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
- `java Gen.java` (single-file mode) can't see `Canvas`: compile generators together with javac (see Tools).
- In Git Bash, a Java classpath with `;` needs Windows paths (`cygpath -m`); `/x/...` entries are silently ignored.
- No Python on this machine; multi-line edits from the shell: perl (watch out, `//` in a replacement ends `s///`).
- createdeco catwalk stairs face their low end (opposite of vanilla stairs); catwalk floors sit at the top of their
  block (catwalk at y = stand at y+1); railings are separate blocks above the catwalk with per-side booleans.
- The capture points lake: surface y 28, bottom about y -31 (60 blocks deep), roughly x -404..-212, z -100..160.
- **Plants cannot stand on the painted grass** (lime terracotta, green concrete powder), stone or gravel: only on
  `#minecraft:dirt` (grass, dirt, podzol, coarse/rooted dirt, moss, mud). BuildKit places them anyway, and they pop
  off as items at the first neighbour update (the next plant placed beside them). Those items, and any dropped while a
  map is open in DEV, are **saved into the template and load into every match** (2026-10-24: 19,065 items, lag). Use
  `BorderHills.holdsPlants` for any plant placement; mushrooms only on podzol. After a DEV session check
  `java Entities list <game_dim/entities>` and strip items with `Entities strip` (map closed / GAME mode).
- Water next to the map's painted grass hardens `green_concrete_powder` into `green_concrete` the moment it is placed,
  so swaps of neighbouring powder cells then fail ("in the way: green_concrete"). For builds that place water, append
  a second swap `green_concrete -> <same block>` for those cells at the end of the file; an undo restores powder next
  to water that is still there and it hardens again: after it, find `green_concrete` and swap it back to powder.
- Library trees come in pieces: trunks with gaps, fence branches, crowns joined only by leaves, floating cross tips on
  the spruces. Anchor a tree by its lowest trunk layer (not the blueprint centre), and don't treat detached pieces or
  tips as "floating" (Tidy handles both).
- **Physics bodies (Sable sub-levels) built or spawned while a map is open in DEV are saved into the template** (`sublevels/`,
  `data/sable_*`, `gadgetsngizmos_sublevel_locations.dat`) and load into every match. v3 has none: after a DEV session check
  `data/gadgetsngizmos_sublevel_locations.dat` with DumpNbt; to clear, reset `sublevels/` and `data/` to v3 with the map closed.
- **Wall and floor decals, signs, buttons hang on a sturdy full face** (createdeco decals are FaceAttachedHorizontalDirectionalBlock).
  On catwalks, gratings, slabs or boilers they drop as items at the first neighbour update, and in DEV those items are saved
  into the template. Check the support block before placing one (RestoreLost does), and after a session `kill` items in
  game_dim (`execute in unrealzaruba:game_dim run kill @e[type=item,x=-2000,y=-64,z=-2000,dx=4000,dy=400,dz=4000]`).
- Pathway `--clear-trees` removes leaves within 6 of the cut wood: big crowns leave pieces in the air. Run `Tidy` with
  no references over the area afterwards and keep only the clears near the track.
