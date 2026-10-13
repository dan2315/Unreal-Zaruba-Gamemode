package com.dod.unrealzaruba.ModItems;

import java.util.List;

import com.dod.unrealzaruba.Pricing.ContraptionInspector;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * Use it on a Sable contraption to see its size, price and what stops it from being published; the panel stays and keeps
 * updating (whatever is in your hand) until you Shift-click with the scanner. The work is done by
 * {@link ContraptionInspector}. The tooltip reads like Create's: a hint to hold Shift, then the summary.
 */
public class ContraptionScannerItem extends Item {
    private static final String KEY = "item.unrealzaruba.contraption_scanner.tooltip.";

    public ContraptionScannerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getLevel().isClientSide) return InteractionResult.SUCCESS;
        if (context.getPlayer() instanceof ServerPlayer player) {
            if (player.isShiftKeyDown()) {
                ContraptionInspector.stop(player);
            } else {
                ContraptionInspector.inspect(player, context.getClickedPos());
            }
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        // Shift-click on nothing resets too, so it works without aiming at anything
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
                ContraptionInspector.stop(serverPlayer);
            }
            return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
        }
        return InteractionResultHolder.pass(player.getItemInHand(hand));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (!ScannerTooltipKeys.shiftDown()) {
            tooltip.add(Component.translatable("tooltip.unrealzaruba.hold_shift",
                    Component.literal("[Shift]").withStyle(ChatFormatting.GRAY)).withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        tooltip.add(line(1, ChatFormatting.GRAY));
        tooltip.add(line(2, ChatFormatting.GRAY));
        tooltip.add(Component.empty());

        tooltip.add(Component.translatable(KEY + "condition1").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal(" ").append(Component.translatable(KEY + "behaviour1")).withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal(" ").append(Component.translatable(KEY + "behaviour1b")).withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable(KEY + "condition2").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal(" ").append(Component.translatable(KEY + "behaviour2")).withStyle(ChatFormatting.GOLD));
    }

    private static Component line(int number, ChatFormatting colour) {
        return Component.translatable(KEY + "summary" + number).withStyle(colour);
    }
}
