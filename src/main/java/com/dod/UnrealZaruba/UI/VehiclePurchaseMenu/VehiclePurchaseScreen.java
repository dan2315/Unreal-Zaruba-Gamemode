package com.dod.UnrealZaruba.UI.VehiclePurchaseMenu;

import com.dod.UnrealZaruba.NetworkPackets.NetworkHandler;
import com.dod.UnrealZaruba.NetworkPackets.VehiclePurchase.PurchaseVehiclePacket;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.ItemStack;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.GameRenderer;
import java.util.ArrayList;
import java.util.List;

import com.dod.UnrealZaruba.UnrealZaruba;
import com.dod.UnrealZaruba.Vehicles.VehicleRegistry;
import com.dod.UnrealZaruba.Vehicles.VehicleData;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;



public class VehiclePurchaseScreen extends AbstractContainerScreen<VehiclePurchaseMenu> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(UnrealZaruba.MOD_ID, "textures/gui/vehicle_purchase.png");
    private static final int DROPDOWN_HEIGHT = 20;
    private static final int DROPDOWN_WIDTH = 120;
    private static final int ERROR_DISPLAY_TICKS = 100;

    private List<String> vehicleKeys;
    private boolean dropdownOpen = false;
    private Button deployButton;
    private List<Button> dropdownButtons = new ArrayList<>();
    private List<ItemStack> requiredItems = new ArrayList<>();
    private String errorMessage = null;
    private int errorDisplayTicks = 0;
    public VehiclePurchaseScreen(VehiclePurchaseMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;

        UnrealZaruba.LOGGER.info("[UnrealZaruba] VehiclePurchaseScreen constructor called");
        UnrealZaruba.LOGGER.info("[UnrealZaruba] Menu: {}", menu);

        vehicleKeys = new ArrayList<>(VehicleRegistry.getVehicleKeys());

        UnrealZaruba.LOGGER.info("[UnrealZaruba] Vehicle names: {}", vehicleKeys);

        if (menu.getSelectedVehicle() == null && !vehicleKeys.isEmpty()) {
            menu.setSelectedVehicle(vehicleKeys.get(0));
        }

        updateItemRequirements();
    }

    private void updateItemRequirements() {
        requiredItems.clear();
        String selectedVehicle = menu.getSelectedVehicle();

        if (selectedVehicle != null) {
            VehicleData vehicleData = VehicleRegistry.getVehicle(selectedVehicle);
            requiredItems.addAll(vehicleData.getRequiredItemStacks());
        }
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
            if (menu.getSelectedVehicle() != null) {
                var result = menu.getBlockEntity().getBlockPos();
                NetworkHandler.CHANNEL.sendToServer(
                    new PurchaseVehiclePacket(result, menu.getSelectedVehicle())
                );
            }
        }).pos(x + 48, y + 130).size(80, 20).build());

        updateDropdown();
    }
    
    private void updateDropdown() {
        for (Button button : dropdownButtons) {
            removeWidget(button);
        }
        dropdownButtons.clear();

        if (dropdownOpen) {
            int x = (width - imageWidth) / 2;
            int y = (height - imageHeight) / 2;

            for (int i = 0; i < vehicleKeys.size(); i++) {
                String vehicleKey = vehicleKeys.get(i);
                String vehicleName = Component.translatable(VehicleRegistry.getVehicle(vehicleKey).getName()).getString();
                Button button = Button.builder(Component.literal(vehicleName), btn -> {
                    menu.setSelectedVehicle(vehicleKey);
                    dropdownOpen = false;
                    updateDropdown();
                    updateItemRequirements();
                    updateDeployButton();
                }).pos(x + 20, y + 20 + DROPDOWN_HEIGHT * (i + 1)).size(DROPDOWN_WIDTH, DROPDOWN_HEIGHT).build();

                addRenderableWidget(button);
                dropdownButtons.add(button);
            }
        }
    }

    private void updateDeployButton() {
        var selectedVehicle = menu.getSelectedVehicle();
        if (selectedVehicle != null) {
            VehicleData vehicleData = VehicleRegistry.getVehicle(selectedVehicle);
            deployButton.active = vehicleData != null && vehicleData.hasRequiredItems(minecraft.player.getInventory());
        } else {
            deployButton.active = false;
            deployButton.setMessage(Component.translatable("button.unrealzaruba.missing_items"));
        }
    }
    
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, delta);

        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        VehicleData selectedVehicle = VehicleRegistry.getVehicle(menu.getSelectedVehicle());
        if (selectedVehicle != null) {
            graphics.drawString(font, Component.translatable(selectedVehicle.getName()), x + 25, y + 25, 0x404040, false);
        }

        if (!dropdownOpen) {
            graphics.drawString(font, Component.translatable("label.unrealzaruba.required_items"), x + 25, y + 50, 0x404040, false);
            renderRequiredItems(graphics, x, y, mouseX, mouseY);
            renderTooltips(graphics, mouseX, mouseY, x, y);
        }

        if (errorMessage != null && errorDisplayTicks > 0) {
            int errorX = x + (imageWidth / 2) - (font.width(errorMessage) / 2);
            int errorY = y + 115;

            graphics.drawString(font, errorMessage, errorX, errorY, 0xAA0000, false);
        }
    }

    private void renderRequiredItems(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
        int itemX = x + 20;
        int itemY = y + 65;

        for (int i = 0; i < requiredItems.size(); i++) {
            ItemStack itemStack = requiredItems.get(i);
            int col = i % 4;
            int row = i / 4;

            graphics.renderItem(itemStack, itemX + col * 36, itemY + row * 20, mouseX, mouseY);
            // graphics.renderItemDecorations(font, itemStack, itemX + col * 36, itemY + row * 20);

            boolean hasEnough = minecraft.player.getInventory().countItem(itemStack.getItem()) >= itemStack.getCount();
            int textColor = hasEnough ? 0x404040 : 0xAA0000;

            String countText = itemStack.getCount() + "x";
            graphics.drawString(font, countText, itemX + col * 36 + 16, itemY + row * 20 + 9, textColor, false);
        }
    }

    private void renderTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY, int x, int y) {
        int itemX = x + 20;
        int itemY = y + 65;
        
        for (int i = 0; i < requiredItems.size(); i++) {
            ItemStack stack = requiredItems.get(i);
            int col = i % 4;
            int row = i / 4;
            
            int slotX = itemX + col * 36;
            int slotY = itemY + row * 20;
            
            if (mouseX >= slotX && mouseX < slotX + 16 && mouseY >= slotY && mouseY < slotY + 16) {
                guiGraphics.renderTooltip(font, stack, mouseX, mouseY);
            }
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
                mouseY >= y + 20 && mouseY <= y + 20 + DROPDOWN_HEIGHT * (vehicleKeys.size() + 1);
                
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