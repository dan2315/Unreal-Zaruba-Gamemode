package com.dod.UnrealZaruba.ModItems;

import com.dod.UnrealZaruba.Gamemodes.BaseGamemode;
import com.dod.UnrealZaruba.Gamemodes.GamemodeManager;
import com.dod.UnrealZaruba.Gamemodes.TeamGamemode;
import com.dod.UnrealZaruba.ModBlocks.Teams.Tent;
import com.dod.UnrealZaruba.Player.PlayerU;
import com.dod.UnrealZaruba.TeamLogic.TeamManager;
import com.dod.UnrealZaruba.TeamLogic.TeamU;
import com.dod.UnrealZaruba.unrealzaruba;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;

import javax.annotation.Nonnull;

public class HandTent extends Item {

    public HandTent(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(@Nonnull UseOnContext context) {
        if (!context.getLevel().isClientSide) {
            Player player = context.getPlayer();

            unrealzaruba.LOGGER.info("Начало");
            TeamGamemode gamemode = PlayerU.Get(player.getUUID()).Gamemode(TeamGamemode.class);
            if (gamemode.GetTeamManager().GetPlayersTeam(context.getPlayer()).active_tent == null) {
                ServerLevel serverLevel = (ServerLevel) context.getLevel();
                placeCustomStructure(serverLevel, context.getClickedPos(), context.getPlayer());
                return InteractionResult.CONSUME;
            } else {
                player.sendMessage(
                        new TextComponent("Вы не можете установить вторую палатку, когда первая все еще существует"),
                        player.getUUID());
            }
            return InteractionResult.PASS;
        }
        return InteractionResult.FAIL;
    }

    /**
     * Не для использования!
     * <p>
     * Юзается в UseOn
     * </p>
     *
     * @param world
     * @param clickPos
     * @param player
     */
    public void placeCustomStructure(ServerLevel world, BlockPos clickPos, Player player) {
        unrealzaruba.LOGGER.info("[Ох, бля] Читаю NBT");
        BaseGamemode gamemode = GamemodeManager.Get(world);
        TeamManager teamManager = ((TeamGamemode) gamemode).GetTeamManager();

        TeamU player_team = teamManager.GetPlayersTeam(player);

        BlockPos center = clickPos;
        BlockPos buildPoint = clickPos.offset(-4, -1, -4);

        Tent tent = new Tent(center);
        player_team.setActiveTent(tent);

        unrealzaruba.LOGGER.info("[Ох, бля] Начинаю ставить");

        teamManager.tent_templates.get(player_team.color).placeInWorld(world, buildPoint, buildPoint,
                new StructurePlaceSettings(), world.random, 2);

        unrealzaruba.LOGGER.info("[Ох, бля] Поставил ёпта");
    }
}
