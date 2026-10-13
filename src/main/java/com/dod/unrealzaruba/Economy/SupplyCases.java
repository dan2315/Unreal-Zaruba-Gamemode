package com.dod.unrealzaruba.Economy;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.dod.unrealzaruba.Commands.Arguments.TeamColor;
import com.dod.unrealzaruba.ModItems.ModItems;
import com.dod.unrealzaruba.NetworkPackets.Economy.ClientboundSupplyCasePacket;
import com.dod.unrealzaruba.TeamLogic.TeamContext;
import com.dod.unrealzaruba.WorldManager.WorldManager;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The supply case from a paradrop is worth cash only at the carrier's own base: a player holding it inside their
 * team's zone ({@link TeamZones}) hands it in, and the team (plus a bonus for the carrier) is paid. Meanwhile
 * everyone sees a column of light over it, in the colour of the team that last held it. A case that is dropped
 * (the carrier dies) stays where it fell, with that colour, until someone picks it up.
 */
public final class SupplyCases {
    private static final int INTERVAL_TICKS = 5;
    private static final String TEAM_TAG = "Team";

    private static final Set<UUID> CARRIERS = new HashSet<>();
    private static List<ClientboundSupplyCasePacket.Entry> lastSent = List.of();

    private SupplyCases() {}

    /** Called every tick of the battle. */
    static void tick(MinecraftServer server, EconomyConfig.SupplyDrops config, int battleTicks) {
        if (battleTicks % INTERVAL_TICKS != 0) return;
        ServerLevel level = WorldManager.gameLevel;
        if (level == null) return;

        List<ClientboundSupplyCasePacket.Entry> entries = new ArrayList<>();
        Set<UUID> carriers = new HashSet<>();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.level() != level || !player.isAlive()) continue;
            List<ItemStack> cases = casesOf(player);
            if (cases.isEmpty()) continue;

            TeamContext team = TeamZones.teamOf(player);
            if (team != null && TeamZones.isInOwnZone(player)) {
                cashIn(server, player, team, config.paradrop);
                continue;
            }
            TeamColor color = team == null ? TeamColor.UNDEFINED : team.Color();
            for (ItemStack stack : cases) paint(stack, color);
            carriers.add(player.getUUID());
            if (!CARRIERS.contains(player.getUUID())) {
                SideIncome.broadcast(server, Component.translatable("supplydrop.unrealzaruba.case_picked",
                        player.getDisplayName(), color.getDisplayName()));
            }
            entries.add(new ClientboundSupplyCasePacket.Entry(player.getId(), player.getX(), player.getY(), player.getZ(), rgb(color)));
        }
        CARRIERS.clear();
        CARRIERS.addAll(carriers);

        for (ItemEntity item : level.getEntities(EntityTypeTest.forClass(ItemEntity.class), entity -> isCase(entity.getItem()))) {
            item.setUnlimitedLifetime(); // a dropped case must not despawn
            item.setInvulnerable(true);
            entries.add(new ClientboundSupplyCasePacket.Entry(item.getId(), item.getX(), item.getY(), item.getZ(),
                    rgb(teamOf(item.getItem()))));
        }
        publish(entries);
    }

    /** Nothing is going on (no battle): make sure no stale beams stay up. */
    static void idle() {
        if (!lastSent.isEmpty() || !CARRIERS.isEmpty()) reset();
    }

    public static void reset() {
        CARRIERS.clear();
        lastSent = List.of();
        if (WorldManager.server != null) {
            PacketDistributor.sendToAllPlayers(new ClientboundSupplyCasePacket(List.of()));
        }
    }

    public static void syncTo(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new ClientboundSupplyCasePacket(lastSent));
    }

    private static void publish(List<ClientboundSupplyCasePacket.Entry> entries) {
        if (entries.equals(lastSent)) return;
        lastSent = entries;
        PacketDistributor.sendToAllPlayers(new ClientboundSupplyCasePacket(entries));
    }

    private static void cashIn(MinecraftServer server, ServerPlayer player, TeamContext team, EconomyConfig.Paradrop settings) {
        int count = player.getInventory().clearOrCountMatchingItems(SupplyCases::isCase, Integer.MAX_VALUE,
                player.inventoryMenu.getCraftSlots());
        if (count <= 0) return;
        player.containerMenu.broadcastChanges();

        int teamReward = Math.max(0, settings.caseTeamReward) * count;
        int bonus = Math.max(0, settings.caseCarrierBonus) * count;
        for (ServerPlayer member : SideIncome.onlineMembers(server, team)) {
            Wallets.add(member, teamReward + (member.getUUID().equals(player.getUUID()) ? bonus : 0));
        }
        if (!SideIncome.onlineMembers(server, team).contains(player)) Wallets.add(player, bonus);
        SideIncome.broadcast(server, Component.translatable("supplydrop.unrealzaruba.case_cashed",
                player.getDisplayName(), team.Color().getDisplayName(), Money.format(teamReward), Money.format(bonus)));
    }

    private static List<ItemStack> casesOf(ServerPlayer player) {
        List<ItemStack> cases = new ArrayList<>();
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (isCase(stack)) cases.add(stack);
        }
        return cases;
    }

    public static boolean isCase(ItemStack stack) {
        return stack.is(ModItems.SUPPLY_CASE.get());
    }

    /** Remembers which team last held the case, so its beam keeps that colour after it is dropped. */
    private static void paint(ItemStack stack, TeamColor color) {
        if (color.name().equals(teamName(stack))) return;
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putString(TEAM_TAG, color.name()));
    }

    private static String teamName(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString(TEAM_TAG);
    }

    private static TeamColor teamOf(ItemStack stack) {
        return TeamColor.fromString(teamName(stack));
    }

    private static int rgb(TeamColor color) {
        return switch (color) {
            case RED -> 0xFF3B30;
            case BLUE -> 0x3B82FF;
            case YELLOW -> 0xFFD60A;
            case PURPLE -> 0xB347EA;
            default -> 0xFFFFFF;
        };
    }
}
