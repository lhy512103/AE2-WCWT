package com.lhy.wcwt.network;

import appeng.api.stacks.AEKey;
import com.lhy.wcwt.WcwtMod;
import com.lhy.wcwt.menu.WirelessComprehensiveWorkTerminalMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.Nullable;

/**
 * 替换样板网络包
 * 客户端点击「替换」按钮 → 服务端按两个 ghost AEKey 批量替换样板内容。
 */
public record ReplacePatternPacket(@Nullable AEKey replaceWhat, @Nullable AEKey replaceWith)
        implements CustomPacketPayload {

    public static final Type<ReplacePatternPacket> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(WcwtMod.MOD_ID, "replace_pattern"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ReplacePatternPacket> STREAM_CODEC =
            StreamCodec.of(ReplacePatternPacket::write, ReplacePatternPacket::read);

    private static void write(RegistryFriendlyByteBuf buf, ReplacePatternPacket pkt) {
        writeKey(buf, pkt.replaceWhat);
        writeKey(buf, pkt.replaceWith);
    }

    private static ReplacePatternPacket read(RegistryFriendlyByteBuf buf) {
        return new ReplacePatternPacket(readKey(buf), readKey(buf));
    }

    private static void writeKey(RegistryFriendlyByteBuf buf, @Nullable AEKey key) {
        if (key == null) {
            buf.writeBoolean(false);
        } else {
            buf.writeBoolean(true);
            var out = net.minecraft.world.level.storage.TagValueOutput.createWithContext(
                    net.minecraft.util.ProblemReporter.DISCARDING, buf.registryAccess());
            key.toTagGeneric(out);
            ByteBufCodecs.COMPOUND_TAG.encode(buf, out.buildResult());
        }
    }

    @Nullable
    private static AEKey readKey(RegistryFriendlyByteBuf buf) {
        if (!buf.readBoolean()) {
            return null;
        }
        var tag = ByteBufCodecs.COMPOUND_TAG.decode(buf);
        var in = net.minecraft.world.level.storage.TagValueInput.create(
                net.minecraft.util.ProblemReporter.DISCARDING, buf.registryAccess(), tag);
        return AEKey.fromTagGeneric(in);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ReplacePatternPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer
                    && serverPlayer.containerMenu instanceof WirelessComprehensiveWorkTerminalMenu menu) {
                menu.replaceInPatterns(packet.replaceWhat, packet.replaceWith);
            }
        });
    }
}
