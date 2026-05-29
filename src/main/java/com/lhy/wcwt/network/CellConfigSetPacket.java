package com.lhy.wcwt.network;

import com.lhy.wcwt.WcwtMod;
import com.lhy.wcwt.menu.WirelessComprehensiveWorkTerminalMenu;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.Nullable;

import appeng.api.stacks.AEKey;

/**
 * 设置（或清除）元件 config 槽中第 {@code slotIndex} 格的过滤模板。<br>
 * key == null 表示清除该格。
 */
public record CellConfigSetPacket(int slotIndex, @Nullable AEKey key) implements CustomPacketPayload {

    public static final Type<CellConfigSetPacket> TYPE =
            new Type<>(com.lhy.wcwt.util.ResourceLocationCompat.id(WcwtMod.MOD_ID, "cell_config_set"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CellConfigSetPacket> STREAM_CODEC =
            StreamCodec.of(CellConfigSetPacket::write, CellConfigSetPacket::read);

    private static void write(RegistryFriendlyByteBuf buf, CellConfigSetPacket pkt) {
        buf.writeVarInt(pkt.slotIndex);
        if (pkt.key == null) {
            buf.writeBoolean(false);
        } else {
            buf.writeBoolean(true);
            // 用 AEKey 自带 toTagGeneric 进行序列化
            var tag = pkt.key.toTagGeneric(buf.registryAccess());
            ByteBufCodecs.COMPOUND_TAG.encode(buf, tag);
        }
    }

    private static CellConfigSetPacket read(RegistryFriendlyByteBuf buf) {
        int slotIndex = buf.readVarInt();
        if (!buf.readBoolean()) {
            return new CellConfigSetPacket(slotIndex, null);
        }
        var tag = ByteBufCodecs.COMPOUND_TAG.decode(buf);
        var key = AEKey.fromTagGeneric(buf.registryAccess(), tag);
        return new CellConfigSetPacket(slotIndex, key);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(CellConfigSetPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer sp
                    && sp.containerMenu instanceof WirelessComprehensiveWorkTerminalMenu menu) {
                menu.setCellConfigSlot(packet.slotIndex, packet.key);
            }
        });
    }
}

