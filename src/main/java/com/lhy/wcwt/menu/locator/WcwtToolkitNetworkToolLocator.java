package com.lhy.wcwt.menu.locator;

import org.jetbrains.annotations.Nullable;

import appeng.items.tools.NetworkToolItem;
import appeng.menu.locator.MenuLocator;
import com.lhy.wcwt.compat.CuriosBridge;
import com.lhy.wcwt.helpers.WirelessComprehensiveWorkTerminalMenuHost;
import com.lhy.wcwt.item.WirelessComprehensiveWorkTerminalItem;
import com.lhy.wcwt.helpers.WcwtToolkitNetworkToolMenuHost;
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
        if (terminalStack.isEmpty() || !(terminalStack.getItem() instanceof WirelessComprehensiveWorkTerminalItem)) {
            return null;
        }

        @Nullable Integer inventorySlot = getPlayerInventorySlot();
        var toolkit = WirelessComprehensiveWorkTerminalMenuHost.createToolkitInventory(player, terminalStack);
        if (toolkit == null || toolkitSlot < 0 || toolkitSlot >= toolkit.size()) {
            return null;
        }
        ItemStack stack = toolkit.getStackInSlot(toolkitSlot);
        if (!(stack.getItem() instanceof NetworkToolItem)) {
            return null;
        }

        var toolHost = new WcwtToolkitNetworkToolMenuHost(
                player,
                inventorySlot,
                terminalStack,
                stack,
                null,
                toolkit,
                toolkitSlot);
        return hostInterface.isInstance(toolHost) ? hostInterface.cast(toolHost) : null;
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

    private static ItemStack locateCurioStack(Player player, int equippedCurioIndex) {
        var curios = CuriosBridge.getEquippedSlots(player);
        if (equippedCurioIndex < 0 || equippedCurioIndex >= curios.size()) {
            return ItemStack.EMPTY;
        }
        var curio = curios.get(equippedCurioIndex);
        return curio.handler().getStackInSlot(curio.slotIndex());
    }

    @Override
    public String toString() {
        return sourceKind.name().toLowerCase() + " slot " + sourceSlot + " toolkit slot " + toolkitSlot;
    }
}
