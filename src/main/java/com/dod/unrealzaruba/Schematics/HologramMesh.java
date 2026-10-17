package com.dod.unrealzaruba.Schematics;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.joml.Quaternionf;
import org.joml.Quaterniond;
import org.joml.Vector3d;

import com.enxv.aeronauticsstructuretool.PreviewBlueprintData;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * The geometry of a vehicle being bought: every block of the blueprint as the quads of its model, in the vehicle's own
 * coordinates, built a few milliseconds per frame into one vertex buffer. Faces hidden by a neighbour are left out.
 * The blueprint comes from the toolgun's public reader; everything from there on is ours.
 */
final class HologramMesh implements AutoCloseable {
    private static final int FULL_BRIGHT = 0xF000F0;

    private record Placed(Vector3d center, Quaterniond rotation, BlockState state) {}

    private final List<Placed> blocks = new ArrayList<>();
    /** The cells with a block that hides the faces against it. */
    private final Set<Long> occluders = new HashSet<>();
    private ByteBufferBuilder bytes;
    private BufferBuilder builder;
    private VertexBuffer buffer;
    private int next;
    private boolean done;
    private double minY = Double.POSITIVE_INFINITY;
    private double maxY = Double.NEGATIVE_INFINITY;

    /** {@code samples}: how many blocks came from the outline points because the blueprint had no block models. */
    final int partCount;
    final int blockCount;
    final int samplePoints;

    HologramMesh(PreviewBlueprintData data) {
        partCount = data.subLevels().size();
        int samples = 0;
        int models = 0;
        for (var sub : data.subLevels()) {
            Vector3d offset = new Vector3d(sub.relativePosition()).sub(data.rootRelativePosition());
            Quaterniond rotation = new Quaterniond(sub.relativeOrientation());
            for (var block : sub.previewBlocks()) {
                add(block.center(), sub.localPlotAnchor(), offset, rotation, block.state());
                models++;
            }
        }
        if (blocks.isEmpty()) {
            // Nothing the toolgun can model: the outline points stand in as plain blocks
            for (var sub : data.subLevels()) {
                Vector3d offset = new Vector3d(sub.relativePosition()).sub(data.rootRelativePosition());
                Quaterniond rotation = new Quaterniond(sub.relativeOrientation());
                for (Vector3d point : sub.sampleBlocks()) {
                    add(point, sub.localPlotAnchor(), offset, rotation, Blocks.WHITE_CONCRETE.defaultBlockState());
                    samples++;
                }
            }
        }
        blockCount = models;
        samplePoints = samples;
        for (Placed placed : blocks) {
            minY = Math.min(minY, placed.center().y - 0.5);
            maxY = Math.max(maxY, placed.center().y + 0.5);
        }
        if (blocks.isEmpty()) {
            done = true; // nothing to draw, and nothing to wait for
        } else {
            bytes = new ByteBufferBuilder(1 << 20);
            builder = new BufferBuilder(bytes, VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);
        }
    }

    private void add(Vector3d center, Vector3d anchor, Vector3d offset, Quaterniond rotation, BlockState state) {
        Vector3d local = new Vector3d(center).sub(anchor);
        rotation.transform(local);
        local.add(offset);
        blocks.add(new Placed(local, rotation, state));
        if (state.canOcclude()) occluders.add(BlockPos.containing(local.x, local.y, local.z).asLong());
    }

    boolean isEmpty() {
        return blocks.isEmpty();
    }

    boolean isDone() {
        return done;
    }

    /** Builds for at most this long; true once the buffer is complete. */
    boolean advance(long budgetNanos) {
        if (done) return true;
        long limit = System.nanoTime() + budgetNanos;
        var dispatcher = Minecraft.getInstance().getBlockRenderer();
        PoseStack pose = new PoseStack();
        while (next < blocks.size() && System.nanoTime() < limit) {
            Placed placed = blocks.get(next++);
            BlockState state = placed.state();
            if (state.getRenderShape() != RenderShape.MODEL) continue;
            BakedModel model = dispatcher.getBlockModel(state);
            RandomSource random = RandomSource.create(state.getSeed(BlockPos.ZERO));

            pose.pushPose();
            pose.translate(placed.center().x, placed.center().y, placed.center().z);
            pose.mulPose(new Quaternionf(placed.rotation()));
            pose.translate(-0.5f, -0.5f, -0.5f);
            BlockPos cell = BlockPos.containing(placed.center().x, placed.center().y, placed.center().z);
            for (Direction side : sides()) {
                if (side != null && hidden(cell, side, state)) continue;
                random.setSeed(state.getSeed(BlockPos.ZERO));
                for (BakedQuad quad : model.getQuads(state, side, random, ModelData.EMPTY, null)) {
                    builder.putBulkData(pose.last(), quad, 1f, 1f, 1f, 1f, FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
                }
            }
            pose.popPose();
        }
        if (next >= blocks.size()) finish();
        return done;
    }

    /** A face against a solid neighbour is never seen. */
    private boolean hidden(BlockPos cell, Direction side, BlockState state) {
        return state.canOcclude() && occluders.contains(cell.relative(side).asLong());
    }

    private static Direction[] sidesCache;

    private static Direction[] sides() {
        if (sidesCache == null) {
            Direction[] all = Direction.values();
            sidesCache = new Direction[all.length + 1];
            System.arraycopy(all, 0, sidesCache, 0, all.length);
            sidesCache[all.length] = null; // quads without a face to cull against
        }
        return sidesCache;
    }

    private void finish() {
        MeshData mesh = builder.build();
        if (mesh != null) {
            buffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
            buffer.bind();
            buffer.upload(mesh);
            VertexBuffer.unbind();
        }
        bytes.close();
        builder = null;
        bytes = null;
        done = true;
    }

    VertexBuffer buffer() {
        return buffer;
    }

    /** The lowest and highest point of the vehicle, in its own coordinates. */
    double minY() {
        return minY;
    }

    double maxY() {
        return maxY;
    }

    /** The lowest block centre, which the vehicle is stood on. */
    double lowestCenterY() {
        return minY + 0.5;
    }

    @Override
    public void close() {
        if (buffer != null) buffer.close();
        if (bytes != null) bytes.close();
        buffer = null;
        bytes = null;
        builder = null;
    }
}
