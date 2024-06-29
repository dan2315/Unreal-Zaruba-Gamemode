package com.dod.UnrealZaruba.TeamItemKits;

import java.util.HashMap;

import org.spongepowered.asm.mixin.injection.struct.InjectorGroupInfo.Map;

import com.dod.UnrealZaruba.Commands.CommandPresets;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.DyeColor;

public class ItemKits {
    public static final HashMap<String, Integer> blueTeamKit;
    public static final HashMap<String, Integer> redTeamKit;

    public static final HashMap<DyeColor, HashMap<String, Integer>> TeamKits;

    static {
        redTeamKit = new HashMap<>();
        // redTeamKit.put("tacz:modern_kinetic_gun{GunId:\"tacz:ak47\",
        // GunFireMod:\"AUTO\"}", 1);
        // redTeamKit.put("tacz:attachment{AttachmentId:\"tacz:sight_coyote\"}", 1);
        // redTeamKit.put("tacz:attachment{AttachmentId:\"tacz:oem_stock_tactical\"}",
        // 1);
        // redTeamKit.put("tacz:attachment{AttachmentId:\"tacz:muzzle_silence_phantom_s1\"}",
        // 1);
        redTeamKit.put("combatgear:altynup_helmet", 1);
        redTeamKit.put("combatgear:terro_chestplate", 1);
        redTeamKit.put("combatgear:modernsand_leggings", 1);
        redTeamKit.put("combatgear:modernsand_boots", 1);
        redTeamKit.put("walkietalkie:netherite_walkietalkie", 1);
        // redTeamKit.put("tacz:ammo{AmmoId:\"tacz:762x39\"}", 99);
        // redTeamKit.put("tacz:ammo{AmmoId:\"tacz:762x39\"}", 99);
        // redTeamKit.put("tacz:ammo{AmmoId:\"tacz:762x39\"}", 40);
        redTeamKit.put("minecraft:golden_carrot", 64);
        redTeamKit.put("cgm:grenade", 8);
        redTeamKit.put("cgm:stun_grenade", 8);
        redTeamKit.put("cgm:assault_rifle{AmmoCount:40, Attachments: {Barrel: {id:\"cgm:silencer\", Count:1b}, Scope:{id:\"cgm:short_scope\", Count:1b}, Stock:{id:\"cgm:tactical_stock\", Count: 1b}, Under_Barrel:{id:\"cgm:specialised_grip\", Count: 1b}}}", 1);
        redTeamKit.put("cgm:basic_bullet", 300);
        
        blueTeamKit = new HashMap<>();
        // blueTeamKit.put("tacz:modern_kinetic_gun{GunId:\"tacz:m4a1\",
        // GunFireMod:\"AUTO\"}", 1);
        // blueTeamKit.put("tacz:attachment{AttachmentId:\"tacz:sight_uh1\"}", 1);
        // blueTeamKit.put("tacz:attachment{AttachmentId:\"tacz:extended_mag_1\"}", 1);
        // blueTeamKit.put("tacz:attachment{AttachmentId:\"tacz:stock_carbon_bone_c5\"}",
        // 1);
        // blueTeamKit.put("tacz:attachment{AttachmentId:\"tacz:muzzle_brake_cthulhu\"}",
        // 1);
        blueTeamKit.put("combatgear:gign_helmet", 1);
        blueTeamKit.put("combatgear:gign_chestplate", 1);
        blueTeamKit.put("combatgear:gign_leggings", 1);
        blueTeamKit.put("combatgear:gign_boots", 1);
        blueTeamKit.put("walkietalkie:netherite_walkietalkie", 1);
        // blueTeamKit.put("tacz:ammo{AmmoId:\"tacz:556x45\"}", 99);
        // blueTeamKit.put("tacz:ammo{AmmoId:\"tacz:556x45\"}", 99);
        // blueTeamKit.put("tacz:ammo{AmmoId:\"tacz:556x45\"}", 40);
        blueTeamKit.put("minecraft:golden_carrot", 64);
        blueTeamKit.put("cgm:grenade", 8);
        blueTeamKit.put("cgm:stun_grenade", 8);
        blueTeamKit.put("cgm:assault_rifle{AmmoCount:40, Attachments: {Barrel: {id:\"cgm:silencer\", Count:1b}, Scope:{id:\"cgm:short_scope\", Count:1b}, Stock:{id:\"cgm:tactical_stock\", Count: 1b}, Under_Barrel:{id:\"cgm:specialised_grip\", Count: 1b}}}", 1);
        blueTeamKit.put("cgm:basic_bullet", 300);

        TeamKits = new HashMap<>();
        TeamKits.put(DyeColor.RED, redTeamKit);
        TeamKits.put(DyeColor.BLUE, blueTeamKit);
    }

    public static void GiveKit(MinecraftServer server, ServerPlayer serverPlayer, DyeColor color) {
        for (Map.Entry<String, Integer> itemElement : ItemKits.TeamKits.get(color).entrySet()) {
            CommandPresets.executeGiveCommandSilent(server, serverPlayer.getName().getString(),
                    itemElement.getKey() + " " + itemElement.getValue());
        }
    }
}
