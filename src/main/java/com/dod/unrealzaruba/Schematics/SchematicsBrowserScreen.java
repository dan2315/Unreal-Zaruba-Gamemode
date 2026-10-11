package com.dod.unrealzaruba.Schematics;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import javax.annotation.Nullable;

import com.dod.unrealzaruba.Economy.Money;
import com.dod.unrealzaruba.Events.KeyBindings;
import com.dod.unrealzaruba.NetworkPackets.Schematics.SpawnSchematicPiecePacket;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Schematics browser (K): public schematics and the player's own, with search, sorting and pages on the left and the
 * selected schematic's gallery, stats, rating and actions on the right. Downloads go into the Create Aeronautics Toolgun's
 * blueprint folder. Opened from a vehicle purchase block, it also offers Spawn: the client downloads the blueprint and
 * sends it to the server, which places it as a vehicle.
 */
public class SchematicsBrowserScreen extends SchematicsScreenBase {
    private static final int GAP = 6;
    private static final int MIN_CARD_WIDTH = 104;
    private static final int SEARCH_DELAY_TICKS = 8;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault());

    private boolean mine;
    private SchematicsApi.Sort sort = SchematicsApi.Sort.NEWEST;
    private int page = 1;
    private String query = "";
    private int searchDelay;

    private EditBox search;

    private SchematicDto.Page list;
    private boolean listLoading;
    private String listError;
    private int listRequest;

    private SchematicDto selected;
    /** Full version of the selected schematic (images, my rating), once loaded. */
    private SchematicDto detail;
    /** Price breakdown of the selected schematic, fetched when it is opened. */
    private SchematicPriceDto breakdown;
    private long breakdownId = -1;
    private String breakdownError;
    private boolean showBreakdown;
    private int gallery;
    private long confirmDelete = -1;
    private boolean busy;
    /** Select this id once the list arrives (after an upload). */
    private long selectAfterLoad = -1;

    private int scroll;
    private int maxScroll;

    /** The vehicle purchase block this browser was opened from. */
    public record Spawner(BlockPos pos, int price, int cooldownSeconds) {}

    private static final int SPAWN_TIMEOUT_TICKS = 30 * 20;

    @Nullable
    private final Spawner spawner;
    private int cooldownTicks;
    /** Waiting for the server to answer a spawn request; counts down to a timeout. */
    private int spawnWaitTicks;

    public SchematicsBrowserScreen() {
        this(null);
    }

    public SchematicsBrowserScreen(@Nullable Spawner spawner) {
        super(Component.translatable(spawner != null ? "schematics.unrealzaruba.spawner_title" : "schematics.unrealzaruba.title"));
        this.spawner = spawner;
        this.cooldownTicks = spawner != null ? spawner.cooldownSeconds() * 20 : 0;
        reload();
    }

    // ---------- loading ----------

    private void reload() {
        int request = ++listRequest;
        listLoading = true;
        listError = null;
        SchematicsApi.then(SchematicsApi.list(mine, query, sort, page), result -> {
            if (request != listRequest) return;
            listLoading = false;
            if (!result.ok()) {
                listError = result.error();
                return;
            }
            list = result.value();
            if (page > list.pageCount() && list.total > 0) {
                page = list.pageCount();
                reload();
                return;
            }
            long keep = selectAfterLoad >= 0 ? selectAfterLoad : selected != null ? selected.id : -1;
            selectAfterLoad = -1;
            SchematicDto match = list.items.stream().filter(s -> s.id == keep).findFirst().orElse(null);
            if (match != null) {
                select(match);
            } else if (selected == null && !list.items.isEmpty()) {
                select(list.items.get(0));
            }
        });
    }

    private void select(SchematicDto schematic) {
        boolean same = selected != null && selected.id == schematic.id;
        selected = schematic;
        if (!same) {
            detail = null;
            gallery = 0;
            confirmDelete = -1;
            showBreakdown = false;
            breakdown = null;
            breakdownError = null;
            breakdownId = -1;
        }
        loadDetail(schematic.id);
        loadBreakdown(schematic);
    }

    private void loadBreakdown(SchematicDto schematic) {
        if (schematic.price == null || breakdown != null && breakdownId == schematic.id) return;
        long id = schematic.id;
        breakdownId = id;
        SchematicsApi.then(SchematicsApi.price(id), result -> {
            if (selected == null || selected.id != id) return;
            breakdown = result.value();
            breakdownError = result.error();
        });
    }

    private void loadDetail(long id) {
        SchematicsApi.then(SchematicsApi.get(id), result -> {
            if (selected == null || selected.id != id) return;
            if (result.ok()) {
                detail = result.value();
                selected = detail;
                if (detail.images != null && gallery >= detail.images.size()) gallery = 0;
            }
        });
    }

    private void setTab(boolean mine) {
        if (this.mine == mine) return;
        this.mine = mine;
        page = 1;
        scroll = 0;
        selected = null;
        detail = null;
        list = null;
        reload();
    }

    private void setSort(SchematicsApi.Sort sort) {
        if (this.sort == sort) return;
        this.sort = sort;
        page = 1;
        scroll = 0;
        reload();
    }

    private void setPage(int page) {
        this.page = page;
        scroll = 0;
        reload();
    }

    /** Called by the edit screen after a successful upload or save. */
    void onSaved(SchematicDto saved, boolean created) {
        if (created) {
            mine = true;
            page = 1;
            sort = SchematicsApi.Sort.NEWEST;
            selected = null;
            detail = null;
            selectAfterLoad = saved.id;
            showStatus(saved.price != null
                    ? Component.translatable("schematics.unrealzaruba.uploaded_priced", saved.name, Money.format(saved.price))
                    : Component.translatable("schematics.unrealzaruba.uploaded", saved.name), false);
        } else {
            showStatus(Component.translatable("schematics.unrealzaruba.saved"), false);
            if (selected != null && selected.id == saved.id) {
                detail = null;
                breakdown = null; // the file may have been replaced
                breakdownError = null;
                breakdownId = -1;
            }
        }
        reload();
    }

    // ---------- actions ----------

    private void download(SchematicDto s) {
        if (!SchematicsApi.isLoggedIn()) {
            showStatus(Component.translatable("schematics.unrealzaruba.login_required"), true);
            return;
        }
        busy = true;
        SchematicsApi.then(SchematicsApi.download(s.id), result -> {
            busy = false;
            if (!result.ok()) {
                showStatus(error(result.error()), true);
                return;
            }
            try {
                String file = LocalFiles.saveSchematic(s.name, result.value());
                showStatus(Component.translatable("schematics.unrealzaruba.downloaded", file), false);
            } catch (IOException e) {
                showStatus(error(e.getMessage()), true);
            }
        });
    }

    /** Downloads the blueprint and sends it to the server to spawn at the purchase block. The server re-checks everything. */
    private void spawn(SchematicDto s) {
        if (!SchematicsApi.isLoggedIn()) {
            showStatus(Component.translatable("schematics.unrealzaruba.login_required"), true);
            return;
        }
        busy = true;
        spawnWaitTicks = SPAWN_TIMEOUT_TICKS;
        SchematicsApi.then(SchematicsApi.download(s.id), result -> {
            if (spawnWaitTicks == 0) return; // timed out or closed
            if (!result.ok()) {
                spawnWaitTicks = 0;
                busy = false;
                showStatus(error(result.error()), true);
                return;
            }
            byte[] data = result.value();
            String name = s.name.length() > 64 ? s.name.substring(0, 64) : s.name;
            for (int offset = 0; offset < data.length; offset += SpawnSchematicPiecePacket.MAX_PIECE) {
                byte[] piece = java.util.Arrays.copyOfRange(data, offset, Math.min(data.length, offset + SpawnSchematicPiecePacket.MAX_PIECE));
                PacketDistributor.sendToServer(new SpawnSchematicPiecePacket(spawner.pos(), s.id, name, data.length, offset, piece));
            }
        });
    }

    /** The server answered a spawn request. Returns false if the message should go to the action bar instead. */
    boolean onSpawnResult(boolean success, Component message) {
        if (spawnWaitTicks == 0) return false;
        spawnWaitTicks = 0;
        busy = false;
        if (success) {
            onClose();
            return false;
        }
        showStatus(message, true);
        return true;
    }

    private void rate(SchematicDto s, int score) {
        busy = true;
        SchematicsApi.then(SchematicsApi.rate(s.id, score), result -> {
            busy = false;
            if (!result.ok()) {
                showStatus(error(result.error()), true);
                return;
            }
            showStatus(Component.translatable("schematics.unrealzaruba.rated", score), false);
            loadDetail(s.id);
        });
    }

    private void delete(SchematicDto s) {
        if (confirmDelete != s.id) {
            confirmDelete = s.id;
            return;
        }
        confirmDelete = -1;
        busy = true;
        SchematicsApi.then(SchematicsApi.delete(s.id), result -> {
            busy = false;
            if (!result.ok()) {
                showStatus(error(result.error()), true);
                return;
            }
            showStatus(Component.translatable("schematics.unrealzaruba.deleted", s.name), false);
            selected = null;
            detail = null;
            reload();
        });
    }

    private void openUpload() {
        if (!SchematicsApi.isLoggedIn()) {
            showStatus(Component.translatable("schematics.unrealzaruba.login_required"), true);
            return;
        }
        minecraft.setScreen(new SchematicEditScreen(this, null));
    }

    private boolean isMine(SchematicDto s) {
        return mine || s.author.equalsIgnoreCase(Minecraft.getInstance().getUser().getName());
    }

    // ---------- screen ----------

    @Override
    protected void init() {
        search = new EditBox(font, 0, 0, 100, 10, Component.translatable("schematics.unrealzaruba.search"));
        search.setBordered(false);
        search.setMaxLength(64);
        search.setValue(query);
        search.setHint(Component.translatable("schematics.unrealzaruba.search").withStyle(ChatFormatting.DARK_GRAY));
        search.setResponder(value -> {
            if (!value.equals(query)) {
                query = value;
                searchDelay = SEARCH_DELAY_TICKS;
            }
        });
        addRenderableWidget(search);
    }

    @Override
    public void tick() {
        super.tick();
        if (cooldownTicks > 0) cooldownTicks--;
        if (spawnWaitTicks > 0 && --spawnWaitTicks == 0) {
            busy = false;
            showStatus(Component.translatable("schematics.unrealzaruba.spawn.no_answer"), true);
        }
        if (searchDelay > 0 && --searchDelay == 0) {
            page = 1;
            scroll = 0;
            reload();
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scroll = Mth.clamp(scroll - (int) (scrollY * 20), 0, maxScroll);
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!typing() && KeyBindings.OPEN_SCHEMATICS.matches(keyCode, scanCode)) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private int detailWidth() {
        return Mth.clamp(width * 36 / 100, 170, 240);
    }

    @Override
    protected void renderContent(GuiGraphics g, int mouseX, int mouseY) {
        header(g, title);
        int x = 10 + font.width(title.copy().withStyle(ChatFormatting.BOLD)) + 16;
        x += tab(g, x, tr("schematics.unrealzaruba.tab.public"), !mine, () -> setTab(false)) + 2;
        tab(g, x, tr("schematics.unrealzaruba.tab.mine"), mine, () -> setTab(true));

        String upload = "+ " + tr("schematics.unrealzaruba.upload");
        int uploadWidth = font.width(upper(upload)) + 16;
        button(g, width - 10 - uploadWidth, 5, uploadWidth, HEADER - 10, upload, Style.PRIMARY, true, this::openUpload);

        int detailW = detailWidth();
        int right = width - detailW - 10;
        renderToolbar(g, 10, right);
        renderList(g, mouseX, mouseY, 10, right);

        g.fill(width - detailW, HEADER, width, height, SIDE);
        g.fill(width - detailW, HEADER, width - detailW + 1, height, BORDER);
        if (selected != null) {
            renderDetail(g, mouseX, mouseY, width - detailW + 8, width - 8);
        }
    }

    private void renderToolbar(GuiGraphics g, int left, int right) {
        int y = HEADER + 6;
        int searchWidth = Math.min(170, (right - left) * 45 / 100);
        field(g, left, y, searchWidth, 16, search.isFocused());
        search.setX(left + 5);
        search.setY(y + 4);
        search.setWidth(searchWidth - 10);

        int x = left + searchWidth + 8;
        for (SchematicsApi.Sort s : SchematicsApi.Sort.values()) {
            x += chip(g, x, y + 1, tr("schematics.unrealzaruba.sort." + s.param), sort == s, () -> setSort(s)) + 3;
        }

        if (list != null && list.pageCount() > 1) {
            String pages = page + " / " + list.pageCount();
            int pw = font.width(pages);
            int nextX = right - 14;
            int pagesX = nextX - 6 - pw;
            int prevX = pagesX - 20;
            if (prevX > x + 4) {
                button(g, prevX, y + 1, 14, 14, "<", Style.NORMAL, page > 1, () -> setPage(page - 1));
                g.drawString(font, pages, pagesX, y + 4, MUTED, false);
                button(g, nextX, y + 1, 14, 14, ">", Style.NORMAL, page < list.pageCount(), () -> setPage(page + 1));
            }
        }
    }

    private void renderList(GuiGraphics g, int mouseX, int mouseY, int left, int right) {
        int top = HEADER + 30;
        int bottom = height - 8;
        int centerX = (left + right) / 2;
        int centerY = (top + bottom) / 2;

        if (mine && !SchematicsApi.isLoggedIn()) {
            g.drawCenteredString(font, Component.translatable("schematics.unrealzaruba.login_required"), centerX, centerY, MUTED);
            return;
        }
        if (listError != null && list == null) {
            g.drawCenteredString(font, error(listError), centerX, centerY - 10, ERROR);
            button(g, centerX - 40, centerY + 4, 80, 16, tr("schematics.unrealzaruba.retry"), Style.NORMAL, true, this::reload);
            return;
        }
        if (list == null) {
            g.drawCenteredString(font, Component.translatable("schematics.unrealzaruba.loading"), centerX, centerY, MUTED);
            return;
        }
        if (list.items.isEmpty()) {
            String key = !query.isBlank() ? "schematics.unrealzaruba.no_results"
                    : mine ? "schematics.unrealzaruba.empty_mine" : "schematics.unrealzaruba.empty";
            g.drawCenteredString(font, Component.translatable(key), centerX, centerY, MUTED);
            return;
        }

        int areaWidth = right - left;
        int cols = Math.max(1, (areaWidth + GAP) / (MIN_CARD_WIDTH + GAP));
        int cardWidth = (areaWidth - GAP * (cols - 1)) / cols;
        int imageHeight = cardWidth * 9 / 16;
        int cardHeight = imageHeight + 30;
        int rows = (list.items.size() + cols - 1) / cols;
        maxScroll = Math.max(0, rows * (cardHeight + GAP) - GAP - (bottom - top));
        scroll = Mth.clamp(scroll, 0, maxScroll);

        g.enableScissor(left, top, right, bottom);
        for (int i = 0; i < list.items.size(); i++) {
            SchematicDto s = list.items.get(i);
            int cx = left + (i % cols) * (cardWidth + GAP);
            int cy = top + (i / cols) * (cardHeight + GAP) - scroll;
            if (cy + cardHeight < top || cy > bottom) continue;

            boolean on = selected != null && selected.id == s.id;
            boolean hover = mouseX >= cx && mouseX < cx + cardWidth && mouseY >= cy && mouseY < cy + cardHeight
                    && mouseY >= top && mouseY < bottom;
            g.fill(cx, cy, cx + cardWidth, cy + cardHeight, on ? CARD_SELECTED : CARD);
            image(g, SchematicImages.remote(s.previewUrl), cx + 1, cy + 1, cardWidth - 2, imageHeight - 1,
                    s.previewUrl == null ? tr("schematics.unrealzaruba.no_preview") : null);
            g.renderOutline(cx, cy, cardWidth, cardHeight, on ? ACCENT : hover ? MUTED : BORDER);
            String price = priceText(s);
            int pw = (int) (font.width(price) * 0.75f) + 6;
            g.fill(cx + cardWidth - 1 - pw, cy + imageHeight - 11, cx + cardWidth - 1, cy + imageHeight - 1, 0xCC0B0E12);
            small(g, price, cx + cardWidth - 1 - pw + 3, cy + imageHeight - 9, s.price != null ? ACCENT : MUTED);

            int ty = cy + imageHeight + 4;
            g.drawString(font, trim(s.name, cardWidth - 10), cx + 5, ty, TEXT, false);
            String rating = ratingText(s);
            int ratingWidth = (int) (font.width(rating) * 0.75f);
            small(g, trim(s.author, (int) ((cardWidth - 16 - ratingWidth) / 0.75f)), cx + 5, ty + 12, MUTED);
            small(g, rating, cx + cardWidth - 5 - ratingWidth, ty + 12, s.rating != null ? ACCENT : MUTED);
            if (!s.isPublic) {
                String badge = upper(tr("schematics.unrealzaruba.private"));
                int bw = (int) (font.width(badge) * 0.75f) + 6;
                g.fill(cx + 3, cy + 3, cx + 3 + bw, cy + 12, 0xCC0B0E12);
                small(g, badge, cx + 6, cy + 5, MUTED);
            }
            hits.add(new Hit(cx, Math.max(cy, top), cx + cardWidth, Math.min(cy + cardHeight, bottom), () -> select(s)));
        }
        g.disableScissor();
        if (listLoading) {
            small(g, tr("schematics.unrealzaruba.loading"), right - (int) (font.width(tr("schematics.unrealzaruba.loading")) * 0.75f), HEADER + 30 - 9, MUTED);
        }
    }

    private void renderDetail(GuiGraphics g, int mouseX, int mouseY, int left, int right) {
        SchematicDto s = detail != null ? detail : selected;
        int w = right - left;
        int y = HEADER + 8;

        // gallery
        List<SchematicDto.Image> images = s.images != null ? s.images : List.of();
        String shown = !images.isEmpty() ? images.get(Mth.clamp(gallery, 0, images.size() - 1)).url : s.previewUrl;
        int imageHeight = w * 9 / 16;
        image(g, SchematicImages.remote(shown), left, y, w, imageHeight,
                shown == null ? tr("schematics.unrealzaruba.no_preview") : null);
        g.renderOutline(left, y, w, imageHeight, BORDER);
        y += imageHeight + 4;
        if (images.size() > 1) {
            int tw = (w - 4 * 3) / 5;
            int th = tw * 9 / 16;
            for (int i = 0; i < images.size(); i++) {
                int tx = left + i * (tw + 3);
                image(g, SchematicImages.remote(images.get(i).url), tx, y, tw, th, null);
                g.renderOutline(tx, y, tw, th, i == gallery ? ACCENT : BORDER);
                int index = i;
                hits.add(new Hit(tx, y, tx + tw, y + th, () -> gallery = index));
            }
            y += th + 5;
        }

        // title
        g.drawString(font, Component.literal(trim(s.name, w)).withStyle(ChatFormatting.BOLD), left, y, TEXT, false);
        y += 11;
        String by = tr("schematics.unrealzaruba.by", s.author);
        small(g, trim(by, (int) (w / 0.75f) - 50), left, y, MUTED);
        if (isMine(s)) {
            String vis = upper(tr(s.isPublic ? "schematics.unrealzaruba.public" : "schematics.unrealzaruba.private"));
            small(g, vis, right - (int) (font.width(vis) * 0.75f), y, s.isPublic ? SUCCESS : MUTED);
        }
        y += 11;

        // actions are laid out from the bottom up; the description takes what's left in between
        boolean own = isMine(s);
        boolean canRate = SchematicsApi.isLoggedIn() && !own;
        int bottom = height - 8;
        int buttonsTop = bottom - 18 - (own ? 21 : 0) - (spawner != null ? 21 : 0);
        int ratingTop = buttonsTop - (canRate ? 26 : 0);
        int statsTop = ratingTop - 5 * 9 - 6;

        if (showBreakdown && s.price != null) {
            renderBreakdown(g, left, right, y, statsTop - 4);
        } else if (s.description != null && !s.description.isBlank()) {
            List<FormattedCharSequence> lines = font.split(Component.literal(s.description), w);
            int maxLines = Math.max(0, (statsTop - 4 - y) / 10);
            for (int i = 0; i < Math.min(lines.size(), maxLines); i++) {
                boolean last = i == maxLines - 1 && lines.size() > maxLines;
                g.drawString(font, last ? Component.literal("…").getVisualOrderText() : lines.get(i), left, y, 0xFFC3CAD2, false);
                y += 10;
            }
        }

        // stats
        int sy = statsTop;
        String priceLabel = tr("schematics.unrealzaruba.stat.price") + (s.price == null ? "" : showBreakdown ? " [-]" : " [+]");
        stat(g, left, right, sy, priceLabel, priceText(s), s.price != null ? ACCENT : MUTED);
        if (s.price != null) {
            hits.add(new Hit(left, sy - 1, right, sy + 9, () -> showBreakdown = !showBreakdown));
        }
        sy += 9;
        stat(g, left, right, sy, tr("schematics.unrealzaruba.stat.rating"), s.rating != null
                ? String.format(Locale.ROOT, "%.2f / 10  (%d)", s.rating, s.ratingsCount) : tr("schematics.unrealzaruba.no_rating"));
        stat(g, left, right, sy + 9, tr("schematics.unrealzaruba.stat.battles"), String.valueOf(s.battlesCount));
        stat(g, left, right, sy + 18, tr("schematics.unrealzaruba.stat.size"), LocalFiles.formatSize(s.fileSize));
        stat(g, left, right, sy + 27, tr("schematics.unrealzaruba.stat.updated"), date(s.updatedAt != null ? s.updatedAt : s.createdAt));

        // rating
        if (canRate) {
            int ry = ratingTop;
            label(g, "schematics.unrealzaruba.your_rating", left, ry);
            ry += 9;
            int cell = (w - 9 * 2) / 10;
            int hovered = -1;
            for (int i = 0; i < 10; i++) {
                int cx = left + i * (cell + 2);
                if (mouseX >= cx && mouseX < cx + cell && mouseY >= ry && mouseY < ry + 12) hovered = i + 1;
            }
            int filled = hovered > 0 ? hovered : s.myRating != null ? s.myRating : 0;
            for (int i = 0; i < 10; i++) {
                int cx = left + i * (cell + 2);
                boolean on = i < filled;
                g.fill(cx, ry, cx + cell, ry + 12, on ? (hovered > 0 ? 0xFFF7C27A : ACCENT) : CARD);
                g.renderOutline(cx, ry, cell, 12, on ? ACCENT : BORDER);
                String n = String.valueOf(i + 1);
                small(g, n, cx + (cell - (int) (font.width(n) * 0.75f)) / 2 + 1, ry + 4, on ? ON_ACCENT : MUTED);
                int score = i + 1;
                if (!busy) hits.add(new Hit(cx, ry, cx + cell, ry + 12, () -> rate(s, score)));
            }
        }

        // buttons
        int actionY = buttonsTop;
        if (spawner != null) {
            String label = busy ? tr("schematics.unrealzaruba.working")
                    : cooldownTicks > 0 ? tr("schematics.unrealzaruba.spawn.wait", (cooldownTicks + 19) / 20)
                    : tr("schematics.unrealzaruba.spawn", Money.format(spawner.price()));
            button(g, left, actionY, w, 18, label, Style.PRIMARY, !busy && cooldownTicks == 0, () -> spawn(s));
            actionY += 21;
        }
        button(g, left, actionY, w, 18, tr(busy && spawner == null ? "schematics.unrealzaruba.working" : "schematics.unrealzaruba.download"),
                spawner != null ? Style.NORMAL : Style.PRIMARY, !busy, () -> download(s));
        if (own) {
            int half = (w - 4) / 2;
            int by2 = actionY + 21;
            button(g, left, by2, half, 18, tr("schematics.unrealzaruba.edit"), Style.NORMAL, !busy && detail != null,
                    () -> minecraft.setScreen(new SchematicEditScreen(this, detail)));
            button(g, left + half + 4, by2, w - half - 4, 18,
                    tr(confirmDelete == s.id ? "schematics.unrealzaruba.delete_confirm" : "schematics.unrealzaruba.delete"),
                    Style.DANGER, !busy, () -> delete(s));
        }
    }

    private void stat(GuiGraphics g, int left, int right, int y, String label, String value) {
        stat(g, left, right, y, label, value, TEXT);
    }

    private void stat(GuiGraphics g, int left, int right, int y, String label, String value, int valueColor) {
        small(g, upper(label), left, y, MUTED);
        small(g, value, right - (int) (font.width(value) * 0.75f), y, valueColor);
    }

    /** Sums per kind, the costliest lines and a note about unpriced things, as many rows as fit between top and bottom. */
    private void renderBreakdown(GuiGraphics g, int left, int right, int top, int bottom) {
        int rows = Math.max(0, (bottom - top) / 9);
        if (breakdown == null) {
            if (rows > 0) small(g, breakdownError != null ? breakdownError : tr("schematics.unrealzaruba.loading"), left, top,
                    breakdownError != null ? ERROR : MUTED);
            return;
        }
        SchematicPriceDto b = breakdown;
        int y = top;
        int used = 0;
        String[][] sums = {
                {"schematics.unrealzaruba.price.blocks", Money.format(b.blocks)},
                {"schematics.unrealzaruba.price.items", Money.format(b.items)},
                {"schematics.unrealzaruba.price.fluids", Money.format(b.fluids)}};
        for (String[] sum : sums) {
            if (used++ >= rows) return;
            stat(g, left, right, y, tr(sum[0]), sum[1]);
            y += 9;
        }
        int reserve = b.unpriced.isEmpty() ? 0 : 1;
        int lineRows = Math.max(0, rows - used - reserve - 1);
        if (lineRows > 0 && !b.lines.isEmpty()) {
            g.fill(left, y + 1, right, y + 2, BORDER);
            y += 4;
            lineRows = Math.max(0, lineRows - 1);
        }
        int maxName = (right - left) * 62 / 100;
        for (int i = 0; i < Math.min(lineRows, b.lines.size()); i++) {
            SchematicPriceDto.Line line = b.lines.get(i);
            String amount = "fluid".equals(line.kind) ? bucketsText(line.count) : line.count + "x";
            String name = trim(amount + " " + shortId(line.id), (int) (maxName / 0.75f));
            small(g, name, left, y, 0xFFC3CAD2);
            String subtotal = Money.format(line.subtotal);
            small(g, subtotal, right - (int) (font.width(subtotal) * 0.75f), y, TEXT);
            y += 9;
        }
        if (reserve == 1 && y + 9 <= bottom + 1) {
            small(g, trim(tr("schematics.unrealzaruba.price.unpriced", b.unpriced.size()), (int) ((right - left) / 0.75f)), left, bottom - 9, ERROR);
        }
    }

    private static String priceText(SchematicDto s) {
        return s.price != null ? Money.format(s.price) : "—";
    }

    private static String shortId(String id) {
        int colon = id.indexOf(':');
        return colon >= 0 && id.startsWith("minecraft:") ? id.substring(colon + 1) : id;
    }

    /** Fluid stacks come in millibuckets; shown as buckets. */
    private static String bucketsText(long millibuckets) {
        double buckets = millibuckets / 1000.0;
        return (buckets == Math.rint(buckets) ? String.format(Locale.ROOT, "%.0f", buckets) : String.format(Locale.ROOT, "%.2f", buckets)) + " B";
    }

    private static String ratingText(SchematicDto s) {
        return s.rating != null ? String.format(Locale.ROOT, "★ %.1f", s.rating) : "★ —";
    }

    private static String date(String iso) {
        if (iso == null) return "—";
        try {
            return DATE.format(Instant.parse(iso));
        } catch (RuntimeException e) {
            return iso.length() >= 10 ? iso.substring(0, 10) : iso;
        }
    }
}
