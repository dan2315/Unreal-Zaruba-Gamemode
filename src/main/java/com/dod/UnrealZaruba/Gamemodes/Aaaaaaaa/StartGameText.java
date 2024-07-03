package com.dod.UnrealZaruba.Gamemodes.Aaaaaaaa;

import net.minecraft.network.chat.TextComponent;

public class StartGameText {
    public String title;
    public String subtitle;

    public StartGameText(String title, String subtitle) {
        this.title = title;
        this.subtitle = subtitle;
    }

    public TextComponent GetTitle()
    {
        return new TextComponent(title);
    }

    public TextComponent GetSubtitle()
    {
        return new TextComponent(subtitle);
    }

}
