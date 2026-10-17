package com.dod.unrealzaruba.Schematics;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.dod.unrealzaruba.Economy.Money;
import com.dod.unrealzaruba.NetworkPackets.Schematics.ContributePurchasePacket;
import com.dod.unrealzaruba.UI.AmountSlider;
import com.dod.unrealzaruba.UI.Brand;
import com.dod.unrealzaruba.UI.HudStyle;
import com.dod.unrealzaruba.UI.MoneyOverlay;
import com.dod.unrealzaruba.UI.UiFont;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The vehicle being bought together at a purchase block: only that vehicle, how much of the price is covered, the
 * seconds left and a slider for how much to add. Closes itself when the purchase ends.
 */
public class PurchaseSessionScreen extends Screen {
    private static final int WIDTH = 320;
    private static final int PAD = 14;
    private static final int WINDOW_MILLIS = 15_000;

    private record Hit(int x0, int y0, int x1, int y1, Runnable action) {
        boolean contains(double x, double y) {
            return x >= x0 && x < x1 && y >= y0 && y < y1;
        }
    }

    private final BlockPos pos;
    private final AmountSlider slider = new AmountSlider();
    private final List<Hit> hits = new ArrayList<>();
    private boolean sliderSet;
    private int lastPaid = -1;

    public PurchaseSessionScreen(BlockPos pos) {
        super(Component.translatable("schematics.unrealzaruba.purchase.title"));
        this.pos = pos;
    }

    @Override
    protected void init() {
        this.font = UiFont.get();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, width, height, Brand.alpha(Brand.WINDOW, 0x99));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g, mouseX, mouseY, partialTick);
        hits.clear();
        PurchaseClientState.View view = PurchaseClientState.get(pos);
        if (view == null) {
            onClose(); // it ended: bought or refunded
            return;
        }

        int balance = MoneyOverlay.INSTANCE.getBalance();
        int minimum = view.minimumContribution();
        int maximum = Math.min(view.remaining(), balance);
        slider.range(minimum, maximum);
        if (!sliderSet) {
            slider.setValue(maximum); // everything that can still be put in, to start with
            sliderSet = true;
        }
        if (view.paid() != lastPaid) {
            lastPaid = view.paid();
            slider.range(minimum, maximum);
        }

        List<Map.Entry<String, Integer>> contributors = new ArrayList<>(view.contributors().entrySet());
        int rows = Math.min(4, contributors.size());
        int panelHeight = 168 + rows * 11;
        int x = (width - WIDTH) / 2;
        int y = (height - panelHeight) / 2;
        int inner = WIDTH - 2 * PAD;

        g.fill(x, y, x + WIDTH, y + panelHeight, Brand.alpha(Brand.PANEL, 0xF2));
        g.renderOutline(x, y, WIDTH, panelHeight, Brand.BORDER);
        Brand.gradient(g, x, y, WIDTH, 2, Brand.MINT_DIM, Brand.CYAN_DIM, Brand.BLUE);

        int left = x + PAD;
        int cy = y + PAD;
        HudStyle.text(g, font, Component.translatable("schematics.unrealzaruba.purchase.title").getString().toUpperCase(),
                left, cy, Brand.MINT, 0.8f);
        cy += 12;
        g.drawString(font, Component.literal(HudStyle.trim(font, view.name(), inner)).withStyle(UiFont.bold()), left, cy, Brand.TEXT, false);
        cy += 13;
        HudStyle.text(g, font, Component.translatable("schematics.unrealzaruba.purchase.started_by", view.initiator()).getString(),
                left, cy, Brand.TEXT_MUTE, 0.85f);
        cy += 14;

        // How much of the price is covered
        g.fill(left, cy, left + inner, cy + 8, Brand.BAR_TRACK);
        int filled = Math.round(inner * view.fraction());
        if (filled > 0) Brand.gradient(g, left, cy, filled, 8, Brand.MINT_DIM, Brand.CYAN_DIM, Brand.BLUE);
        cy += 12;
        String covered = Money.format(view.paid()) + " / " + Money.format(view.price());
        g.drawString(font, covered, left, cy, Brand.TEXT, false);
        String percent = Math.round(view.fraction() * 100) + "%";
        g.drawString(font, percent, left + inner - font.width(percent), cy, Brand.TEXT_DIM, false);
        cy += 14;

        // The time left until everybody gets their money back
        int seconds = view.secondsLeft();
        long leftMillis = Math.max(0, view.deadlineMillis() - System.currentTimeMillis());
        String time = Component.translatable("schematics.unrealzaruba.purchase.time_left", seconds).getString();
        int timeColor = seconds <= 3 ? Brand.WARN : Brand.TEXT_DIM;
        g.drawString(font, time, left, cy, timeColor, false);
        int timerWidth = Math.round(inner * Math.min(1f, leftMillis / (float) WINDOW_MILLIS));
        g.fill(left, cy + 11, left + inner, cy + 13, Brand.BAR_TRACK);
        if (timerWidth > 0) g.fill(left, cy + 11, left + timerWidth, cy + 13, timeColor);
        cy += 20;

        for (int i = 0; i < rows; i++) {
            var entry = contributors.get(i);
            g.drawString(font, HudStyle.trim(font, entry.getKey(), inner - 60), left, cy, Brand.TEXT_DIM, false);
            String amount = Money.format(entry.getValue());
            g.drawString(font, amount, left + inner - font.width(amount), cy, Brand.TEXT_DIM, false);
            cy += 11;
        }
        if (rows > 0) cy += 4;

        // How much to add
        boolean canPay = maximum >= minimum && maximum > 0;
        String amountText = Component.translatable("schematics.unrealzaruba.purchase.contribute", Money.format(canPay ? slider.value() : 0)).getString();
        g.drawString(font, amountText, left, cy, canPay ? Brand.TEXT : Brand.TEXT_MUTE, false);
        String wallet = Component.translatable("schematics.unrealzaruba.purchase.wallet", Money.format(balance)).getString();
        g.drawString(font, wallet, left + inner - font.width(wallet), cy, Brand.TEXT_MUTE, false);
        cy += 14;
        slider.render(g, left, cy, inner, mouseX, mouseY, slider.usable());
        cy += 22;

        int half = (inner - 8) / 2;
        int amount = slider.value();
        boolean enabled = canPay && amount >= minimum && amount <= maximum && balance >= amount;
        primaryButton(g, left, cy, half, 22, Component.translatable("schematics.unrealzaruba.purchase.pay", Money.format(canPay ? amount : 0)).getString(),
                enabled, mouseX, mouseY, () -> PacketDistributor.sendToServer(new ContributePurchasePacket(pos, slider.value())));
        plainButton(g, left + half + 8, cy, inner - half - 8, 22, Component.translatable("schematics.unrealzaruba.purchase.close").getString(),
                mouseX, mouseY, this::onClose);
    }

    private void primaryButton(GuiGraphics g, int x, int y, int w, int h, String label, boolean enabled, int mouseX, int mouseY, Runnable action) {
        boolean hover = enabled && mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
        if (!enabled) {
            g.fill(x, y, x + w, y + h, Brand.DISABLED);
        } else if (hover) {
            Brand.gradient(g, x, y, w, h, Brand.MINT, Brand.CYAN, Brand.BLUE_BRIGHT);
        } else {
            Brand.gradient(g, x, y, w, h, Brand.MINT_DIM, Brand.CYAN_DIM, Brand.BLUE);
        }
        String text = HudStyle.trim(font, label.toUpperCase(), w - 8);
        g.drawString(font, Component.literal(text).withStyle(UiFont.bold()), x + (w - font.width(text)) / 2, y + (h - 8) / 2 + 1,
                enabled ? Brand.TEXT : Brand.TEXT_MUTE, false);
        if (enabled) hits.add(new Hit(x, y, x + w, y + h, action));
    }

    private void plainButton(GuiGraphics g, int x, int y, int w, int h, String label, int mouseX, int mouseY, Runnable action) {
        boolean hover = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
        g.fill(x, y, x + w, y + h, hover ? Brand.BTN_HI : Brand.BTN);
        g.renderOutline(x, y, w, h, hover ? Brand.MINT : Brand.BTN_BORDER);
        g.drawString(font, label, x + (w - font.width(label)) / 2, y + (h - 8) / 2 + 1, Brand.TEXT, false);
        hits.add(new Hit(x, y, x + w, y + h, action));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (slider.mouseClicked(mouseX, mouseY)) return true;
            for (Hit hit : hits) {
                if (hit.contains(mouseX, mouseY)) {
                    hit.action().run();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (slider.mouseDragged(mouseX)) return true;
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        slider.mouseReleased();
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (slider.keyPressed(keyCode, (modifiers & 1) != 0)) return true;
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
