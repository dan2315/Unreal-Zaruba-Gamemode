package com.dod.unrealzaruba.UI.Objectives;

import com.dod.unrealzaruba.Commands.Arguments.TeamColor;
import com.dod.unrealzaruba.NetworkPackets.SerializationUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

/** Capture point state sent to clients. Drawn by {@link ObjectivesOverlay}. */
public class HudCapturePointObjective extends HudObjective {
    public static final int TYPE_ID = 1;

    private final String name;
    private final int ownerColor;
    private final int capturedByColor;
    /** Null when nobody holds / captures the point. */
    private final TeamColor owner;
    private final TeamColor capturedBy;
    private float progress;
    private final BlockPos position;
    /** A respawn point to capture, not a capture point: shown as a house and without a letter. */
    private final boolean respawnOnly;

    public HudCapturePointObjective(FriendlyByteBuf buffer) {
        this.runtimeId = buffer.readByte();
        this.name = buffer.readUtf();
        this.ownerColor = buffer.readVarInt();
        this.capturedByColor = buffer.readVarInt();
        this.owner = readTeam(buffer);
        this.capturedBy = readTeam(buffer);
        this.progress = buffer.readFloat();
        this.position = SerializationUtils.decodeBlockPos(buffer);
        this.respawnOnly = buffer.readBoolean();
    }

    public HudCapturePointObjective(Byte runtimeId, String name, int ownerColor, int capturedByColor,
                                    TeamColor owner, TeamColor capturedBy, float progress, BlockPos position,
                                    boolean respawnOnly) {
        this.runtimeId = runtimeId;
        this.name = name;
        this.ownerColor = ownerColor;
        this.capturedByColor = capturedByColor;
        this.owner = owner;
        this.capturedBy = capturedBy;
        this.progress = progress;
        this.position = position;
        this.respawnOnly = respawnOnly;
    }

    @Override
    public void Serialize(FriendlyByteBuf buffer) {
        buffer.writeVarInt(TYPE_ID);
        buffer.writeByte(runtimeId);
        buffer.writeUtf(name);
        buffer.writeVarInt(ownerColor);
        buffer.writeVarInt(capturedByColor);
        writeTeam(buffer, owner);
        writeTeam(buffer, capturedBy);
        buffer.writeFloat(progress);
        SerializationUtils.encodeBlockPos(buffer, position);
        buffer.writeBoolean(respawnOnly);
    }

    private static void writeTeam(FriendlyByteBuf buffer, TeamColor team) {
        buffer.writeByte(team == null ? -1 : team.ordinal());
    }

    private static TeamColor readTeam(FriendlyByteBuf buffer) {
        int ordinal = buffer.readByte();
        return ordinal < 0 || ordinal >= TeamColor.values().length ? null : TeamColor.values()[ordinal];
    }

    @Override
    public void Update(byte progress) {
        this.progress = (progress - Byte.MIN_VALUE) / (float)(Byte.MAX_VALUE - Byte.MIN_VALUE);
    }

    public int getOwnerColor() {
        return ownerColor;
    }

    public TeamColor getOwner() {
        return owner;
    }

    public TeamColor getCapturedBy() {
        return capturedBy;
    }

    public float getProgress() {
        return progress;
    }

    public String GetName() {
        return name;
    }

    public boolean isRespawnOnly() {
        return respawnOnly;
    }

    public BlockPos GetPosition() {
        return position;
    }
}
