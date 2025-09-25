package com.dod.unrealzaruba.NetworkPackets.CharacterClasses;

import com.dod.unrealzaruba.ModBlocks.ClassAssignerBlock.ClassAssignerBlockEntity;
import com.dod.unrealzaruba.CharacterClass.CharacterClassData;
import com.dod.unrealzaruba.CharacterClass.CharacterClassRegistry;
import com.dod.unrealzaruba.CharacterClass.CharacterClassEquipper;
import com.dod.unrealzaruba.Commands.Arguments.TeamColor;
import com.dod.unrealzaruba.ConfigurationManager.ConfigManager;
import com.dod.unrealzaruba.Player.PlayerContext;
import com.dod.unrealzaruba.Player.TeamPlayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

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
                                player.sendSystemMessage(Component.literal("Выбранный класс: " + classData.getDisplayName() + ". Экипировка будет выдана по началу игры."));
                                
                                if (ConfigManager.isDevMode()) {
                                    boolean success = CharacterClassEquipper.equipPlayerWithSelectedClass(player);
                                if (!success) {
                                        player.sendSystemMessage(Component.literal("§c[TEST] §eEquipping failed. Check server logs for details."));
                                    }
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