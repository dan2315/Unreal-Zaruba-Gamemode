package com.dod.unrealzaruba.Economy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.dod.unrealzaruba.Commands.Arguments.TeamColor;
import com.dod.unrealzaruba.Gamemodes.BaseGamemode;
import com.dod.unrealzaruba.Gamemodes.GamemodeManager;
import com.dod.unrealzaruba.Gamemodes.TeamGamemode;
import com.dod.unrealzaruba.TeamLogic.TeamContext;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Money that doesn't come from the objectives: income every interval during the battle (more for the team that's
 * behind), bounties on players with kill streaks, and supply drops ({@link SupplyDrops}).
 */
public final class SideIncome {
    private static final Map<UUID, Integer> STREAKS = new HashMap<>();
    private static int battleTicks;

    private SideIncome() {}

    /** New match. */
    public static void reset() {
        STREAKS.clear();
        battleTicks = 0;
        SupplyDrops.reset();
    }

    public static void tick(MinecraftServer server) {
        BaseGamemode gamemode = GamemodeManager.instance.GetActiveGamemode();
        if (!(gamemode instanceof TeamGamemode teamGamemode) || !gamemode.IsBattleActive()) {
            SupplyCases.idle();
            return;
        }
        EconomyConfig config = EconomyData.current();
        battleTicks++;

        EconomyConfig.PassiveIncome income = config.passiveIncome;
        if (income != null && income.intervalSeconds > 0 && income.amount > 0 && battleTicks % (income.intervalSeconds * 20) == 0) {
            payIncome(server, teamGamemode, income);
        }
        SupplyDrops.tick(server, config.supplyDrops, battleTicks);
        SupplyCases.tick(server, config.supplyDrops, battleTicks);
    }

    private static void payIncome(MinecraftServer server, TeamGamemode gamemode, EconomyConfig.PassiveIncome income) {
        Map<TeamColor, TeamContext> teams = gamemode.GetTeamManager().GetTeams();
        int leaderScore = Integer.MIN_VALUE;
        for (TeamColor color : teams.keySet()) {
            Integer score = gamemode.GetTeamScore(color);
            if (score != null) leaderScore = Math.max(leaderScore, score);
        }
        int grown = income.growthEverySeconds > 0 ? (battleTicks / 20 / income.growthEverySeconds) * Math.max(0, income.growthAmount) : 0;
        int base = income.amount + grown;
        for (Map.Entry<TeamColor, TeamContext> team : teams.entrySet()) {
            Integer score = gamemode.GetTeamScore(team.getKey());
            int catchUp = score == null || leaderScore == Integer.MIN_VALUE ? 0
                    : Math.min(income.maxCatchUp, Math.max(0, leaderScore - score) * income.catchUpPerPoint);
            Component message = catchUp > 0
                    ? Component.translatable("economy.unrealzaruba.income_catch_up", Money.format(base), Money.format(catchUp))
                    : Component.translatable("economy.unrealzaruba.income", Money.format(base));
            for (ServerPlayer player : onlineMembers(server, team.getValue())) {
                Wallets.add(player, base + catchUp);
                player.displayClientMessage(message, true);
            }
        }
    }

    /**
     * Called for every death in a match. {@code enemyKiller} is the killing player when they're on another team,
     * otherwise null (suicide, team kill, environment): the streak ends but nobody collects.
     */
    public static void onDeath(ServerPlayer victim, ServerPlayer enemyKiller, MinecraftServer server) {
        EconomyConfig.Bounties bounties = EconomyData.current().bounties;
        int victimStreak = STREAKS.getOrDefault(victim.getUUID(), 0);
        STREAKS.remove(victim.getUUID());
        if (enemyKiller == null || bounties == null) return;

        int bounty = bountyFor(victimStreak, bounties);
        if (bounty > 0) {
            Wallets.add(enemyKiller, bounty);
            broadcast(server, Component.translatable("economy.unrealzaruba.bounty_claimed",
                    enemyKiller.getDisplayName(), Money.format(bounty), victim.getDisplayName()));
        }

        int streak = STREAKS.merge(enemyKiller.getUUID(), 1, Integer::sum);
        int newBounty = bountyFor(streak, bounties);
        if (newBounty > 0) {
            broadcast(server, Component.translatable("economy.unrealzaruba.bounty_placed",
                    enemyKiller.getDisplayName(), streak, Money.format(newBounty)));
        }
    }

    private static int bountyFor(int streak, EconomyConfig.Bounties bounties) {
        if (bounties.killStreak <= 0 || streak < bounties.killStreak) return 0;
        return Math.min(bounties.maxAmount, (streak - bounties.killStreak + 1) * bounties.amountPerKill);
    }

    static List<ServerPlayer> onlineMembers(MinecraftServer server, TeamContext team) {
        return team.Members().stream()
                .map(id -> server.getPlayerList().getPlayer(id))
                .filter(player -> player != null)
                .toList();
    }

    static void broadcast(MinecraftServer server, Component message) {
        server.getPlayerList().getPlayers().forEach(player -> player.sendSystemMessage(message));
    }
}
