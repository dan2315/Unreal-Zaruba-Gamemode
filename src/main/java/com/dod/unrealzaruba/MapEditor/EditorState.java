package com.dod.unrealzaruba.MapEditor;

import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

/**
 * What the map editor shows of the open map: its capture points (area, name, initial owner, respawn), the targets to
 * destroy (volume, name, share to destroy) and the team bases. Indexes are positions in the map's objectives list.
 *
 * @param editable whether edits are taken (DEV mode); in GAME mode the editor only shows
 */
public record EditorState(String mapKey, String mode, boolean editable, List<Point> points, List<Target> targets,
                          Map<String, BlockPos> bases) {
    public static final EditorState EMPTY = new EditorState("", "", false, List.of(), List.of(), Map.of());

    /** @param owner RED, BLUE or "" for neutral; respawn is the feet position, or null */
    public record Point(int index, String name, String owner, BlockPos min, BlockPos max, BlockPos respawn) {}

    /** @param required share of the blocks that must be destroyed, 0..1 */
    public record Target(int index, String name, BlockPos min, BlockPos max, float required) {}

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(mapKey);
        buf.writeUtf(mode);
        buf.writeBoolean(editable);
        buf.writeCollection(points, (b, p) -> {
            b.writeVarInt(p.index());
            b.writeUtf(p.name());
            b.writeUtf(p.owner());
            b.writeBlockPos(p.min());
            b.writeBlockPos(p.max());
            b.writeBoolean(p.respawn() != null);
            if (p.respawn() != null) b.writeBlockPos(p.respawn());
        });
        buf.writeCollection(targets, (b, t) -> {
            b.writeVarInt(t.index());
            b.writeUtf(t.name());
            b.writeBlockPos(t.min());
            b.writeBlockPos(t.max());
            b.writeFloat(t.required());
        });
        buf.writeMap(bases, (b, team) -> b.writeUtf(team), (b, pos) -> b.writeBlockPos(pos));
    }

    public static EditorState decode(FriendlyByteBuf buf) {
        String mapKey = buf.readUtf();
        String mode = buf.readUtf();
        boolean editable = buf.readBoolean();
        List<Point> points = buf.readList(b -> new Point(b.readVarInt(), b.readUtf(), b.readUtf(), b.readBlockPos(), b.readBlockPos(),
                b.readBoolean() ? b.readBlockPos() : null));
        List<Target> targets = buf.readList(b -> new Target(b.readVarInt(), b.readUtf(), b.readBlockPos(), b.readBlockPos(), b.readFloat()));
        Map<String, BlockPos> bases = buf.readMap(b -> b.readUtf(), b -> b.readBlockPos());
        return new EditorState(mapKey, mode, editable, points, targets, bases);
    }
}
