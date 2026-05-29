package com.lhy.wcwt.network;

import com.lhy.wcwt.WcwtMod;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.lhy.wcwt.menu.WirelessComprehensiveWorkTerminalMenu;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import net.minecraft.network.FriendlyByteBuf;

/**
 * 方向改变网络包
 * 客户端发送到服务端，用于修改样板输入方向
 */
public record DirectionChangePacket(
    @NotNull AEKey key,
    @Nullable Direction direction
) implements CustomPacketPayload {
    
    public static final Type<DirectionChangePacket> TYPE = 
        new Type<>(com.lhy.wcwt.util.ResourceLocationCompat.id(WcwtMod.MOD_ID, "direction_change"));
    
    public static final StreamCodec<RegistryFriendlyByteBuf, DirectionChangePacket> STREAM_CODEC = 
        StreamCodec.composite(
            // AEKey编解码
            new StreamCodec<RegistryFriendlyByteBuf, AEKey>() {
                @Override
                public AEKey decode(RegistryFriendlyByteBuf buf) {
                    var keyType = AEKeyType.fromRawId(buf.readByte());
                    if (keyType == null) {
                        return null;
                    }
                    return keyType.readFromPacket(buf);
                }
                
                @Override
                public void encode(RegistryFriendlyByteBuf buf, AEKey key) {
                    buf.writeByte(key.getType().getRawId());
                    key.writeToPacket(buf);
                }
            },
            DirectionChangePacket::key,
            // Direction编解码 (nullable)
            new StreamCodec<RegistryFriendlyByteBuf, Direction>() {
                @Override
                public Direction decode(RegistryFriendlyByteBuf buf) {
                    int ordinal = buf.readByte();
                    if (ordinal < 0) return null;
                    return Direction.values()[ordinal];
                }
                
                @Override
                public void encode(RegistryFriendlyByteBuf buf, Direction dir) {
                    if (dir == null) {
                        buf.writeByte(-1);
                    } else {
                        buf.writeByte(dir.ordinal());
                    }
                }
            },
            DirectionChangePacket::direction,
            DirectionChangePacket::new
        );
    
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
    
    public static void handle(DirectionChangePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer
                    && serverPlayer.containerMenu instanceof WirelessComprehensiveWorkTerminalMenu menu) {
                menu.updateCopyPatternDirection(packet.key, packet.direction);
            }
        });
    }
}

