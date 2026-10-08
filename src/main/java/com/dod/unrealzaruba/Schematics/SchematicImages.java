package com.dod.unrealzaruba.Schematics;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import com.dod.unrealzaruba.UnrealZaruba;
import com.mojang.blaze3d.platform.NativeImage;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * Textures for schematic previews (downloaded from the backend) and screenshot thumbnails (read from disk), loaded
 * in the background on first use. Keeps the most recently used ones and frees the rest.
 */
public final class SchematicImages {
    private static final int MAX_ENTRIES = 96;

    public record Texture(ResourceLocation location, int width, int height) {}

    private static final class Entry {
        Texture texture;
        boolean failed;
    }

    private static final Map<String, Entry> CACHE = new LinkedHashMap<>(16, 0.75f, true);
    private static int nextId;

    private SchematicImages() {}

    /** The texture for a backend image path, or null while it loads or if it failed. */
    public static Texture remote(String url) {
        if (url == null) return null;
        return get("remote:" + url, () -> SchematicsApi.image(url).thenApply(r -> {
            if (!r.ok()) throw new RuntimeException(r.error());
            return r.value();
        }));
    }

    /** A thumbnail of an image file on disk, or null while it loads or if it failed. */
    public static Texture thumbnail(Path path, long modified) {
        return get("file:" + path + "@" + modified, () -> CompletableFuture.supplyAsync(() -> {
            try {
                return LocalFiles.thumbnail(path);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }));
    }

    public static boolean failed(String url) {
        Entry entry = url == null ? null : CACHE.get("remote:" + url);
        return entry != null && entry.failed;
    }

    private static Texture get(String key, Supplier<CompletableFuture<byte[]>> loader) {
        Entry entry = CACHE.get(key);
        if (entry != null) return entry.texture;

        Entry created = new Entry();
        CACHE.put(key, created);
        trim();
        loader.get()
                .thenApply(SchematicImages::decode)
                .whenComplete((image, error) -> Minecraft.getInstance().execute(() -> {
                    if (error != null || image == null) {
                        created.failed = true;
                        if (error != null) UnrealZaruba.LOGGER.debug("Schematic image {} failed: {}", key, error.toString());
                        return;
                    }
                    if (CACHE.get(key) != created) { // evicted while loading
                        image.close();
                        return;
                    }
                    ResourceLocation location = ResourceLocation.fromNamespaceAndPath(UnrealZaruba.MOD_ID, "schematic_image/" + nextId++);
                    Minecraft.getInstance().getTextureManager().register(location, new DynamicTexture(image));
                    created.texture = new Texture(location, image.getWidth(), image.getHeight());
                }));
        return null;
    }

    // PNG directly; anything else ImageIO reads (older JPEG previews) through a PNG conversion
    private static NativeImage decode(byte[] data) {
        try {
            return NativeImage.read(data);
        } catch (IOException | RuntimeException e) {
            byte[] png = LocalFiles.toPng(data);
            if (png == null) return null;
            try {
                return NativeImage.read(png);
            } catch (IOException ex) {
                return null;
            }
        }
    }

    private static void trim() {
        var it = CACHE.entrySet().iterator();
        while (CACHE.size() > MAX_ENTRIES && it.hasNext()) {
            Entry old = it.next().getValue();
            if (old.texture != null) {
                Minecraft.getInstance().getTextureManager().release(old.texture.location());
            }
            it.remove();
        }
    }

    /** Draws the texture filling the box, cropped to keep its aspect ratio. */
    public static void drawCover(GuiGraphics g, Texture texture, int x, int y, int w, int h) {
        float boxAspect = (float) w / h;
        float texAspect = (float) texture.width() / texture.height();
        int u = 0, v = 0, uw = texture.width(), vh = texture.height();
        if (texAspect > boxAspect) {
            uw = Math.round(texture.height() * boxAspect);
            u = (texture.width() - uw) / 2;
        } else {
            vh = Math.round(texture.width() / boxAspect);
            v = (texture.height() - vh) / 2;
        }
        g.blit(texture.location(), x, y, w, h, u, v, uw, vh, texture.width(), texture.height());
    }
}
