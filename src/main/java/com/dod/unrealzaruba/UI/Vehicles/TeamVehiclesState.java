package com.dod.unrealzaruba.UI.Vehicles;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.dod.unrealzaruba.Commands.Arguments.TeamColor;
import com.dod.unrealzaruba.NetworkPackets.Vehicles.ClientboundTeamVehiclesPacket;

/**
 * The team's vehicles as the server last said. The server speaks twice a second, so a vehicle's drawn position glides
 * from where it was to where it is now instead of jumping.
 */
public final class TeamVehiclesState {
    private static final long MIN_GLIDE_MILLIS = 100;
    private static final long MAX_GLIDE_MILLIS = 1000;

    /** A vehicle with the position it is shown at now. */
    public record Shown(UUID id, String name, TeamColor team, double x, double topY, double z, double centerY) {}

    private static final class Vehicle {
        String name;
        TeamColor team;
        double fromX, fromTop, fromZ, fromCenter;
        double toX, toTop, toZ, toCenter;
        long changedAt;
        /** How long the last step took to arrive: the glide to the next one takes as long, so it never stops short or waits. */
        long glideMillis = 250;
    }

    private static final Map<UUID, Vehicle> VEHICLES = new HashMap<>();

    private TeamVehiclesState() {}

    public static void set(List<ClientboundTeamVehiclesPacket.Entry> entries) {
        long now = System.currentTimeMillis();
        Map<UUID, Vehicle> next = new HashMap<>();
        for (var entry : entries) {
            Vehicle vehicle = VEHICLES.get(entry.id());
            if (vehicle == null) {
                vehicle = new Vehicle();
                vehicle.fromX = entry.x();
                vehicle.fromTop = entry.topY();
                vehicle.fromZ = entry.z();
                vehicle.fromCenter = entry.centerY();
            } else {
                Shown now0 = shownAt(vehicle, entry.id(), now);
                vehicle.fromX = now0.x();
                vehicle.fromTop = now0.topY();
                vehicle.fromZ = now0.z();
                vehicle.fromCenter = now0.centerY();
            }
            if (vehicle.changedAt != 0) {
                vehicle.glideMillis = Math.max(MIN_GLIDE_MILLIS, Math.min(MAX_GLIDE_MILLIS, now - vehicle.changedAt));
            }
            vehicle.name = entry.name();
            vehicle.team = entry.team();
            vehicle.toX = entry.x();
            vehicle.toTop = entry.topY();
            vehicle.toZ = entry.z();
            vehicle.toCenter = entry.centerY();
            vehicle.changedAt = now;
            next.put(entry.id(), vehicle);
        }
        VEHICLES.clear();
        VEHICLES.putAll(next);
    }

    public static Collection<Shown> all() {
        long now = System.currentTimeMillis();
        List<Shown> shown = new ArrayList<>(VEHICLES.size());
        for (var entry : VEHICLES.entrySet()) shown.add(shownAt(entry.getValue(), entry.getKey(), now));
        return shown;
    }

    public static void reset() {
        VEHICLES.clear();
    }

    private static Shown shownAt(Vehicle v, UUID id, long now) {
        double t = Math.min(1.0, (now - v.changedAt) / (double) v.glideMillis);
        return new Shown(id, v.name, v.team, lerp(v.fromX, v.toX, t), lerp(v.fromTop, v.toTop, t), lerp(v.fromZ, v.toZ, t),
                lerp(v.fromCenter, v.toCenter, t));
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }
}
