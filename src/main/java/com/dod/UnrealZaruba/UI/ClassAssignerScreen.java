package com.dod.UnrealZaruba.UI;

import com.dod.UnrealZaruba.CharacterClass.CharacterClassData;
import com.dod.UnrealZaruba.CharacterClass.CharacterClassRegistry;
import com.dod.UnrealZaruba.Commands.Arguments.TeamColor;
import com.dod.UnrealZaruba.NetworkPackets.NetworkHandler;
import com.dod.UnrealZaruba.NetworkPackets.CharacterClasses.SetClassPacket;
import com.dod.UnrealZaruba.NetworkPackets.CharacterClasses.AssignClassToPlayerPacket;
import com.dod.UnrealZaruba.UnrealZaruba;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class ClassAssignerScreen extends Screen {
    private static final ResourceLocation TEXTURE = new ResourceLocation(UnrealZaruba.MOD_ID, "textures/gui/class_assigner.png");
    private static final int DROPDOWN_HEIGHT = 20;
    private static final int DROPDOWN_WIDTH = 120;
    
    private final BlockPos blockPos;
    private final boolean isAdmin; // Whether the current player is an admin/operator
    // This is only used for preview visualization, not for actual class assignment
    private TeamColor visualizationTeam;
    private String selectedClassId;
    private boolean dropdownOpen = false;
    private List<String> classIds;
    private List<Button> dropdownButtons = new ArrayList<>();
    private Button teamRedButton;
    private Button teamBlueButton;
    private Button applyButton;
    private Button selectButton;
    private List<ItemStack> classItems = new ArrayList<>();
    
    public ClassAssignerScreen(BlockPos blockPos, String currentClassId) {
        this(blockPos, currentClassId, false); // Default to non-admin
    }
    
    public ClassAssignerScreen(BlockPos blockPos, String currentClassId, boolean isAdmin) {
        super(Component.literal("Class Assigner"));
        this.blockPos = blockPos;
        this.isAdmin = isAdmin;
        // Default to RED team for visualization
        this.visualizationTeam = TeamColor.RED;
        this.selectedClassId = currentClassId;
        this.classIds = new ArrayList<>(CharacterClassRegistry.getCharacterClassIds());
        
        if (selectedClassId == null && !classIds.isEmpty()) {
            selectedClassId = classIds.get(0);
        }
        
        updateClassItems();
    }
    
    private void updateClassItems() {
        classItems.clear();
        if (selectedClassId != null) {
            CharacterClassData classData = CharacterClassRegistry.getCharacterClass(selectedClassId, visualizationTeam);
            if (classData != null) {
                classItems.addAll(classData.getKit());
            }
        }
    }
    
    @Override
    protected void init() {
        int x = (width - 176) / 2;
        int y = (height - 166) / 2;
        
        // Class dropdown button
        if (isAdmin) {
            addRenderableWidget(Button.builder(Component.literal("▼"), button -> {
                dropdownOpen = !dropdownOpen;
                updateDropdown();
            }).pos(x + 300, y + 20).size(20, DROPDOWN_HEIGHT).build());
        }
        
        // Team color buttons - Only for visualization, not for actual assignment
        teamRedButton = addRenderableWidget(Button.builder(Component.literal("RED"), button -> {
            visualizationTeam = TeamColor.RED;
            updateTeamButtons();
            updateClassItems();
        }).pos(x + 20, y + 50).size(60, 20).build());
        
        teamBlueButton = addRenderableWidget(Button.builder(Component.literal("BLUE"), button -> {
            visualizationTeam = TeamColor.BLUE;
            updateTeamButtons();
            updateClassItems();
        }).pos(x + 90, y + 50).size(60, 20).build());
        
        // Apply button (for admins to set the block's class) - only visible for admins
        if (isAdmin) {
            applyButton = addRenderableWidget(Button.builder(Component.literal("Apply"), button -> {
                NetworkHandler.CHANNEL.sendToServer(new SetClassPacket(blockPos, selectedClassId));
                onClose();
            }).pos(x + 300, y + 130).size(60, 20).build());
        }
        
        // Position the select button based on whether the admin button is shown
        // Select button (for players to select the class)
        selectButton = addRenderableWidget(Button.builder(Component.literal("Select Class"), button -> {
            NetworkHandler.CHANNEL.sendToServer(new AssignClassToPlayerPacket(blockPos));
            onClose();
        }).pos(x + 50, y + 130).size(80, 20).build());
        
        updateTeamButtons();
        updateDropdown();
    }
    
    private void updateTeamButtons() {
        teamRedButton.active = visualizationTeam != TeamColor.RED;
        teamBlueButton.active = visualizationTeam != TeamColor.BLUE;
    }
    
    private void updateDropdown() {
        for (Button button : dropdownButtons) {
            removeWidget(button);
        }
        dropdownButtons.clear();
        
        if (dropdownOpen) {
            int x = (width - 176) / 2;
            int y = (height - 166) / 2;
            
            for (int i = 0; i < classIds.size(); i++) {
                String classId = classIds.get(i);
                CharacterClassData classData = CharacterClassRegistry.getCharacterClass(classId, visualizationTeam);
                String displayName = classData != null ? classData.getDisplayName() : classId;
                
                Button button = Button.builder(Component.literal(displayName), btn -> {
                    System.out.println("Selected class: " + classId);
                    selectedClassId = classId;
                    dropdownOpen = false;
                    updateDropdown();
                    updateClassItems();
                }).pos(x + 178, y + DROPDOWN_HEIGHT * (i + 1)).size(DROPDOWN_WIDTH, DROPDOWN_HEIGHT).build();
                
                addRenderableWidget(button);
                dropdownButtons.add(button);
            }
        }
    }
    
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);
        
        int x = (width - 176) / 2;
        int y = (height - 166) / 2;
        
        graphics.blit(TEXTURE, x, y, 0, 0, 176, 166, 176, 166);
        
        // Draw dropdown box
        graphics.fill(x + 20, y + 15, x + 140, y + 15 + DROPDOWN_HEIGHT, 0xFFCCCCCC);
        graphics.fill(x + 21, y + 16, x + 139, y + 15 + DROPDOWN_HEIGHT - 1, 0xFFFFFFFF);
        
        // Draw selected class name
        if (selectedClassId != null) {
            CharacterClassData classData = CharacterClassRegistry.getCharacterClass(selectedClassId, visualizationTeam);
            if (classData != null) {
                graphics.drawString(font, classData.getDisplayName(), x + 25, y + 22, 0x404040, false);
            } else {
                graphics.drawString(font, selectedClassId, x + 25, y + 22, 0x404040, false);
            }
        }
        
        // Draw team visualization label
        graphics.drawString(font, "Preview Team:", x + 20, y + 40, 0x404040, false);
        
        // Draw class items label
        if (!dropdownOpen) {
            graphics.drawString(font, "Class Items:", x + 20, y + 75, 0x404040, false);
            renderClassItems(graphics, x, y, mouseX, mouseY);
        }
        
        super.render(graphics, mouseX, mouseY, partialTick);
        
        // Render tooltips
        if (!dropdownOpen) {
            renderTooltips(graphics, mouseX, mouseY, x, y);
        }
    }
    
    private void renderClassItems(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
        int itemX = x + 20;
        int itemY = y + 90;
        
        for (int i = 0; i < classItems.size(); i++) {
            ItemStack itemStack = classItems.get(i);
            int col = i % 7;
            int row = i / 7;
            
            graphics.renderItem(itemStack, itemX + col * 20, itemY + row * 20, mouseX, mouseY);
        }
    }
    
    private void renderTooltips(GuiGraphics graphics, int mouseX, int mouseY, int x, int y) {
        int itemX = x + 20;
        int itemY = y + 90;
        
        for (int i = 0; i < classItems.size(); i++) {
            ItemStack stack = classItems.get(i);
            int col = i % 7;
            int row = i / 7;
            
            int slotX = itemX + col * 20;
            int slotY = itemY + row * 20;
            
            if (mouseX >= slotX && mouseX < slotX + 16 && mouseY >= slotY && mouseY < slotY + 16) {
                graphics.renderTooltip(font, stack, mouseX, mouseY);
            }
        }
    }
    
    // @Override
    // public boolean mouseClicked(double mouseX, double mouseY, int button) {
    //     if (dropdownOpen) {
    //         int x = (width - 176) / 2;
    //         int y = (height - 166) / 2;
            
    //         boolean clickedInDropdown = mouseX >= x + 20 && mouseX <= x + 20 + DROPDOWN_WIDTH && 
    //             mouseY >= y + 20 && mouseY <= y + 20 + DROPDOWN_HEIGHT * (classIds.size() + 1);
                
    //         if (!clickedInDropdown) {
    //             dropdownOpen = false;
    //             updateDropdown();
    //             return true;
    //         }
    //     }
    //     return super.mouseClicked(mouseX, mouseY, button);
    // }
}