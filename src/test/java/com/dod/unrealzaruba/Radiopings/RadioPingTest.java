package com.dod.unrealzaruba.Radiopings;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import static org.junit.jupiter.api.Assertions.*;

class RadioPingTest {
    @Test
    void pingTypeIds() {
        assertEquals(4, PingType.values().length);
        assertEquals(PingType.ENEMY, PingType.byId(0));
        assertEquals(PingType.DANGER, PingType.byId(3));
        assertNull(PingType.byId(-1));
        assertNull(PingType.byId(4));
    }

    @Test
    void radioPhrases() {
        assertEquals(8, RadioPhrase.values().length);
        assertEquals(RadioPhrase.ENEMY_SPOTTED, RadioPhrase.byId(0));
        assertEquals(RadioPhrase.DANGER, RadioPhrase.byId(7));
        assertNull(RadioPhrase.byId(8));
        assertNull(RadioPhrase.byId(-1));
        assertEquals(PingType.ENEMY, RadioPhrase.byId(0).autoPing());
        assertEquals(PingType.RALLY, RadioPhrase.byId(1).autoPing());
        assertEquals(PingType.DANGER, RadioPhrase.byId(7).autoPing());
        assertNull(RadioPhrase.byId(2).autoPing());
    }

    @Test
    void glyphMappingsAreComplete() {
        for (RadioPhrase phrase : RadioPhrase.values()) {
            assertNotNull(phrase.glyph, phrase.name());
        }
        for (PingType type : PingType.values()) {
            assertNotNull(type.glyph());
        }
        // auto-ping phrases share the glyph of their ping kind
        assertEquals(PingGlyph.EXCLAMATION, RadioPhrase.ENEMY_SPOTTED.glyph);
        assertEquals(PingGlyph.DOWN_ARROW, RadioPhrase.RALLY_HERE.glyph);
        assertEquals(PingGlyph.WARN_TRIANGLE, RadioPhrase.DANGER.glyph);
        assertEquals(PingGlyph.EXCLAMATION, PingType.ENEMY.glyph());
        assertEquals(PingGlyph.DOWN_ARROW, PingType.RALLY.glyph());
        assertEquals(PingGlyph.WARN_TRIANGLE, PingType.DANGER.glyph());
    }

    @Test
    void cooldowns() {
        Map<UUID, Long> last = new HashMap<>();
        UUID id = UUID.randomUUID();
        assertTrue(RadioService.cooldownOk(last, id, 10_000, 3000));
        last.put(id, 9_000L);
        assertFalse(RadioService.cooldownOk(last, id, 10_000, 3000));
        assertTrue(RadioService.cooldownOk(last, id, 12_000, 3000));
    }

    @Test
    void pingVisibleToNearbyTeammatesAndAuthor() {
        UUID author = UUID.randomUUID();
        UUID mate = UUID.randomUUID();
        Vec3 mark = new Vec3(0, 64, 0);
        assertTrue(PingService.shouldReceive(author, new Vec3(500, 64, 500), author, mark));
        assertTrue(PingService.shouldReceive(mate, new Vec3(99, 64, 0), author, mark));
        assertFalse(PingService.shouldReceive(mate, new Vec3(101, 64, 0), author, mark));
    }

    @Test
    void clientCapsPingsPerSenderAndPurges() {
        PingClientState.reset();
        UUID author = UUID.randomUUID();
        Vec3 anchor = new Vec3(0.5, 64.1, 0.5);
        long now = System.currentTimeMillis();
        PingClientState.add(author, "a", PingType.ENEMY, anchor, 66.5, now + 8000, null, -1);
        PingClientState.add(author, "a", PingType.RALLY, anchor, 66.5, now + 8000, null, -1);
        PingClientState.add(author, "a", PingType.DEFEND, anchor, 66.5, now + 8000, null, -1);
        assertEquals(2, PingClientState.entries().stream().filter(e -> e.sender().equals(author)).count());
        // The oldest is evicted, the two newest stay
        assertTrue(PingClientState.entries().stream().noneMatch(e -> e.type() == PingType.ENEMY));

        UUID other = UUID.randomUUID();
        PingClientState.add(other, "b", PingType.ENEMY, anchor, 66.5, now - 1, null, -1);
        assertTrue(PingClientState.entries().stream().noneMatch(e -> e.sender().equals(other)));

        PingClientState.cycleSelected();
        assertEquals(PingType.RALLY, PingClientState.selected());
        PingClientState.reset();
        assertTrue(PingClientState.entries().isEmpty());
        assertEquals(PingType.ENEMY, PingClientState.selected());
    }

    @Test
    void vehicleUpdatesMoveThePingButKeepItsExpiry() {
        PingClientState.reset();
        UUID author = UUID.randomUUID();
        UUID hull = UUID.randomUUID();
        long expireAt = System.currentTimeMillis() + 8000;
        PingClientState.add(author, "a", PingType.ENEMY, new Vec3(0.5, 64.1, 0.5), 66.5, expireAt, hull, -1);
        PingClientState.updateVehicle(hull, new Vec3(10.5, 64.1, 0.5), 68.0);
        var entries = PingClientState.entries();
        assertEquals(1, entries.size());
        assertEquals(10.5, entries.get(0).anchor().x, 1e-9);
        assertEquals(64.1, entries.get(0).anchor().y, 1e-9);
        assertEquals(0.5, entries.get(0).anchor().z, 1e-9);
        assertEquals(68.0, entries.get(0).diamondY());
        assertEquals(expireAt, entries.get(0).expireAt());
        // Unknown hulls are ignored
        PingClientState.updateVehicle(UUID.randomUUID(), new Vec3(99.5, 64.1, 0.5), 68.0);
        assertEquals(10.5, PingClientState.entries().get(0).anchor().x, 1e-9);
        PingClientState.reset();
    }

    private static PingService.ShipBox box(UUID id, double minX, double minY, double minZ,
            double maxX, double maxY, double maxZ) {
        return new PingService.ShipBox(id, minX, minY, minZ, maxX, maxY, maxZ);
    }

    @Test
    void rayHitsBox() {
        var box = box(UUID.randomUUID(), 10, 60, 0, 14, 64, 4);
        Double t = PingService.rayBoxT(new Vec3(0, 62, 2), new Vec3(1, 0, 0), box, 160);
        assertNotNull(t);
        assertEquals(10.0, t, 1e-9);
    }

    @Test
    void rayMissesBox() {
        var box = box(UUID.randomUUID(), 10, 60, 0, 14, 64, 4);
        assertNull(PingService.rayBoxT(new Vec3(0, 62, 20), new Vec3(1, 0, 0), box, 160));
        // Parallel and outside on Y
        assertNull(PingService.rayBoxT(new Vec3(12, 70, 2), new Vec3(1, 0, 0), box, 160));
        // Beyond max distance
        assertNull(PingService.rayBoxT(new Vec3(0, 62, 2), new Vec3(1, 0, 0), box, 5));
        // Pointing away
        assertNull(PingService.rayBoxT(new Vec3(0, 62, 2), new Vec3(-1, 0, 0), box, 160));
    }

    @Test
    void rayStartingInsideCountsAsHit() {
        var box = box(UUID.randomUUID(), 10, 60, 0, 14, 64, 4);
        assertEquals(0.0, PingService.rayBoxT(new Vec3(12, 62, 2), new Vec3(1, 0, 0), box, 160));
    }

    @Test
    void aimedBlockInsideHullWinsOverNearerRay() {
        UUID near = UUID.randomUUID();
        UUID aimed = UUID.randomUUID();
        var boxes = List.of(
                box(near, 10, 60, -2, 14, 64, 2),
                box(aimed, 30, 60, 8, 34, 64, 12));
        // The aimed block sits inside the far hull: it wins even though the near hull is on the ray first.
        Vec3 eye = new Vec3(0, 62, 0);
        Vec3 dir = new Vec3(1, 0, 0.2).normalize();
        Vec3 hitCenter = new Vec3(32.5, 62.5, 10.5);
        PingService.HullHit hit = PingService.resolveHull(boxes, hitCenter, eye, dir, Double.POSITIVE_INFINITY);
        assertNotNull(hit);
        assertEquals(aimed, hit.box().id());
        assertEquals(eye.distanceTo(hitCenter), hit.t(), 1e-9);
    }

    @Test
    void drivingNeverSelfAttaches() {
        UUID own = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        var boxes = List.of(
                box(own, -5, 60, -5, 5, 64, 5),
                box(other, 50, 60, -2, 54, 64, 2));
        // The eye is inside the own hull: the own box is skipped even though the ray starts in it.
        Vec3 eye = new Vec3(0, 62, 0);
        Vec3 dir = new Vec3(1, 0, 0);
        Vec3 hitCenter = new Vec3(0.5, 62.5, 10.5); // sky past the hull, not inside anything
        PingService.HullHit hit = PingService.resolveHull(boxes, hitCenter, eye, dir, Double.POSITIVE_INFINITY);
        assertNotNull(hit);
        assertEquals(other, hit.box().id());
        assertEquals(50.0, hit.t(), 1e-9);
    }

    @Test
    void wallBetweenCrosshairAndHullBlocksAttach() {
        UUID hull = UUID.randomUUID();
        var boxes = List.of(box(hull, 50, 60, -2, 54, 64, 2));
        Vec3 eye = new Vec3(0, 62, 0);
        Vec3 dir = new Vec3(1, 0, 0);
        Vec3 hitCenter = new Vec3(20.5, 62.5, 0.5); // the wall the client clip stopped at
        // Terrain 20 blocks out, hull at 50: occluded, no attach.
        assertNull(PingService.resolveHull(boxes, hitCenter, eye, dir, 20.0));
        // Same geometry, clear line of fire: attaches.
        PingService.HullHit hit = PingService.resolveHull(boxes, hitCenter, eye, dir, Double.POSITIVE_INFINITY);
        assertNotNull(hit);
        assertEquals(hull, hit.box().id());
    }

    @Test
    void screenCenterProjectsToScreenCenter() {
        org.joml.Matrix4f view = new org.joml.Matrix4f();
        org.joml.Matrix4f proj = new org.joml.Matrix4f().perspective((float) Math.toRadians(90), 16f / 9f, 0.1f, 1000f);
        var p = com.dod.unrealzaruba.UI.Compass.PingScreenOverlay.project(new Vec3(0, 0, -10), view, proj, 800, 450);
        assertEquals(400f, p.x(), 0.5f);
        assertEquals(225f, p.y(), 0.5f);
        assertFalse(p.behind());
        assertTrue(p.onScreen());
    }

    @Test
    void behindCameraIsOffScreen() {
        org.joml.Matrix4f view = new org.joml.Matrix4f();
        org.joml.Matrix4f proj = new org.joml.Matrix4f().perspective((float) Math.toRadians(90), 16f / 9f, 0.1f, 1000f);
        var p = com.dod.unrealzaruba.UI.Compass.PingScreenOverlay.project(new Vec3(0, 0, 10), view, proj, 800, 450);
        assertTrue(p.behind());
        assertFalse(p.onScreen());
    }

    @Test
    void farRightIsOffScreen() {
        org.joml.Matrix4f view = new org.joml.Matrix4f();
        org.joml.Matrix4f proj = new org.joml.Matrix4f().perspective((float) Math.toRadians(90), 16f / 9f, 0.1f, 1000f);
        var p = com.dod.unrealzaruba.UI.Compass.PingScreenOverlay.project(new Vec3(100, 0, -10), view, proj, 800, 450);
        assertFalse(p.behind());
        assertFalse(p.onScreen());
        assertTrue(p.x() > 800);
    }

    @Test
    void edgeClampKeepsSpotOnTheFrame() {
        float[] right = com.dod.unrealzaruba.UI.Compass.PingScreenOverlay.clampToEdge(2650f, 225f, 800, 450, 16);
        assertEquals(784f, right[0], 0.5f);
        assertEquals(225f, right[1], 0.5f);
        assertTrue(right[2] > 0); // outward: to the right

        float[] corner = com.dod.unrealzaruba.UI.Compass.PingScreenOverlay.clampToEdge(2000f, 2000f, 800, 450, 16);
        assertEquals(588.4f, corner[0], 1f);
        assertEquals(450 - 16, corner[1], 1f);
    }

    @Test
    void noHullsNoAttach() {
        assertNull(PingService.resolveHull(List.of(), new Vec3(0, 64, 0),
                new Vec3(0, 64, 0), new Vec3(1, 0, 0), Double.POSITIVE_INFINITY));
    }

    @Test
    void debugToggleResetsItself() {
        boolean was = RadioDebug.DEBUG_FORCE;
        try {
            RadioDebug.DEBUG_FORCE = false;
            assertTrue(RadioDebug.toggleDebug());
            assertFalse(RadioDebug.toggleDebug());
            assertNull(RadioDebug.demoCenter);
        } finally {
            if (was) RadioDebug.toggleDebug();
            else RadioDebug.DEBUG_FORCE = false;
            RadioDebug.demoCenter = null;
        }
    }

    @Test
    void entityAnchorNeedsALevel() {
        PingClientState.reset();
        assertNull(PingClientState.entityAnchor(null, 5));
        assertNull(PingClientState.entityAnchor(null, -1));
        Vec3 anchor = new Vec3(1, 64, 1);
        PingClientState.add(UUID.randomUUID(), "a", PingType.ENEMY, anchor, 66.5,
                System.currentTimeMillis() + 8000, null, 7);
        PingClientState.Entry entry = PingClientState.entries().get(0);
        // No level to resolve from: falls back to the static spot.
        Vec3 shown = PingClientState.liveAnchor(null, entry);
        assertEquals(anchor.x, shown.x, 1e-9);
        assertEquals(anchor.y, shown.y, 1e-9);
        assertEquals(anchor.z, shown.z, 1e-9);
        PingClientState.reset();
    }

    @Test
    void demoOrbitIsACircleAroundTheSpot() {
        Vec3 center = new Vec3(10.5, 64.0, -3.5);
        Vec3 at0 = RadioDebug.demoAnchor(center, 100_000L, 100_000L);
        assertEquals(center.x + 8.0, at0.x, 1e-9);
        assertEquals(center.z, at0.z, 1e-9);
        assertEquals(center.y, at0.y, 1e-9);
        // A quarter period later (~2.6s at 0.6 rad/s) it is 8 blocks to the side, same height.
        Vec3 quarter = RadioDebug.demoAnchor(center, 100_000L + 2618L, 100_000L);
        assertEquals(8.0, Math.sqrt(Math.pow(quarter.x - center.x, 2) + Math.pow(quarter.z - center.z, 2)), 0.1);
        assertEquals(center.y, quarter.y, 1e-9);
    }

    @Test
    void shownTClamps() {
        assertEquals(0.0, PingClientState.shownT(1000L, 200L, 900L), 1e-9);
        assertEquals(0.0, PingClientState.shownT(1000L, 200L, 1000L), 1e-9);
        assertEquals(0.5, PingClientState.shownT(1000L, 200L, 1100L), 1e-9);
        assertEquals(1.0, PingClientState.shownT(1000L, 200L, 1200L), 1e-9);
        assertEquals(1.0, PingClientState.shownT(1000L, 0L, 1000L), 1e-9);
    }

    @Test
    void lerpVecBlends() {
        Vec3 mid = PingClientState.lerpVec(new Vec3(0, 0, 0), new Vec3(10, 20, 30), 0.5);
        assertEquals(5.0, mid.x, 1e-9);
        assertEquals(10.0, mid.y, 1e-9);
        assertEquals(15.0, mid.z, 1e-9);
    }

    @Test
    void vehiclePingGlidesInsteadOfJumping() {
        PingClientState.reset();
        UUID author = UUID.randomUUID();
        UUID hull = UUID.randomUUID();
        long now = System.currentTimeMillis();
        Vec3 start = new Vec3(0.5, 64.1, 0.5);
        PingClientState.add(author, "a", PingType.ENEMY, start, 66.5, now + 8000, hull, -1);
        PingClientState.Entry entry = PingClientState.entries().get(0);
        // No update yet: shown is the placed spot.
        assertEquals(0.5, PingClientState.shownAnchor(entry, now).x, 1e-9);
        // First update starts a glide from the placed spot.
        PingClientState.updateVehicle(hull, new Vec3(10.5, 64.1, 0.5), 68.0);
        entry = PingClientState.entries().get(0);
        assertEquals(0.5, PingClientState.shownAnchor(entry, now + 1).x, 2.0); // barely moved right away
        assertEquals(10.5, PingClientState.shownAnchor(entry, now + 10_000).x, 1e-9); // arrived eventually
        assertEquals(68.0, PingClientState.shownDiamondY(entry, now + 10_000), 1e-9);
        PingClientState.reset();
    }

    @Test
    void aimSanitizeKeepsMarksOnTheSegment() {
        Vec3 eye = new Vec3(0, 64, 0);
        Vec3 end = new Vec3(0, 64, 150);
        assertEquals(new BlockPos(10, 64, 10),
                com.dod.unrealzaruba.Radiopings.PingAim.sanitize(eye, new BlockPos(10, 64, 10), end));
        // Garbage far away falls back to the max-reach point
        assertEquals(BlockPos.containing(end),
                com.dod.unrealzaruba.Radiopings.PingAim.sanitize(eye, new BlockPos(20481028, 126, 20481027), end));
    }
}
