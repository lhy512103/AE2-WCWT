package com.lhy.wcwt.menu.locator;

import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import appeng.menu.locator.MenuLocator;
import com.lhy.wcwt.compat.CuriosBridge;
import com.lhy.wcwt.helpers.WirelessComprehensiveWorkTerminalMenuHost;
import com.lhy.wcwt.item.WirelessComprehensiveWorkTerminalItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 定位 WCWT 终端工具包中的 AE 网络工具。
 */
public record WcwtToolkitNetworkToolLocator(SourceKind sourceKind, int sourceSlot, int toolkitSlot)
        implements MenuLocator {

    public enum SourceKind {
        INVENTORY,
        CURIOS
    }

    @Override
    public <T> T locate(Player player, Class<T> hostInterface) {
        ItemStack terminalStack = locateTerminalStack(player);
        if (terminalStack.isEmpty() || !(terminalStack.getItem() instanceof WirelessComprehensiveWorkTerminalItem terminalItem)) {
            return null;
        }

        if (sourceKind == SourceKind.CURIOS) {
            return null;
        }
        var terminalHost = terminalItem.getMenuHost(player, sourceSlot, terminalStack, null);
        if (!(terminalHost instanceof WirelessComprehensiveWorkTerminalMenuHost wcwtHost)) {
            return null;
        }

        var toolkit = wcwtHost.getSubInventory(WirelessComprehensiveWorkTerminalMenuHost.INV_TOOLKIT);
        if (toolkit == null || toolkitSlot < 0 || toolkitSlot >= toolkit.size()) {
            return null;
        }
        ItemStack stack = toolkit.getStackInSlot(toolkitSlot);
        return hostInterface.isInstance(stack) ? hostInterface.cast(stack) : null;
    }

    public @Nullable Integer getPlayerInventorySlot() {
        return sourceKind == SourceKind.INVENTORY ? sourceSlot : null;
    }

    public @Nullable BlockHitResult hitResult() {
        return null;
    }

    public void writeToPacket(FriendlyByteBuf buf) {
        buf.writeUtf(sourceKind.name());
        buf.writeInt(sourceSlot);
        buf.writeInt(toolkitSlot);
    }

    public static WcwtToolkitNetworkToolLocator readFromPacket(FriendlyByteBuf buf) {
        return new WcwtToolkitNetworkToolLocator(
                SourceKind.valueOf(buf.readUtf()),
                buf.readInt(),
                buf.readInt());
    }

    private ItemStack locateTerminalStack(Player player) {
        return switch (sourceKind) {
            case INVENTORY -> player.getInventory().getItem(sourceSlot);
            case CURIOS -> locateCurioStack(player, sourceSlot);
        };
    }

    private static ItemStack locateCurioStack(Player player, int curioSlot) {
        var curios = CuriosBridge.getVisibleSlots(player);
        for (var curio : curios) {
            if (curio.slotIndex() == curioSlot) {
                return curio.handler().getStackInSlot(curio.slotIndex());
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public String toString() {
        return sourceKind.name().toLowerCase() + " slot " + sourceSlot + " toolkit slot " + toolkitSlot;
    }
}
