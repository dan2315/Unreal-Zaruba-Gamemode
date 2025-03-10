package com.dod.UnrealZaruba.Gamemodes.GameText;

import net.minecraft.network.chat.Component;

public class StartGameText {
    public String title;
    public String subtitle;

    public StartGameText(String title, String subtitle) {
        this.title = title;
        this.subtitle = subtitle;
    }

    public Component GetTitle()
    {
        return Component.literal(title);
    }

    public Component GetSubtitle()
    {
        return Component.literal(subtitle);
    }

}
