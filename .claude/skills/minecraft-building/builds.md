# Builds in the Zaruba maps

Everything built so far, so later sessions know what stands where, how it was made and why. Coordinates are absolute
(game_dim of the map). "Ground" = top ground block, players stand one higher. Update this file after every build.

## Capture points map (`capturepointsgamemode`), state on 2026-10-08

Teams: RED base -938 32 449 (south-west), BLUE base 943 34 -426 (north-east).

| Capture point (objectives.json) | Capture area | Respawn base (respawnPosition) |
|---|---|---|
| Вертолётная площадка | -911 33 -332 .. -900 35 -321 | A, -712 33 -389 |
| Дорожный аванпост | -473 36 333 .. -463 38 343 | B, -445 33 467 |
| Насосная станция (built by us) | -159 33 2 .. -141 36 18 | C, -143 33 -111 |
| Склад боеприпасов | 665 34 -299 .. 674 36 -290 | D, 707 34 -222 |
| Электростанция | 768 49 194 .. 773 51 199 (center 770 41 226) | E, 681 39 246 |

"Мэрия" was removed from the objectives at the user's request. The template backup with all of the below is the
user's `X:\Projects\UnrealZaruba\Maps\City Capture Points\game_dim.v3` (made before the pumping station and the deck).

### Respawn bases A–E (generator `Base`)

Built with the old destructive generator (cleared a square R+4 around the centre up to ground+29, then the vehicle pad
strip, before building). Current `Base` is additive and would not clear.

Compound layout (`Base compound <cx> <ground> <cz> <R> <padLen> <tents>`): square walled yard of half-size R, stone
brick walls (1 block + wall on top), 7-wide gates in all four sides with lantern pillars, plaza in the middle with the
**lodestone at the centre on the ground layer** (respawn = centre, ground+1), watchtower with ladder in the NW corner,
two tents (or a supply corner), barrels/hay corners, azalea hedge outside (not on the east side), **vehicle spawner in
the east gate facing west** at (cx+R, ground+1, cz) with a sign, and a grey concrete vehicle pad east of the base from
cx+R+2 to cx+R+1+padLen, half-width R, yellow border.

| Base | Command | Spawner | For |
|---|---|---|---|
| A | `compound -712 32 -389 15 31 yes` | -697 33 -389 | Вертолётная площадка (replaced the base at X) |
| B | `compound -445 32 467 15 31 yes` | -430 33 467 | Дорожный аванпост |
| C | `compound -143 32 -111 15 31 yes` | -128 33 -111 | Насосная станция |
| D | `outpost 707 33 -222 30` | 713 34 -222 | Склад боеприпасов |
| E | `compound 681 38 246 12 20 no` | 693 39 246 | Электростанция (higher ground, smaller, no tents) |

D is an open outpost on the existing paved area next to the ammo depot: blackstone-ringed plaza with the lodestone at
the centre, flag and supplies on the west side, spawner on the east edge, pad markings painted east of it.

### Removed: the first base "at X"

The first respawn base for Вертолётная площадка stood at about -656 106 (lodestone -656 32 106). The user marked it as
a bad spot; it was removed by restoring those chunks to the original meadow, and replaced by base A. Earlier still, a
FOB was accidentally built in the LOBBY (RCON without `execute in`), restored from the zaruba-1 original.

### Насосная станция — lakeside pumping station (generators `Complex`, then `PumpDeck`)

Centre -150 32 10 (`Complex -150 32 10`, `PumpDeck -150 32 10`). A new capture point by the lake at site C, built on
open ground; the lake is to the west. Mirrored north/south so RED (coming from the south-west) and BLUE (north-east) get
equal approaches. Palette taken from the brick industrial buildings in the map's south-west: bricks, cut granite and
dripstone bricks, smooth stone, polished andesite, Create girders and casings, dndecor boilers as pipes.

Ground floor (`Complex`, built with the old destructive site clearing over x -206..-122, z -20..40, up to y 63):
- Site: coarse dirt ground x -190..-124, z -18..38; gravel roads: north-south through the yard (x -152..-148), two east
  approaches (z 0..3 and z 17..20 from x -140 east), a link west to the pier.
- **Capture yard** x -160..-140, z 1..19, yellow hazard border, smooth stone with an andesite cross. Cover: a 3x3,
  2-high valve station in the centre (copper casing, valve handle, a pipe going up), low stone-brick walls at
  z 6 and 14 on both sides (crouch cover), barrel stacks at x -157 and -143 on the centre line.
- **Pump house** (west, by the water) x -186..-166, z 0..20: brick walls on a dripstone course, granite pillars, flat
  smooth-stone roof at y 39 with a stone-brick-wall parapet (stand on the roof at y 40). Doors: wide east door to the
  yard (z 8..12), west door to the pier, small north and south doors (x -177..-175). Inside: two boiler lines on
  plinths (waist-high cover) at z 5 and 15, barrels, hanging lanterns.
- **Intake pipes and pier**: two boiler pipelines on girders from the pump house west to x -214 at z 5 and 15, bending
  down into the lake; spruce pier between them with fences.
- **Storage tanks**: four round tanks, radius 3, 8 high, hollow, at (-159, -9), (-141, -9), (-159, 29), (-141, 29),
  each with a pipe from the top to the rack.
- **Pipe racks**: girder frames along z -1 and z 21, x -165..-137, beam at y 41 and a boiler pipe at y 42 on top.
- **Control building** (east) x -137..-127, z 5..15: granite, two floors (upper floor y 37, stand 38) and a flat roof
  (stand 42) with a parapet, ladder inside at x -128 z 14 from the ground to the roof, long window over the yard,
  doors west (yard) and east (forest), antenna with a lightning rod.
- **Approach cover**: dndecor containers on the north-south road at z -4 (red) and z 24 (blue), crates and a low wall
  on the east approaches (x -134 / -129), slab lines at the outer ends of the road (z -15, z 35).
- Lamps: girder posts with lanterns at the yard corners (x -161/-139, z 0/20) and on the road ends.

Second floor (`PumpDeck`, 2026-10-08, built with the old commands; the generator is now additive):
- **Catwalk ring** of createdeco industrial iron catwalk at y 37 (stand at 38, above the capture area which ends at
  y 36: you can't capture from the deck). North strip x -163..-138, z -2..0 and south strip z 20..22 (under the pipe
  racks, whose columns pass through the deck), west bridge x -163..-161 and east bridge x -140..-138 over z 1..19.
  Railings on every edge except the access points.
- **Access**: catwalk stairs on iron wedges from the north road (x -147..-146, z -7..-3) and the south road
  (z 23..27); ladders from the yard at x -160, z 4 and z 16; the control building's upper floor through a doorway in
  its west wall (x -137, z 9..11); a two-step stair from the west bridge up onto the pump house roof (x -164..-165,
  z 9..11, parapet opened) — the roof is the third level, centred on the north-south axis.
- **Cover on the deck**: 2-high barrel stacks at x -159 and -141 on the outer edges of both strips, single barrels on
  the bridges (x -162 z 7/13, x -139 z 3/17). The lamp-post lanterns at the ring corners were replaced by catwalk.
- Gotcha found here: createdeco catwalk stairs face their **low** end (opposite of vanilla stairs).

Camera viewpoints that show it well: overview `camera look -185 58 -25 -150 34 10`, deck from the north-west
`camera look -175 52 -25 -150 37 10`, roof stair `camera look -160 40 4 -165 39 10`.

Ground (2026-10-08, `Reground <region> -190 -18 -124 38 32 coarse_dirt`, applied as BuildKit build `pumpground`):
the coarse dirt site floor (2,314 cells) was re-covered to match the natural surface around it: gravel beach on the
lake side, grass / moss / lime terracotta / green concrete powder patches with grass, ferns and tulips inland. Roads,
yard, tank and building floors were left as they were. `/buildkit undo pumpground` would bring the coarse dirt back.

Trail from 0,0 (2026-10-10, `Pathway <region> 8 -7 0 -35 -3 -65 -6 -95 -1 -115 2 -126 2`, BuildKit
build `pathway`, 985 swaps): a dirt trail in the style of the existing north-south trail at x ~ -7 by
0,0, branching off it and running west ~120 blocks (south of the wheat plots at -62..-58 z 2..8, through the wood
around x -110..-96) to the station's north-east approach road at x -124, z 0..3. About 16 wide, dense clumped middle,
soft edges. No stumps: the user found the generated stump line ugly (2026-10-10), don't add `--stumps` again.
A first, sparser version (half width 7, per-cell speckle, no stumps) was undone: it looked like a thin dotted line.

Known leftovers: the site clearing cut the trees standing on the edges of its box (x -206..-122, z -20..40, y 33+):
canopies sliced flat on the side facing the site, mostly along the north (z -21), south (z 41) and east (x -121)
edges. Not fixed yet; options: trim them into whole smaller trees, or remove those trees.

### Vehicle road from base B (2026-10-10)

`Pathway <region> 9 -412 450 -412 425 -409 395 -398 365 -380 340 -358 322 -335 313 -315 311 --density=0.75
--clear-trees`, BuildKit build `vehicleroad`, applied with `apply vehicleroad clear` (user asked to remove trees in
the way). ~18 wide dirt road in the style of the east-west road at z ~305..317 (coarse dirt / podzol / rooted dirt,
moss): from the middle of base B's vehicle pad north edge (-412 452) north, curving right (east) into that road around
x -335..-315. Six whole trees in the corridor were removed (634 wood, ~1,960 of their own leaves; root wood on the
ground layer covered with moss); trees beside the road kept their full crowns. `/buildkit undo vehicleroad` restores
trees and ground. Then `LevelMounds <region> 32 9 <same waypoints>` (build `levelmounds`, applied with clear): the six
small raised mounds (cobblestone/granite/wheat bumps, 1-3 high, ~7x8) touching the road were levelled whole to the
ground layer (254 columns), road texture inside the corridor, grass outside. Mounds beside the road are untouched.
Undo order if ever needed: `undo levelmounds`, then `undo vehicleroad`.

### Removed: ender dragon test statue (2026-10-08)

`Dragon -305 28 25`, applied with `/buildkit apply dragon` and removed with `/buildkit undo dragon` to test BuildKit.
9,339 blocks: end-stone island at the lake centre (-305 25, from y 20 under water to ~32), dragon facing east, wings up
to y 61. Nothing of it is left (verified with `check` and a camera shot). The build file is still in
`data/unrealzaruba/builds/dragon.build` if the user wants it back.

### Map-wide fixes (not builds)

- design_decor -> dndecor and createdeco block migration, Copycats+ block entity fix (`Migrate` with
  `examples/decor-migration-mapping.txt` and `copycatfix`), whole template, offline.
- Old 1.18/1.20 chunks still hold unknown blocks of removed mods underground (scguns ores, vs_clockwork ore): harmless,
  they turn to air when those chunks are saved.

## Session 2026-10-24 (dev server `testing-zaruba`, data `X:\Projects\UnrealFrontiers\unrealfrontiers-testing\data\zaruba`)

All applied with BuildKit in DEV mode on the open capturepoints template; each is undoable by its build name.
Generators in `tools/worldkit/generators/`, blueprints in `tools/worldkit/blueprints/`.

### Vehicle spawn plots, standard 30x30x30 (generator `SpawnPlots`)

One plot: 30x30 pad (TFMG `yellow_caution_block` border, polished andesite / andesite / gravel or cobblestone inside),
the vehicle purchase block 3 blocks in front of the pad facing away from it (vehicles spawn on the pad: the spawner's gap
of 2 is the 2 blocks between), lamp posts of `tfmg:steel_frame` + lantern beside it, a paved apron in front of the row.
Scaffold gantries on both sides of each pad (shared between neighbours): `tfmg:steel_frame` columns at z 0/9/19/29,
createdeco industrial iron catwalk decks at ground+4/9/14/19, railings on the outer side and the ends, an industrial iron
ladder at the front of each gantry. Style of the small plots at the RED base (blueprint `bases/red_vehicle_plots_small`).
- RED (`spawnplots_red`, applied): `SpawnPlots east -862 32 374 3 red`: pads x -862..-833, z 374..403 / 409..438 /
  444..473, terminals at x -865 z 388 / 423 / 458 facing west, right outside the base's east gate (gate z 414..434: the
  middle terminal is straight ahead). Old track sleepers at x -869..-859 z 421..428 left in place (43 blocks skipped).
- BLUE (`spawnplots_blue`, NOT applied): `SpawnPlots south 893 32 -333 3 blue --cut`: pads z -333..-304, x 893..922 /
  928..957 / 963..992, terminals z -336. Needs `apply spawnplots_blue clear`: 1,429 terrain columns of the hill south
  of the base cut down to y 32 (incl. two decorative trees at x 904..912) — waiting for the user's OK. Plus build
  `bluegate` (also needs clear): an 11-wide opening x 937..947 in the base's south wall (z -355..-347, wall, curb and the
  barrier blocks on top), paved, with tuff pillars and lanterns.
- Blueprint: `bases/spawn_plots_red`.

### Fishing hut west of the lake (generator `FishingHut -430 32 22 28 <region>`, build `fishinghut`)

Izba-style hut (spruce log walls, dark oak corners, spruce stair roof, chimney with a campfire), door x -430 z 22, net
rack and barrels, a 9-wide slipway road z 26..34 from the yard down the gravel shore into the water (cobblestone/
andesite/gravel, stripped spruce curbs, lantern posts at x -414), a plank pier on log posts z 15..19 out to x -398.
Blueprint `lake/fishing_hut`.

### Shore path from Дорожный аванпост to Насосная станция (build `shorepath`)

`Pathway <region> 4 -462 328 -445 295 -418 262 -385 238 -340 224 -295 222 -255 208 -222 182 -196 150 -176 115 -170 80
-165 52 -152 40 --density=0.85`: ~9 wide dirt path just outside the gravel beach, around the south and east shore to the
station's south road. A first version on the gravel itself was invisible and was undone.

### Border hills and forests (generator `BorderHills`)

- Left and right edges (`LR 110 26 10`, excluding the RED base + plots, the BLUE base + plots, and 60 around the helipad
  capture point): rolling hills up to 26 high at the edge, fading to nothing over 110 blocks with a wandering inner edge,
  12 blocks clear of every built block / road / water / tree trunk; forest of ~830 library trees + bushes with clearings.
  Applied in parts `bh_L1..bh_L4`, `bh_R5..bh_R8` (BuildKit limit is 500,000 blocks per build).
- Top and bottom edges (`TB 80 20 0`, no trees; corners and the snowy mountain excluded): parts `bhtb_1..bhtb_8`.

### Garbage fields in the city (generator `GarbageFill`, build `garbagefill`)

The piles' bottom layer had been a removed mod's block (turned to air), so the junk floated. Fields x 594..711 z -141..8
and x 582..650 z -250..-167: air under every pile filled down to the ground, plus a patchy 1-2 high junk layer on the open
field ground. Palette: gravel, cobbled deepslate, tuff, soul soil, coarse dirt, create andesite casing.

### Rural infill (generator `RuralInfill`, build `ruralinfill`)

12 copies of the village izbas (blueprints `rural/izba_*`) on empty land beside the basalt roads of the rural area
(x 230..760, z 100..470): door turned to the road, cobblestone foundation, oak fence around the yard following the
ground, dirt path to the road, vegetable rows and a library tree at the back. Positions are printed by the generator.

### Blueprint library (`tools/worldkit/blueprints/`)

Format and tools: `Blueprint.java` (text format, rotation of block states), `Export` (box -> blueprint, `--built-only`,
ground `auto`), `generators/Stamp` (blueprints -> build, with foundation), `Trees` (finds trees and saves one per kind),
`Lots` (houses and their fenced lots), `Find`, `MapImage` (top-down picture of the map from the region files).
- `bases/`: red_vehicle_plots_small, spawn_plots_red. `lake/`: fishing_hut.
- `rural/`: izba_birch, izba_spruce, izba_spruce_darkoak, izba_oak_log, izba_oak_stripped, house_darkoak_stone, house_brick.
- `trees/`: 30 kinds picked from the ~1,400 trees on the map (the map's own oak trunk + dark oak leaves is by far the most
  common), from bushes to 24-high.
