package com.dod.UnrealZaruba.Utils;

import java.util.HashMap;
import java.util.Map;
import java.io.FileNotFoundException;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import java.io.InputStream;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraftforge.server.ServerLifecycleHooks;
import net.minecraft.world.level.Level;
import net.minecraft.client.Minecraft;

public class SchematicLoader {
    private static final Map<ResourceLocation, CompoundTag> SCHEMATIC_CACHE = new HashMap<>();
    private static final Map<ResourceLocation, StructureTemplate> TEMPLATE_CACHE = new HashMap<>();

    public static CompoundTag GetSchematicNbt(ResourceLocation location) {
        if (SCHEMATIC_CACHE.containsKey(location)) {
            return SCHEMATIC_CACHE.get(location);
        }

        try {
            CompoundTag nbt = LoadFromResources(location);
            SCHEMATIC_CACHE.put(location, nbt);
            return nbt;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static CompoundTag LoadFromResources(ResourceLocation location) {
        try {
            String path = "assets/" + location.getNamespace() + "/" + location.getPath() + ".nbt";
            InputStream inputStream = SchematicLoader.class.getResourceAsStream(path);

            if (inputStream == null) {
                throw new FileNotFoundException("Schematic not found: " + path);
            }

            CompoundTag tag = NbtIo.readCompressed(inputStream);
            inputStream.close();
            return tag;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static StructureTemplate GetStructureTemplate(ResourceLocation location) {
        if (TEMPLATE_CACHE.containsKey(location)) {
            return TEMPLATE_CACHE.get(location);
        }

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return GetStructureTemplateClient(location);
        }
        Level overworld = server.getLevel(Level.OVERWORLD);
        StructureTemplate template = new StructureTemplate();

        CompoundTag nbt = GetSchematicNbt(location);
        template.load(overworld.holderLookup(Registries.BLOCK), nbt);
        TEMPLATE_CACHE.put(location, template);
        return template;
    }

    public static StructureTemplate GetStructureTemplateClient(ResourceLocation location) {
        Minecraft minecraft = Minecraft.getInstance();

        StructureTemplate template = new StructureTemplate();
        CompoundTag nbt = GetSchematicNbt(location);
        template.load(minecraft.level.holderLookup(Registries.BLOCK), nbt);
        TEMPLATE_CACHE.put(location, template);
        return template;
    }

}
