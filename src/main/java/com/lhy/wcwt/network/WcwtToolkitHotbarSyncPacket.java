package com.lhy.wcwt.network;

import com.lhy.wcwt.WcwtMod;
import com.lhy.wcwt.helpers.WcwtToolkitAccess;
import com.lhy.wcwt.helpers.WcwtToolkitHotbarState;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Client copy of the 18 extra-bar cells and their remembered items. The page is only included when
 * the server changed it, so it cannot undo a selection the client is already showing.
 */
public record WcwtToolkitHotbarSyncPacket(List<ItemStack> stacks, List<ItemStack> memories, boolean hasCard,
                                          int page) implements CustomPacketPayload {
    public static final int PAGE_UNCHANGED = -1;
    public static final Type<WcwtToolkitHotbarSyncPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(WcwtMod.MOD_ID, "toolkit_hotbar_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, WcwtToolkitHotbarSyncPacket> STREAM_CODEC =
            StreamCodec.of(WcwtToolkitHotbarSyncPacket::write, WcwtToolkitHotbarSyncPacket::read);

    public WcwtToolkitHotbarSyncPacket {
        stacks = List.copyOf(stacks);
        memories = List.copyOf(memories);
    }

    private static void write(RegistryFriendlyByteBuf buf, WcwtToolkitHotbarSyncPacket packet) {
        writeHotbar(buf, packet.stacks);
        writeHotbar(buf, packet.memories);
        buf.writeBoolean(packet.hasCard);
        buf.writeByte(packet.page);
    }

    private static WcwtToolkitHotbarSyncPacket read(RegistryFriendlyByteBuf buf) {
        List<ItemStack> stacks = readHotbar(buf);
        List<ItemStack> memories = readHotbar(buf);
        boolean hasCard = buf.readBoolean();
        int page = buf.readByte();
        if (page != PAGE_UNCHANGED && (page < 0 || page >= WcwtToolkitHotbarState.Bar.values().length)) {
            page = PAGE_UNCHANGED;
        }
        return new WcwtToolkitHotbarSyncPacket(stacks, memories, hasCard, page);
    }

    private static void writeHotbar(RegistryFriendlyByteBuf buf, List<ItemStack> stacks) {
        for (int i = 0; i < WcwtToolkitAccess.HOTBAR_SLOTS; i++) {
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, i < stacks.size() ? stacks.get(i) : ItemStack.EMPTY);
        }
    }

    private static List<ItemStack> readHotbar(RegistryFriendlyByteBuf buf) {
        List<ItemStack> stacks = new ArrayList<>(WcwtToolkitAccess.HOTBAR_SLOTS);
        for (int i = 0; i < WcwtToolkitAccess.HOTBAR_SLOTS; i++) {
            stacks.add(ItemStack.OPTIONAL_STREAM_CODEC.decode(buf));
        }
        return stacks;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(WcwtToolkitHotbarSyncPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() != null && context.player().level().isClientSide()) {
                WcwtToolkitHotbarState.applyClientSync(context.player(), packet.hasCard, packet.stacks,
                        packet.memories, packet.page);
            }
        });
    }
}
