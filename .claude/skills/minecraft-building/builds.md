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
`respawnD_dress` (user: "make it nicer"; it is BLUE's point, so BLUE base style), generator `RespawnDress <region>
bases 33 701 -228 713 -216 702 -222 --west -228 -225 --north 706 710 --south 706 710 --east -225 -219` (clear): the old
fence pole with a white banner replaced by the BLUE base's flag (blueprint `bases/blue_flag`: stone brick pedestal,
30-high spruce mast, blue wool/lapis flag), four BLUE street lamps (`bases/blue_street_lamp`, limestone poles with
quartz double heads) at x 704/712 z -230/-214, a low cut limestone wall with tuff pillar posts round the plaza with
openings on all sides (east one in front of the purchase block, toward the pad), Create brown seat benches in the east
corners. Respawn spot (707 34 -222) and the purchase block untouched. Also saved: `bases/army_tent_red_base` (RED's
white tent, for RED-side points). The user meant only the plaza itself ("зона спавна"); the lamps were approved
("такими фонари должны быть"), the rest kept.
`respawnD_plaza` (`RespawnPlaza <region> 707 33 -222 6`): the plaza floor inside the blackstone ring re-laid as swaps:
polished cut limestone diamond round the lodestone, a blue concrete ring (d 3, the BLUE flag's colour; blue terracotta
was tried and read purple), smooth stone / polished andesite checker outside, cut limestone brick paths on the axes;
moss + flowering azalea planters in the inner corners; the hay bale became an ammo container.

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
- BLUE (`spawnplots_blue`, applied with clear on the user's OK): `SpawnPlots south 893 32 -333 3 blue --cut`: pads z -333..-304,
  x 893..922 / 928..957 / 963..992, terminals z -336 facing north. The hill south of the base was cut down to y 32 under
  the plots (two decorative trees at x 904..912 went with it; tree remains over the pads removed by build `padtrees`).
  `bluegate`: 11-wide opening x 937..947 in the base's south wall (wall, curb, barrier blocks), paved, tuff pillars and
  lanterns. `blueramp` (generator `Ramp 889 996 -303 1 32 3 60`, clear): a 108-wide ramp from the pads' south edge up the
  hill, 1 block per 3, so vehicles can drive out forward.
  `bluetowerout` (clear, user's request): the deepslate lift tower x 937..944 z -369..-362 (up to y 68 with its antenna)
  that stood between the base's main avenue and the south gate was removed, so the avenue runs straight to the gate and
  the plots; its plating/iron foundation at y 34 swapped to the yard's slab mix. Blueprint `bases/blue_lift_tower`.
- Old small plots removed in both bases (`oldplots_remove`, clear): RED x -969..-956 z 415..447, BLUE x 909..925
  z -470..-438: towers, ladders, catwalks, purchase blocks; caution borders swapped back to the yard floor. Blueprints
  `bases/red_vehicle_plots_small`, `bases/blue_vehicle_plots_small`.
- Blueprint: `bases/spawn_plots_red`.
- Склад боеприпасов (respawn D), user: "the vehicle spawn zone needs redoing" (the old one was only a yellow outline
  painted across the N-S road and the grass east of it, x 715..744 z -234..-210, terminal on the plaza's east edge).
  `padD_plot`: `SpawnPlots east 750 33 -232 1 blue --region <r> --cut`: one standard plot east of the road on the
  meadow, pad x 750..779 z -232..-203, gantries z -236..-234 and -202..-200, apron x 743..749 on the worn road edge
  (apron cells on road blocks skipped: they blend in), terminal 747 34 -218 facing west. `padD_tidy` (clear, one-off
  scratchpad generator): the yellow outline swapped back to its neighbours' blocks, a zebra crossing (white concrete
  bars on even x 720..740, z -221..-217) from the plaza's east gate to the terminal, the old purchase block at
  713 34 -222 and its "Спавн техники" sign removed (lantern on the freed post), plants and bushes cleared off the plot.
  `groveD_clear` (user: "remove this grove"), `ClearTrees <region> 780 -250 817 -206`: the grove east of the plot
  (14 trees of the f5_east forest edge, x 782..815 z -245..-210) removed whole, podzol/moss under it back to grass;
  `groveD_leaves`: 9 loose leaves left in the air (ClearTrees now clears those itself). The tree at 820 -200 stays.

### Fishing hut west of the lake (generator `FishingHut -430 32 22 28 <region>`, build `fishinghut`)

Izba-style hut (spruce log walls, dark oak corners, spruce stair roof, chimney with a campfire), door x -430 z 22, net
rack and barrels, a 9-wide slipway road z 26..34 from the yard down the gravel shore into the water (cobblestone/
andesite/gravel, stripped spruce curbs, lantern posts at x -414), a plank pier on log posts z 15..19 out to x -398.
Blueprint `lake/fishing_hut`.

### Road from the BLUE spawn plots to the main road (builds `blueroad`, `blueroadstep`, `blueramp2`, `bluebank`, `bluetidy`)

`LaneRoad <region> 845 996 -303 -279 32 869` (applied with clear): a 25-wide road along the south edge of the pads, in
the style of the main road west of the base (worn brown edges, yellow terracotta edge lines, grey lanes of stone /
andesite / cut andesite with limestone, dead fire coral and cut dripstone patches) and a white dashed centre line of bone
blocks (3 on, 4 off, a few worn away; the user's "white blocks are the centre dashes"). Level with the pads (y 32) so
vehicles roll off the pads straight onto it; west of x 869 it is at the main road's y 33, with a 2-wide band of smooth
stone slabs at x 869..870 (`blueroadstep` adds 4 slabs at z -278). Old main-road blocks inside it were kept.
The hill ramp is behind the road: `Ramp <region> 889 996 -278 1 32 3 80 0 24` (`blueramp2`, clear), 1 block per 3
rows, with a bank on the east side rising 1 per block. `BankFill <region> 866 -345 888 -304 32` (`bluebank`): the
ditch between pad 1 and the main road filled level with grass.

Leftovers levelled with `Tidy <region> 840 -334 1023 -196 1 20 4  845 -303 868 -279 33  869 -303 996 -279 32  889 -278
996 -199 auto` (`bluetidy`, clear; plus `bluetidypits`, 2 cells): the 7-28 high cut face where the road ends against
the east border hill is now a 1:1 bank up to the map edge (x 997..1023, ~11,600 blocks cut), the 1-deep ditch along the
road's south edge at x 846..868 filled to y 33, ragged one-block steps at the ramp's edges smoothed, 20 broken trees
(fence/copycat trunks left on pillars, half-cut crowns) and 46 floating leaf clumps removed. Kept: the twisted oak at
955 -269 (intact library tree), the library trees on the ramp. The jagged stone face visible at x 1023 is the map edge.

### Shore road from Дорожный аванпост to Насосная станция (builds `shoreroad`, `shoremounds`)

Vehicle road, about 13 wide (user asked for 12+ so vehicles can pass): `Pathway <region> 6 ... --density=0.75
--clear-trees` (applied with clear: trees standing in the corridor removed whole), then `LevelMounds <region> 32 6 <same
waypoints>` (small rock/flower mounds in the corridor levelled). Waypoints: `-462 328 -445 295 -418 262 -385 238 -340 224 -295 222 -255 208 -222 182 -196 150 -176 115 -170 80
-165 52 -152 40`. It runs just outside the gravel beach, around the south and east shore to the station's south road.
Earlier versions (a 9-wide footpath `shorepath`, and one on the gravel itself) were undone.

### Spruce forest west of the lake (generator `Forest`, builds `westforest`, `westtrack1..3`, `westleaves`, `westleaves2`)

User's request: take the spruces of the planted grove by the BLUE ammo depot (x 733..752, z -56..4: dark oak trunks,
spruce needles flecked with birch leaves, fence branches, floating cross tips) and make a pine forest left of the lake,
with some relief: small swamps, hillocks, alternative vehicle routes to key places. The grove is untouched; its 14
kinds are the blueprints `trees/spruce_grove/` (Trees scan, up to 3 per size class).

`Forest <region> trees/spruce_grove -775 -150 -445 292` with (also in this order for re-runs):
- tracks (half width 6, ~13 wide, surface by `Pathway ... 6 <same points> --density=0.75 --clear-trees`):
  - T1 west forest road, south road ↔ north road: `-600 308 -606 265 -628 215 -640 160 -622 105 -604 60 -618 5
    -646 -45 -650 -95 -632 -140 -622 -168` (no old trees in the way);
  - T2 to the fishing hut's slipway (boats/amphibious across the lake to the pumping station): `-606 62 -570 48
    -530 40 -492 36 -462 31 -440 30` (12 old deciduous trees removed);
  - T3 along the edge of the old woodland, south road west of the bunker ↔ hut ↔ north road by the bridge: `-490 307
    -500 265 -510 215 -512 165 -503 115 -494 70 -492 36 -505 -15 -520 -70 -505 -120 -480 -160 -462 -184` (20 removed);
- swamps `-690 95 16`, `-565 -60 13`, `-560 205 10` (water flush with the ground, 2 deep in the middle, mud/clay
  bottom, reeds, lily pads, dead dark oak trunks);
- hills `-705 225 30 7`, `-555 130 24 6`, `-705 -40 26 8`, `-560 -125 18 5`, `-745 140 18 5`, `-660 30 14 4`,
  `-680 -120 20 6`, `-560 270 16 4` (mossy boulders on top, meadows on several);
- `--avoid -440 8 -395 40` (hut, slipway, pier).
Result: 1,382 spruces, ~365k blocks; clearings from noise; forest floor of podzol/moss/coarse dirt, ferns, berries,
mushrooms. The ground here was flat at y 32. `westleaves`, `westleaves2`: crown pieces of the old trees cut for T2/T3
left in the air (found with `Tidy` without references, filtered to the tracks).
Gameplay: covered infantry approach from RED's side (outpost, helipad) to the west shore; vehicles have two
north–south routes (T1, T3) besides the roads, and the slipway crossing; hills give firing positions over T1/T3.
Undo order if ever needed: westleaves2, westleaves, westtrack3..1, westforest (then check `green_concrete` below).

### Filling the bare land (2026-10-24, user: "fill the empty boring places with what the map already has")

Backup before it: `X:\Projects\UnrealFrontiers\unrealfrontiers-testing\backups\capturepoints_game_dim_2026-10-24_2153`
(game_dim + gamemodedata). Bare places found with `Empty` (distance from open ground to the nearest tree, build, road
or water; largest bare disks listed). Order: farmsteads and houses, then forests/groves (they keep away from builds),
then the meadow last.
- Windmill: the user's download "molino de viento" by F3deMatt25 (`props/windmill_molino.bp`, imported with
  `ImportNbt`: the Create sails and a rotating harvester were contraption entities, added as static blocks at angle 0;
  1.21.9+ blocks replaced: cinnabar stairs/slabs -> mangrove, shelves -> open trapdoors, iron_chain -> chain, copper
  chest -> chest). Standalone build `millbp` at -620 -330 (NW field).
- Farmsteads (`Farmstead`): `farm_nw` around that mill, yard -612 -306 (`--no-mill`, izba_spruce + izba_oak_log);
  `farm_e` 870 95 (mill, izba_spruce_darkoak + izba_birch); `farm_s` 168 238 (mill, izba_oak_stripped + izba_spruce).
  Each: two izbas facing the yard, well, haystacks, log pile, trodden yard, fenced wheat/carrot field to the south.
  Trails to the roads (`Pathway` half width 2.5): `trail_nw` to the deepslate road at -784 -232, `trail_e` to the
  village basalt road at 768 98, `trail_s` to the dirt road at 150 302.
- Lone houses (`lonehouses`, `RuralInfill` one per box, `--road=coarse_dirt,rooted_dirt,podzol,cobbled_deepslate,
  cyan_terracotta,deepslate,basalt,dirt_path,gravel --designs=izba_,house_ --seed=3i`): izba_oak_log -783 -272,
  izba_spruce_darkoak -679 -148, izba_birch -847 -48, izba_spruce -933 161, house_darkoak_stone 114 317, house_brick
  526 131, izba_birch -344 331, izba_spruce -183 290 (door positions), each with fence, path, garden and a tree.
- Forests (`Forest`, trees `trees,trees/spruce_grove` = mixed: oak, birch, dark oak, spruce; args in this order):
  dense belts on the bare border hills `f1_north -60 -505 740 -395`, `f2_nwtop -900 -505 -450 -432`, `f3_south 140 425
  570 511` (density 0.85, clearings 0.3); groves with low hills and three small swamps `f4_west -930 -120 -790 370`,
  `f5_east 780 -250 990 420`, `f6_midsouth -20 60 270 430`, `f7_mid 270 20 720 140`, `f8_nwfield -880 -430 -460 -150`,
  `f9_southlake -600 250 -150 430` (density 0.3-0.5, clearings 0.45-0.55; --avoid around the helipad, its respawn,
  the farmsteads, the outpost capture area, the power station). ~2,800 trees. Swamps at 830 -60, 0 260, -820 -200.
  `concretefix2`: painted grass hardened by the swamp water, swapped back to powder.
- Meadow (`Meadow`, `meadow_0..7`, strips of 256 along x over the whole map, plus `meadowtest` on the NW field):
  ~430k plants: grass, flower patches, bushes, ferns and berries at forest edges, verges along roads.
- Not placed: the drawn watchtower / chapel ruin / car wreck / bus stop (`Props`): the user finds them weak and offered
  to find better structures (asked for: watchtower, ruined chapel, barn, well, abandoned houses, Soviet bus stop,
  car/truck wreck or tractor, water tower).

### City tower cranes (build `craneA_fix`)

Four identical brass tower cranes in the city: A masts 404/408 z -175/-171 (boom along +x to 442), B 431/435 z -99/-95
(turned 180°), C 511/515 z -243/-239, D 535/539 z -299/-295 (boom along z); two more at 592..600 z -304..-296 and
642..646 z -172..-168 have their booms hanging to the ground (left as they are). The jib is only vertical
`createdeco:brass_support` posts (y 79-81) every 3 blocks with nothing between, and the counterweight (stone, x 393-397)
floats 7 blocks from the mast: from below it reads as floating chunks. Same in all source map versions (v1-v3).
`craneA_fix` (user's request): horizontal chords `brass_support[facing=east]` at y 79 and 81 on both sides (x 397..437
between the posts), floor cross members `facing=south` at y 79 every 3, two posts on the counter-jib.
`craneB_fix`, `craneC_fix`, `craneD_fix`: the same file transformed, checked block by block against the cranes first
(`DiffT`: only the 152 fix blocks differ): C = A shifted (x+107, z-68); B = A mirrored in x (839-x, z+76; east<->west);
D = A turned a quarter clockwise (364-z, x-703; east->south, south->west).

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
That first pass took the road through the big field (deepslate / cyan terracotta / cut tuff with white dashes, worn with
rooted dirt and podzol) for field ground and piled junk on it, and only filled air directly on the ground.
Second pass, user's request ("roads clean, no air under the piles"): `GarbageFix <region> 32 1 592 -146 714 14 581 -250
650 -166 --avoid 650 -144 674 -118` (build `garbagefix`, clear): 1,134 junk blocks cleared off the road columns (road
surface detected in the field, widened by 1), 7,915 air/plant/carpet cells under pile tops filled (mix without gravel).
`garbageground`: 151 hidden voids in the ground layer (y 31 under x 594..602 z -39..-25, y 32 at 623..625 z -64..-58)
filled with dirt / coarse dirt. Left alone: the shack at 654..670 z -140..-122 (doors, levers, smokestack), the field
fences, trees over the west strip.

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

### Lag fix: saved items, plants on painted grass (2026-10-24, after switching dev to GAME)

The user saw lag and red dots on the map. Cause: my vegetation passes (Meadow, Forest floor, BorderHills) put plants on
painted grass (lime terracotta / green concrete powder), where plants cannot stand; placing neighbours broke them, and
the dropped items (19,065: flowers, wheat seeds from grass, berries, mushrooms) were saved into the template's
`entities/` and loaded into every match. The ~45 hostile mobs (zombies, skeletons, creepers) are live spawns
(`doMobSpawning true`, Normal), not saved ones; the dense forests give them dark spots.
- Template `entities/` stripped of `minecraft:item` with `Entities strip` (backup of the files:
  `X:\Projects\UnrealFrontiers\unrealfrontiers-testing\backups\capturepoints_entities_before_strip_2026-10-24_2325`);
  left: 3 armor stands, 1 simulated:honey_glue (as in the 21:53 backup). Live match: items and mobs killed by RCON.
- Generators fixed (`BorderHills.holdsPlants`). Prepared, NOT applied yet (needs DEV mode): `plantfix_0..3`
  (strips of 512 along x; scratchpad PlantFix): 12,997 flowers get a grass block under them, 40,012 grass/fern plants on
  painted cells removed, 604 mushrooms outside podzol removed. Superseded on 2026-10-25 by `plantclean_0..3`, regenerated
  from the map of that day (PlantFix) and applied: 40,535 plants cleared, 12,893 flowers got a grass block; a re-scan finds
  none left. The original map (v3) had ~400 such plants.

### Электростанция turbine hall: lights and machinery (2026-10-25)

The capture platform (767-774 x 193-200, stand 49) sits in the middle of a closed turbine hall (inside 752-788 x
178-214, floor stand 42, catwalks stand 49 along the walls at x 754-755 / 785-786, z 180-181 / 211-212 and a spine at
x 770-771, ceiling 59). It was nearly black: 1013 of 1167 floor spots below light 7, the platform at 0; only the two
doors and 4 dndecor copper lamps (758/783 54 196-197) lit it. User: "make the point nicer, add lighting".
- 58 `createdeco:yellow_industrial_iron_lamp[facing=down,inverted=true,lit=true]` (inverted: lit without redstone;
  players can toggle one with a click), 282 `minecraft:chain` from the ceiling: 6 over the capture area at y53, 16 over
  the catwalks at y53 every 8 blocks, 20 under the catwalks / spine / platform at y47 (floor along the walls), 16
  pendants at y46 in the two open bays (x 760, 765, 776, 781; z 185, 192, 199, 206), clear of a jump from the floor.
- Planned with HallLights' block light simulation (prints the predicted light map per standing level): floor average
  7.6 with no spot at 0 (no mob spawns), catwalks 8.8, platform 9-10+. Additive, no clears; `buildkit undo
  power_station_lights` removes all of it. Screenshots before/after: `ps_*` and `ps_*_lit`.

Second pass (user: "do everything" — machinery, wall pipes, consoles, hazard markings; "you may drill the walls a bit"):
the first lights were undone (`power_station_lights`, its bay pendants would have stood inside the machines) and
re-planned around the machinery as `power_station_lights2` (62 lamps, 282 chain; floor average 7.4, nothing at 0).
- Hall layout found on the way: a lower catwalk ring at y45 (x 762-779, z 191-202) round the platform tower, stairs
  from the ring up to the platform at z 196-197, and weighted ejectors in the floor (x 760 / 781, z 196-197, marked
  with "up" decals) that launch players from the floor onto the ring. Kept clear: ring, ejector lanes, doors (north
  x 770-772, south x 766-768), the wall stairs (west x 754 z 203-208, east x 786 z 185-190).
- `ph_units` (generator `PowerHall <region> units`): four turbine-generator sets, point-symmetric (x mirror 1540-x,
  z mirror 392-z): NW at x 755-761 z 181-190, NE 779-785 / 181-190, SW 755-761 / 202-211, SE 779-785 / 202-211, each
  along z: hazard-striped plinth (railways yellow diagonal on black) with industrial iron inside; turbine = dndecor
  industrial boilers size large (3x3 cylinder, the boiler places its own invisible boiler_structure ring) with a brass
  band and a medium end cap, on industrial iron supports; a steam pipe (industrial boiler axis y, normal) to the
  ceiling; green flywheel on a bearing; green dark-metal generator (5 wide, 4 high) with tfmg glass insulators on top
  and high-voltage decals on its sides; copper exciter at the end toward the middle. They give cover at both doors.
- `ph_walls` (`PowerHall ... walls`, applied with `clear`): a channel cut into the inner plating layer of all four
  walls (x 753, x 787, z 179, z 213; y 44-46; around the doors), holding a copper pipe (copper boiler small) at y46 and
  two tfmg cable tubes at y44-45. 342 plating blocks cleared; the walls stay two blocks thick. `buildkit undo ph_walls`
  puts the plating back.
- `ph_details` (`PowerHall ... consoles,hazard`): control desks in the four platform corners outside the capture area
  (smooth stone + speedometer = 2 high at the corner, stressometers as 1-high arms: cover on the point); warning decals
  at the foot and head of the wall stairs and of the ring-to-platform stairs.
- Screenshots: `ph_*` (shoot from open air: a camera next to a steam pipe sees only the pipe's model).

### Электростанция: west yard restored (build `station_restore`, generator `RestoreLost`, 2026-10-25)

User: outside the station blocks were missing, there should be transformers and fencing. Not lost by our builds: the
yard (x 727-751, z 176-233) was built with mods this pack dropped, and those blocks became air when the map moved to
1.21 (already missing in v3). Compared against `game_dim.v2` and put back as current blocks (115):
16 `s_a_b:lightsteelblock` bases of the brick fence pillars along x 730 (the pillars stood on air) -> industrial iron
block; 13 + 4 `cbcmodernwarfare:apds_shot` (insulators on the outer fence posts at x 727 every 6, and on the gantry) ->
tfmg glass cable insulators; the transformer at 733-738 205-210: 43 `s_a_b:whitehardsteelblock` -> dndecor white dark
metal block, 10 solid fuel thrusters (bushings, coolers) -> small industrial boilers, 3 `create_connected:item_silo` ->
create fluid tank; 26 `s_a_b:sandbag` (emplacement 746-751 221-233, its oak signs were floating) -> packed mud (dndecor
beige blocks are not registered). The 4 old design_decor signs were left out (`--no-signs`; the user removed the hall's
decals themselves after the catwalk ones dropped). Screenshots `yard_before_*`, `yard_after_*`.
Dropped items: 7 killed in game_dim; 3 red tulip items remain in the template's entity files (unloaded chunks): strip
them with `Entities strip` the next time the map is closed.

### Clean-up and transfer to prod (2026-10-25)

`plantclean_0..3` applied (see the lag fix above); dropped items killed in game_dim, then with the server stopped the
template's `entities/` stripped of 12 `minecraft:item` (backup `unrealfrontiers-testing/backups/
capturepoints_entities_before_strip_2026-10-25`); left: 3 armor stands, 1 honey glue. Dev restarted in DEV.
The template (`game_worlds/capturepointsgamemode/dimensions`) and `objectives.json` + `economy.json` copied to the prod
server (teamData, barriervolumes, scoring were already the same); prod's previous template and configs are in
`zaruba-server/backups/capturepoints_before_transfer_2026-10-25`. Copy procedure: dev `save-off` + `save-all flush`,
prod stopped, copy, dev `save-on`, prod started (it stays in GAME mode; the match copies the new template). The
pre-rendered Xaero map was then rescanned on dev (GAME mode, `/setgamemode capturepoints`, `execute as soslo run
mapscan`: 8 stops at view distance 24, the client at render distance 24; 24 regions, 2.6 MB) and copied to prod
(old pack in the same backup folder). Prod: `minPlayersToStart` 1 -> 6 in mainConfig.json (cached: restart).
Physics junk removed from the template (dev and prod, 2026-10-25): Sable had saved two physics bodies into
`sublevels/` during DEV sessions (a supply drop stand at 0 35 0, an unnamed test body with Drive By Wire wiring at
283 40 146), which loaded into every match. `sublevels/` and `data/` were reset to the original v3 (empty sub-level
index, no Sable / Gadgets & Gizmos / Drive By Wire / Simulated data, v3 chunks.dat and raids.dat), and the
`simulated:honey_glue` entity stripped; the 3 armor stands are the map's own (v3). Backups:
`unrealfrontiers-testing/backups/capturepoints_physics_junk_2026-10-25`, `zaruba-server/backups/...` (same name).
Chunk timestamps (InhabitedTime, LastUpdate) stay: they live in every chunk and do nothing for play.

### Pre-rendered world map: named scans (2026-10-26)

`/mapscan <name> [seconds]` (any open map, also DEV after `/openmap`) renders into an empty Xaero map and saves the result as
`unrealzaruba/xaero-maps/<name>/`; it becomes the open map's world map (`xaero-maps/maps.json`, map key ->
scan; a mode's first map without an entry uses the scan named after the mode class, which is how the old `CapturePointsGamemode` pack keeps
working). A mode start loads its scan for everyone; clients switch to it at once, also inside the game dimension.
`/mappack` shows the loaded scan, the mode's scan and all scans; `/mappack load <scan>` loads one for everyone until
the next mode start; `/mappack bind <scan>` makes it the running mode's map. Copy `xaero-maps/` (with `modes.json`)
when moving a map to prod.
