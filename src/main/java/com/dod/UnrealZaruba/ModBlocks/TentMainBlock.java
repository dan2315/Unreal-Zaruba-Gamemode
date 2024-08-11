package com.dod.UnrealZaruba.ModBlocks;

import com.dod.UnrealZaruba.Commands.Arguments.TeamColor;
import com.dod.UnrealZaruba.Gamemodes.BaseGamemode;
import com.dod.UnrealZaruba.Utils.Explosions;

import com.dod.UnrealZaruba.Utils.Explosions;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Material;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.Objects;

public class TentMainBlock extends Block {

    public TeamColor teamColor = TeamColor.UNDEFINED;

    public TentMainBlock(TeamColor teamColor) {
        super(BlockBehaviour.Properties.of(Material.METAL)
        .strength(2.0f, 3.0f)
        .sound(SoundType.METAL));
        this.teamColor = teamColor;
    }


    /**
     * Рассматривает кейс врыва блока ядра палатки
     *
     * @param state
     * @param level The current level
     * @param pos Block position in level
     * @param explosion The explosion instance affecting the block
     */
   @Override
   public void onBlockExploded(BlockState state, Level level, BlockPos pos, Explosion explosion) {
        sendGlobalMessage(ServerLifecycleHooks.getCurrentServer(), new TextComponent("§4Палатка команды " + BaseGamemode.currentGamemode.TeamManager.Get(teamColor).color.toString() + " была разрушена!"));
        BaseGamemode.currentGamemode.TeamManager.Get(teamColor).setActiveTent(null);
//       Explosions.createExplosionNoPlayerDamage(level, null, pos.offset(0, 4, 0), 6.0F);
       super.onBlockExploded(state, level, pos, explosion);
   }

    /**
     * Рассматривает кейс уничтожения блока ядра палатки игроком
     *
     * @param state The current state.
     * @param level The current level
     * @param pos Block position in level
     * @param player The player damaging the block, may be null
     * @param willHarvest True if Block.harvestBlock will be called after this, if the return in true.
     *        Can be useful to delay the destruction of tile entities till after harvestBlock
     * @param fluid The current fluid state at current position
     * @return
     */
   @Override
   public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player, boolean willHarvest, FluidState fluid) {
       sendGlobalMessage(ServerLifecycleHooks.getCurrentServer(), new TextComponent("§4Палатка команды " + BaseGamemode.currentGamemode.TeamManager.Get(teamColor).color.toString() + " была разрушена!"));
        BaseGamemode.currentGamemode.TeamManager.Get(teamColor).setActiveTent(null);
//       Explosions.createExplosionNoPlayerDamage(level, null, pos.offset(0, 4, 0), 6.0F);
       return super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid);
   }

    /**
     * Вызывает ошибку NullException
     * НЕ ЮЗАТЬ ДО РЕВОРКА И ПЕРЕНОСА В Utils
     *
     * @throws NullPointerException
     * @param server
     * @param message
     */
    public void sendGlobalMessage(MinecraftServer server, TextComponent message) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.sendMessage(message, player.getUUID());
        }
    }
}
