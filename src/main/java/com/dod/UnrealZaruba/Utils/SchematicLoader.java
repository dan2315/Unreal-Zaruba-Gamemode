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

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;

import net.spaceeye.vmod.schematic.VModShipSchematicV2;
import net.spaceeye.valkyrien_ship_schematics.interfaces.IShipSchematic;
import net.spaceeye.valkyrien_ship_schematics.ShipSchematic;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dod.UnrealZaruba.UnrealZaruba;

public class SchematicLoader {
    private static final Map<ResourceLocation, CompoundTag> SCHEMATIC_CACHE = new HashMap<>();
    private static final Map<ResourceLocation, StructureTemplate> TEMPLATE_CACHE = new HashMap<>();
    private static final Map<ResourceLocation, IShipSchematic> VSCHEM_CACHE = new HashMap<>();
    private static final Logger LOGGER = LogManager.getLogger();

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


    // VSCHEM
    public static IShipSchematic GetVSchem(ResourceLocation location) {
        UnrealZaruba.LOGGER.info("GetVSchem called for {}", location.toString());
        if (VSCHEM_CACHE.containsKey(location)) {
            return VSCHEM_CACHE.get(location);
        }

        try {
            byte[] bytes;
            String source;
            
            java.io.File file = new java.io.File("schematics/vs/" + location.getPath() + ".vschem");
            UnrealZaruba.LOGGER.info("Trying to find file {}", file);
            if (file.exists()) {
                source = "server directory: " + file.getAbsolutePath();
                try (java.io.FileInputStream fileInputStream = new java.io.FileInputStream(file)) {
                    bytes = fileInputStream.readAllBytes();
                }
            } else {
                String resourcePath = "/assets/unrealzaruba/schematics/" + location.getPath();
                try (InputStream inputStream = SchematicLoader.class.getResourceAsStream(resourcePath)) {
                    if (inputStream == null) {
                        throw new IOException("Schematic file not found in both server directory and mod resources: " + location);
                    }
                    source = "mod resources: " + resourcePath;
                    bytes = inputStream.readAllBytes();
                }
            }
            
            UnrealZaruba.LOGGER.info("Loading schematic from " + source);
            UnrealZaruba.LOGGER.info("Bytes: " + bytes.length);
            var buf = new FriendlyByteBuf(Unpooled.wrappedBuffer(bytes));


            if (bytes.length > 3 && bytes[3] == 1) {
                UnrealZaruba.LOGGER.error("Vschem version 1 is not supported");
                return null;
            }
            else if (buf.readUtf().equals("vschem")) {
                try {
                    String type = buf.readUtf();
                    UnrealZaruba.LOGGER.info("Type: {}", type);
                    if (type.equals("VModShipSchematicV1")) {
                        UnrealZaruba.LOGGER.error("Vschem version 1 is not supported");
                        return null;
                    } else {
                        IShipSchematic instance = ShipSchematic.getSchematicFromBytes(bytes);
                        UnrealZaruba.LOGGER.info("Result: {}", instance != null);
                        VSCHEM_CACHE.put(location, instance);
                        return instance;
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                    return ShipSchematic.getSchematicFromBytes(bytes);
                }
                catch (Error e) {
                    e.printStackTrace();
                    return ShipSchematic.getSchematicFromBytes(bytes);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        catch (Error e) {
            e.printStackTrace();
            return null;
        }
        return null;
    }
}
