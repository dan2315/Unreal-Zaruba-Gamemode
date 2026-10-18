# Dev server crash: Sable heat-map split on an already removed sub-level

Hand-off note for another agent. Written 2026-10-18 after investigating a crash of the dev server (`testing-zaruba`).
Nothing in this repository has been changed for it yet: this file is the explanation and the proposed fix.

## Summary

The dev server crashed with `RuntimeException: Sub-level assembly attempted inside plot of already removed sub-level`.
It is a bug in Sable (2.0.6) in the code that splits a broken vehicle into pieces, hit right after a very large explosion
destroyed a vehicle. None of our classes (`com.dod.unrealzaruba`) is in the stack. A second crash report written a minute later
(`ServerHangWatchdog`) is only a side effect of the first crash, not a separate problem.

The server restarted by itself (Docker restart policy `unless-stopped`) and is healthy again. The crash will come back whenever a
sub-level is destroyed in the same way.

## Evidence

Files (host paths):

- `X:\Projects\UnrealFrontiers\unrealfrontiers-testing\data\zaruba\crash-reports\crash-2026-10-17_20.11.35-server.txt`  the real crash
- `X:\Projects\UnrealFrontiers\unrealfrontiers-testing\data\zaruba\crash-reports\crash-2026-10-17_20.12.38-server.txt`  watchdog, follows from the first
- container log: `docker logs --since 2026-10-17T20:08:00Z --until 2026-10-17T20:13:30Z testing-zaruba`

Timeline (UTC), from the container log:

1. 20:06-20:10  players `soslo` and `ProGaMEr11052` spawn a structure `Bullet mk3` (Aeronautics, 72 block entities) three times.
2. 20:11:34  explosions: three of power 4, then one of **power 100**:
   `Blast power 100.0: 216 ship blocks queued, 2366 terrain sections x 200 ticks scheduled for collider refresh`
3. 20:11:35  `Exception ticking world` caused by
   `java.lang.RuntimeException: Sub-level assembly attempted inside plot of already removed sub-level`
4. 20:11:35  the server starts to stop ("Saving players", "Saving worlds"); at 20:11:46 the last server-thread lines appear (Create warnings
   `Tried to Access unprepared network space of unrealzaruba:custom_dimension_type`).
5. 20:12:38  `ServerHangWatchdog detected that a single server tick took 60000004.00 seconds`: the JVM did not exit within 60 s after the stop
   began, so the watchdog killed it and wrote a second report (the figure is meaningless, the server thread was already dead).
6. 20:13:26  Docker restarted the container, `Done (1.326s)`.

Stack of the crash (our code is absent):

```
RuntimeException: Sub-level assembly attempted inside plot of already removed sub-level
  at dev.ryanhcode.sable.api.SubLevelAssemblyHelper.assembleBlocks(SubLevelAssemblyHelper.java:85)
  at ...SubLevelAssemblyHelper.wrapMethod$bom000$gadgetsngizmos$assembleAttachments(...)   <- Gadgets and Gizmos mixin wrapper
  at dev.ryanhcode.sable.api.SubLevelAssemblyHelper.assembleBlocks(...)
  at dev.ryanhcode.sable.sublevel.plot.heat.SubLevelHeatMapManager.split(SubLevelHeatMapManager.java:242)
  at dev.ryanhcode.sable.sublevel.plot.heat.SubLevelHeatMapManager.step(SubLevelHeatMapManager.java:118)
  at dev.ryanhcode.sable.sublevel.plot.heat.SubLevelHeatMapManager.tick(SubLevelHeatMapManager.java:85)
  at dev.ryanhcode.sable.sublevel.ServerSubLevel.wrapOperation$bpb000$gadgetsngizmos$delaySplitting(ServerSubLevel.java:1168)  <- G&G mixin
  at dev.ryanhcode.sable.sublevel.ServerSubLevel.tick(ServerSubLevel.java:235)
  at dev.ryanhcode.sable.api.sublevel.SubLevelContainer.tick(SubLevelContainer.java:144)
  at dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer.tick(ServerSubLevelContainer.java:90)
  at net.minecraft.server.level.ServerLevel.tick(...)   (Sable mixin tickPlotContainer)
```

Versions: Sable 2.0.6 (`sable-neoforge-1.21.1-2.0.6.jar`), Gadgets and Gizmos 2.0.0-Beta-4 (jar-in-jar of
`gadgets-and-gizmos-bundled-2.0-prerelease.jar`), Create Aeronautics 1.3.2, NeoForge 21.1.256, Minecraft 1.21.1.

## Root cause (from the bytecode of Sable 2.0.6)

- `SubLevelAssemblyHelper.assembleBlocks(ServerLevel, BlockPos, Iterable<BlockPos>, BoundingBox3ic)` looks up the sub-level that contains the
  first block position (`SableCompanion.getContaining`) and throws the exception above when that sub-level `isRemoved()`.
- `SubLevelHeatMapManager` (one per `ServerSubLevel`) is driven from `ServerSubLevel.tick()` through `tick()` -> `step()` -> `split()`.
  `split()` calls `assembleBlocks` for the new pieces, with positions inside **its own** sub-level's plot.
- When a big blast removes the vehicle's blocks, Sable removes the sub-level (it became empty or was destroyed), but in the same tick (or a
  tick before) its heat-map manager still has a pending split and is ticked. `split()` then assembles inside the plot of a sub-level that is
  already removed, and the exception goes up through `ServerLevel.tick` and stops the whole server.

So the invariant that is missing is: a heat-map manager of a removed sub-level must not split. The check exists in `assembleBlocks`, but it throws
instead of the caller skipping. (This is my reading of the bytecode and the stack, it has not been reproduced in a debugger.)

The Gadgets and Gizmos mixins (`delaySplitting`, `assembleAttachments`) wrap these calls but are not the origin: the exception comes from
Sable's own check.

## Proposed fix: a small defensive mixin in this mod

Skip the heat-map tick of a removed sub-level. It changes nothing for sub-levels that are alive, and becomes irrelevant if Sable fixes it itself.

Target: `dev.ryanhcode.sable.sublevel.plot.heat.SubLevelHeatMapManager`, method `public void tick()` (no arguments, returns void).
The class has a private field `subLevel` of type `dev.ryanhcode.sable.sublevel.ServerSubLevel`. Sable is a normal `implementation`
dependency of this project (`build.gradle`, `sable_version=2.0.6`), so these types can be named in source.

Sketch (file `src/main/java/com/dod/unrealzaruba/mixin/sable/HeatMapRemovedSubLevelMixin.java`):

```java
@Mixin(value = SubLevelHeatMapManager.class, remap = false)
public class HeatMapRemovedSubLevelMixin {
    @Shadow @Final private ServerSubLevel subLevel;

    /** A removed sub-level does not split any more: Sable throws from assembleBlocks when it does, and that stops the server. */
    @Inject(method = "tick()V", at = @At("HEAD"), cancellable = true)
    private void unrealzaruba$noSplitWhenRemoved(CallbackInfo ci) {
        if (subLevel.isRemoved()) ci.cancel();
    }
}
```

Notes on the sketch:

- Check the exact modifiers of `subLevel` with `javap -p` (it is declared `private final`); if `@Final` is wrong for Sable's build, drop it.
- If cancelling `tick()` turns out not to be enough (for example the removal happens between `tick()`'s check and `split()` inside the same
  call), add a second `@Inject` at `HEAD` of `private void split()` with the same check.
- Optional second layer: also guard against the same exception from other callers by wrapping `SubLevelAssemblyHelper.assembleBlocks`
  in a try/catch of exactly this message, log a warning and return `null`. I would not do this first: callers may not expect `null`.

### How to register it (this project's conventions, learned the hard way)

- Put the mixin in its **own optional config**, not in `mixins.unrealzaruba.json` (that one has `"required": true, "defaultRequire": 1`, so a
  mismatch would crash the game at startup). Copy `src/main/resources/mixins.unrealzaruba.simulated.json`
  (`"required": false`, `"injectors": {"defaultRequire": 0}`), name it `mixins.unrealzaruba.sable.json`, package `com.dod.unrealzaruba.mixin.sable`,
  list the class under `"mixins"` (it is needed on the server, so NOT under `"client"`).
- Register the config in `src/main/resources/META-INF/neoforge.mods.toml` next to the others:
  `[[mixins]]  config="mixins.${mod_id}.sable.json"`.
- Mixin handler signatures must match **exactly**. Earlier in this project a `@WrapOperation` handler with `Object` where a mod type was expected
  was rejected, and because the config is optional this only shows up as a WARN line, not a crash. After deploying, grep the log for
  `Mixin apply for mod unrealzaruba failed` (client: `Instances\Unreal Zaruba\logs\*.log.gz`; server: `docker logs testing-zaruba`).
- Source files in the working copy use CRLF; sed/regex edits across lines often fail to match. Edit with the Edit tool or a small Java program
  that normalises `\r\n` first. Bash heredocs containing apostrophes break the shell tool.

## Verification

1. `./gradlew build` (tests included) must pass.
2. Deploy to dev (the dev stack runs in Docker; client modpack publishing is not needed, this mixin is server side):
   stop the container, copy `build/libs/unrealzaruba-0.2.jar` to
   `X:\Projects\UnrealFrontiers\unrealfrontiers-testing\data\zaruba\mods\`, start it again. Never replace the jar under a running server
   (lazy class loading gives `NoClassDefFoundError`).
3. Confirm in the server log that the mixin applied (no `Mixin apply ... failed` line for `HeatMapRemovedSubLevelMixin`).
4. Reproduce: spawn a vehicle with the schematics spawner or `Bullet mk3`, then destroy it with a very large blast (Create Big Cannons /
   explosion of power around 100). Before the fix the server dies within a tick or two; after it the server keeps running. Watch for
   leftover floating blocks of the vehicle (a side effect of skipping the split) and report them.

## Other options (not mine to decide)

- Report upstream: [Sable issues](https://github.com/ryanhcode/sable/issues) with this stack, the versions above and the blast line. Related open
  issue titles found while searching: #92 "Flicker when placing/breaking blocks on sublevel", #128 "Dynamic Light issue" (not the same bug).
- Limit explosion power in Create Big Cannons' config if power 100 is not intended in normal play.

## The second crash report (watchdog) is a side effect

After the first crash the server begins to stop; the JVM stayed alive for more than 60 s (no "Server thread" appears in the thread dump), so
`ServerHangWatchdog` wrote its own report and killed the process. If this needs attention separately, look at what keeps the JVM from exiting
(non-daemon threads, e.g. voice chat / Create network teardown after "Saving worlds"). It disappears when the first crash is prevented.
