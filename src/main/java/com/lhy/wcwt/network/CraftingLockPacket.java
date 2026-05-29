package com.lhy.wcwt.network;

import com.lhy.wcwt.WcwtMod;
import com.lhy.wcwt.menu.WirelessComprehensiveWorkTerminalMenu;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 合成网格锁定状态同步数据包
 */
public record CraftingLockPacket(boolean locked) implements CustomPacketPayload {
    
    public static final CustomPacketPayload.Type<CraftingLockPacket> TYPE = 
        new CustomPacketPayload.Type<>(com.lhy.wcwt.util.ResourceLocationCompat.id(WcwtMod.MOD_ID, "crafting_lock"));
    
    public static final StreamCodec<ByteBuf, CraftingLockPacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.BOOL,
        CraftingLockPacket::locked,
        CraftingLockPacket::new
    );
    
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
    
    public static void handle(CraftingLockPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            var player = context.player();
            if (player.containerMenu instanceof WirelessComprehensiveWorkTerminalMenu menu) {
                var host = menu.getMenuHost();
                if (host != null) {
                    host.setCraftingGridLocked(packet.locked());
                }
            }
        });
    }
}

