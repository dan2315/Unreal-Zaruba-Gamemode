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
import net.minecraftforge.server.ServerLifecycleHooks;
import net.minecraft.world.level.Level;
import net.minecraft.client.Minecraft;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.DataInputStream;

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
        InputStream inputStream = null;
        try {
            String path = "data/" + location.getNamespace() + "/" + location.getPath() + ".nbt";
            
            inputStream = SchematicLoader.class.getClassLoader().getResourceAsStream(path);
    
            byte[] bytes = inputStream.readAllBytes();
            System.out.println("File size: " + bytes.length + " bytes for " + path);
            
            if (bytes.length == 0) {
                throw new IOException("File is empty: " + path);
            }
            
            inputStream = new ByteArrayInputStream(bytes);
            
            try {
                CompoundTag tag = NbtIo.readCompressed(new ByteArrayInputStream(bytes));
                if (tag == null) {
                    System.out.println("readCompressed returned null for " + path);
                } else {
                    // It's actually working way, other is fallbacks that could be removed I guess
                    System.out.println("Successfully read NBT with " + tag.size() + " entries"); 
                }
                return tag;
            } catch (IOException e) {
                System.out.println("Failed to read compressed NBT, trying uncompressed: " + e.getMessage());
                
                try {
                    DataInputStream dataInputStream = new DataInputStream(new ByteArrayInputStream(bytes));
                    CompoundTag tag = NbtIo.read(dataInputStream);
                    if (tag != null) {
                        System.out.println("Successfully read uncompressed NBT with " + tag.size() + " entries");
                        return tag;
                    }
                } catch (IOException e2) {
                    System.out.println("Failed to read uncompressed NBT: " + e2.getMessage());
                }
                
                throw new IOException("File is not a valid NBT format: " + path);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
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
