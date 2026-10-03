package com.dod.unrealzaruba.Gamemodes.Objectives.ProgressDisplay;

import net.minecraft.server.level.ServerPlayer;

import java.util.List;

public class CombinedProgressDisplay implements IProgressDisplay {

    public List<IProgressDisplay> progressDisplayList;

    public CombinedProgressDisplay(List<IProgressDisplay> progressDisplayList) {
        this.progressDisplayList = progressDisplayList;
    }

    @Override
    public void updateProgress(float progress) {
        progressDisplayList.forEach(iProgressDisplay -> iProgressDisplay.updateProgress(progress));
    }

    @Override
    public void updatePlayerVisibility(ServerPlayer player) {
        progressDisplayList.forEach(iProgressDisplay -> iProgressDisplay.updatePlayerVisibility(player));
    }

    @Override
    public void clear() {
        progressDisplayList.forEach(IProgressDisplay::clear);
    }
}
