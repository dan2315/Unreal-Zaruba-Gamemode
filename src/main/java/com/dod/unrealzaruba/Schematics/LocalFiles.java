package com.dod.unrealzaruba.Schematics;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;

import com.dod.unrealzaruba.Vehicles.Blueprints;
import com.enxv.aeronauticsstructuretool.blueprint.storage.BlueprintFileRepository;

import net.minecraft.client.Minecraft;

/**
 * Files on the player's machine: Create Aeronautics Toolgun blueprints (.excraft, what the toolgun saves and prints)
 * and Minecraft screenshots for gallery images.
 */
public final class LocalFiles {
    private static final int MAX_SCREENSHOTS = 30;
    private static final int IMAGE_MAX_SIDE = 1600;
    private static final int THUMB_WIDTH = 192;

    private LocalFiles() {}

    public record LocalFile(Path path, String name, long size, long modified) {}

    /** What's in a blueprint: its size and block counts, or why it can't be uploaded. */
    public record SchematicInfo(byte[] data, Blueprints.Info info, String error) {
        static SchematicInfo error(String key) {
            return new SchematicInfo(null, null, key);
        }
    }

    /** The toolgun's blueprint folder. */
    public static Path schematicsDir() {
        return BlueprintFileRepository.clientDirectory(Minecraft.getInstance().gameDirectory.toPath());
    }

    public static Path screenshotsDir() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve("screenshots");
    }

    /** Toolgun blueprints, newest first. */
    public static List<LocalFile> schematics() {
        return list(schematicsDir(), BlueprintFileRepository.FILE_EXTENSION, Integer.MAX_VALUE);
    }

    /** Recent screenshots, newest first. */
    public static List<LocalFile> screenshots() {
        return list(screenshotsDir(), ".png", MAX_SCREENSHOTS);
    }

    private static List<LocalFile> list(Path dir, String extension, int limit) {
        if (!Files.isDirectory(dir)) return List.of();
        try (Stream<Path> files = Files.list(dir)) {
            return files.filter(p -> Files.isRegularFile(p) && p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(extension))
                    .map(LocalFiles::describe)
                    .filter(f -> f != null)
                    .sorted(Comparator.comparingLong(LocalFile::modified).reversed())
                    .limit(limit)
                    .toList();
        } catch (IOException e) {
            return List.of();
        }
    }

    private static LocalFile describe(Path path) {
        try {
            String file = path.getFileName().toString();
            String name = file.substring(0, file.lastIndexOf('.'));
            return new LocalFile(path, name, Files.size(path), Files.getLastModifiedTime(path).toMillis());
        } catch (IOException e) {
            return null;
        }
    }

    /** Reads and checks a blueprint: gzip (what the backend checks), at most 1 MB, and readable by the toolgun. */
    public static SchematicInfo readSchematic(Path path) {
        byte[] data;
        try {
            data = Files.readAllBytes(path);
        } catch (IOException e) {
            return SchematicInfo.error("schematics.unrealzaruba.error.read");
        }
        if (data.length > SchematicsApi.MAX_FILE_BYTES) {
            return SchematicInfo.error("schematics.unrealzaruba.error.too_big");
        }
        if (data.length < 2 || (data[0] & 0xFF) != 0x1F || (data[1] & 0xFF) != 0x8B) {
            return SchematicInfo.error("schematics.unrealzaruba.error.not_gzip");
        }
        // Blueprints with runtime contraptions need a level to read their blocks; null only works for the simple ones
        Blueprints.Info info = Blueprints.read(path.getFileName().toString(), data, Minecraft.getInstance().level);
        if (info == null) {
            return SchematicInfo.error("schematics.unrealzaruba.error.not_structure");
        }
        return new SchematicInfo(data, info, null);
    }

    /**
     * Saves a downloaded blueprint into the toolgun's folder as "&lt;name&gt;.excraft", so the toolgun lists it.
     * An identical file already there is reused; a different one with that name gets " 2", " 3", ...
     * Returns the file name.
     */
    public static String saveSchematic(String name, byte[] data) throws IOException {
        Path dir = schematicsDir();
        Files.createDirectories(dir);
        // The toolgun only opens names made of letters, digits, spaces, '_' and '-'
        String base = BlueprintFileRepository.normalizeName(name);
        if (base.isEmpty()) base = "schematic";
        for (int n = 1; ; n++) {
            String file = (n == 1 ? base : base + " " + n) + BlueprintFileRepository.FILE_EXTENSION;
            Path target = dir.resolve(file);
            if (!Files.exists(target)) {
                Files.write(target, data);
                return file;
            }
            if (Arrays.equals(Files.readAllBytes(target), data)) {
                return file;
            }
        }
    }

    /** A screenshot shrunk to at most 1600 px and re-encoded as JPEG to stay under the backend's 1 MB limit. */
    public static SchematicsApi.Upload prepareImage(Path path) throws IOException {
        BufferedImage source = ImageIO.read(path.toFile());
        if (source == null) throw new IOException("Unsupported image " + path.getFileName());
        double scale = Math.min(1.0, (double) IMAGE_MAX_SIDE / Math.max(source.getWidth(), source.getHeight()));
        BufferedImage image = scaled(source, scale);
        String file = path.getFileName().toString();
        String name = file.substring(0, file.lastIndexOf('.')) + ".jpg";
        // Lower the quality first, then the size
        while (true) {
            for (float quality : new float[] {0.88f, 0.75f, 0.6f, 0.45f}) {
                byte[] jpeg = jpeg(image, quality);
                if (jpeg.length <= SchematicsApi.MAX_FILE_BYTES) {
                    return new SchematicsApi.Upload(name, "image/jpeg", jpeg);
                }
            }
            image = scaled(image, 0.7);
        }
    }

    /** A small PNG of an image file, for picker thumbnails. */
    public static byte[] thumbnail(Path path) throws IOException {
        BufferedImage source = ImageIO.read(path.toFile());
        if (source == null) throw new IOException("Unsupported image " + path.getFileName());
        BufferedImage thumb = scaled(source, Math.min(1.0, (double) THUMB_WIDTH / source.getWidth()));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(thumb, "png", out);
        return out.toByteArray();
    }

    /** Converts an image ImageIO can read (JPEG etc.) to PNG, for textures. Null if it can't be read. */
    public static byte[] toPng(byte[] data) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(data));
            if (image == null) return null;
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "png", out);
            return out.toByteArray();
        } catch (IOException e) {
            return null;
        }
    }

    private static BufferedImage scaled(BufferedImage source, double scale) {
        int w = Math.max(1, (int) Math.round(source.getWidth() * scale));
        int h = Math.max(1, (int) Math.round(source.getHeight() * scale));
        // RGB without alpha: JPEG can't store alpha
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(source, 0, 0, w, h, null);
        g.dispose();
        return out;
    }

    private static byte[] jpeg(BufferedImage image, float quality) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
        if (!writers.hasNext()) throw new IOException("No JPEG encoder");
        ImageWriter writer = writers.next();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (MemoryCacheImageOutputStream stream = new MemoryCacheImageOutputStream(out)) {
            writer.setOutput(stream);
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(Math.max(0.1f, quality));
            writer.write(null, new IIOImage(image, null, null), param);
        } finally {
            writer.dispose();
        }
        return out.toByteArray();
    }

    public static String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format(Locale.ROOT, "%.1f KB", bytes / 1024.0);
        return String.format(Locale.ROOT, "%.2f MB", bytes / (1024.0 * 1024.0));
    }
}
