package com.dod.unrealzaruba.Gamemodes.RespawnPoints;

import java.util.List;

import com.dod.unrealzaruba.Commands.Arguments.TeamColor;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

/**
 * A player's respawn options as the client sees them: the team's respawn points, the one the player picked, and the
 * one they'll actually use (the pick, or the default by priority).
 */
public record RespawnPointsView(List<Entry> points, Byte selectedId, Byte effectiveId, List<EnemyBase> enemyBases) {
    public enum Kind { BASE, CAPTURE_POINT, TENT, VEHICLE }

    /** {@code objectiveId}: runtime id of the capture point objective for CAPTURE_POINT entries, else null. */
    public record Entry(byte id, String name, BlockPos position, Kind kind, Byte objectiveId) {}

    /** The main base of another team, shown on the map. */
    public record EnemyBase(TeamColor team, BlockPos position) {}

    public static final RespawnPointsView EMPTY = new RespawnPointsView(List.of(), null, null, List.of());

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
        buf.writeCollection(enemyBases, (b, base) -> {
            b.writeEnum(base.team());
            b.writeBlockPos(base.position());
        });
    }

    public static RespawnPointsView decode(FriendlyByteBuf buf) {
        List<Entry> points = buf.readList(b -> new Entry(b.readByte(), b.readUtf(), b.readBlockPos(),
                b.readEnum(Kind.class), readOptionalByte(b)));
        Byte selected = readOptionalByte(buf);
        Byte effective = readOptionalByte(buf);
        List<EnemyBase> enemyBases = buf.readList(b -> new EnemyBase(b.readEnum(TeamColor.class), b.readBlockPos()));
        return new RespawnPointsView(points, selected, effective, enemyBases);
    }

    private static void writeOptionalByte(FriendlyByteBuf buf, Byte value) {
        buf.writeBoolean(value != null);
        if (value != null) buf.writeByte(value);
    }

    private static Byte readOptionalByte(FriendlyByteBuf buf) {
        return buf.readBoolean() ? buf.readByte() : null;
    }
}
