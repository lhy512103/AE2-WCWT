package com.lhy.wcwt.network;

import com.lhy.wcwt.WcwtMod;
import com.lhy.wcwt.compat.UselessModCompat;
import com.lhy.wcwt.menu.WirelessComprehensiveWorkTerminalMenu;
import appeng.parts.encoding.EncodingMode;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Recipe identity and slot contents travel together, preventing selection/fill packet races. */
public record OmniversalPatternTransferPacket(int containerId, UselessModCompat.Selection selection,
                                             JeiCraftingTransferPacket transfer) implements CustomPacketPayload {
    public static final Type<OmniversalPatternTransferPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(WcwtMod.MOD_ID, "omniversal_pattern_transfer"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OmniversalPatternTransferPacket> STREAM_CODEC =
            StreamCodec.of((buf, packet) -> {
                buf.writeVarInt(packet.containerId());
                buf.writeResourceLocation(packet.selection().recipeId());
                buf.writeUtf(packet.selection().fingerprint(), 256);
                buf.writeUtf(packet.selection().sourceId(), 256);
                JeiCraftingTransferPacket.STREAM_CODEC.encode(buf, packet.transfer());
            }, buf -> new OmniversalPatternTransferPacket(buf.readVarInt(),
                    new UselessModCompat.Selection(buf.readResourceLocation(), buf.readUtf(256), buf.readUtf(256)),
                    JeiCraftingTransferPacket.STREAM_CODEC.decode(buf)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    boolean matchesEncodingMenu(int activeContainerId) {
        return containerId == activeContainerId && !transfer.toCraftingGrid()
                && transfer.mode() == EncodingMode.PROCESSING && !selection.fingerprint().isBlank();
    }

    public static void handle(OmniversalPatternTransferPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player().containerMenu instanceof WirelessComprehensiveWorkTerminalMenu menu
                    && packet.matchesEncodingMenu(menu.containerId) && UselessModCompat.available()) {
                menu.transferOmniversalRecipe(packet.transfer().inputs(), packet.transfer().outputs(), packet.selection());
            }
        });
    }
}
