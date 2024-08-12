package com.dod.UnrealZaruba.ModItems;

import com.dod.UnrealZaruba.Gamemodes.BaseGamemode;
import com.dod.UnrealZaruba.Gamemodes.GamemodeManager;
import com.dod.UnrealZaruba.Gamemodes.TeamGamemode;
import com.dod.UnrealZaruba.ModBlocks.Teams.Tent;
import com.dod.UnrealZaruba.Player.PlayerU;
import com.dod.UnrealZaruba.TeamLogic.TeamManager;
import com.dod.UnrealZaruba.TeamLogic.TeamU;
import com.dod.UnrealZaruba.UnrealZaruba;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.util.List;

public class HandTent extends Item {

    public HandTent(Properties properties) {
        super(properties);
    }


    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 100;  // 5 seconds (100 ticks)
    }

    // 2. Override to set the use animation to the bow's animation
    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;  // Use bow animation
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level world, LivingEntity livingEntity) {
        // Perform an action after the use duration completes (e.g., shoot a projectile, apply an effect, etc.)
        // For example, give the player a potion effect:
        // player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, 1));

        // This will be called after the 5-second duration
        if (!world.isClientSide) {
            if (livingEntity instanceof Player player) {
                TeamGamemode gamemode = PlayerU.Get(player.getUUID()).Gamemode(TeamGamemode.class);
                if (gamemode.GetTeamManager().GetPlayersTeam(player).active_tent == null) {
                    ServerLevel serverLevel = (ServerLevel) world;
                    placeCustomStructure(serverLevel, new BlockPos(player.position()), player);
                    stack.setCount(stack.getCount() - 1);
                    return stack;
                } else {
                    player.sendMessage(
                            new TextComponent("Вы не можете установить вторую палатку, когда первая все еще существует"),
                            player.getUUID());
                }
            }
        }
        UnrealZaruba.LOGGER.info("[Ох, бля] Вышел нахуй из комнаты");
        return stack;
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
//    public void placeCustomStructure(ServerLevel world, BlockPos clickPos, Player player) {
//        UnrealZaruba.LOGGER.info("[Ох, бля] Читаю NBT");
//        BaseGamemode gamemode = GamemodeManager.Get(world);
//        TeamManager teamManager = ((TeamGamemode) gamemode).GetTeamManager();
//
//        TeamU player_team = teamManager.GetPlayersTeam(player);
//
//        BlockPos center = clickPos;
//        BlockPos buildPoint = clickPos.offset(-4, -2, -4);
//
//        Tent tent = new Tent(center);
//        player_team.setActiveTent(tent);
//
//        UnrealZaruba.LOGGER.info("[Ох, бля] Начинаю ставить");
//
//        teamManager.tent_templates.get(player_team.color).placeInWorld(world, buildPoint, buildPoint,
//                new StructurePlaceSettings(), world.random, 2);
//
//        UnrealZaruba.LOGGER.info("[Ох, бля] Поставил ёпта");
//    }

    public void placeCustomStructure(ServerLevel world, BlockPos pos, Player player) {
        BaseGamemode gamemode = GamemodeManager.Get(world);
        TeamManager teamManager = ((TeamGamemode) gamemode).GetTeamManager();
        TeamU player_team = teamManager.GetPlayersTeam(player);

        BlockPos center = pos;
        BlockPos buildPoint = pos.offset(-4, -2, -4);

        Tent tent = new Tent(center);
        player_team.setActiveTent(tent);

        StructureTemplate template = teamManager.tent_templates.get(player_team.color);
        StructurePlaceSettings settings = new StructurePlaceSettings();

        UnrealZaruba.LOGGER.info("[Ох, бля] Читаю NBT");
//        List<StructureTemplate.StructureBlockInfo> blocks = template.
//        UnrealZaruba.LOGGER.info("[Ох, бля] Number of blocks to place:" + blocks.size());

        // Iterate over the blocks and place them in the world
        for (StructureTemplate.StructureBlockInfo blockInfo : blocks) {
            BlockPos blockPos = blockInfo.pos; // Position of the block relative to the structure's origin
            BlockState blockState = blockInfo.state; // The block state to place

            // Calculate the absolute position in the world
            BlockPos absolutePos = pos.offset(blockPos);


            // Place the block in the world
            world.setBlock(absolutePos, blockState, 3); // 3 = Block update flags
            UnrealZaruba.LOGGER.info("[Ох, бля] Placing block:" + blockState.getBlock().getName().getString() + " at " + absolutePos);
        }
        UnrealZaruba.LOGGER.info("[Ох, бля] Поставил ёпта");
    }
}
