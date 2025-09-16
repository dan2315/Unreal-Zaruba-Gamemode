package com.dod.UnrealZaruba.UI.Objectives;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import java.util.HashMap;
import java.util.List;

public class ObjectivesOverlay implements IGuiOverlay {
    public boolean isVisible = true;
    HashMap<Byte, HudObjective> objectivesMap = new HashMap<>();

    public static ObjectivesOverlay INSTANCE = new ObjectivesOverlay();

    @Override
    public void render(ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        if (!isVisible || objectivesMap.isEmpty()) return;
        int baseHeight = 1080;
        float scale = (float) screenHeight / baseHeight;
        scale = Mth.clamp(scale, 0.5f, 2.0f) * 1.5f;

        int x = (int) (screenWidth * 0.01f);
        int y = (int) (screenHeight * 0.3f);

        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(scale, scale, scale);

        int xScaled = (int)(x / scale);
        int yScaled = (int)(y / scale);

        var titleScale = 1.2f;
        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(titleScale, titleScale, titleScale);
        guiGraphics.drawString(gui.getFont(),"- Обжективы -",(int)(xScaled / titleScale), (int) (yScaled/ titleScale),0xC97136);
        guiGraphics.pose().popPose();
        yScaled += 12;

        int[] heightReference = new int[] {yScaled};
        for (var objective : objectivesMap.values()) {
            objective.Render(guiGraphics, x, heightReference);
        }

        guiGraphics.pose().popPose();
    }

    public void AddObjective(HudObjective objective) {
        objectivesMap.put(objective.GetRuntimeId() ,objective);
    }

    public void SetObjectives(List<HudObjective> objectives) {
        for (var objective : objectives) {
            objectivesMap.put(objective.GetRuntimeId(), objective);
        }
    }

    public void UpdateObjectives(List<HudObjectiveUpdate> updates) {
        for (var update : updates) {
            if (objectivesMap.containsKey(update.getRuntimeId())) {
                var hudObjective = objectivesMap.get(update.getRuntimeId());
                hudObjective.Update(update.getProgress());
            }
        }
    }

    public void Clear() {
        objectivesMap.clear();
    }
}
