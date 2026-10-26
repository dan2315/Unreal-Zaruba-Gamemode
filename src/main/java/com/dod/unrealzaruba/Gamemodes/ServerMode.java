package com.dod.unrealzaruba.Gamemodes;

import com.dod.unrealzaruba.Config.MainConfig;
import com.dod.unrealzaruba.ConfigurationManager.ConfigManager;
import com.dod.unrealzaruba.Lobby.LobbyService;
import com.dod.unrealzaruba.NetworkPackets.ClientboundRemoveObjectivesPacket;
import com.dod.unrealzaruba.UnrealZaruba;
import com.dod.unrealzaruba.WorldManager.GameMaps;
import com.dod.unrealzaruba.WorldManager.WorldManager;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Switches the server between DEV (maps are edited in place) and GAME (matches on throwaway copies) without a
 * restart; the choice is kept in mainConfig.json.
 * <ul>
 * <li>To GAME: the open map is saved (saving is off in GAME), everyone goes to the lobby empty-handed in adventure
 * mode, as on joining in GAME, and a match starts on that map; with no map open, the vote starts instead.</li>
 * <li>To DEV: a running match is called off, everyone goes to the lobby (ops in creative), and the map it was on
 * opens for editing.</li>
 * </ul>
 */
public final class ServerMode {
    private ServerMode() {}

    public static Component toGame(MinecraftServer server) {
        if (!ConfigManager.isDevMode()) return Component.literal("The server is already in GAME mode");
        String mapKey = WorldManager.gameLevel != null ? WorldManager.currentMapKey() : null;

        // Everything is written while DEV still allows saving; closing the map saves it once more
        server.saveEverything(false, true, true);
        dropGamemode();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            WorldManager.teleportPlayerToDimension(player, WorldManager.LOBBY_DIMENSION, MainConfig.getInstance().getLobbySpawnPoint());
            player.setGameMode(GameType.ADVENTURE);
            player.getInventory().clearContent();
        }
        WorldManager.deleteGameWorld();
        ConfigManager.setGameMode();

        GameMaps.GameMap map = GameMaps.get(mapKey);
        if (map != null) {
            GamemodeManager.instance.StartGame(map);
        } else {
            LobbyService.broadcast();
        }
        UnrealZaruba.LOGGER.info("[UnrealZaruba] Switched to GAME mode{}", map == null ? ", vote started" : ", match on " + map.key());
        return Component.literal("GAME mode: " + (map == null ? "the vote is on" : "a match starts on " + map.key()));
    }

    public static Component toDev(MinecraftServer server) {
        if (ConfigManager.isDevMode()) return Component.literal("The server is already in DEV mode");
        BaseGamemode gamemode = GamemodeManager.instance.GetActiveGamemode();
        String mapKey = gamemode != null ? gamemode.MapKey() : WorldManager.currentMapKey();

        if (GamemodeFactory.timer != null) GamemodeFactory.timer.stop();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            PacketDistributor.sendToPlayer(player, new ClientboundRemoveObjectivesPacket());
            WorldManager.teleportPlayerToDimension(player, WorldManager.LOBBY_DIMENSION, MainConfig.getInstance().getLobbySpawnPoint());
            player.setGameMode(player.hasPermissions(2) ? GameType.CREATIVE : GameType.ADVENTURE);
        }
        dropGamemode();
        // The match's ships go while GAME still says the level is a throwaway copy; it is closed by the reload below
        WorldManager.clearShipsInDimension(WorldManager.gameLevel);
        ConfigManager.setDevMode();

        GameMaps.GameMap map = GameMaps.get(mapKey);
        if (map != null) {
            GameMaps.select(map.key());
            WorldManager.ReloadGameWorldDelayed(map.key());
        } else {
            WorldManager.DisposeGameWorld();
        }
        LobbyService.broadcast();
        UnrealZaruba.LOGGER.info("[UnrealZaruba] Switched to DEV mode{}", map == null ? "" : ", opening " + map.key());
        return Component.literal("DEV mode" + (map == null ? ": open a map with /openmap" : ": opening " + map.key() + " for editing"));
    }

    /** The mode set up now (a DEV one has no match phases, a GAME match is called off) goes; voting is on again. */
    private static void dropGamemode() {
        BaseGamemode gamemode = GamemodeManager.instance.GetActiveGamemode();
        if (gamemode == null) return;
        try {
            gamemode.Cleanup();
        } catch (RuntimeException e) {
            UnrealZaruba.LOGGER.warn("[UnrealZaruba] Cleaning up the mode on a server mode switch failed", e);
        }
        GamemodeManager.instance.CleanupCurrentGamemode();
    }
}
