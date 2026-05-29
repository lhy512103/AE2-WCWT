package com.lhy.wcwt.network;

import com.lhy.wcwt.WcwtMod;
import com.lhy.wcwt.helpers.ToolkitItemRules;
import com.lhy.wcwt.helpers.WirelessComprehensiveWorkTerminalMenuHost;
import com.lhy.wcwt.menu.WirelessComprehensiveWorkTerminalMenu;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 将 AE 网络工具从鼠标携带或背包放入工具包专用槽（双击槽位触发），绕过部分客户端拖拽限制。
 */
public record ToolkitNetworkToolDepositPacket() implements CustomPacketPayload {
    public static final Type<ToolkitNetworkToolDepositPacket> TYPE =
            new Type<>(com.lhy.wcwt.util.ResourceLocationCompat.id(WcwtMod.MOD_ID, "toolkit_network_tool_deposit"));

    public static final StreamCodec<ByteBuf, ToolkitNetworkToolDepositPacket> STREAM_CODEC =
            StreamCodec.unit(new ToolkitNetworkToolDepositPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ToolkitNetworkToolDepositPacket ignored, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            if (!(player.containerMenu instanceof WirelessComprehensiveWorkTerminalMenu menu)) {
                return;
            }

            WirelessComprehensiveWorkTerminalMenuHost host = menu.getMenuHost();
            if (host == null || host.getSubInventory(WirelessComprehensiveWorkTerminalMenuHost.INV_TOOLKIT) == null) {
                return;
            }

            var toolkitInv = host.getSubInventory(WirelessComprehensiveWorkTerminalMenuHost.INV_TOOLKIT);
            int slot = ToolkitItemRules.NETWORK_TOOL_DEDICATED_INDEX;
            if (slot < 0 || slot >= toolkitInv.size()) {
                return;
            }

            ItemStack current = toolkitInv.getStackInSlot(slot);
            if (!current.isEmpty()) {
                player.displayClientMessage(Component.translatable("message.wcwt.toolkit_network_slot_occupied"),
                        false);
                return;
            }

            ItemStack carried = menu.getCarried();
            ItemStack moved = ItemStack.EMPTY;
            if (!carried.isEmpty() && ToolkitItemRules.isAeNetworkToolkitItem(carried)) {
                moved = carried.copyWithCount(1);
                carried.shrink(1);
                menu.setCarried(carried.isEmpty() ? ItemStack.EMPTY : carried);
            } else {
                int invSlots = menu.slots.size();
                for (int i = 0; i < invSlots && moved.isEmpty(); i++) {
                    Slot s = menu.slots.get(i);
                    if (s.container != player.getInventory()) {
                        continue;
                    }
                    ItemStack inner = s.getItem();
                    if (!inner.isEmpty() && ToolkitItemRules.isAeNetworkToolkitItem(inner)) {
                        moved = inner.copyWithCount(1);
                        inner.shrink(1);
                        s.setByPlayer(inner.isEmpty() ? ItemStack.EMPTY : inner);
                    }
                }
            }

            if (moved.isEmpty()) {
                player.displayClientMessage(Component.translatable("message.wcwt.toolkit_network_tool_not_found"),
                        false);
                return;
            }

            toolkitInv.setItemDirect(slot, moved);
            menu.broadcastChanges();
        });
    }
}

