package com.lhy.wcwt.network;

import com.lhy.wcwt.WcwtMod;
import com.lhy.wcwt.helpers.ToolkitItemRules;
import com.lhy.wcwt.helpers.WcwtToolkitAccess;
import com.lhy.wcwt.helpers.WcwtToolkitHotbarState;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record WcwtToolkitHotbarActionPacket(int action, int slot, boolean all) implements CustomPacketPayload {
    public static final int CLICK = 0;
    public static final int DROP = 1;
    public static final Type<WcwtToolkitHotbarActionPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(WcwtMod.MOD_ID, "toolkit_hotbar_action"));
    public static final StreamCodec<io.netty.buffer.ByteBuf, WcwtToolkitHotbarActionPacket> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, WcwtToolkitHotbarActionPacket::action,
                    ByteBufCodecs.VAR_INT, WcwtToolkitHotbarActionPacket::slot,
                    ByteBufCodecs.BOOL, WcwtToolkitHotbarActionPacket::all,
                    WcwtToolkitHotbarActionPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(WcwtToolkitHotbarActionPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)
                    || packet.slot < 0 || packet.slot >= WcwtToolkitAccess.HOTBAR_SLOTS) {
                return;
            }
            if (packet.action == CLICK) {
                click(player, packet.slot, packet.all ? 1 : 0);
            } else if (packet.action == DROP) {
                drop(player, packet.slot, packet.all);
            }
        });
    }

    private static void click(ServerPlayer player, int slotIndex, int button) {
        var inventory = WcwtToolkitHotbarState.getToolkitInventory(player);
        if (inventory == null || slotIndex >= inventory.size()) {
            return;
        }
        ItemStack target = inventory.getStackInSlot(slotIndex);
        ItemStack carried = player.containerMenu.getCarried();
        if (button == 0) {
            if (carried.isEmpty()) {
                inventory.setItemDirect(slotIndex, ItemStack.EMPTY);
                player.containerMenu.setCarried(target);
            } else if (target.isEmpty()) {
                inventory.setItemDirect(slotIndex, carried.copyWithCount(1));
                carried.shrink(1);
                player.containerMenu.setCarried(carried);
            } else {
                if (!ToolkitItemRules.mayPlace(slotIndex, carried)) {
                    return;
                }
                inventory.setItemDirect(slotIndex, carried.copyWithCount(1));
                carried.shrink(1);
                player.containerMenu.setCarried(target);
            }
        } else if (carried.isEmpty()) {
            if (!target.isEmpty()) {
                player.containerMenu.setCarried(target.copyWithCount(1));
                target.shrink(1);
                inventory.setItemDirect(slotIndex, target);
            }
        } else if (target.isEmpty() && ToolkitItemRules.mayPlace(slotIndex, carried)) {
            inventory.setItemDirect(slotIndex, carried.copyWithCount(1));
            carried.shrink(1);
            player.containerMenu.setCarried(carried);
        }
        player.containerMenu.broadcastChanges();
        WcwtToolkitHotbarSyncPacket.send(player);
    }

    private static void drop(ServerPlayer player, int slotIndex, boolean all) {
        if (!WcwtToolkitHotbarState.isToolkitSelected(player)
                || WcwtToolkitHotbarState.toolkitIndex(player) != slotIndex) {
            return;
        }
        ItemStack selected = WcwtToolkitHotbarState.getSelectedToolkit(player);
        if (selected.isEmpty()) {
            return;
        }
        ItemStack dropped = selected.copyWithCount(all ? selected.getCount() : 1);
        selected.shrink(dropped.getCount());
        WcwtToolkitHotbarState.setSelectedToolkit(player, selected);
        player.drop(dropped, false);
        WcwtToolkitHotbarSyncPacket.send(player);
    }
}
