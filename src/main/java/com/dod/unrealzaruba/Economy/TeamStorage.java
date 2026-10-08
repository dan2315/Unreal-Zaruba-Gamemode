package com.dod.unrealzaruba.Economy;

import java.util.HashMap;
import java.util.Map;

import com.dod.unrealzaruba.Commands.Arguments.TeamColor;
import com.dod.unrealzaruba.TeamLogic.TeamContext;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.level.GameType;

/**
 * One shared chest per team for the match, opened anywhere in the team's respawn zones. Leaving the zone closes it.
 * Contents survive deaths but not the end of the match.
 */
public final class TeamStorage {
    private static final int ROWS = 6;
    private static final Map<TeamColor, SimpleContainer> STORAGES = new HashMap<>();

    private TeamStorage() {}

    public static void reset() {
        STORAGES.clear();
    }

    public static void open(ServerPlayer player) {
        TeamContext team = TeamZones.teamOf(player);
        if (team == null || !player.isAlive() || player.gameMode.getGameModeForPlayer() == GameType.SPECTATOR) {
            player.displayClientMessage(Component.translatable("teamstorage.unrealzaruba.no_team"), true);
            return;
        }
        if (!TeamZones.isInOwnZone(player)) {
            player.displayClientMessage(Component.translatable("teamstorage.unrealzaruba.not_in_zone"), true);
            return;
        }
        TeamColor color = team.Color();
        SimpleContainer storage = STORAGES.computeIfAbsent(color, c -> new SimpleContainer(ROWS * 9) {
            @Override
            public boolean stillValid(Player viewer) {
                return viewer instanceof ServerPlayer serverPlayer
                        && TeamZones.teamOf(serverPlayer) != null
                        && TeamZones.teamOf(serverPlayer).Color() == color
                        && TeamZones.isInOwnZone(serverPlayer);
            }
        });
        player.openMenu(new SimpleMenuProvider((id, inventory, p) -> ChestMenu.sixRows(id, inventory, storage),
                Component.translatable("teamstorage.unrealzaruba.title")));
    }
}
