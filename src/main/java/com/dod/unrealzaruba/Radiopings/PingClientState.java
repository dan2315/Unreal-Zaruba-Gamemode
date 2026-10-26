package com.dod.unrealzaruba.Radiopings;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * What the client knows about pings: the live ones to draw, the selected kind for the next ping,
 * and the client-side cooldown mirrors (the server still has the last word).
 * The list is replaced wholesale, so the render thread never sees a half-updated one.
 * Anchor is the exact ground point of the mark (rings); diamondY the absolute height of the
 * floating diamond. Riding pings glide between server updates ({@link #shownAnchor}).
 * An entity ridden ping ({@link #liveAnchor}) is resolved from the local level every frame instead.
 */
public final class PingClientState {
    public record Entry(java.util.UUID sender, String senderName, PingType type, Vec3 anchor, double diamondY,
            long expireAt, java.util.UUID vehicleId, int entityId) {}

    static final long RADIO_COOLDOWN_MS = 3000;
    static final long PING_COOLDOWN_MS = 1000;
    /**
     * The server enforces the same cooldowns on its own clock: a machine whose clock runs behind would otherwise
     * see its sends silently dropped right at the boundary, with no feedback. The extra margin covers skew + ping.
     */
    static final long CLIENT_MARGIN_MS = 200;

    private static volatile List<Entry> entries = List.of();
    private static PingType selected = PingType.ENEMY;
    private static long lastRadioMs;
    private static long lastPingMs;
    /** Glide between server hull updates, so ridden pings slide instead of jumping 5 times a second. */
    private static final java.util.Map<java.util.UUID, Glide> glides = new java.util.HashMap<>();
    /** Vehicle ids already reported as followed (debug log only, reset with the entries). */
    private static final java.util.Set<java.util.UUID> loggedFollow = new java.util.HashSet<>();

    private record Glide(Vec3 fromAnchor, double fromDiamond, long changedAt, long glideMs) {}

    private PingClientState() {}

    public static List<Entry> entries() {
        purgeExpired(System.currentTimeMillis());
        return entries;
    }

    public static void add(java.util.UUID sender, String senderName, PingType type, Vec3 anchor, double diamondY,
            long expireAt, java.util.UUID vehicleId, int entityId) {
        if (type == null || anchor == null) return;
        if (RadioDebug.DEBUG_FORCE) {
            com.dod.unrealzaruba.UnrealZaruba.LOGGER.info("[PingDebug] received ping type={} anchor={} vehicle={} entity={}",
                    type, (int) anchor.x + "," + (int) anchor.y + "," + (int) anchor.z, vehicleId, entityId);
        }
        List<Entry> next = new ArrayList<>(entries);
        next.removeIf(entry -> entry.expireAt() <= System.currentTimeMillis());
        long own = next.stream().filter(entry -> entry.sender().equals(sender)).count();
        if (own >= PingService.MAX_PER_PLAYER) {
            for (int i = 0; i < next.size(); i++) {
                if (next.get(i).sender().equals(sender)) {
                    next.remove(i);
                    break;
                }
            }
        }
        next.add(new Entry(sender, senderName, type, anchor, diamondY, expireAt, vehicleId, entityId));
        entries = List.copyOf(next);
    }

    /** A ridden hull moved: glide from the currently shown spot to the fresh one. */
    public static void updateVehicle(java.util.UUID vehicleId, Vec3 anchor, double diamondY) {
        if (vehicleId == null || anchor == null) return;
        List<Entry> current = entries;
        boolean any = false;
        for (Entry entry : current) {
            if (vehicleId.equals(entry.vehicleId())) {
                any = true;
                break;
            }
        }
        if (!any) return;
        if (RadioDebug.DEBUG_FORCE && loggedFollow.add(vehicleId)) {
            com.dod.unrealzaruba.UnrealZaruba.LOGGER.info("[PingDebug] following vehicle {}", vehicleId);
        }
        long now = System.currentTimeMillis();
        Glide prev = glides.get(vehicleId);
        // The spot shown right now: the previous glide's endpoint, i.e. the current entry before we replace it.
        Entry latest = null;
        for (Entry entry : current) {
            if (vehicleId.equals(entry.vehicleId())) latest = entry;
        }
        Vec3 oldAnchor = latest != null ? latest.anchor() : anchor;
        double oldDiamond = latest != null ? latest.diamondY() : diamondY;
        Vec3 fromAnchor;
        double fromDiamond;
        long glideMs;
        if (prev == null) {
            fromAnchor = oldAnchor;
            fromDiamond = oldDiamond;
            glideMs = 250;
        } else {
            double t = shownT(prev.changedAt(), prev.glideMs(), now);
            fromAnchor = lerpVec(prev.fromAnchor(), oldAnchor, t);
            fromDiamond = prev.fromDiamond() + (oldDiamond - prev.fromDiamond()) * t;
            glideMs = Math.max(100L, Math.min(1000L, now - prev.changedAt()));
        }
        glides.put(vehicleId, new Glide(fromAnchor, fromDiamond, now, glideMs));
        List<Entry> next = new ArrayList<>(current.size());
        for (Entry entry : current) {
            next.add(vehicleId.equals(entry.vehicleId())
                    ? new Entry(entry.sender(), entry.senderName(), entry.type(), anchor, diamondY,
                            entry.expireAt(), entry.vehicleId(), entry.entityId())
                    : entry);
        }
        entries = List.copyOf(next);
    }

    /** Where the entry draws right now: the glide position for hulls, the static spot otherwise. */
    public static Vec3 shownAnchor(Entry entry) {
        return shownAnchor(entry, System.currentTimeMillis());
    }

    public static double shownDiamondY(Entry entry) {
        return shownDiamondY(entry, System.currentTimeMillis());
    }

    /** Where the entry draws right now, entities included: the live mob/boat/contraption or the glide/static spot. */
    public static Vec3 liveAnchor(Level level, Entry entry) {
        Vec3 onEntity = entityAnchor(level, entry.entityId());
        return onEntity != null ? onEntity : shownAnchor(entry);
    }

    public static double liveDiamondY(Level level, Entry entry) {
        Double onEntity = entityDiamondY(level, entry.entityId());
        return onEntity != null ? onEntity : shownDiamondY(entry);
    }

    /** Live anchor of a ridden entity, or null when it is gone, untracked or not ours to follow. */
    static Vec3 entityAnchor(Level level, int entityId) {
        Entity target = entityTarget(level, entityId);
        return target == null ? null : new Vec3(target.getX(), target.getY() + 0.1, target.getZ());
    }

    static Double entityDiamondY(Level level, int entityId) {
        Entity target = entityTarget(level, entityId);
        return target == null ? null : target.getY() + target.getBbHeight() + 2.0;
    }

    private static Entity entityTarget(Level level, int entityId) {
        if (level == null || entityId < 0) return null;
        Entity target = level.getEntity(entityId);
        if (target == null || target.isRemoved() || target instanceof Player) return null;
        return target;
    }

    static Vec3 shownAnchor(Entry entry, long nowMs) {
        Glide glide = entry.vehicleId() == null ? null : glides.get(entry.vehicleId());
        if (glide == null) return entry.anchor();
        return lerpVec(glide.fromAnchor(), entry.anchor(), shownT(glide.changedAt(), glide.glideMs(), nowMs));
    }

    static double shownDiamondY(Entry entry, long nowMs) {
        Glide glide = entry.vehicleId() == null ? null : glides.get(entry.vehicleId());
        if (glide == null) return entry.diamondY();
        double t = shownT(glide.changedAt(), glide.glideMs(), nowMs);
        return glide.fromDiamond() + (entry.diamondY() - glide.fromDiamond()) * t;
    }

    static double shownT(long changedAt, long glideMs, long nowMs) {
        if (glideMs <= 0) return 1.0;
        return Math.max(0.0, Math.min(1.0, (nowMs - changedAt) / (double) glideMs));
    }

    static Vec3 lerpVec(Vec3 from, Vec3 to, double t) {
        return new Vec3(from.x + (to.x - from.x) * t, from.y + (to.y - from.y) * t, from.z + (to.z - from.z) * t);
    }

    static void purgeExpired(long nowMs) {
        List<Entry> current = entries;
        if (current.stream().noneMatch(entry -> entry.expireAt() <= nowMs)) {
            dropOrphanGlides(current);
            return;
        }
        List<Entry> next = new ArrayList<>(current);
        next.removeIf(entry -> entry.expireAt() <= nowMs);
        entries = List.copyOf(next);
        dropOrphanGlides(next);
    }

    private static void dropOrphanGlides(List<Entry> live) {
        java.util.Set<java.util.UUID> used = new java.util.HashSet<>();
        for (Entry entry : live) {
            if (entry.vehicleId() != null) used.add(entry.vehicleId());
        }
        glides.keySet().retainAll(used);
    }

    public static PingType selected() {
        return selected;
    }

    public static void cycleSelected() {
        PingType[] values = PingType.values();
        selected = values[(selected.ordinal() + 1) % values.length];
    }

    public static boolean canRadioNow() {
        return System.currentTimeMillis() - lastRadioMs >= RADIO_COOLDOWN_MS + CLIENT_MARGIN_MS;
    }

    public static boolean canPingNow() {
        return System.currentTimeMillis() - lastPingMs >= PING_COOLDOWN_MS + CLIENT_MARGIN_MS;
    }

    public static void markRadio() {
        lastRadioMs = System.currentTimeMillis();
    }

    public static void markPing() {
        lastPingMs = System.currentTimeMillis();
    }

    public static void reset() {
        entries = List.of();
        glides.clear();
        loggedFollow.clear();
        selected = PingType.ENEMY;
        lastRadioMs = 0;
        lastPingMs = 0;
    }
}
