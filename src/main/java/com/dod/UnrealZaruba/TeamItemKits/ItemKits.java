package com.dod.UnrealZaruba.TeamItemKits;

import java.util.HashMap;

import com.dod.UnrealZaruba.ModBlocks.ModBlocks;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import org.spongepowered.asm.mixin.injection.struct.InjectorGroupInfo.Map;

import com.dod.UnrealZaruba.Commands.CommandPresets;
import com.dod.UnrealZaruba.Commands.Arguments.TeamColor;
import com.dod.UnrealZaruba.TeamLogic.TeamU;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public class ItemKits {
    public static final HashMap<String, Integer> redTeamKit;
    public static final HashMap<String, String> redTeamArmorKit;
    public static final HashMap<String, Integer> blueTeamKit;
    public static final HashMap<String, String> blueTeamArmorKit;

    public static final HashMap<TeamColor, HashMap<String, Integer>> TeamKits;
    public static final HashMap<TeamColor, HashMap<String, String>> TeamArmorKits;

    static {
        redTeamArmorKit = new HashMap<>();
        redTeamArmorKit.put("uz_armor:armor_r_helmet", "head");
        redTeamArmorKit.put("uz_armor:armor_r_chestplate", "chest");
        redTeamArmorKit.put("uz_armor:armor_r_leggings", "legs");
        redTeamArmorKit.put("uz_armor:armor_r_boots", "feet");

        redTeamKit = new HashMap<>();
        // redTeamKit.put("tacz:modern_kinetic_gun{GunId:\"tacz:ak47\",
        // GunFireMod:\"AUTO\"}", 1);
        // redTeamKit.put("tacz:attachment{AttachmentId:\"tacz:sight_coyote\"}", 1);
        // redTeamKit.put("tacz:attachment{AttachmentId:\"tacz:oem_stock_tactical\"}",
        // 1);
        // redTeamKit.put("tacz:attachment{AttachmentId:\"tacz:muzzle_silence_phantom_s1\"}",
        // 1);
        // redTeamKit.put("tacz:ammo{AmmoId:\"tacz:762x39\"}", 99);
        // redTeamKit.put("tacz:ammo{AmmoId:\"tacz:762x39\"}", 99);
        // redTeamKit.put("tacz:ammo{AmmoId:\"tacz:762x39\"}", 40);
        redTeamKit.put("walkietalkie:netherite_walkietalkie", 1);
        redTeamKit.put("minecraft:golden_carrot", 64);
        redTeamKit.put("cgm:grenade", 8);
        redTeamKit.put("cgm:stun_grenade", 8);
        redTeamKit.put(
            "nzgexpansion:heavy_assault_rifle{AmmoCount:30, Attachments:{Barrel:{id:\"nzgexpansion:extended_barrel\", Count:1b}, Stock:{id:\"nzgexpansion:solid_stock\", Count:1b}, Under_Barrel:{id:\"cgmspecialised_grip\", Count:1b}}}",
            1
        );
        redTeamKit.put("nzgexpansion:medium_bullet", 600);
        redTeamKit.put("minecraft:stone_hoe{CanDestroy:[\"unrealzaruba:tent_main_block\"], display:{Name:'{\"text\":\"Ломотык\"}',Lore:['{\"text\":\"Для уничтожения временных спавнов. НЫЫЫЫЫЫЫЫЫАААААААААААААААА\",\"color\":\"#ffa600\"}']}}", 1);


        blueTeamArmorKit = new HashMap<>();
        blueTeamArmorKit.put("uz_armor:armor_b_helmet",  "head");
        blueTeamArmorKit.put("uz_armor:armor_b_chestplate", "chest");
        blueTeamArmorKit.put("uz_armor:armor_b_leggings",  "legs");
        blueTeamArmorKit.put("uz_armor:armor_b_boots",  "feet");

        blueTeamKit = new HashMap<>();
        // blueTeamKit.put("tacz:modern_kinetic_gun{GunId:\"tacz:m4a1\",
        // GunFireMod:\"AUTO\"}", 1);
        // blueTeamKit.put("tacz:attachment{AttachmentId:\"tacz:sight_uh1\"}", 1);
        // blueTeamKit.put("tacz:attachment{AttachmentId:\"tacz:extended_mag_1\"}", 1);
        // blueTeamKit.put("tacz:attachment{AttachmentId:\"tacz:stock_carbon_bone_c5\"}",
        // 1);
        // blueTeamKit.put("tacz:attachment{AttachmentId:\"tacz:muzzle_brake_cthulhu\"}",
        // 1);
        // blueTeamKit.put("tacz:ammo{AmmoId:\"tacz:556x45\"}", 99);
        // blueTeamKit.put("tacz:ammo{AmmoId:\"tacz:556x45\"}", 99);
        // blueTeamKit.put("tacz:ammo{AmmoId:\"tacz:556x45\"}", 40);
        blueTeamKit.put("walkietalkie:netherite_walkietalkie", 1);
        blueTeamKit.put("minecraft:golden_carrot", 64);
        blueTeamKit.put("cgm:grenade", 8);
        blueTeamKit.put("cgm:stun_grenade", 8);
        blueTeamKit.put(
                "nzgexpansion:battle_rifle{AmmoCount:20, Attachments:{Barrel:{id:\"nzgexpansion:extended_barrel\", Count:1b}, Stock:{id:\"nzgexpansion:carbine_stock\", Count:1b}}}",
                1);
        blueTeamKit.put("nzgexpansion:medium_bullet", 600);
        blueTeamKit.put("minecraft:stone_hoe{CanDestroy:[\"unrealzaruba:tent_main_block\"], display:{Name:'{\"text\":\"Ломотык\"}',Lore:['{\"text\":\"Для уничтожения временных спавнов. НЫЫЫЫЫЫЫЫЫАААААААААААААААА\",\"color\":\"#ffa600\"}']}}", 1);

        TeamKits = new HashMap<>();
        TeamKits.put(TeamColor.RED, redTeamKit);
        TeamKits.put(TeamColor.BLUE, blueTeamKit);
        TeamArmorKits = new HashMap<>();
        TeamArmorKits.put(TeamColor.RED, redTeamArmorKit);
        TeamArmorKits.put(TeamColor.BLUE, blueTeamArmorKit);
    }

    public static void GiveKit(MinecraftServer server, ServerPlayer serverPlayer, TeamU team) {
        for (Map.Entry<String, Integer> itemElement : ItemKits.TeamKits.get(team.Color()).entrySet()) {
            CommandPresets.executeGiveCommandSilent(server, serverPlayer.getName().getString(),
                    itemElement.getKey() + " " + itemElement.getValue());
        }
    }

    public static void GiveArmorKit(MinecraftServer server, ServerPlayer serverPlayer, TeamU team) {
        for (Map.Entry<String, String> itemElement : ItemKits.TeamArmorKits.get(team.Color()).entrySet()) {
            CommandPresets.executeEquipArmorCommandSilent(server, serverPlayer.getName().getString(), itemElement.getValue(), itemElement.getKey());
        }
    }

//    public static void GiveSpecItem(MinecraftServer server, ServerPlayer serverPlayer, ItemStack item) {
//        ListTag canDestroy = new ListTag();
//        canDestroy.add(StringTag.valueOf(ModBlocks.TENT_MAIN_BLOCK.toString()));
//        item.add
//        item.addTagElement("CanDestroy", canDestroy);
//        serverPlayer.getInventory().add(item);
//    }
}
