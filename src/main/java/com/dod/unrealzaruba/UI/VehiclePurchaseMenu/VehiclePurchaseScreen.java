package com.dod.unrealzaruba.UI.VehiclePurchaseMenu;
import net.neoforged.neoforge.network.PacketDistributor;

import com.dod.unrealzaruba.NetworkPackets.VehiclePurchase.PurchaseVehiclePacket;
import com.dod.unrealzaruba.Economy.Money;
import com.dod.unrealzaruba.UI.MoneyOverlay;
import com.dod.unrealzaruba.UnrealZaruba;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.GameRenderer;
import java.util.ArrayList;
import java.util.List;

import com.dod.unrealzaruba.Vehicles.VehicleRegistry;
import com.dod.unrealzaruba.Vehicles.VehicleData;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;


public class VehiclePurchaseScreen extends AbstractContainerScreen<VehiclePurchaseMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(UnrealZaruba.MOD_ID, "textures/gui/vehicle_purchase.png");
    private static final int DROPDOWN_HEIGHT = 20;
    private static final int DROPDOWN_WIDTH = 120;
    private static final int ERROR_DISPLAY_TICKS = 100;

    private final List<VehiclePurchaseMenu.Offer> offers;
    private boolean dropdownOpen = false;
    private Button deployButton;
    private List<Button> dropdownButtons = new ArrayList<>();
    private String errorMessage = null;
    private int errorDisplayTicks = 0;
    public VehiclePurchaseScreen(VehiclePurchaseMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;

        offers = menu.getOffers();

        boolean selectedIsOffered = offers.stream().anyMatch(offer -> offer.vehicleKey().equals(menu.getSelectedVehicle()));
        if (!selectedIsOffered && !offers.isEmpty()) {
            menu.setSelectedVehicle(offers.get(0).vehicleKey());
        }
    }

    private VehiclePurchaseMenu.Offer selectedOffer() {
        String selected = menu.getSelectedVehicle();
        return offers.stream().filter(offer -> offer.vehicleKey().equals(selected)).findFirst().orElse(null);
    }

    @Override
    protected void init() {
        super.init();

        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        addRenderableWidget(Button.builder(Component.literal("▼"), button -> {
            dropdownOpen = !dropdownOpen;
            updateDropdown();
        }).pos(x + 140, y + 20).size(20, DROPDOWN_HEIGHT).build());


        deployButton = addRenderableWidget(Button.builder(Component.translatable("button.unrealzaruba.deploy"), button -> {
            if (selectedOffer() != null) {
                var result = menu.getBlockEntity().getBlockPos();
                PacketDistributor.sendToServer(
                    new PurchaseVehiclePacket(result, menu.getSelectedVehicle())
                );
            }
        }).pos(x + 48, y + 130).size(80, 20).build());

        updateDropdown();
        updateDeployButton();
    }

    private void updateDropdown() {
        for (Button button : dropdownButtons) {
            removeWidget(button);
        }
        dropdownButtons.clear();

        if (dropdownOpen) {
            int x = (width - imageWidth) / 2;
            int y = (height - imageHeight) / 2;

            for (int i = 0; i < offers.size(); i++) {
                VehiclePurchaseMenu.Offer offer = offers.get(i);
                VehicleData vehicle = VehicleRegistry.getVehicle(offer.vehicleKey());
                String label = Component.translatable(vehicle.getName()).getString() + "  " + Money.format(offer.price());
                Button button = Button.builder(Component.literal(label), btn -> {
                    menu.setSelectedVehicle(offer.vehicleKey());
                    dropdownOpen = false;
                    updateDropdown();
                    updateDeployButton();
                }).pos(x + 20, y + 20 + DROPDOWN_HEIGHT * (i + 1)).size(DROPDOWN_WIDTH, DROPDOWN_HEIGHT).build();

                addRenderableWidget(button);
                dropdownButtons.add(button);
            }
        }
    }

    private void updateDeployButton() {
        VehiclePurchaseMenu.Offer offer = selectedOffer();
        boolean canAfford = offer != null && MoneyOverlay.INSTANCE.getBalance() >= offer.price();
        deployButton.active = canAfford;
        deployButton.setMessage(Component.translatable(offer == null || canAfford
                ? "button.unrealzaruba.deploy" : "button.unrealzaruba.no_money"));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.render(graphics, mouseX, mouseY, delta);

        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        VehiclePurchaseMenu.Offer offer = selectedOffer();
        if (offer == null) {
            graphics.drawString(font, Component.translatable("label.unrealzaruba.no_vehicles"), x + 25, y + 25, 0x404040, false);
        } else {
            graphics.drawString(font, Component.translatable(VehicleRegistry.getVehicle(offer.vehicleKey()).getName()), x + 25, y + 25, 0x404040, false);
        }

        if (!dropdownOpen) {
            if (offer != null) {
                boolean canAfford = MoneyOverlay.INSTANCE.getBalance() >= offer.price();
                graphics.drawString(font, Component.translatable("label.unrealzaruba.price"), x + 25, y + 55, 0x404040, false);
                graphics.drawString(font, Money.format(offer.price()), x + 25, y + 67, canAfford ? 0x404040 : 0xAA0000, false);
            }
            graphics.drawString(font, Component.translatable("label.unrealzaruba.balance"), x + 25, y + 85, 0x404040, false);
            graphics.drawString(font, Money.format(MoneyOverlay.INSTANCE.getBalance()), x + 25, y + 97, 0x404040, false);
        }

        if (errorMessage != null && errorDisplayTicks > 0) {
            int errorX = x + (imageWidth / 2) - (font.width(errorMessage) / 2);
            int errorY = y + 115;

            graphics.drawString(font, errorMessage, errorX, errorY, 0xAA0000, false);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);

        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        graphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight, imageWidth, imageHeight);

        graphics.fill(x + 20, y + 20, x + 140, y + 20 + DROPDOWN_HEIGHT, 0xFFCCCCCC);
        graphics.fill(x + 21, y + 21, x + 139, y + 20 + DROPDOWN_HEIGHT - 1, 0xFFFFFFFF);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // graphics.drawString(font, title, titleLabelX, titleLabelY, 0x404040);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (dropdownOpen) {
            int x = (width - imageWidth) / 2;
            int y = (height - imageHeight) / 2;

            boolean clickedInDropdown = mouseX >= x + 20 && mouseX <= x + 20 + DROPDOWN_WIDTH &&
                mouseY >= y + 20 && mouseY <= y + 20 + DROPDOWN_HEIGHT * (offers.size() + 1);

            if (!clickedInDropdown) {
                dropdownOpen = false;
                updateDropdown();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        updateDeployButton();

        if (errorDisplayTicks > 0) {
            errorDisplayTicks--;
            if (errorDisplayTicks <= 0) {
                errorMessage = null;
            }
        }
    }

    public void setErrorMessage(String message) {
        errorMessage = message;
        errorDisplayTicks = ERROR_DISPLAY_TICKS;
    }
}
