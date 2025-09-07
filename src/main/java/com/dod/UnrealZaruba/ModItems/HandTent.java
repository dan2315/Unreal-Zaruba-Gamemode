package com.dod.UnrealZaruba.ModItems;

import com.dod.UnrealZaruba.Gamemodes.BaseGamemode;
import com.dod.UnrealZaruba.Gamemodes.GamemodeManager;
import com.dod.UnrealZaruba.Gamemodes.TeamGamemode;
import com.dod.UnrealZaruba.ModBlocks.Tent.Tent;
import com.dod.UnrealZaruba.Player.PlayerContext;
import com.dod.UnrealZaruba.TeamLogic.TeamManager;
import com.dod.UnrealZaruba.TeamLogic.TeamContext;
import com.dod.UnrealZaruba.UnrealZaruba;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;

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

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level world, LivingEntity livingEntity) {
        // Perform an action after the use duration completes (e.g., shoot a projectile, apply an effect, etc.)
        // For example, give the player a potion effect:
        // player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, 1));

        // This will be called after the 5-second duration
        if (!world.isClientSide) {
            if (livingEntity instanceof Player player) {
                TeamGamemode gamemode = PlayerContext.Get(player.getUUID()).Gamemode(TeamGamemode.class);
                TeamContext playerTeam = gamemode.GetTeamManager().GetPlayersTeam(player);
                if (playerTeam.RespawnPoints().stream().noneMatch(respawnPoint -> respawnPoint instanceof Tent)) {
                    ServerLevel serverLevel = (ServerLevel) world;
                    placeCustomStructure(serverLevel, new BlockPos((int)player.getX(), (int)player.getY(), (int)player.getZ()), player);
                    stack.setCount(stack.getCount() - 1);
                    return stack;
                } else {
                    player.sendSystemMessage(Component.literal("Вы не можете установить вторую палатку, когда первая все еще существует"));
                }
            }
        }
        UnrealZaruba.LOGGER.info("[Ох, бля] Вышел нахуй из комнаты");
        return stack;
    }

    /**
     * @implSpec HandTent.java
     * @param world - ServerLevel
     * @param clickPos - BlockPos
     * @param player - Player
     */
    public void placeCustomStructure(ServerLevel world, BlockPos clickPos, Player player) {
        UnrealZaruba.LOGGER.info("[Ох, бля] Читаю NBT");
        BaseGamemode gamemode = GamemodeManager.instance.GetActiveGamemode();
        TeamManager teamManager = ((TeamGamemode) gamemode).GetTeamManager();

        TeamContext team = teamManager.GetPlayersTeam(player);

        BlockPos center = clickPos;
        BlockPos buildPoint = clickPos.offset(-4, -2, -4);

        Tent tent = new Tent(center);
        team.AddRespawnPoint(tent);

        UnrealZaruba.LOGGER.info("[Ох, бля] Начинаю ставить");


        //TODO: Вот тут рабочий вариант поблочного выставления, но нужно сетапить копикаты и правильно считать позицию
        // List<StructureTemplate.Palette> palettes = null;
        // try {
        //     Field palettesField = StructureTemplate.class.getDeclaredField("f_74482_");
        //     palettesField.setAccessible(true);
        //     palettes = (List<StructureTemplate.Palette>) palettesField.get(template);
        // } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
        //     e.printStackTrace();
        // } 

        // List<StructureBlockInfo> blockInfos = settings.getRandomPalette(palettes, buildPoint).blocks(); 
        teamManager.tent_templates.get(team.Color()).placeInWorld(world, buildPoint, buildPoint,
                new StructurePlaceSettings(), world.random, 2);

        UnrealZaruba.LOGGER.info("[Ох, бля] Поставил ёпта");
    }
}
