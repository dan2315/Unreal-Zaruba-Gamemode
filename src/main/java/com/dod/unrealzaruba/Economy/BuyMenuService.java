package com.dod.unrealzaruba.Economy;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.dod.unrealzaruba.Gamemodes.GamemodeManager;
import com.dod.unrealzaruba.NetworkPackets.Economy.BuyResultPacket;
import com.dod.unrealzaruba.NetworkPackets.Economy.OpenBuyMenuPacket;
import com.dod.unrealzaruba.Player.PlayerContext;
import com.dod.unrealzaruba.Player.TeamPlayerContext;
import com.dod.unrealzaruba.UnrealZaruba;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Server side of the buy menu. The client only says which entries it wants; prices and items always come from the
 * map's config here.
 */
public final class BuyMenuService {
    private BuyMenuService() {}

    public static void open(ServerPlayer player) {
        Component reason = whyCantBuy(player);
        if (reason != null) {
            player.displayClientMessage(reason, true);
            return;
        }
        sendView(player);
        Wallets.sync(player);
    }

    /** The menu as this player sees it: items for their team, fit marks for the guns they carry now. */
    private static void sendView(ServerPlayer player) {
        java.util.function.Predicate<ItemStack> fits = TaczCompat.fitsGunsOf(player);
        PacketDistributor.sendToPlayer(player, new OpenBuyMenuPacket(
                BuyMenuView.of(EconomyData.current(), item -> resolve(player, item), fits)));
    }

    /** {@code items}: entry indices in the category. A grid tab buys one entry, a sets tab any of the set's pieces. */
    public static void buy(ServerPlayer player, int tab, int group, int category, List<Integer> items) {
        Component reason = whyCantBuy(player);
        if (reason != null) {
            reply(player, false, reason);
            return;
        }

        EconomyConfig.Category configCategory = findCategory(EconomyData.current(), tab, group, category);
        if (configCategory == null || items.isEmpty()) {
            reply(player, false, Component.translatable("buymenu.unrealzaruba.error.unknown_item"));
            return;
        }
        boolean sets = EconomyConfig.Layout.SETS.equals(EconomyData.current().buyMenu.get(tab).layout);
        if (!sets && items.size() > 1) {
            reply(player, false, Component.translatable("buymenu.unrealzaruba.error.unknown_item"));
            return;
        }

        Set<Integer> unique = new LinkedHashSet<>(items);
        List<ItemStack> stacks = new ArrayList<>();
        int total = 0;
        for (int index : unique) {
            if (index < 0 || index >= configCategory.items.size()) {
                reply(player, false, Component.translatable("buymenu.unrealzaruba.error.unknown_item"));
                return;
            }
            EconomyConfig.Item item = configCategory.items.get(index);
            ItemStack stack = resolve(player, item);
            if (stack.isEmpty()) {
                reply(player, false, Component.translatable("buymenu.unrealzaruba.error.unknown_item"));
                return;
            }
            stacks.add(stack);
            total += Math.max(0, item.price);
        }

        if (!Wallets.trySpend(player, total)) {
            reply(player, false, Component.translatable("buymenu.unrealzaruba.error.no_money"));
            return;
        }
        stacks.forEach(stack -> give(player, stack));
        reply(player, true, Component.translatable("buymenu.unrealzaruba.bought", Money.format(total)));
        sendView(player); // a new gun changes what fits
    }

    /** Null when the player may buy right now. */
    private static Component whyCantBuy(ServerPlayer player) {
        if (!EconomyData.current().buyMenuEnabled || GamemodeManager.instance.GetActiveGamemode() == null) {
            return Component.translatable("buymenu.unrealzaruba.error.no_match");
        }
        PlayerContext context = PlayerContext.Get(player.getUUID());
        if (!(context instanceof TeamPlayerContext teamContext) || teamContext.Team() == null) {
            return Component.translatable("buymenu.unrealzaruba.error.no_team");
        }
        if (player.gameMode.getGameModeForPlayer() == GameType.SPECTATOR || player.isDeadOrDying()) {
            return Component.translatable("buymenu.unrealzaruba.error.dead");
        }
        if (EconomyData.current().buyOnlyInRespawnZones && !TeamZones.isInOwnZone(player)) {
            return Component.translatable("buymenu.unrealzaruba.error.not_in_zone");
        }
        return null;
    }

    private static EconomyConfig.Category findCategory(EconomyConfig config, int tab, int group, int category) {
        if (tab < 0 || tab >= config.buyMenu.size()) return null;
        var groups = config.buyMenu.get(tab).groups;
        if (groups == null || group < 0 || group >= groups.size()) return null;
        var categories = groups.get(group).categories;
        if (categories == null || category < 0 || category >= categories.size()) return null;
        var found = categories.get(category);
        return found.items != null ? found : null;
    }

    /** The item for the player's team, or empty if the config string doesn't parse. */
    private static ItemStack resolve(ServerPlayer player, EconomyConfig.Item item) {
        String spec = item.item;
        PlayerContext context = PlayerContext.Get(player.getUUID());
        if (item.teamItems != null && context instanceof TeamPlayerContext teamContext && teamContext.Team() != null) {
            spec = item.teamItems.getOrDefault(teamContext.Team().Color().name(), spec);
        }
        return ItemSpecs.parse(player.server.registryAccess(), spec, item.count);
    }

    /** Armor goes straight into an empty armor slot, everything else into the inventory (or at the feet if full). */
    private static void give(ServerPlayer player, ItemStack stack) {
        EquipmentSlot slot = player.getEquipmentSlotForItem(stack);
        if (slot.isArmor() && player.getItemBySlot(slot).isEmpty()) {
            player.setItemSlot(slot, stack);
            return;
        }
        player.getInventory().add(stack);
        if (!stack.isEmpty()) {
            player.drop(stack, false);
        }
    }

    private static void reply(ServerPlayer player, boolean success, Component message) {
        PacketDistributor.sendToPlayer(player, new BuyResultPacket(success, message));
    }
}
