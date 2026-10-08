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

Known leftovers: cut tree trunks and dirt mounds south-west of the site around -175 -10 (from the site clearing).

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
