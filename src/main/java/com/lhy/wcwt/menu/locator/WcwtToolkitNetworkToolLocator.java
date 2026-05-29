package com.lhy.wcwt.menu.locator;

import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import appeng.integration.modules.curios.CuriosIntegration;
import appeng.menu.locator.ItemMenuHostLocator;
import appeng.menu.locator.MenuLocators;
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
        implements ItemMenuHostLocator {

    public enum SourceKind {
        INVENTORY,
        CURIOS
    }

    @Override
    public ItemStack locateItem(Player player) {
        ItemStack terminalStack = locateTerminalStack(player);
        if (terminalStack.isEmpty() || !(terminalStack.getItem() instanceof WirelessComprehensiveWorkTerminalItem terminalItem)) {
            return ItemStack.EMPTY;
        }

        ItemMenuHostLocator terminalLocator = switch (sourceKind) {
            case INVENTORY -> MenuLocators.forInventorySlot(sourceSlot);
            case CURIOS -> MenuLocators.forCurioSlot(sourceSlot);
        };
        var terminalHost = terminalItem.getMenuHost(player, terminalLocator, null);
        if (terminalHost == null) {
            return ItemStack.EMPTY;
        }

        var toolkit = terminalHost.getSubInventory(WirelessComprehensiveWorkTerminalMenuHost.INV_TOOLKIT);
        if (toolkit == null || toolkitSlot < 0 || toolkitSlot >= toolkit.size()) {
            return ItemStack.EMPTY;
        }
        return toolkit.getStackInSlot(toolkitSlot);
    }

    @Override
    public @Nullable Integer getPlayerInventorySlot() {
        return sourceKind == SourceKind.INVENTORY ? sourceSlot : null;
    }

    @Override
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
        var cap = player.getCapability(CuriosIntegration.ITEM_HANDLER);
        if (cap == null || curioSlot < 0 || curioSlot >= cap.getSlots()) {
            return ItemStack.EMPTY;
        }
        return cap.getStackInSlot(curioSlot);
    }

    @Override
    public String toString() {
        return sourceKind.name().toLowerCase() + " slot " + sourceSlot + " toolkit slot " + toolkitSlot;
    }
}
