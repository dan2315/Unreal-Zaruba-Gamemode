package com.dod.UnrealZaruba.NetworkPackets.CharacterClasses;

import com.dod.UnrealZaruba.ModBlocks.ClassAssignerBlock.ClassAssignerBlockEntity;
import com.dod.UnrealZaruba.CharacterClass.CharacterClassData;
import com.dod.UnrealZaruba.CharacterClass.CharacterClassRegistry;
import com.dod.UnrealZaruba.CharacterClass.CharacterClassEquipper;
import com.dod.UnrealZaruba.Commands.Arguments.TeamColor;
import com.dod.UnrealZaruba.Player.PlayerContext;
import com.dod.UnrealZaruba.Player.TeamPlayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.function.Supplier;

public class AssignClassToPlayerPacket {
    private final BlockPos blockPos;

    public AssignClassToPlayerPacket(BlockPos blockPos) {
        this.blockPos = blockPos;
    }

    public static void encode(AssignClassToPlayerPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.blockPos);
    }

    public static AssignClassToPlayerPacket decode(FriendlyByteBuf buffer) {
        return new AssignClassToPlayerPacket(buffer.readBlockPos());
    }

    public static void handle(AssignClassToPlayerPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                BlockEntity blockEntity = player.level().getBlockEntity(packet.blockPos);
                if (blockEntity instanceof ClassAssignerBlockEntity classAssigner) {
                    String classId = classAssigner.getClassId();
                    if (classId != null) {
                        // Get player's team if available, otherwise default to RED
                        TeamColor playerTeam = TeamColor.RED; // Default
                        PlayerContext playerContext = null;
                        try {
                            // Get the player's context
                            playerContext = PlayerContext.Get(player.getUUID());
                            if (playerContext instanceof TeamPlayerContext teamPlayerContext) {
                                if (teamPlayerContext.Team() != null) {
                                    playerTeam = teamPlayerContext.Team().Color();
                                }
                            }
                        } catch (Exception e) {
                            // If there's an error getting the team, just use the default
                        }
                        
                        CharacterClassData classData = CharacterClassRegistry.getCharacterClass(classId, playerTeam);
                        if (classData != null) {
                            // Store the selected class in the player context instead of giving items immediately
                            if (playerContext instanceof TeamPlayerContext teamPlayerContext) {
                                teamPlayerContext.SetSelectedClassId(classId);
                                player.sendSystemMessage(Component.literal("Selected class: " + classData.getDisplayName() + ". You'll receive your items when the game starts."));
                                
                                // TEMPORARY: Immediately equip the player to test our implementation
                                player.sendSystemMessage(Component.literal("§6[TEST] §eEquipping you with your selected class immediately..."));
                                boolean success = CharacterClassEquipper.equipPlayerWithSelectedClass(player);
                                if (!success) {
                                    player.sendSystemMessage(Component.literal("§c[TEST] §eEquipping failed. Check server logs for details."));
                                }
                            } else {
                                player.sendSystemMessage(Component.literal("Failed to save class selection."));
                            }
                        } else {
                            player.sendSystemMessage(Component.literal("Invalid class configuration!"));
                        }
                    } else {
                        player.sendSystemMessage(Component.literal("This class assigner is not configured yet!"));
                    }
                }
            }
        });
        context.setPacketHandled(true);
    }
}