package com.lhy.wcwt.network;

import com.lhy.wcwt.WcwtMod;
import com.lhy.wcwt.helpers.WcwtToolkitAccess;
import com.lhy.wcwt.helpers.WcwtToolkitHotbarState;
import com.lhy.wcwt.helpers.WirelessComprehensiveWorkTerminalMenuHost;
import appeng.api.inventories.InternalInventory;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public record WcwtToolkitHotbarSyncPacket(List<ItemStack> stacks, List<ItemStack> memories)
        implements CustomPacketPayload {
    private static final Map<ServerPlayer, ItemStack> LAST_SENT_SELECTION = new WeakHashMap<>();
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
    }

    private static WcwtToolkitHotbarSyncPacket read(RegistryFriendlyByteBuf buf) {
        return new WcwtToolkitHotbarSyncPacket(readHotbar(buf), readHotbar(buf));
    }

    private static void writeHotbar(RegistryFriendlyByteBuf buf, List<ItemStack> stacks) {
        for (int i = 0; i < WcwtToolkitAccess.HOTBAR_SLOTS; i++) {
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buf,
                    i < stacks.size() ? stacks.get(i) : ItemStack.EMPTY);
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

    public static void send(ServerPlayer player) {
        send(player,
                WirelessComprehensiveWorkTerminalMenuHost.snapshotToolkitHotbar(player, false),
                WirelessComprehensiveWorkTerminalMenuHost.snapshotToolkitHotbar(player, true));
    }

    public static void send(ServerPlayer player, InternalInventory inventory) {
        send(player, copyHotbar(inventory),
                WirelessComprehensiveWorkTerminalMenuHost.snapshotToolkitHotbar(player, true));
    }

    public static void send(ServerPlayer player, List<ItemStack> stacks, InternalInventory memories) {
        send(player, stacks, copyHotbar(memories));
    }

    public static void send(ServerPlayer player, InternalInventory inventory, List<ItemStack> memories) {
        send(player, copyHotbar(inventory), memories);
    }

    public static void send(ServerPlayer player, List<ItemStack> stacks, List<ItemStack> memories) {
        LAST_SENT_SELECTION.put(player, WcwtToolkitHotbarState.getSelectedToolkit(player).copy());
        PacketDistributor.sendToPlayer(player, new WcwtToolkitHotbarSyncPacket(stacks, memories));
    }

    private static List<ItemStack> copyHotbar(InternalInventory inventory) {
        List<ItemStack> stacks = new ArrayList<>(WcwtToolkitAccess.HOTBAR_SLOTS);
        for (int i = 0; i < WcwtToolkitAccess.HOTBAR_SLOTS; i++) {
            stacks.add(inventory != null && i < inventory.size()
                    ? inventory.getStackInSlot(i).copy() : ItemStack.EMPTY);
        }
        return stacks;
    }

    public static void sendIfChanged(ServerPlayer player) {
        ItemStack current = WcwtToolkitHotbarState.getSelectedToolkit(player);
        ItemStack previous = LAST_SENT_SELECTION.get(player);
        if (previous == null || !ItemStack.matches(previous, current)) {
            if (!WcwtToolkitHotbarState.persistSelectedToolkit(player)) {
                send(player);
            }
        }
    }

    public static void handle(WcwtToolkitHotbarSyncPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() != null && context.player().level().isClientSide()) {
                WcwtToolkitHotbarState.setClientSnapshot(context.player(), packet.stacks);
                WcwtToolkitHotbarState.setClientMemorySnapshot(context.player(), packet.memories);
            }
        });
    }
}
