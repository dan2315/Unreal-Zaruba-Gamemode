package com.dod.unrealzaruba.UI;

import com.dod.unrealzaruba.ModItems.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

public class SkullCurrencyOverlay implements IGuiOverlay {
    public boolean is_visible = false;
    public int amount = 0;

    public static IGuiOverlay INSTANCE = new SkullCurrencyOverlay();
    public static SkullCurrencyOverlay OVERLAY_INSTANCE = (SkullCurrencyOverlay) INSTANCE;
    // Даня, это надо перех@ячить))
    @Override
    public void render(ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();

        if (!is_visible) {
            return;
        }

        if (mc.player == null || mc.level == null) {
            return;
        }

        Font font = mc.font;

        int y = 10;
        int x = screenWidth - 20;

        guiGraphics.renderItem(new ItemStack(ModItems.SKULL.get()), x, y - 3);
        guiGraphics.drawString(font, String.valueOf(amount), x, y, 0xFFFFFF, true);
    }
}
