package com.dod.UnrealZaruba.ModItems;

import com.dod.UnrealZaruba.Gamemodes.BaseGamemode;
import com.dod.UnrealZaruba.ModBlocks.Teams.Tent;
import com.dod.UnrealZaruba.TeamLogic.TeamU;
import com.dod.UnrealZaruba.unrealzaruba;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import java.util.Objects;

import javax.annotation.Nonnull;


public class HandTent extends Item {

    public HandTent(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(@Nonnull UseOnContext context) {
        unrealzaruba.LOGGER.info("Начало");
        if (!(BaseGamemode.currentGamemode.TeamManager.GetPlayersTeam(context.getPlayer()).active_tent == null)) {
            ServerLevel serverLevel = (ServerLevel) context.getLevel();
            placeCustomStructure(serverLevel, context.getClickedPos(), context.getPlayer());
            return InteractionResult.SUCCESS;
        } else {
            context.getPlayer().sendMessage(new TextComponent("Вы не можете установить вторую палатку, когда первая все еще существует"), context.getPlayer().getUUID());
        }
        return InteractionResult.PASS;
    }

    /**
     * Не для использования!
     * <p>Юзается в UseOn</p>
     *
     * @param world
     * @param clickPos
     * @param player
     */
    public void placeCustomStructure(ServerLevel world, BlockPos clickPos, Player player) {
        unrealzaruba.LOGGER.info("[Ох, бля] Читаю NBT");

        TeamU player_team = BaseGamemode.currentGamemode.TeamManager.GetPlayersTeam(player);

        BlockPos center = clickPos;
        BlockPos buildPoint = clickPos.offset(-4, -1, -4);

        Tent tent = new Tent(center);
        player_team.setActiveTent(tent);

        StructurePlaceSettings settings = new StructurePlaceSettings();
        unrealzaruba.LOGGER.info("[Ох, бля] Начинаю ставить");

        BaseGamemode.currentGamemode.TeamManager.tent_templates.get(player_team.color).placeInWorld(world, buildPoint, buildPoint, new StructurePlaceSettings(), world.random, 2);
        unrealzaruba.LOGGER.info("[Ох, бля] Поставил ёпта");
    }
}
