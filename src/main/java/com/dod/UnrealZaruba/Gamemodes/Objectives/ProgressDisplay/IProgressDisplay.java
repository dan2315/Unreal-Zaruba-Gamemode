package com.dod.UnrealZaruba.Gamemodes.Objectives.ProgressDisplay;

import net.minecraft.server.level.ServerPlayer;

public interface IProgressDisplay {

    void updateProgress(float progress);

    void updatePlayerVisibility(ServerPlayer player);

    void clear();
} 