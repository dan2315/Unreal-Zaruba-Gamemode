package com.dod.unrealzaruba.Schematics;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * Upload a toolgun blueprint (.excraft from the Create Aeronautics Toolgun's folder), or edit one of the player's own: name, description,
 * visibility, gallery (screenshots, first image is the cover) and, when editing, an optional replacement file.
 */
public class SchematicEditScreen extends SchematicsScreenBase {
    private static final int ROW = 14;
    private static final int GAP = 4;

    private final SchematicsBrowserScreen parent;
    /** Null when uploading a new schematic. */
    private final SchematicDto existing;

    private final List<LocalFiles.LocalFile> files = LocalFiles.schematics();
    private final List<LocalFiles.LocalFile> screenshots = LocalFiles.screenshots();
    private LocalFiles.LocalFile file;
    private LocalFiles.SchematicInfo fileInfo;

    private String name;
    private String description;
    private boolean isPublic;

    private final List<SchematicDto.Image> images;
    private final Set<Long> removedImages = new HashSet<>();
    private Long coverImage;
    private final Set<Path> picked = new LinkedHashSet<>();

    private EditBox nameBox;
    private MultiLineEditBox descriptionBox;

    private boolean busy;
    private Component progress;
    private int fileScroll;
    private int fileMaxScroll;
    private int shotScroll;
    private int shotMaxScroll;

    // layout, computed in init()
    private int mid;
    private int fieldsTop;
    private int descriptionTop;
    private int descriptionHeight;

    public SchematicEditScreen(SchematicsBrowserScreen parent, SchematicDto existing) {
        super(Component.translatable(existing == null ? "schematics.unrealzaruba.upload_title" : "schematics.unrealzaruba.edit_title"));
        this.parent = parent;
        this.existing = existing;
        this.name = existing != null ? existing.name : "";
        this.description = existing != null && existing.description != null ? existing.description : "";
        this.isPublic = existing != null && existing.isPublic;
        this.images = existing != null && existing.images != null ? new ArrayList<>(existing.images) : new ArrayList<>();
    }

    private boolean editing() {
        return existing != null;
    }

    private int keptImages() {
        return (int) images.stream().filter(i -> !removedImages.contains(i.id)).count();
    }

    // ---------- choices ----------

    private void selectFile(LocalFiles.LocalFile choice) {
        String previousDefault = file != null ? file.name() : "";
        file = choice;
        fileInfo = choice != null ? LocalFiles.readSchematic(choice.path()) : null;
        if (choice != null && !editing() && (name.isBlank() || name.equals(previousDefault))) {
            name = choice.name().length() > SchematicsApi.MAX_NAME ? choice.name().substring(0, SchematicsApi.MAX_NAME) : choice.name();
            nameBox.setValue(name);
        }
    }

    private void togglePicked(Path path) {
        if (picked.remove(path)) return;
        if (keptImages() + picked.size() >= SchematicsApi.MAX_IMAGES) {
            showStatus(Component.translatable("schematics.unrealzaruba.error.too_many_images", SchematicsApi.MAX_IMAGES), true);
            return;
        }
        picked.add(path);
    }

    private void toggleRemoved(long id) {
        if (!removedImages.remove(id)) {
            removedImages.add(id);
            if (Objects.equals(coverImage, id)) coverImage = null;
        } else if (keptImages() + picked.size() > SchematicsApi.MAX_IMAGES) {
            removedImages.add(id);
            showStatus(Component.translatable("schematics.unrealzaruba.error.too_many_images", SchematicsApi.MAX_IMAGES), true);
        }
    }

    // ---------- submit ----------

    private String validate() {
        if (name.isBlank()) return "schematics.unrealzaruba.error.no_name";
        if (!editing() && file == null) return "schematics.unrealzaruba.error.no_file";
        if (file != null && fileInfo != null && fileInfo.error() != null) return fileInfo.error();
        return null;
    }

    private void submit() {
        String problem = validate();
        if (problem != null) {
            showStatus(Component.translatable(problem), true);
            return;
        }
        busy = true;
        progress = Component.translatable(picked.isEmpty() ? "schematics.unrealzaruba.uploading" : "schematics.unrealzaruba.preparing_images");
        if (editing()) {
            saveChanges();
        } else {
            uploadNew();
        }
    }

    private void uploadNew() {
        String fileName = file.path().getFileName().toString();
        byte[] data = fileInfo.data();
        prepareImages().thenCompose(prepared -> {
            if (!prepared.ok()) return CompletableFuture.completedFuture(new SchematicsApi.Result<SchematicDto>(null, prepared.error()));
            return SchematicsApi.upload(name.strip(), description.strip(), isPublic, fileName, data, prepared.value());
        }).thenAccept(result -> minecraft.execute(() -> {
            if (!result.ok()) {
                fail(result.error());
                return;
            }
            parent.onSaved(result.value(), true);
            minecraft.setScreen(parent);
        }));
    }

    private void saveChanges() {
        long id = existing.id;
        List<Supplier<CompletableFuture<? extends SchematicsApi.Result<?>>>> steps = new ArrayList<>();

        String newName = name.strip().equals(existing.name) ? null : name.strip();
        String oldDescription = existing.description == null ? "" : existing.description;
        String newDescription = description.strip().equals(oldDescription.strip()) ? null : description.strip();
        Boolean newPublic = isPublic == existing.isPublic ? null : isPublic;
        if (newName != null || newDescription != null || newPublic != null) {
            steps.add(() -> SchematicsApi.update(id, newName, newDescription, newPublic));
        }
        if (file != null) {
            String fileName = file.path().getFileName().toString();
            byte[] data = fileInfo.data();
            steps.add(() -> SchematicsApi.replaceFile(id, fileName, data));
        }
        for (long imageId : removedImages) {
            steps.add(() -> SchematicsApi.deleteImage(id, imageId));
        }
        if (!picked.isEmpty()) {
            steps.add(() -> prepareImages().thenCompose(prepared -> prepared.ok()
                    ? SchematicsApi.addImages(id, prepared.value())
                    : CompletableFuture.completedFuture(new SchematicsApi.Result<>(null, prepared.error()))));
        }
        Long cover = coverImage;
        if (cover != null) {
            // Order from the server after the other changes, with the chosen cover moved to the front
            steps.add(() -> SchematicsApi.get(id).thenCompose(current -> {
                if (!current.ok()) return CompletableFuture.completedFuture(new SchematicsApi.Result<Void>(null, current.error()));
                List<Long> order = new ArrayList<>();
                order.add(cover);
                if (current.value().images != null) {
                    current.value().images.stream().map(i -> i.id).filter(i -> !i.equals(cover)).forEach(order::add);
                }
                if (current.value().images == null || current.value().images.isEmpty() || current.value().images.get(0).id == cover) {
                    return CompletableFuture.completedFuture(new SchematicsApi.Result<Void>(null, null));
                }
                return SchematicsApi.reorderImages(id, order);
            }));
        }

        runSteps(steps, 0).thenAccept(error -> minecraft.execute(() -> {
            if (error != null) {
                fail(error);
                return;
            }
            parent.onSaved(existing, false);
            minecraft.setScreen(parent);
        }));
    }

    /** Runs the steps one after another; completes with the first error, or null when all succeed. */
    private static CompletableFuture<String> runSteps(List<Supplier<CompletableFuture<? extends SchematicsApi.Result<?>>>> steps, int index) {
        if (index >= steps.size()) return CompletableFuture.completedFuture(null);
        return steps.get(index).get().thenCompose(result -> result.ok()
                ? runSteps(steps, index + 1)
                : CompletableFuture.completedFuture(result.error()));
    }

    private CompletableFuture<SchematicsApi.Result<List<SchematicsApi.Upload>>> prepareImages() {
        List<Path> paths = new ArrayList<>(picked);
        return CompletableFuture.supplyAsync(() -> {
            List<SchematicsApi.Upload> uploads = new ArrayList<>();
            for (Path path : paths) {
                try {
                    uploads.add(LocalFiles.prepareImage(path));
                } catch (IOException | RuntimeException e) {
                    return new SchematicsApi.Result<>(null, path.getFileName() + ": " + e.getMessage());
                }
            }
            return new SchematicsApi.Result<>(uploads, null);
        });
    }

    private void fail(String message) {
        busy = false;
        progress = null;
        showStatus(error(message), true);
    }

    // ---------- screen ----------

    @Override
    protected void init() {
        mid = Mth.clamp(width * 42 / 100, 150, 260);
        fieldsTop = HEADER + 20;
        descriptionTop = fieldsTop + 34;
        descriptionHeight = Mth.clamp((height - HEADER) / 4, 40, 80);
        int left = mid + 10;
        int w = width - 10 - left;

        nameBox = new EditBox(font, left + 5, fieldsTop + 4, w - 10, 10, Component.translatable("schematics.unrealzaruba.name"));
        nameBox.setBordered(false);
        nameBox.setMaxLength(SchematicsApi.MAX_NAME);
        nameBox.setValue(name);
        nameBox.setHint(Component.translatable("schematics.unrealzaruba.name_hint").withStyle(ChatFormatting.DARK_GRAY));
        nameBox.setResponder(value -> name = value);
        addRenderableWidget(nameBox);

        descriptionBox = new MultiLineEditBox(font, left, descriptionTop, w, descriptionHeight,
                Component.translatable("schematics.unrealzaruba.description_hint"), Component.translatable("schematics.unrealzaruba.description"));
        descriptionBox.setCharacterLimit(SchematicsApi.MAX_DESCRIPTION);
        descriptionBox.setValue(description);
        descriptionBox.setValueListener(value -> description = value);
        addRenderableWidget(descriptionBox);
    }

    @Override
    public void onClose() {
        if (!busy) minecraft.setScreen(parent);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (descriptionBox.isMouseOver(mouseX, mouseY)) {
            return descriptionBox.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
        if (mouseX < mid) {
            fileScroll = Mth.clamp(fileScroll - (int) (scrollY * ROW * 2), 0, fileMaxScroll);
        } else {
            shotScroll = Mth.clamp(shotScroll - (int) (scrollY * 20), 0, shotMaxScroll);
        }
        return true;
    }

    @Override
    protected void renderContent(GuiGraphics g, int mouseX, int mouseY) {
        header(g, title);
        int submitWidth = 90;
        button(g, width - 10 - submitWidth, 5, submitWidth, HEADER - 10,
                tr(busy ? "schematics.unrealzaruba.working" : editing() ? "schematics.unrealzaruba.save" : "schematics.unrealzaruba.upload"),
                Style.PRIMARY, !busy, this::submit);
        button(g, width - 10 - submitWidth - 4 - 70, 5, 70, HEADER - 10, tr("schematics.unrealzaruba.cancel"), Style.NORMAL, !busy,
                () -> minecraft.setScreen(parent));
        if (progress != null) {
            String text = progress.getString();
            g.drawString(font, text, width - 10 - submitWidth - 4 - 70 - 10 - font.width(text), (HEADER - 8) / 2, MUTED, false);
        }

        g.fill(0, HEADER, mid, height, SIDE);
        g.fill(mid - 1, HEADER, mid, height, BORDER);
        renderFiles(g, mouseX, mouseY, 10, mid - 10);
        renderFields(g, mouseX, mouseY, mid + 10, width - 10);
    }

    private void renderFiles(GuiGraphics g, int mouseX, int mouseY, int left, int right) {
        int y = HEADER + 8;
        label(g, editing() ? "schematics.unrealzaruba.replace_file" : "schematics.unrealzaruba.file", left, y);
        y += 9;
        small(g, trim(tr("schematics.unrealzaruba.file_hint"), (int) ((right - left) / 0.75f)), left, y, 0xFF6E7A86);
        y += 12;

        int infoHeight = 34;
        int listBottom = height - 10 - infoHeight - GAP;
        List<LocalFiles.LocalFile> rows = new ArrayList<>();
        if (editing()) rows.add(null); // "keep current file"
        rows.addAll(files);

        if (files.isEmpty()) {
            List<net.minecraft.util.FormattedCharSequence> lines = font.split(Component.translatable("schematics.unrealzaruba.no_files"), right - left);
            int ly = y + (editing() ? ROW + 4 : 4);
            for (var line : lines) {
                g.drawString(font, line, left, ly, MUTED, false);
                ly += 10;
            }
        }

        fileMaxScroll = Math.max(0, rows.size() * ROW - (listBottom - y));
        fileScroll = Mth.clamp(fileScroll, 0, fileMaxScroll);
        g.enableScissor(left, y, right, listBottom);
        for (int i = 0; i < rows.size(); i++) {
            LocalFiles.LocalFile row = rows.get(i);
            int ry = y + i * ROW - fileScroll;
            if (ry + ROW < y || ry > listBottom) continue;
            boolean on = row == file;
            boolean hover = mouseX >= left && mouseX < right && mouseY >= ry && mouseY < ry + ROW - 1 && mouseY >= y && mouseY < listBottom;
            if (on || hover) {
                g.fill(left, ry, right, ry + ROW - 1, on ? CARD_SELECTED : CARD);
            }
            if (on) g.renderOutline(left, ry, right - left, ROW - 1, ACCENT);
            if (row == null) {
                g.drawString(font, trim(tr("schematics.unrealzaruba.keep_file"), right - left - 8), left + 4, ry + 3, on ? ACCENT : MUTED, false);
            } else {
                String size = LocalFiles.formatSize(row.size());
                int sizeWidth = (int) (font.width(size) * 0.75f);
                g.drawString(font, trim(row.name(), right - left - 14 - sizeWidth), left + 4, ry + 3, on ? ACCENT : TEXT, false);
                small(g, size, right - 4 - sizeWidth, ry + 4, MUTED);
            }
            if (!busy) {
                hits.add(new Hit(left, Math.max(ry, y), right, Math.min(ry + ROW - 1, listBottom), () -> selectFile(row)));
            }
        }
        g.disableScissor();

        // info about the chosen file
        int iy = height - 10 - infoHeight;
        g.fill(left, iy, right, iy + infoHeight, CARD);
        g.renderOutline(left, iy, right - left, infoHeight, BORDER);
        if (file == null) {
            small(g, trim(tr(editing() ? "schematics.unrealzaruba.keeping_file" : "schematics.unrealzaruba.pick_file"),
                    (int) ((right - left - 10) / 0.75f)), left + 5, iy + 6, MUTED);
        } else if (fileInfo.error() != null) {
            g.drawString(font, trim(file.name(), right - left - 10), left + 5, iy + 5, TEXT, false);
            small(g, trim(tr(fileInfo.error()), (int) ((right - left - 10) / 0.75f)), left + 5, iy + 18, ERROR);
        } else {
            g.drawString(font, trim(file.name(), right - left - 10), left + 5, iy + 5, TEXT, false);
            String dims = tr("schematics.unrealzaruba.file_info", fileInfo.info().width(), fileInfo.info().height(), fileInfo.info().depth(),
                    fileInfo.info().blocks(), fileInfo.info().sublevels(), LocalFiles.formatSize(file.size()));
            small(g, trim(dims, (int) ((right - left - 10) / 0.75f)), left + 5, iy + 18, SUCCESS);
        }
    }

    private void renderFields(GuiGraphics g, int mouseX, int mouseY, int left, int right) {
        int w = right - left;
        label(g, "schematics.unrealzaruba.name", left, fieldsTop - 9);
        field(g, left, fieldsTop, w, 18, nameBox.isFocused());

        label(g, "schematics.unrealzaruba.description", left, descriptionTop - 9);
        String count = description.length() + " / " + SchematicsApi.MAX_DESCRIPTION;
        small(g, count, right - (int) (font.width(count) * 0.75f), descriptionTop - 9, MUTED);

        int y = descriptionTop + descriptionHeight + 8;
        label(g, "schematics.unrealzaruba.visibility", left, y);
        y += 9;
        int x = left;
        x += chip(g, x, y, tr("schematics.unrealzaruba.private"), !isPublic, () -> isPublic = false) + 3;
        chip(g, x, y, tr("schematics.unrealzaruba.public"), isPublic, () -> isPublic = true);
        y += 14 + 3;
        small(g, trim(tr(isPublic ? "schematics.unrealzaruba.public_hint" : "schematics.unrealzaruba.private_hint"), (int) (w / 0.75f)), left, y, 0xFF6E7A86);
        y += 13;

        int total = keptImages() + picked.size();
        label(g, "schematics.unrealzaruba.images", left, y);
        String counter = total + " / " + SchematicsApi.MAX_IMAGES;
        small(g, counter, right - (int) (font.width(counter) * 0.75f), y, total > SchematicsApi.MAX_IMAGES ? ERROR : MUTED);
        y += 9;
        small(g, trim(tr("schematics.unrealzaruba.images_hint"), (int) (w / 0.75f)), left, y, 0xFF6E7A86);
        y += 11;

        int cols = 5;
        int tw = (w - GAP * (cols - 1)) / cols;
        int th = tw * 9 / 16;

        // current gallery (editing): click to make it the cover, x to remove
        if (!images.isEmpty()) {
            Long shownCover = coverImage;
            if (shownCover == null) {
                shownCover = images.stream().filter(i -> !removedImages.contains(i.id)).map(i -> i.id).findFirst().orElse(null);
            }
            for (int i = 0; i < images.size(); i++) {
                SchematicDto.Image image = images.get(i);
                int tx = left + i * (tw + GAP);
                boolean removed = removedImages.contains(image.id);
                image(g, SchematicImages.remote(image.url), tx, y, tw, th, null);
                if (removed) g.fill(tx, y, tx + tw, y + th, 0xC00D1014);
                boolean isCover = !removed && Objects.equals(shownCover, image.id);
                g.renderOutline(tx, y, tw, th, isCover ? ACCENT : BORDER);
                if (isCover) small(g, upper(tr("schematics.unrealzaruba.cover")), tx + 3, y + th - 8, ACCENT);
                // remove toggle in the corner
                g.fill(tx + tw - 10, y, tx + tw, y + 10, removed ? 0xFF2C353F : 0xFF5A2326);
                small(g, removed ? "+" : "×", tx + tw - 7, y + 2, TEXT);
                long id = image.id;
                if (!busy) {
                    hits.add(new Hit(tx, y, tx + tw, y + th, () -> {
                        if (!removedImages.contains(id)) coverImage = id;
                    }));
                    hits.add(new Hit(tx + tw - 10, y, tx + tw, y + 10, () -> toggleRemoved(id)));
                }
            }
            y += th + 6;
        }

        // screenshots to add
        label(g, "schematics.unrealzaruba.screenshots", left, y);
        y += 10;
        if (screenshots.isEmpty()) {
            small(g, trim(tr("schematics.unrealzaruba.no_screenshots"), (int) (w / 0.75f)), left, y + 2, MUTED);
            return;
        }
        int top = y;
        int bottom = height - 10;
        int rows = (screenshots.size() + cols - 1) / cols;
        shotMaxScroll = Math.max(0, rows * (th + GAP) - GAP - (bottom - top));
        shotScroll = Mth.clamp(shotScroll, 0, shotMaxScroll);
        List<Path> order = new ArrayList<>(picked);
        g.enableScissor(left, top, right, bottom);
        for (int i = 0; i < screenshots.size(); i++) {
            LocalFiles.LocalFile shot = screenshots.get(i);
            int tx = left + (i % cols) * (tw + GAP);
            int ty = top + (i / cols) * (th + GAP) - shotScroll;
            if (ty + th < top || ty > bottom) continue;
            image(g, SchematicImages.thumbnail(shot.path(), shot.modified()), tx, ty, tw, th, "…");
            int index = order.indexOf(shot.path());
            boolean on = index >= 0;
            g.renderOutline(tx, ty, tw, th, on ? ACCENT : BORDER);
            if (on) {
                // new images go after the kept ones; with no gallery the first pick is the cover
                String n = String.valueOf(keptImages() + index + 1);
                g.fill(tx + 2, ty + 2, tx + 12, ty + 12, ACCENT);
                small(g, n, tx + 7 - (int) (font.width(n) * 0.375f), ty + 4, ON_ACCENT);
            }
            Path path = shot.path();
            if (!busy) {
                hits.add(new Hit(tx, Math.max(ty, top), tx + tw, Math.min(ty + th, bottom), () -> togglePicked(path)));
            }
        }
        g.disableScissor();
    }
}
