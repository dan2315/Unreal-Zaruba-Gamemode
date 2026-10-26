package com.dod.unrealzaruba.Radiopings;

import java.util.UUID;

import net.minecraft.world.phys.Vec3;

/**
 * Singleplayer testing bypass for radio and pings, toggled by {@code /pingdebug}. There is no team,
 * no match and no game dimension in singleplayer, so with the flag on the server checks for those are
 * skipped and everything is delivered back to the sender. Real vehicle attach still needs a Sable hull;
 * {@link #DEMO_VEHICLE_ID} plus {@code /pingdebug demo} fakes one: a ping riding a point that orbits
 * the placement spot, exercising the whole follow path (server tick, update packets, renderer, compass).
 */
public final class RadioDebug {
    /** Fixed id of the fake demo hull: never produced by the attach resolver, only by the demo command. */
    public static final UUID DEMO_VEHICLE_ID = UUID.fromString("0d3b0f1d-5e5a-4f1e-9d0e-deb000000001");

    public static volatile boolean DEBUG_FORCE = false;
    static volatile Vec3 demoCenter;
    static volatile long demoStartMs;

    private RadioDebug() {}

    public static boolean toggleDebug() {
        DEBUG_FORCE = !DEBUG_FORCE;
        if (!DEBUG_FORCE) {
            demoCenter = null;
            PingService.clear();
            RadioService.clear();
        }
        return DEBUG_FORCE;
    }

    /** Where the demo hull is at this moment: a slow circle around the placement spot. Pure, for tests. */
    static Vec3 demoAnchor(Vec3 center, long nowMs, long startMs) {
        double angle = (nowMs - startMs) / 1000.0 * 0.6;
        return new Vec3(center.x + Math.cos(angle) * 8.0, center.y, center.z + Math.sin(angle) * 8.0);
    }
}
