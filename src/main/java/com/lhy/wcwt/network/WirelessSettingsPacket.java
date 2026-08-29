package com.lhy.wcwt.network;

import com.lhy.wcwt.WcwtMod;
import com.lhy.wcwt.compat.reflect.WcwtMagnetReflect;
import com.lhy.wcwt.menu.WirelessComprehensiveWorkTerminalMenu;
import de.mari_023.ae2wtlib.api.AE2wtlibComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record WirelessSettingsPacket(boolean pickBlock, boolean restock, boolean magnet, boolean pickupToMe,
                                     boolean craftIfMissing) implements CustomPacketPayload {
    public static final Type<WirelessSettingsPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(WcwtMod.MOD_ID, "wireless_settings"));

    public static final StreamCodec<RegistryFriendlyByteBuf, WirelessSettingsPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL, WirelessSettingsPacket::pickBlock,
                    ByteBufCodecs.BOOL, WirelessSettingsPacket::restock,
                    ByteBufCodecs.BOOL, WirelessSettingsPacket::magnet,
                    ByteBufCodecs.BOOL, WirelessSettingsPacket::pickupToMe,
                    ByteBufCodecs.BOOL, WirelessSettingsPacket::craftIfMissing,
                    WirelessSettingsPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(WirelessSettingsPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player().containerMenu instanceof WirelessComprehensiveWorkTerminalMenu menu
                    && menu.getMenuHost() != null) {
                ItemStack stack = menu.getMenuHost().getItemStack();
                stack.set(AE2wtlibComponents.PICK_BLOCK, packet.pickBlock());
                stack.set(AE2wtlibComponents.CRAFT_IF_MISSING, packet.craftIfMissing());
                stack.set(AE2wtlibComponents.RESTOCK, packet.restock());
                setMagnetSettings(stack, packet.magnet(), packet.pickupToMe());
            }
        });
    }

    private static void setMagnetSettings(ItemStack stack, boolean magnet, boolean pickupToMe) {
        // 磁力设置走 WcwtMagnetReflect：AE2WTLib 内部类拿不到时直接跳过，不影响其余四项设置。
        WcwtMagnetReflect.applySettings(stack, magnet, pickupToMe);
    }
}
