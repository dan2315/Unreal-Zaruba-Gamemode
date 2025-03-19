package com.dod.UnrealZaruba.Commands;

import com.dod.UnrealZaruba.Commands.Arguments.TeamColorArgument;
import com.dod.UnrealZaruba.Services.LeaderboardService;
import com.dod.UnrealZaruba.Commands.Arguments.TeamColor;
import com.dod.UnrealZaruba.Gamemodes.TeamGamemode;
import com.dod.UnrealZaruba.Gamemodes.Objectives.DestructibleObjective;
import com.dod.UnrealZaruba.Gamemodes.Objectives.DestructibleObjectivesHandler;
import com.dod.UnrealZaruba.Mobs.ClickableHumanoidEntity;
import com.dod.UnrealZaruba.Mobs.ModMobs;
import com.dod.UnrealZaruba.Player.PlayerContext;
import com.dod.UnrealZaruba.RespawnCooldown.PlayerRespawnEventHandler;
import com.dod.UnrealZaruba.SoundHandler.SoundHandler;
import com.dod.UnrealZaruba.TeamLogic.TeamManager;
import com.dod.UnrealZaruba.Title.TitleMessage;
import com.dod.UnrealZaruba.CommanderSystem.CommanderSystem;
import com.dod.UnrealZaruba.Utils.Gamerules;
import com.dod.UnrealZaruba.Utils.Utils;
import com.dod.UnrealZaruba.Utils.DataStructures.BlockVolume;
import com.dod.UnrealZaruba.WorldManager.SimpleWorldManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.*;

public class CommandRegistration {
    private static final Map<DyeColor, Item> itemMap = new HashMap<>();

    static {
        initializeItemMap();
    }

    private static void initializeItemMap() {
        itemMap.put(DyeColor.WHITE, Items.WHITE_WOOL);
        itemMap.put(DyeColor.ORANGE, Items.ORANGE_WOOL);
        itemMap.put(DyeColor.MAGENTA, Items.MAGENTA_WOOL);
        itemMap.put(DyeColor.LIGHT_BLUE, Items.LIGHT_BLUE_WOOL);
        itemMap.put(DyeColor.YELLOW, Items.YELLOW_WOOL);
        itemMap.put(DyeColor.LIME, Items.LIME_WOOL);
        itemMap.put(DyeColor.PINK, Items.PINK_WOOL);
        itemMap.put(DyeColor.GRAY, Items.GRAY_WOOL);
        itemMap.put(DyeColor.LIGHT_GRAY, Items.LIGHT_GRAY_WOOL);
        itemMap.put(DyeColor.CYAN, Items.CYAN_WOOL);
        itemMap.put(DyeColor.PURPLE, Items.PURPLE_WOOL);
        itemMap.put(DyeColor.BLUE, Items.BLUE_WOOL);
        itemMap.put(DyeColor.BROWN, Items.BROWN_WOOL);
        itemMap.put(DyeColor.GREEN, Items.GREEN_WOOL);
        itemMap.put(DyeColor.RED, Items.RED_WOOL);
        itemMap.put(DyeColor.BLACK, Items.BLACK_WOOL);
    }

    public static void onCommandRegister(RegisterCommandsEvent event, LeaderboardService leaderboardService) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("testlb")
                .requires(cs -> cs.hasPermission(3)).executes(context -> {
                    List<UUID> won = new ArrayList<UUID>();
                    List<UUID> lost = new ArrayList<UUID>();
                    List<ServerPlayer> players = ServerLifecycleHooks.getCurrentServer()
                            .getPlayerList().getPlayers();

                    for (int i = 0; i < players.size(); i++) {
                        if (i % 2 == 0) {
                            won.add(players.get(i).getUUID());
                        } else {
                            lost.add(players.get(i).getUUID());
                        }
                    }

                    leaderboardService.UpdatePlayerRanking(won, lost);

                    return 1;
                }));

        dispatcher.register(Commands.literal("tptodim")
                .executes(context -> {
                    SimpleWorldManager.teleportPlayerToDimension(
                            context.getSource().getPlayerOrException(),
                            SimpleWorldManager.GAME_DIMENSION);
                    return 1;
                }));

        dispatcher.register(Commands.literal("getRPG")
                .executes(context -> kill_pashalka(context)));

        dispatcher.register(Commands.literal("getwool")
                .requires(cs -> cs.hasPermission(3))
                .executes(context -> giveColoredWool(context)));

        dispatcher.register(Commands.literal("setprefix")
                .requires(cs -> cs.hasPermission(3))
                .then(Commands.argument("player", StringArgumentType.string())
                        .then(Commands.argument("prefix", StringArgumentType.string())
                                .executes(context -> {
                                    String playerName = StringArgumentType
                                            .getString(context, "player");
                                    String prefix = StringArgumentType
                                            .getString(context, "prefix");

                                    ServerPlayer player = context.getSource()
                                            .getServer().getPlayerList()
                                            .getPlayerByName(playerName);
                                    if (player != null) {
                                        Utils.SetPrefixTo(player, prefix);
                                        context.getSource().sendSuccess(() ->
                                                Component.literal(
                                                        "Set prefix for " + playerName + " to " + prefix),
                                                true);
                                    } else {
                                        context.getSource()
                                                .sendFailure(Component.literal(
                                                        "Player " + playerName + " not found."));
                                    }

                                    return 1;
                                }))));

        dispatcher.register(Commands.literal("dolinkssafe")
                .requires(cs -> cs.hasPermission(3))
                .then(Commands.argument("isSafe", BoolArgumentType.bool())
                        .executes(context -> {
                            boolean isSafe = BoolArgumentType
                                    .getBool(context, "isSafe");

                            Gamerules.DO_LINKS_SAFE = isSafe;

                            return 1;
                        })));

        dispatcher.register(Commands.literal("sendtestmessage")
                .requires(cs -> cs.hasPermission(3))
                .executes(context -> {
                    ServerPlayer serverPlayer = context.getSource().getPlayerOrException();
                    serverPlayer.sendSystemMessage(Component.literal("Пока что так скоро будет"));
                    return 1;
                }));

        dispatcher.register(Commands.literal("createnpc")
                .requires(cs -> cs.hasPermission(3))
                .then(Commands.argument("npc_id", IntegerArgumentType.integer())
                        .executes(context -> {
                            ServerPlayer sender = context.getSource()
                                    .getPlayerOrException();
                            Level world = sender.getCommandSenderWorld();
                            ClickableHumanoidEntity mob = new ClickableHumanoidEntity(
                                    ModMobs.CLICKABLE_HUMANOID_ENTITY.get(), world);
                            mob.Initialize(IntegerArgumentType.getInteger(context, "npc_id"));
                            mob.setPos(sender.position().x, sender.position().y,
                                    sender.position().z);
                            world.addFreshEntity(mob);

                            return 1;
                        })));

        dispatcher.register(Commands.literal("vote")
                .requires(commandSourceStack -> commandSourceStack.hasPermission(0))
                .then(Commands.argument("playerr", EntityArgument.player())
                        .executes(context -> {
                            voteForPlayer(context,
                                    EntityArgument.getPlayer(context, "playerr"));
                            return 1;
                        })));

        dispatcher.register(Commands.literal("setteambase")
                .requires(cs -> cs.hasPermission(3))
                .then(Commands.argument("name", TeamColorArgument.color())
                        .then(Commands.argument("x1", IntegerArgumentType.integer())
                                .then(Commands.argument("y1",
                                                IntegerArgumentType.integer())
                                        .then(Commands.argument("z1",
                                                        IntegerArgumentType
                                                                .integer())
                                                .then(Commands.argument(
                                                                "x2",
                                                                IntegerArgumentType
                                                                        .integer())
                                                        .then(Commands.argument(
                                                                        "y2",
                                                                        IntegerArgumentType
                                                                                .integer())
                                                                .then(Commands
                                                                        .argument("z2", IntegerArgumentType
                                                                                .integer())
                                                                        .executes(context -> {
                                                                            TeamColor Team = TeamColorArgument
                                                                                    .getColor(context,
                                                                                            "name");
                                                                            int x1 = IntegerArgumentType
                                                                                    .getInteger(context,
                                                                                            "x1");
                                                                            int y1 = IntegerArgumentType
                                                                                    .getInteger(context,
                                                                                            "y1");
                                                                            int z1 = IntegerArgumentType
                                                                                    .getInteger(context,
                                                                                            "z1");
                                                                            int x2 = IntegerArgumentType
                                                                                    .getInteger(context,
                                                                                            "x2");
                                                                            int y2 = IntegerArgumentType
                                                                                    .getInteger(context,
                                                                                            "y2");
                                                                            int z2 = IntegerArgumentType
                                                                                    .getInteger(context,
                                                                                            "z2");

                                                                            BlockVolume volume = new BlockVolume(
                                                                                    new BlockPos(x1, y1,
                                                                                            z1),
                                                                                    new BlockPos(x2, y2,
                                                                                            z2),
                                                                                    false);
                                                                            TeamManager teamManager = ((TeamGamemode) (PlayerContext
                                                                                    .Get(context.getSource()
                                                                                            .getPlayerOrException()
                                                                                            .getUUID())
                                                                                    .Gamemode()))
                                                                                    .GetTeamManager();

                                                                            teamManager
                                                                                    .Get(Team)
                                                                                    .AddBarrierVolume(
                                                                                            volume);

                                                                            context.getSource()
                                                                                    .sendSuccess(() -> Component.literal(
                                                                                                    "Created team base "
                                                                                                            + Team),
                                                                                            true);
                                                                            return 1;
                                                                        })))))))));

        dispatcher.register(Commands.literal("startbattle")
                .requires(cs -> cs.hasPermission(3))
                .executes(context -> PlayerContext
                        .Get(context.getSource().getPlayerOrException().getUUID())
                        .Gamemode().StartGame(context)));

        dispatcher.register(Commands.literal("crtobj")
                .requires(cs -> cs.hasPermission(3))
                .then(Commands.argument("name", StringArgumentType.string())
                        .then(Commands.argument("x1", IntegerArgumentType.integer())
                                .then(Commands.argument("y1",
                                                IntegerArgumentType.integer())
                                        .then(Commands.argument("z1",
                                                        IntegerArgumentType
                                                                .integer())
                                                .then(Commands.argument(
                                                                "x2",
                                                                IntegerArgumentType
                                                                        .integer())
                                                        .then(Commands.argument(
                                                                        "y2",
                                                                        IntegerArgumentType
                                                                                .integer())
                                                                .then(Commands
                                                                        .argument("z2", IntegerArgumentType
                                                                                .integer())
                                                                        .executes(context -> {
                                                                            String name = StringArgumentType
                                                                                    .getString(context,
                                                                                            "name");
                                                                            int x1 = IntegerArgumentType
                                                                                    .getInteger(context,
                                                                                            "x1");
                                                                            int y1 = IntegerArgumentType
                                                                                    .getInteger(context,
                                                                                            "y1");
                                                                            int z1 = IntegerArgumentType
                                                                                    .getInteger(context,
                                                                                            "z1");
                                                                            int x2 = IntegerArgumentType
                                                                                    .getInteger(context,
                                                                                            "x2");
                                                                            int y2 = IntegerArgumentType
                                                                                    .getInteger(context,
                                                                                            "y2");
                                                                            int z2 = IntegerArgumentType
                                                                                    .getInteger(context,
                                                                                            "z2");

                                                                            BlockVolume volume = new BlockVolume(
                                                                                    new BlockPos(x1, y1,
                                                                                            z1),
                                                                                    new BlockPos(x2, y2,
                                                                                            z2),
                                                                                    true);

                                                                            DestructibleObjective objective = new DestructibleObjective(
                                                                                    volume,
                                                                                    name);
                                                                            DestructibleObjectivesHandler
                                                                                    .Add(objective);

                                                                            context.getSource()
                                                                                    .sendSuccess(() -> Component.literal(
                                                                                                    "Created objective: "
                                                                                                            + objective),
                                                                                            true);
                                                                            return 1;
                                                                        })))))))));

        // Pray to Dod this code works
        // Бермудский треугольник, не раскоменчивать
        // Ошибка происходит после .requires(cs -> cs.hasPermission(3))
        // dispatcher.register(Commands.literal("setteamspawn")
        // .requires(cs -> cs.hasPermission(3))
        // .then(Commands.argument("team_color", TeamColorArgument.color())
        // .executes(CommandRegistration::SetTeamSpawn)
        // .then(Commands.argument("x", IntegerArgumentType.integer())
        // .then(Commands.argument("y", IntegerArgumentType.integer())
        // .then(Commands.argument("z", IntegerArgumentType.integer())
        // .executes(CommandRegistration::SetTeamSpawnTo))))));
    }

    private static int voteForPlayer(CommandContext<CommandSourceStack> context, ServerPlayer player)
            throws CommandSyntaxException {
        CommanderSystem.ProcessCommanderVote(context.getSource().getPlayerOrException().getUUID(), player.getUUID());
        return 1;
    }

    private static int SetTeamSpawnTo(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        int x = IntegerArgumentType.getInteger(context, "x");
        int y = IntegerArgumentType.getInteger(context, "y");
        int z = IntegerArgumentType.getInteger(context, "z");
        BlockPos position = new BlockPos(x, y, z);
        TeamColor color = TeamColorArgument.getColor(context, TeamColorArgument.PropertyName);
        TeamManager teamManager = ((TeamGamemode) (PlayerContext
                .Get(context.getSource().getPlayerOrException().getUUID()).Gamemode()))
                .GetTeamManager();

        teamManager.SetSpawn(color, position);
        context.getSource().sendSuccess( () ->
                Component.literal("Спавн команды " + color.toString().toUpperCase() + " поставлен в "
                        + position),
                true);
        return 0;
    }

    private static int ChooseRespawnPoint(CommandContext<CommandSourceStack> context, boolean tentChosen,
                                          String response, SoundEvent sound) throws CommandSyntaxException {
        ServerPlayer serverPlayer = context.getSource().getPlayerOrException();

        PlayerRespawnEventHandler.DeadPlayers.put(serverPlayer.getUUID(), tentChosen);
        TitleMessage.sendSubtitle(serverPlayer, Component.literal(response));
        SoundHandler.playSoundToPlayer(serverPlayer, sound, 1.0f, 1.0f);

        return 1;
    }

    private static int kill_pashalka(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer serverPlayer = context.getSource().getPlayerOrException();

        serverPlayer.kill();
        return 1;
    }

    private static int SetTeamSpawn(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        BlockPos position = new BlockPos(player.getBlockX(), player.getBlockY(), player.getBlockZ());
        TeamColor color = TeamColorArgument.getColor(context, TeamColorArgument.PropertyName);
        TeamManager teamManager = ((TeamGamemode) (PlayerContext
                .Get(context.getSource().getPlayerOrException().getUUID()).Gamemode()))
                .GetTeamManager();

        teamManager.SetSpawn(color, position);
        context.getSource().sendSuccess(() ->
                Component.literal("Спавн команды " + color.toString().toUpperCase() + " поставлен в "
                        + position),
                true);
        return 0;
    }

    private static int giveColoredWool(CommandContext<CommandSourceStack> context) {

        CommandSourceStack source = context.getSource();
        ServerPlayer player;

        try {
            player = source.getPlayerOrException();
        } catch (CommandSyntaxException e) {
            source.sendFailure(Component.literal("This command can only be run by a player."));
            return 0;
        }

        player.sendSystemMessage(Component.literal("Нашёл тебя"));

        DyeColor[] colors = DyeColor.values(); // Get all wool colors.
        for (DyeColor color : colors) {
            player.sendSystemMessage(Component.literal("Даю цвет" + color));

            String woolName = player.getName().getString() + " " + color.getName();
            ItemStack itemStack = new ItemStack((itemMap.get(color)), 1);

            if (!itemStack.hasTag()) {
                itemStack.setTag(new CompoundTag());
            }
            CompoundTag nbtData = itemStack.getTag();
            nbtData.putString("Owner", player.getName().getString());

            itemStack.setTag(nbtData);

            itemStack.setHoverName(Component.literal(woolName));
            player.getInventory().add(itemStack);
        }
        source.sendSuccess(() -> Component.literal("Given all colored wool to " + player.getName().getString()),
                true);
        return 1;
    }

}