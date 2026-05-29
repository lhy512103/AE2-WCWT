package com.lhy.wcwt.network;

import com.lhy.wcwt.WcwtMod;
import com.lhy.wcwt.init.ModComponents;
import com.lhy.wcwt.menu.WirelessComprehensiveWorkTerminalMenu;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record WirelessSettingsPacket(boolean pickBlock, boolean restock, boolean magnet, boolean pickupToMe,
                                     boolean craftIfMissing) implements CustomPacketPayload {
    public static final Type<WirelessSettingsPacket> TYPE =
            new Type<>(com.lhy.wcwt.util.ResourceLocationCompat.id(WcwtMod.MOD_ID, "wireless_settings"));

    public static final StreamCodec<RegistryFriendlyByteBuf, WirelessSettingsPacket> STREAM_CODEC =
            StreamCodec.ofMember(WirelessSettingsPacket::write, WirelessSettingsPacket::read);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static WirelessSettingsPacket read(RegistryFriendlyByteBuf buf) {
        return new WirelessSettingsPacket(buf.readBoolean(), buf.readBoolean(), buf.readBoolean(), buf.readBoolean(),
                buf.readBoolean());
    }

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeBoolean(pickBlock);
        buf.writeBoolean(restock);
        buf.writeBoolean(magnet);
        buf.writeBoolean(pickupToMe);
        buf.writeBoolean(craftIfMissing);
    }

    public static void handle(WirelessSettingsPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player().containerMenu instanceof WirelessComprehensiveWorkTerminalMenu menu
                    && menu.getMenuHost() != null) {
                ItemStack stack = menu.getMenuHost().getItemStack();
                CompoundTag root = getOrCreateRootTag(stack);
                root.putBoolean("pick_block", packet.pickBlock());
                root.putBoolean("craft_if_missing", packet.craftIfMissing());
                root.putBoolean("restock", packet.restock());
                setMagnetSettings(stack, packet.magnet(), packet.pickupToMe());
            }
        });
    }

    private static void setMagnetSettings(ItemStack stack, boolean magnet, boolean pickupToMe) {
        CompoundTag root = getOrCreateRootTag(stack);
        root.putBoolean("magnet", magnet);
        root.putBoolean("pickup_to_me", pickupToMe);
    }

    private static CompoundTag getOrCreateRootTag(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        CompoundTag root = tag.getCompound(ModComponents.ROOT_TAG);
        tag.put(ModComponents.ROOT_TAG, root);
        return root;
    }
}

