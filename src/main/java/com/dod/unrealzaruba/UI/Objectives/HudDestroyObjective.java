package com.dod.unrealzaruba.UI.Objectives;

import com.dod.unrealzaruba.NetworkPackets.SerializationUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

public class HudDestroyObjective extends HudObjective{
    public static final int TYPE_ID = 3;

    private final String name;
    private float progress;
    private final BlockPos position;

    public HudDestroyObjective(FriendlyByteBuf buffer) {
        this.runtimeId = buffer.readByte();
        this.name = buffer.readUtf();
        this.progress = buffer.readFloat();
        this.position = SerializationUtils.decodeBlockPos(buffer);
    }

    public HudDestroyObjective(Byte runtimeId ,String name, float progress, BlockPos position) {
        this.runtimeId = runtimeId;
        this.name = name;
        this.progress = progress;
        this.position = position;
    }


    @Override
    public void Serialize(FriendlyByteBuf buffer) {
        buffer.writeVarInt(TYPE_ID);
        buffer.writeByte(runtimeId);
        buffer.writeUtf(name);
        buffer.writeFloat(progress);
        SerializationUtils.encodeBlockPos(buffer, position);
    }

    public BlockPos GetPosition() {
        return position;
    }

    @Override
    public void Update(byte progress) {
        this.progress = (progress - Byte.MIN_VALUE) / (float)(Byte.MAX_VALUE - Byte.MIN_VALUE);
    }

    public float getProgress() {
        return progress;
    }

    public String GetName() {
        return name;
    }
}
