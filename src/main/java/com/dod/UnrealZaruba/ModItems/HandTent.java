package com.dod.UnrealZaruba.ModItems;

import com.dod.UnrealZaruba.Gamemodes.BaseGamemode;
import com.dod.UnrealZaruba.ModBlocks.Teams.Tent;
import com.dod.UnrealZaruba.TeamLogic.TeamU;
import com.dod.UnrealZaruba.unrealzaruba;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
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
        if (!context.getLevel().isClientSide) {
            unrealzaruba.LOGGER.info("Щелкнуло!");
            ServerLevel serverLevel = (ServerLevel) context.getLevel();
            placeCustomStructure(serverLevel, context.getClickedPos(), Objects.requireNonNull(context.getPlayer()));
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;

    }

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

//        if (player_team.color == TeamColor.RED) {
//            .placeInWorld(world, clickPos.offset(-4, -1, -4), clickPos.offset(-4, -1, -4), new StructurePlaceSettings(), world.random, 2);
//
//            unrealzaruba.LOGGER.info("[Ох, бля] Закончил ставить палатку команды RED");
//            player.sendMessage(new TextComponent("Успешно."), player.getUUID());
//
//        } else if (BaseGamemode.currentGamemode.TeamManager.GetPlayersTeam(player).color == TeamColor.BLUE){
//            template_blue.placeInWorld(world, clickPos.offset(-4, -1, -4), clickPos.offset(-4, -1, -4), new StructurePlaceSettings(), world.random, 2);
//
//            player.sendMessage(new TextComponent("Успешно."), player.getUUID());
//            unrealzaruba.LOGGER.info("[Ох, бля] Закончил ставить палатку команды BLUE");
//
//        } else {
//            player.sendMessage(new TextComponent("Ты не находишься ни в одной команде."), player.getUUID());
//            unrealzaruba.LOGGER.warn("[Ай, бля] Ошибка. Игрок {} не находится ни в одной команде", player.getName().getString());
//        }

//        for (StructureTemplate.StructureBlockInfo blockInfo : blockInfos) {
//            BlockPos blockPos = startPos.offset(blockInfo.pos);
//
//            BlockState blockState = blockInfo.state;
//
//            // Example condition: only place stone blocks, or modify the state before placing
//            if (blockState.getBlock() == Blocks.STONE) {
//                world.setBlock(blockPos, blockState, 3); // Place block with flags (3 means notify neighbors and update)
//            } else if (blockState.getBlock() == Blocks.DIAMOND_BLOCK) {
//                // Modify blockState before placing, e.g., replace diamond blocks with gold blocks
//                BlockState newState = Blocks.GOLD_BLOCK.defaultBlockState();
//                world.setBlock(blockPos, newState, 3);
//            } else {
//                unrealzaruba.LOGGER.info("[Ох, бля] Поставил " + blockInfo);
//                // Place the block as is
//                world.setBlock(blockPos, blockState, 3);
//            }
//        }
    }
}
