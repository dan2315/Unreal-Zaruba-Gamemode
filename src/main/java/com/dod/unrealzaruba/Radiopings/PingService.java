package com.dod.unrealzaruba.Radiopings;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.dod.unrealzaruba.NetworkPackets.Radio.ClientboundPingPacket;
import com.dod.unrealzaruba.NetworkPackets.Radio.ClientboundPingVehiclePacket;
import com.dod.unrealzaruba.Player.PlayerContext;
import com.dod.unrealzaruba.Player.TeamPlayerContext;
import com.dod.unrealzaruba.SableIntegration.SableShips;
import com.dod.unrealzaruba.TeamLogic.TeamContext;
import com.dod.unrealzaruba.UnrealZaruba;
import com.dod.unrealzaruba.WorldManager.WorldManager;

import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3dc;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Server side of pings: keeps a few live pings per player, checks a new one and shows it to teammates
 * within {@link #VIS_RADIUS} blocks of the mark (plus the author). A ping placed on a vehicle hull rides it:
 * clients get its live position a few times a second. Pings die on their own after {@link #LIFETIME_MS},
 * so no removal packets are needed; a reconnecting player just misses old ones.
 */
public final class PingService {
    static final long LIFETIME_MS = 8000;
    static final long COOLDOWN_MS = 1000;
    static final double MAX_PLACE_DIST = 160.0;
    static final double VIS_RADIUS = 100.0;
    static final int MAX_PER_PLAYER = 2;
    /** Live position of a ridden vehicle goes out this often. */
    static final int VEHICLE_UPDATE_EVERY_TICKS = 4;

    /** A ship hull, in plain numbers so the picking math stays unit-testable without the Sable classes. */
    record ShipBox(UUID id, double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        double volume() {
            return Math.max(0, maxX - minX) * Math.max(0, maxY - minY) * Math.max(0, maxZ - minZ);
        }

        Vec3 center() {
            return new Vec3((minX + maxX) / 2, (minY + maxY) / 2, (minZ + maxZ) / 2);
        }
    }

    /** A hull under the crosshair and where along the ray it starts. */
    record HullHit(ShipBox box, double t) {}

    record Ping(UUID sender, String senderName, PingType type, Vec3 anchor, double diamondY, long expireAt,
            UUID vehicleId, int entityId, Vec3 rideOffset, List<UUID> recipients) {}

    private static final Map<UUID, Deque<Ping>> pings = new HashMap<>();
    private static final Map<UUID, Long> lastPlaced = new HashMap<>();
    private static long lastFollowLogMs;

    private PingService() {}

    public static void onPing(ServerPlayer sender, byte typeId, BlockPos pos, int entityId, Vec3 eye, Vec3 look,
            boolean attach) {
        PingType type = PingType.byId(typeId);
        debug("place: type=" + typeId + " pos=" + pos + " dim=" + sender.level().dimension().location());
        if (type == null || pos == null || eye == null || look == null) {
            debug("reject: bad type/pos/eye/look");
            return;
        }
        TeamContext team = PlayerContext.Get(sender.getUUID()) instanceof TeamPlayerContext context ? context.Team() : null;
        boolean debug = team == null && RadioDebug.DEBUG_FORCE;
        if (team == null && !debug) {
            debug("reject: no team and debug off");
            return;
        }
        // The dead neither place nor see pings; radio is their channel.
        if (sender.isSpectator() || sender.isDeadOrDying()) {
            debug("reject: sender is spectator/dead");
            return;
        }
        if (!debug && !RadioService.matchRunning()) {
            debug("reject: no running match");
            return;
        }
        if (!debug && sender.level().dimension() != WorldManager.GAME_DIMENSION) {
            debug("reject: wrong dimension");
            return;
        }
        if (sender.position().distanceToSqr(Vec3.atCenterOf(pos)) > MAX_PLACE_DIST * MAX_PLACE_DIST) {
            debug("reject: too far");
            return;
        }

        long now = System.currentTimeMillis();
        purgeExpired(now);
        if (!RadioService.cooldownOk(lastPlaced, sender.getUUID(), now, COOLDOWN_MS)) {
            debug("reject: cooldown");
            return;
        }
        lastPlaced.put(sender.getUUID(), now);

        UUID vehicleId = null;
        int attachedEntity = -1;
        Vec3 anchor = new Vec3(pos.getX() + 0.5, pos.getY() + 0.1, pos.getZ() + 0.5);
        double diamondY = pos.getY() + 2.5;
        Vec3 rideOffset = Vec3.ZERO;
        if (attach && sender.level() instanceof ServerLevel level) {
            net.minecraft.world.entity.Entity aimed = entityId >= 0 ? level.getEntity(entityId) : null;
            if (aimed != null && !(aimed instanceof net.minecraft.world.entity.player.Player) && !aimed.isRemoved()
                    && aimed.position().distanceToSqr(sender.position()) <= MAX_PLACE_DIST * MAX_PLACE_DIST) {
                // The crosshair was on something alive: ride it (boats, mobs, contraptions, Sable or not).
                attachedEntity = aimed.getId();
                anchor = new Vec3(aimed.getX(), aimed.getY() + 0.1, aimed.getZ());
                diamondY = aimed.getY() + aimed.getBbHeight() + 2.0;
                debug("attach: entity " + aimed.getType().getDescriptionId());
            } else {
                Vec3 dir = look.normalize();
                HullHit hit = resolveHull(liveBoxes(level), Vec3.atCenterOf(pos), eye, dir, terrainDist(level, eye, dir));
                if (hit != null) {
                    // Pin the mark to the aimed spot on the hull, not to the box: on a tilted hull the box
                    // faces hang in empty air. The offset rides the hull's center as it moves (rotation of the
                    // hull itself is not tracked: on a sharp turn the mark slides around, accepted for v1).
                    vehicleId = hit.box().id();
                    Vec3 entry = eye.add(dir.scale(hit.t()));
                    rideOffset = entry.subtract(hit.box().center());
                    anchor = new Vec3(entry.x, entry.y + 0.1, entry.z);
                    diamondY = entry.y + 2.0;
                    debug("attach: hull " + vehicleId);
                }
            }
        }

        Deque<Ping> own = pings.computeIfAbsent(sender.getUUID(), id -> new ArrayDeque<>());
        // Recipients are frozen at placement: teammates around the mark (plus the author), alive, in the game.
        // Without a team (singleplayer debug) the sender is the only recipient.
        List<UUID> recipients = new ArrayList<>();
        Vec3 mark = new Vec3(anchor.x, anchor.y, anchor.z);
        MinecraftServer server = sender.server;
        if (team == null) {
            recipients.add(sender.getUUID());
        } else {
            for (UUID id : List.copyOf(team.Members())) {
                ServerPlayer player = server.getPlayerList().getPlayer(id);
                if (player == null) continue;
                if (player.level().dimension() != WorldManager.GAME_DIMENSION) continue;
                if (player.isSpectator() || player.isDeadOrDying()) continue;
                if (!shouldReceive(player.getUUID(), player.position(), sender.getUUID(), mark)) continue;
                recipients.add(id);
            }
        }
        Ping ping = new Ping(sender.getUUID(), sender.getGameProfile().getName(), type, anchor, diamondY,
                now + LIFETIME_MS, vehicleId, attachedEntity, rideOffset, recipients);
        own.addLast(ping);
        while (own.size() > MAX_PER_PLAYER) own.pollFirst();
        debug("broadcast to " + recipients.size() + " recipients, vehicle=" + vehicleId + " entity=" + attachedEntity);

        for (UUID id : recipients) {
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player == null) continue;
            PacketDistributor.sendToPlayer(player,
                    new ClientboundPingPacket(sender.getUUID(), sender.getGameProfile().getName(),
                            (byte) type.ordinal(), anchor, diamondY, ping.expireAt(), vehicleId, attachedEntity));
        }
    }

    /**
     * Debug only: a ping riding a fake hull that orbits the placement spot, so the whole follow path
     * (server tick, update packets, renderer, compass) can be watched in singleplayer with no ships around.
     */
    public static void placeDemo(ServerPlayer sender) {
        if (!RadioDebug.DEBUG_FORCE) return;
        long now = System.currentTimeMillis();
        purgeExpired(now);
        Vec3 feet = sender.position();
        RadioDebug.demoCenter = new Vec3(feet.x, feet.y, feet.z);
        RadioDebug.demoStartMs = now;
        Vec3 anchor = RadioDebug.demoAnchor(RadioDebug.demoCenter, now, now);
        double diamondY = feet.y + 2.5;
        List<UUID> recipients = List.of(sender.getUUID());
        Ping ping = new Ping(sender.getUUID(), sender.getGameProfile().getName(), PingType.ENEMY,
                anchor, diamondY, now + LIFETIME_MS, RadioDebug.DEMO_VEHICLE_ID, -1, Vec3.ZERO, recipients);
        Deque<Ping> own = pings.computeIfAbsent(sender.getUUID(), id -> new ArrayDeque<>());
        own.addLast(ping);
        while (own.size() > MAX_PER_PLAYER) own.pollFirst();
        PacketDistributor.sendToPlayer(sender, new ClientboundPingPacket(sender.getUUID(),
                sender.getGameProfile().getName(), (byte) PingType.ENEMY.ordinal(), anchor, diamondY,
                ping.expireAt(), ping.vehicleId(), -1));
    }

    /** Sends the live hull positions of ridden vehicles to the pings' recipients, a few times a second. */
    public static void tick(MinecraftServer server) {
        if (server.getTickCount() % VEHICLE_UPDATE_EVERY_TICKS != 0) return;
        long now = System.currentTimeMillis();
        purgeExpired(now);
        for (Deque<Ping> own : pings.values()) {
            for (Ping ping : own) {
                if (ping.vehicleId() == null) continue;
                Vec3 anchor;
                double diamondY;
                if (RadioDebug.DEMO_VEHICLE_ID.equals(ping.vehicleId()) && RadioDebug.demoCenter != null) {
                    anchor = RadioDebug.demoAnchor(RadioDebug.demoCenter, now, RadioDebug.demoStartMs);
                    diamondY = RadioDebug.demoCenter.y + 2.5;
                } else {
                    ServerPlayer owner = server.getPlayerList().getPlayer(ping.sender());
                    if (owner == null || !(owner.level() instanceof ServerLevel level)) continue;
                    ShipBox hull = boxOf(level, ping.vehicleId());
                    if (hull == null) continue; // destroyed: the mark freezes at the last spot until it expires
                    // The mark rides the aimed spot, not the box bottom: re-apply the placement offset.
                    Vec3 center = new Vec3((hull.minX() + hull.maxX()) / 2, (hull.minY() + hull.maxY()) / 2, (hull.minZ() + hull.maxZ()) / 2);
                    anchor = new Vec3(center.x + ping.rideOffset().x, center.y + ping.rideOffset().y + 0.1, center.z + ping.rideOffset().z);
                    diamondY = anchor.y + 1.9;
                    if (RadioDebug.DEBUG_FORCE && now - lastFollowLogMs > 2000) {
                        lastFollowLogMs = now;
                        UnrealZaruba.LOGGER.info("[PingDebug] follow {} anchor={} boxCenter={}", ping.vehicleId(), anchor, center);
                    }
                }
                for (UUID id : ping.recipients()) {
                    ServerPlayer player = server.getPlayerList().getPlayer(id);
                    if (player == null) continue;
                    PacketDistributor.sendToPlayer(player,
                            new ClientboundPingVehiclePacket(ping.vehicleId(), anchor, diamondY));
                }
            }
        }
    }

    /** Drop everything of a player who left, or clear for the singleplayer debug toggle. */
    static void clear() {
        pings.clear();
        lastPlaced.clear();
    }

    private static void debug(String message) {
        if (RadioDebug.DEBUG_FORCE) {
            UnrealZaruba.LOGGER.info("[PingDebug] {}", message);
        }
    }

    /** A hull under the crosshair: the ship the aimed block sits in, else the nearest unoccluded hull on the ray. */
    static HullHit resolveHull(List<ShipBox> boxes, Vec3 hitCenter, Vec3 eye, Vec3 dir, double terrainDist) {
        ShipBox inside = null;
        for (ShipBox box : boxes) {
            if (containsPoint(box, hitCenter, 0.01) && (inside == null || box.volume() < inside.volume())) {
                inside = box;
            }
        }
        if (inside != null) {
            if (RadioDebug.DEBUG_FORCE) {
                UnrealZaruba.LOGGER.info("[PingDebug] resolve: aimed block inside hull {}", inside.id());
            }
            return new HullHit(inside, eye.distanceTo(hitCenter));
        }

        ShipBox best = null;
        double bestT = Double.POSITIVE_INFINITY;
        for (ShipBox box : boxes) {
            if (containsPoint(box, eye, 0.0)) continue; // driving: never self-attach to the own hull
            Double t = rayBoxT(eye, dir, box, MAX_PLACE_DIST);
            if (t == null || t >= bestT) continue;
            if (terrainDist < t - 1.0) continue; // a wall stands between the crosshair and this hull
            best = box;
            bestT = t;
        }
        if (best == null) {
            if (RadioDebug.DEBUG_FORCE) {
                UnrealZaruba.LOGGER.info("[PingDebug] resolve: no hull (terrain at {})", terrainDist);
            }
            return null;
        }
        return new HullHit(best, bestT);
    }

    static List<ShipBox> liveBoxes(ServerLevel level) {
        var container = SubLevelContainer.getContainer(level);
        if (container == null) return List.of();
        List<ShipBox> boxes = new ArrayList<>();
        for (ServerSubLevel sub : container.getAllSubLevels()) {
            if (sub.isRemoved()) continue;
            BoundingBox3dc box = sub.boundingBox();
            boxes.add(new ShipBox(sub.getUniqueId(), box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ()));
        }
        return boxes;
    }

    static ShipBox boxOf(ServerLevel level, UUID id) {
        ServerSubLevel sub = SableShips.get(level, id);
        if (sub == null || sub.isRemoved()) return null;
        BoundingBox3dc box = sub.boundingBox();
        return new ShipBox(id, box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ());
    }

    static double terrainDist(ServerLevel level, Vec3 eye, Vec3 dir) {
        HitResult hit = level.clip(new ClipContext(eye, eye.add(dir.scale(MAX_PLACE_DIST)),
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, (net.minecraft.world.entity.Entity) null));
        if (hit.getType() == HitResult.Type.MISS) return Double.POSITIVE_INFINITY;
        return eye.distanceTo(hit.getLocation());
    }

    static boolean containsPoint(ShipBox box, Vec3 p, double eps) {
        return p.x >= box.minX() - eps && p.x <= box.maxX() + eps
                && p.y >= box.minY() - eps && p.y <= box.maxY() + eps
                && p.z >= box.minZ() - eps && p.z <= box.maxZ() + eps;
    }

    /**
     * Ray vs box (slab method): the distance along a normalized ray, or null on a miss.
     * An origin inside the box counts as an immediate hit (t = 0).
     */
    static Double rayBoxT(Vec3 o, Vec3 d, ShipBox box, double maxDist) {
        double tmin = 0.0;
        double tmax = maxDist;
        double[] origin = {o.x, o.y, o.z};
        double[] direction = {d.x, d.y, d.z};
        double[] mins = {box.minX(), box.minY(), box.minZ()};
        double[] maxs = {box.maxX(), box.maxY(), box.maxZ()};
        for (int i = 0; i < 3; i++) {
            if (Math.abs(direction[i]) < 1e-9) {
                if (origin[i] < mins[i] || origin[i] > maxs[i]) return null;
            } else {
                double t1 = (mins[i] - origin[i]) / direction[i];
                double t2 = (maxs[i] - origin[i]) / direction[i];
                tmin = Math.max(tmin, Math.min(t1, t2));
                tmax = Math.min(tmax, Math.max(t1, t2));
                if (tmin > tmax) return null;
            }
        }
        return tmin;
    }

    /** The author always gets their own ping back; teammates only within the visibility radius of the mark. */
    static boolean shouldReceive(UUID recipient, Vec3 recipientPos, UUID sender, Vec3 pingPos) {
        if (recipient.equals(sender)) return true;
        return withinRadius(recipientPos, pingPos, VIS_RADIUS);
    }

    static boolean withinRadius(Vec3 a, Vec3 b, double radius) {
        return a.distanceToSqr(b) <= radius * radius;
    }

    static void purgeExpired(long nowMs) {
        for (var it = pings.entrySet().iterator(); it.hasNext(); ) {
            Deque<Ping> list = it.next().getValue();
            list.removeIf(ping -> ping.expireAt() <= nowMs);
            if (list.isEmpty()) it.remove();
        }
    }

    /** Drop everything of a player who left: cooldown and live pings alike. */
    public static void forget(UUID id) {
        pings.remove(id);
        lastPlaced.remove(id);
    }
}
