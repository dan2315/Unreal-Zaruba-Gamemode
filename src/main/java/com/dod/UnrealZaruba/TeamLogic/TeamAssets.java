package com.dod.UnrealZaruba.TeamLogic;

import com.dod.UnrealZaruba.Commands.Arguments.TeamColor;
import com.dod.UnrealZaruba.SoundHandler.ModSounds;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

/**
 * Static mappings for team-related assets like tent templates and sound events
 */
public enum TeamAssets {
    RED(
        TeamColor.RED, 
        new ResourceLocation("unrealzaruba", "red_tent"),
        ModSounds.HORN_DIRE.get()
    ),
    BLUE(
        TeamColor.BLUE, 
        new ResourceLocation("unrealzaruba", "blue_tent"),
        ModSounds.HORN_RADIANT.get()
    ),
    PURPLE(
        TeamColor.PURPLE, 
        new ResourceLocation("unrealzaruba", "purple_tent"),
        ModSounds.HORN_DIRE.get()
    ),
    YELLOW(
        TeamColor.YELLOW, 
        new ResourceLocation("unrealzaruba", "yellow_tent"),
        ModSounds.HORN_RADIANT.get()
    ),
    UNDEFINED(
        TeamColor.UNDEFINED, 
        new ResourceLocation("unrealzaruba", "tent"),
        ModSounds.SELECT1.get()
    );

    private final TeamColor teamColor;
    private final ResourceLocation tentTemplate;
    private final SoundEvent hornSound;

    TeamAssets(TeamColor teamColor, ResourceLocation tentTemplate, SoundEvent hornSound) {
        this.teamColor = teamColor;
        this.tentTemplate = tentTemplate;
        this.hornSound = hornSound;
    }

    public TeamColor getTeamColor() {
        return teamColor;
    }

    public ResourceLocation getTentTemplate() {
        return tentTemplate;
    }

    public SoundEvent getHornSound() {
        return hornSound;
    }

    /**
     * Get the team assets by team color
     * @param color The team color
     * @return The TeamAssets enum value for the given color
     */
    public static TeamAssets getByTeamColor(TeamColor color) {
        for (TeamAssets asset : values()) {
            if (asset.teamColor == color) {
                return asset;
            }
        }
        return UNDEFINED;
    }
} 