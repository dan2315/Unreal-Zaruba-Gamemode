package com.dod.UnrealZaruba.UI.Objectives;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.FriendlyByteBuf;

import java.util.HashMap;
import java.util.function.Function;

public abstract class HudObjective {
    byte runtimeId;
    public abstract void Serialize(FriendlyByteBuf buffer);
    public static HudObjective Deserialize(FriendlyByteBuf buffer) {
        var typeId = buffer.readVarInt();
        return factoryMethodsMap.get(typeId).apply(buffer);
    }
    public abstract void Render(GuiGraphics guiGraphics, int x, int[] yReference);
    public byte GetRuntimeId() {
        return runtimeId;
    }

    public static HashMap<Integer, Function<FriendlyByteBuf, HudObjective>> factoryMethodsMap = new HashMap<>();

    public static void RegisterFactoryMethod(Integer typeId, Function<FriendlyByteBuf, HudObjective> factoryMethod) {
        factoryMethodsMap.put(typeId, factoryMethod);
    }

    public static void InitializeAllHudObjectiveTypes() {
        HudObjective.RegisterFactoryMethod(HudCapturePointObjective.TYPE_ID, HudCapturePointObjective::new);
        HudObjective.RegisterFactoryMethod(HudStringObjective.TYPE_ID, HudStringObjective::new);
    }

    public abstract void Update(byte progress);
}
