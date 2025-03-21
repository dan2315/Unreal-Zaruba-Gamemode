package com.dod.UnrealZaruba.UI;

import com.dod.UnrealZaruba.UnrealZaruba;
import com.dod.UnrealZaruba.NetworkPackets.NetworkHandler;
import com.dod.UnrealZaruba.NetworkPackets.VotePlayerPacket;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.UUID;

public class PlayerVoteScreen extends Screen {

    private static final ResourceLocation BACKGROUND_TEXTURE = new ResourceLocation(UnrealZaruba.MOD_ID,
            "textures/gui/player_vote_screen.png");

    private EditBox searchField;
    private PlayerListWidget playerList;
    private Button voteButton;
    private Player selectedPlayer;

    List<UUID> teammates;

    public PlayerVoteScreen(List<UUID> teammates) {
        super(Component.literal("Vote for a Player"));
        this.teammates = teammates;
    }

    @Override
    protected void init() {
        super.init();

        // Ensure there is enough space for the header and adjust other UI elements accordingly

        int headerHeight = 20; // Height reserved for the header
        int padding = 5;       // Space between elements

        // Adjust the position of the search field to be below the header
        this.searchField = new EditBox(this.font, this.width / 2 - 100, headerHeight + padding, 200, 20, Component.literal("Search"));
        this.searchField.setResponder(this::onSearchTextChanged);
        this.addRenderableWidget(this.searchField);

        // Adjust the position of the player list to start below the search field
        int playerListTop = headerHeight + padding * 2 + this.searchField.getHeight();
        this.playerList = new PlayerListWidget(this.minecraft, this.width, this.height, playerListTop, this.height - 50, 24);
        this.addRenderableWidget(this.playerList);

        // Position the vote button at the bottom of the screen
        this.voteButton = Button.builder(Component.literal("Vote"), button -> voteForSelectedPlayer())
                .bounds(this.width / 2 - 50, this.height - 30, 100, 20)
                .build();
        this.addRenderableWidget(voteButton);

        populatePlayerList("");
    }

    private void onSearchTextChanged(String searchText) {
        populatePlayerList(searchText);
    }

    private void populatePlayerList(String filter) {
        this.playerList.children().clear();
        List<AbstractClientPlayer> players = this.minecraft.level.players();

        for (Player player : players) {
            if (player.getName().getString().toLowerCase().contains(filter.toLowerCase())) {
                if (teammates.contains(player.getUUID())) {
                    if (!Minecraft.getInstance().player.getUUID().equals(player.getUUID())) {
                        this.playerList.children().add(new PlayerListEntry(player));
                    }
                }
            }
        }
    }

    private void voteForSelectedPlayer() {
        if (this.selectedPlayer != null) {
            UnrealZaruba.LOGGER.warn("Кликнул на проголосовать за " + selectedPlayer.getName().toString());
            //TODO: NetworkHandler.CHANNEL.sendToServer(new VotePlayerPacket(selectedPlayer.getUUID()));
            this.onClose();
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        this.renderBackground(guiGraphics);
        RenderSystem.setShaderTexture(0, BACKGROUND_TEXTURE);
        guiGraphics.blit(BACKGROUND_TEXTURE, 0, 0, 0, 0, this.width, this.height);
        super.render(guiGraphics, mouseX, mouseY, delta);
        this.searchField.render(guiGraphics, mouseX, mouseY, delta);
        // this.playerList.render(matrices, mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    private class PlayerListWidget extends ObjectSelectionList<PlayerListEntry> {
        public PlayerListWidget(Minecraft mc, int width, int height, int top, int bottom, int itemHeight) {
            super(mc, width, height, top, bottom, itemHeight);
        }
    }

    private class PlayerListEntry extends ObjectSelectionList.Entry<PlayerListEntry> {
        private final Player player;

        public PlayerListEntry(Player player) {
            this.player = player;
        }

        @Override
        public void render(GuiGraphics guiGraphics, int index, int y, int x, int itemWidth, int itemHeight, int mouseX,
                int mouseY, boolean hovered, float delta) {
            // Check if this player is the selected player and apply a highlight if so
            if (selectedPlayer != null && selectedPlayer.getUUID().equals(this.player.getUUID())) {
                int borderColor = 0xFFFFA500; // Example border color (orange)
                int borderThickness = 2;

                // Top border
                guiGraphics.fill(x - borderThickness, y - borderThickness, x + itemWidth + borderThickness, y,
                        borderColor);
                // Bottom border
                guiGraphics.fill(x - borderThickness, y + itemHeight, x + itemWidth + borderThickness,
                        y + itemHeight + borderThickness, borderColor);
                // Left border
                guiGraphics.fill(x - borderThickness, y, x, y + itemHeight, borderColor);
                // Right border
                guiGraphics.fill(x + itemWidth, y, x + itemWidth + borderThickness, y + itemHeight, borderColor);
            } else if (hovered) {
                guiGraphics.fill(x, y, x + itemWidth, y + itemHeight, 0x80FFFFFF); // Highlight on hover
            }
            Minecraft.getInstance().font.drawInBatch(
                    this.player.getName(), // Component, no need to use getString()
                    x + 2,
                    y + 2,
                    0xFFFFFF,
                    false,
                    guiGraphics.pose().last().pose(), // Use pose() from GuiGraphics
                    guiGraphics.bufferSource(),
                    Font.DisplayMode.NORMAL,
                    0,
                    15728880 // Light value
            );
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button == 0) {
                selectedPlayer = this.player;
                return true;
            }
            return false;
        }

        @Override
        public Component getNarration() {
            return Component.literal("Если ты глухой, пойди пива попей, как ты в майнкрафт играть собрался?");
        }
    }
}
