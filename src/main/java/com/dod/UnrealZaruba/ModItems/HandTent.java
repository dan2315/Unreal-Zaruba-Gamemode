package com.dod.UnrealZaruba.ModItems;

import com.dod.UnrealZaruba.Commands.Arguments.TeamColor;
import com.dod.UnrealZaruba.Gamemodes.BaseGamemode;
import com.dod.UnrealZaruba.ModBlocks.ModBlocks;
import com.dod.UnrealZaruba.ModBlocks.Teams.Tent;
import com.dod.UnrealZaruba.TeamLogic.TeamManager;
import com.dod.UnrealZaruba.TeamLogic.TeamU;
import com.dod.UnrealZaruba.unrealzaruba;
import com.fasterxml.jackson.databind.ser.Serializers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureManager;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.logging.Level;

public class HandTent extends Item {

    public HandTent(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        unrealzaruba.LOGGER.info("Начало");
        if (!context.getLevel().isClientSide) {
            if (!(TeamU.tent_Spawns.get(context.getClickedPos()) == BaseGamemode.currentGamemode.TeamManager.GetPlayersTeam(context.getPlayer()).color)) {
                unrealzaruba.LOGGER.info("Щелкнуло!");
                ServerLevel serverLevel = (ServerLevel) context.getLevel();
                placeCustomStructure(serverLevel, context.getClickedPos(), Objects.requireNonNull(context.getPlayer()));
                return InteractionResult.SUCCESS;
            }
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
