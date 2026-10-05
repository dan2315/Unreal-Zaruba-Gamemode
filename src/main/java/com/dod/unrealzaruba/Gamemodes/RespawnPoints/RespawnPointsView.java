package com.dod.unrealzaruba.Gamemodes.RespawnPoints;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

/**
 * A player's respawn options as the client sees them: the team's respawn points, the one the player picked, and the
 * one they'll actually use (the pick, or the default by priority).
 */
public record RespawnPointsView(List<Entry> points, Byte selectedId, Byte effectiveId) {
    public enum Kind { BASE, CAPTURE_POINT, TENT, VEHICLE }

    /** {@code objectiveId}: runtime id of the capture point objective for CAPTURE_POINT entries, else null. */
    public record Entry(byte id, String name, BlockPos position, Kind kind, Byte objectiveId) {}

    public static final RespawnPointsView EMPTY = new RespawnPointsView(List.of(), null, null);

    public Entry find(byte id) {
        return points.stream().filter(entry -> entry.id() == id).findFirst().orElse(null);
    }

    public Entry effective() {
        return effectiveId == null ? null : find(effectiveId);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeCollection(points, (b, entry) -> {
            b.writeByte(entry.id());
            b.writeUtf(entry.name());
            b.writeBlockPos(entry.position());
            b.writeEnum(entry.kind());
            writeOptionalByte(b, entry.objectiveId());
        });
        writeOptionalByte(buf, selectedId);
        writeOptionalByte(buf, effectiveId);
    }

    public static RespawnPointsView decode(FriendlyByteBuf buf) {
        List<Entry> points = buf.readList(b -> new Entry(b.readByte(), b.readUtf(), b.readBlockPos(),
                b.readEnum(Kind.class), readOptionalByte(b)));
        return new RespawnPointsView(points, readOptionalByte(buf), readOptionalByte(buf));
    }

    private static void writeOptionalByte(FriendlyByteBuf buf, Byte value) {
        buf.writeBoolean(value != null);
        if (value != null) buf.writeByte(value);
    }

    private static Byte readOptionalByte(FriendlyByteBuf buf) {
        return buf.readBoolean() ? buf.readByte() : null;
    }
}
