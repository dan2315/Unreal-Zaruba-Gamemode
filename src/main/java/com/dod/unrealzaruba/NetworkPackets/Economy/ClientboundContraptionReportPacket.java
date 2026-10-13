package com.dod.unrealzaruba.NetworkPackets.Economy;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import com.dod.unrealzaruba.NetworkPackets.ModPacket;
import com.dod.unrealzaruba.NetworkPackets.PacketContext;
import com.dod.unrealzaruba.Pricing.ContraptionReport;
import com.dod.unrealzaruba.UI.ContraptionReportOverlay;

import net.minecraft.network.FriendlyByteBuf;

/** The scanner result for the HUD panel; a null report clears the panel. */
public class ClientboundContraptionReportPacket implements ModPacket {
    private final ContraptionReport report;

    public ClientboundContraptionReportPacket(ContraptionReport report) {
        this.report = report;
    }

    public static void encode(ClientboundContraptionReportPacket msg, FriendlyByteBuf buf) {
        ContraptionReport report = msg.report;
        buf.writeBoolean(report != null);
        if (report == null) return;
        buf.writeUtf(report.name(), 128);
        writeSize(buf, report.size());
        writeSize(buf, report.maxSize());
        buf.writeDouble(report.total());
        buf.writeDouble(report.blocks());
        buf.writeDouble(report.items());
        buf.writeDouble(report.fluids());
        buf.writeVarInt(report.lines().size());
        for (ContraptionReport.Line line : report.lines()) {
            buf.writeByte(line.kind());
            buf.writeUtf(line.id(), 160);
            buf.writeVarLong(line.count());
            buf.writeDouble(line.subtotal());
        }
        buf.writeVarInt(report.unpricedCount());
        buf.writeVarInt(report.problems().size());
        for (String problem : report.problems()) buf.writeUtf(problem, 1024);
        buf.writeBoolean(report.allowed());
        buf.writeBoolean(report.rulesMissing());
    }

    public static ClientboundContraptionReportPacket decode(FriendlyByteBuf buf) {
        if (!buf.readBoolean()) return new ClientboundContraptionReportPacket(null);
        String name = buf.readUtf(128);
        int[] size = readSize(buf);
        int[] maxSize = readSize(buf);
        double total = buf.readDouble(), blocks = buf.readDouble(), items = buf.readDouble(), fluids = buf.readDouble();
        int lineCount = Math.min(buf.readVarInt(), 32);
        List<ContraptionReport.Line> lines = new ArrayList<>(lineCount);
        for (int i = 0; i < lineCount; i++) {
            lines.add(new ContraptionReport.Line(buf.readByte(), buf.readUtf(160), buf.readVarLong(), buf.readDouble()));
        }
        int unpriced = buf.readVarInt();
        int problemCount = Math.min(buf.readVarInt(), 16);
        List<String> problems = new ArrayList<>(problemCount);
        for (int i = 0; i < problemCount; i++) problems.add(buf.readUtf(1024));
        boolean allowed = buf.readBoolean();
        boolean rulesMissing = buf.readBoolean();
        return new ClientboundContraptionReportPacket(new ContraptionReport(name, size, maxSize, total, blocks, items,
                fluids, lines, unpriced, problems, allowed, rulesMissing));
    }

    public static void handle(ClientboundContraptionReportPacket msg, Supplier<PacketContext> ctx) {
        ctx.get().enqueueWork(() -> ContraptionReportOverlay.INSTANCE.set(msg.report));
        ctx.get().setPacketHandled(true);
    }

    private static void writeSize(FriendlyByteBuf buf, int[] size) {
        boolean present = size != null && size.length == 3;
        buf.writeBoolean(present);
        if (present) for (int value : size) buf.writeVarInt(value);
    }

    private static int[] readSize(FriendlyByteBuf buf) {
        return buf.readBoolean() ? new int[]{buf.readVarInt(), buf.readVarInt(), buf.readVarInt()} : null;
    }
}
