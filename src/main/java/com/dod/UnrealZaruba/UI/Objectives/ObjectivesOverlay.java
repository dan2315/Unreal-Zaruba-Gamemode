package com.dod.UnrealZaruba.UI.Objectives;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import java.util.ArrayList;

public class ObjectivesOverlay implements IGuiOverlay {
    public boolean isVisible;
    ArrayList<HudObjective> objectives = new ArrayList<>();

    public static ObjectivesOverlay INSTANCE = new ObjectivesOverlay();

    @Override
    public void render(ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        if (!isVisible) return;
        int startingHeight = 0;
        int[] heightReference = new int[] {startingHeight};
        for (var objective : objectives) {
            objective.Render(guiGraphics, heightReference);
        }
    }

    public void AddObjective(HudObjective objective) {
        objectives.add(objective);
    }
}
