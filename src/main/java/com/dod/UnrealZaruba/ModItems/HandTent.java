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
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;

public class HandTent extends Item {

    public HandTent(Properties properties) {
        super(properties);
    }


    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        UnrealZaruba.LOGGER.info("[Ох, бля] Юзаюсь");
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        UnrealZaruba.LOGGER.info("[Ох, бля] Длюсь");
        return 100;  // 5 seconds (100 ticks)
    }

    // 2. Override to set the use animation to the bow's animation
    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        UnrealZaruba.LOGGER.info("[Ох, бля] Анимируюсь");
        return UseAnim.BOW;  // Use bow animation
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level world, LivingEntity livingEntity) {
        // Perform an action after the use duration completes (e.g., shoot a projectile, apply an effect, etc.)
        // For example, give the player a potion effect:
        // player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, 1));

        // This will be called after the 5-second duration
        UnrealZaruba.LOGGER.info("[Ох, бля] финиширую");
        if (!world.isClientSide) {
            if (livingEntity instanceof Player player) {

                UnrealZaruba.LOGGER.info("[Ох, бля] LivingEntity instanceof Player");
                TeamGamemode gamemode = PlayerU.Get(player.getUUID()).Gamemode(TeamGamemode.class);
                if (gamemode.GetTeamManager().GetPlayersTeam(player).active_tent == null) {
                    UnrealZaruba.LOGGER.info("[Ох, бля] Стакаю");
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

//    @Override
//    public InteractionResult useOn(UseOnContext context) {
//        if (!context.getLevel().isClientSide) {
//            Player player = context.getPlayer();
//
//            TeamGamemode gamemode = PlayerU.Get(player.getUUID()).Gamemode(TeamGamemode.class);
//            if (gamemode.GetTeamManager().GetPlayersTeam(context.getPlayer()).active_tent == null) {
//                ItemStack item = context.getItemInHand();
//                item.setCount(item.getCount() - 1);
//                ServerLevel serverLevel = (ServerLevel) context.getLevel();
//                placeCustomStructure(serverLevel, context.getClickedPos(), context.getPlayer());
//                return InteractionResult.SUCCESS;
//            } else {
//                player.sendMessage(
//                        new TextComponent("Вы не можете установить вторую палатку, когда первая все еще существует"),
//                        player.getUUID());
//            }
//            return InteractionResult.SUCCESS;
//        }
//        return InteractionResult.SUCCESS;
//    }

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
        UnrealZaruba.LOGGER.info("[Ох, бля] Читаю NBT");
        BaseGamemode gamemode = GamemodeManager.Get(world);
        TeamManager teamManager = ((TeamGamemode) gamemode).GetTeamManager();

        TeamU player_team = teamManager.GetPlayersTeam(player);

        BlockPos center = clickPos;
        BlockPos buildPoint = clickPos.offset(-4, -1, -4);

        Tent tent = new Tent(center);
        player_team.setActiveTent(tent);

        UnrealZaruba.LOGGER.info("[Ох, бля] Начинаю ставить");

        teamManager.tent_templates.get(player_team.color).placeInWorld(world, buildPoint, buildPoint,
                new StructurePlaceSettings(), world.random, 2);

        UnrealZaruba.LOGGER.info("[Ох, бля] Поставил ёпта");
    }
}
