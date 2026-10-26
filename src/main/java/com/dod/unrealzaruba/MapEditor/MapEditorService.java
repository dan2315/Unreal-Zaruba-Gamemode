package com.dod.unrealzaruba.MapEditor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.dod.unrealzaruba.ConfigurationManager.ConfigManager;
import com.dod.unrealzaruba.Gamemodes.BaseGamemode;
import com.dod.unrealzaruba.Gamemodes.GamemodeData.GamemodeDataManager;
import com.dod.unrealzaruba.Gamemodes.GamemodeManager;
import com.dod.unrealzaruba.Gamemodes.Objectives.ObjectivesData;
import com.dod.unrealzaruba.NetworkPackets.MapEditor.ClientboundMapEditorStatePacket;
import com.dod.unrealzaruba.NetworkPackets.MapEditor.ServerboundMapEditorPacket;
import com.dod.unrealzaruba.TeamLogic.TeamData;
import com.dod.unrealzaruba.UnrealZaruba;
import com.dod.unrealzaruba.WorldManager.GameMaps;
import com.dod.unrealzaruba.WorldManager.WorldManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Server side of the map editor: reads the open map's objectives.json and teamData.json and changes them as an op's
 * editor asks (DEV mode only, the map's template is what is being edited). The files are edited as JSON, so objective
 * kinds the editor doesn't know and fields it doesn't set stay as they are. A mode set up on the map reloads them.
 */
public final class MapEditorService {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final String CAPTURE_POINT = "capturepoint";
    private static final String DESTRUCTIBLE = "destructible";
    private static final int MAX_NAME = 48;

    private MapEditorService() {}

    public static void handle(ServerPlayer player, ServerboundMapEditorPacket msg) {
        if (!player.hasPermissions(2)) return;
        GameMaps.GameMap map = GameMaps.get(WorldManager.currentMapKey());
        if (msg.action == ServerboundMapEditorPacket.Action.REQUEST) {
            sendState(player, map);
            return;
        }
        if (map == null || WorldManager.gameLevel == null) {
            player.displayClientMessage(Component.translatable("editor.unrealzaruba.no_map"), true);
            return;
        }
        if (!ConfigManager.isDevMode()) {
            player.displayClientMessage(Component.translatable("editor.unrealzaruba.dev_only"), true);
            return;
        }
        try {
            if (msg.action == ServerboundMapEditorPacket.Action.SET_BASE) {
                setBase(map, msg.owner, msg.a);
            } else {
                editObjectives(map, msg);
            }
        } catch (IOException | JsonParseException | IllegalStateException e) {
            UnrealZaruba.LOGGER.error("[UnrealZaruba] Map editor could not change {}", map.key(), e);
            player.displayClientMessage(Component.translatable("editor.unrealzaruba.failed"), true);
            return;
        }
        reloadMode(map);
        // Everyone with the editor sees the change
        for (ServerPlayer other : player.server.getPlayerList().getPlayers()) {
            if (other.hasPermissions(2)) sendState(other, map);
        }
    }

    private static void sendState(ServerPlayer player, GameMaps.GameMap map) {
        PacketDistributor.sendToPlayer(player, new ClientboundMapEditorStatePacket(state(map)));
    }

    /** The open map as the editor shows it; an empty state when no map is open. */
    static EditorState state(GameMaps.GameMap map) {
        if (map == null) return EditorState.EMPTY;
        List<EditorState.Point> points = new ArrayList<>();
        List<EditorState.Target> targets = new ArrayList<>();
        Map<String, BlockPos> bases = new HashMap<>();
        try {
            JsonArray objectives = objectives(read(map, "objectives"));
            for (int i = 0; i < objectives.size(); i++) {
                if (!(objectives.get(i) instanceof JsonObject o)) continue;
                String type = string(o, "type");
                if (CAPTURE_POINT.equals(type) && o.get("captureArea") instanceof JsonObject area) {
                    points.add(new EditorState.Point(i, string(o, "name"), string(o, "initialOwner"), pos(area.get("minPos")),
                            pos(area.get("maxPos")), o.has("respawnPosition") ? pos(o.get("respawnPosition")) : null));
                } else if (DESTRUCTIBLE.equals(type) && o.get("volume") instanceof JsonObject volume) {
                    float required = o.has("requiredDegreeOfDestruction") ? o.get("requiredDegreeOfDestruction").getAsFloat() : 0.01f;
                    targets.add(new EditorState.Target(i, string(o, "name"), pos(volume.get("minPos")), pos(volume.get("maxPos")), required));
                }
            }
            if (read(map, "teamData").get("teams") instanceof JsonObject teams) {
                for (var team : teams.entrySet()) {
                    if (team.getValue() instanceof JsonObject entry && entry.has("blockPos")) {
                        bases.put(team.getKey(), pos(entry.get("blockPos")));
                    }
                }
            }
        } catch (IOException | JsonParseException | IllegalStateException | NullPointerException e) {
            UnrealZaruba.LOGGER.error("[UnrealZaruba] Map editor could not read {}", map.key(), e);
        }
        return new EditorState(map.key(), map.mode(), ConfigManager.isDevMode(), points, targets, bases);
    }

    private static void editObjectives(GameMaps.GameMap map, ServerboundMapEditorPacket msg) throws IOException {
        JsonObject root = read(map, "objectives");
        JsonArray objectives = objectives(root);
        String wantedType = switch (msg.action) {
            case SAVE_POINT, DELETE_POINT, SET_POINT_RESPAWN -> CAPTURE_POINT;
            default -> DESTRUCTIBLE;
        };
        JsonObject existing = null;
        if (msg.index >= 0) {
            if (msg.index >= objectives.size() || !(objectives.get(msg.index) instanceof JsonObject o) || !wantedType.equals(string(o, "type"))) {
                throw new IllegalStateException("objective " + msg.index + " is not a " + wantedType + " (the list changed?)");
            }
            existing = o;
        }
        boolean saving = msg.action == ServerboundMapEditorPacket.Action.SAVE_POINT || msg.action == ServerboundMapEditorPacket.Action.SAVE_TARGET;
        if (existing == null && !saving) {
            throw new IllegalStateException(msg.action + " needs an existing objective");
        }
        String name = msg.name.strip();
        if (name.length() > MAX_NAME) name = name.substring(0, MAX_NAME);

        switch (msg.action) {
            case SAVE_POINT -> {
                JsonObject point = existing != null ? existing : new JsonObject();
                if (existing == null) {
                    point.addProperty("type", CAPTURE_POINT);
                    point.addProperty("progress", 0.0);
                    point.addProperty("isCompleted", false);
                    objectives.add(point);
                }
                setArea(point, "captureArea", msg.a, msg.b);
                point.addProperty("name", name);
                if (msg.owner.equals("RED") || msg.owner.equals("BLUE")) {
                    point.addProperty("initialOwner", msg.owner);
                } else {
                    point.remove("initialOwner");
                }
            }
            case SET_POINT_RESPAWN -> existing.add("respawnPosition", json(msg.a));
            case SAVE_TARGET -> {
                JsonObject target = existing != null ? existing : new JsonObject();
                if (existing == null) {
                    // The block count is taken when a match starts
                    target.addProperty("blockAmount", 0);
                    target.addProperty("remainingBlockAmount", 0);
                    target.addProperty("type", DESTRUCTIBLE);
                    target.addProperty("progress", 0.0);
                    target.addProperty("isCompleted", false);
                    objectives.add(target);
                }
                setArea(target, "volume", msg.a, msg.b);
                target.addProperty("requiredDegreeOfDestruction", Math.max(0.001f, Math.min(1f, msg.required)));
                target.addProperty("name", name);
            }
            case DELETE_POINT, DELETE_TARGET -> objectives.remove(msg.index);
            default -> throw new IllegalStateException("not an objective action: " + msg.action);
        }
        write(map, "objectives", root);
    }

    /** An area as the objectives keep it: min and max corners (inclusive) and the center on the floor. */
    private static void setArea(JsonObject objective, String field, BlockPos a, BlockPos b) {
        BlockPos min = new BlockPos(Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY()), Math.min(a.getZ(), b.getZ()));
        BlockPos max = new BlockPos(Math.max(a.getX(), b.getX()), Math.max(a.getY(), b.getY()), Math.max(a.getZ(), b.getZ()));
        BlockPos center = new BlockPos(Math.floorDiv(min.getX() + max.getX(), 2), min.getY(), Math.floorDiv(min.getZ() + max.getZ(), 2));
        JsonObject area = new JsonObject();
        area.add("minPos", json(min));
        area.add("maxPos", json(max));
        area.add("center", json(center));
        objective.add(field, area);
        objective.add("position", json(center));
    }

    private static void setBase(GameMaps.GameMap map, String team, BlockPos pos) throws IOException {
        if (!team.equals("RED") && !team.equals("BLUE")) throw new IllegalStateException("no team " + team);
        JsonObject root = read(map, "teamData");
        if (!(root.get("teams") instanceof JsonObject)) root.add("teams", new JsonObject());
        JsonObject teams = root.getAsJsonObject("teams");
        JsonObject entry = teams.get(team) instanceof JsonObject existing ? existing : new JsonObject();
        entry.add("blockPos", json(pos));
        teams.add(team, entry);
        write(map, "teamData", root);
    }

    /** A mode set up on this map takes the new settings now (its settings commands would otherwise save the old ones back). */
    private static void reloadMode(GameMaps.GameMap map) {
        BaseGamemode gamemode = GamemodeManager.instance.GetActiveGamemode();
        if (gamemode == null || !map.key().equals(gamemode.MapKey())) return;
        ObjectivesData objectives = GamemodeDataManager.getHandler(gamemode.getClass(), ObjectivesData.class);
        if (objectives != null) objectives.loadData();
        TeamData teams = GamemodeDataManager.getHandler(gamemode.getClass(), TeamData.class);
        if (teams != null) teams.loadData();
    }

    private static Path file(GameMaps.GameMap map, String name) {
        return map.dataDir().resolve(name + ".json");
    }

    private static JsonObject read(GameMaps.GameMap map, String name) throws IOException {
        Path file = file(map, name);
        if (!Files.exists(file)) return new JsonObject();
        JsonElement root = JsonParser.parseString(Files.readString(file));
        return root instanceof JsonObject object ? object : new JsonObject();
    }

    private static void write(GameMaps.GameMap map, String name, JsonObject root) throws IOException {
        Path file = file(map, name);
        Files.createDirectories(file.getParent());
        Files.writeString(file, GSON.toJson(root));
    }

    private static JsonArray objectives(JsonObject root) {
        if (!(root.get("objectives") instanceof JsonArray)) root.add("objectives", new JsonArray());
        return root.getAsJsonArray("objectives");
    }

    private static String string(JsonObject object, String field) {
        return object.has(field) && object.get(field).isJsonPrimitive() ? object.get(field).getAsString() : "";
    }

    private static BlockPos pos(JsonElement element) {
        JsonObject o = element.getAsJsonObject();
        return new BlockPos(o.get("x").getAsInt(), o.get("y").getAsInt(), o.get("z").getAsInt());
    }

    private static JsonObject json(BlockPos pos) {
        JsonObject o = new JsonObject();
        o.addProperty("x", pos.getX());
        o.addProperty("y", pos.getY());
        o.addProperty("z", pos.getZ());
        return o;
    }
}
