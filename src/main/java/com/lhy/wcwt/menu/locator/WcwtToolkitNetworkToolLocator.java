package com.lhy.wcwt.menu.locator;


import org.jetbrains.annotations.Nullable;

import appeng.api.inventories.InternalInventory;
import appeng.core.definitions.AEItems;
import appeng.integration.modules.curios.CuriosIntegration;
import appeng.menu.locator.ItemMenuHostLocator;
import com.lhy.wcwt.helpers.WcwtToolkitStore;
import com.lhy.wcwt.item.WirelessComprehensiveWorkTerminalItem;
import de.mari_023.ae2wtlib.api.terminal.ItemWT;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 定位 WCWT 终端工具包中的 AE 网络工具。
 *
 * <p>AE2 在菜单打开期间每个服务端 tick 都会重新定位宿主物品，所以这里只读取按玩家共享的工具包库存，
 * 不再为了读一格而构造整个终端宿主（那会解析终端 NBT 并扫描无线接入点）。
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
        if (terminalStack.isEmpty() || !(terminalStack.getItem() instanceof ItemWT)) {
            return ItemStack.EMPTY;
        }

        if (toolkitSlot < 0) {
            return AEItems.NETWORK_TOOL.stack();
        }

        if (!(terminalStack.getItem() instanceof WirelessComprehensiveWorkTerminalItem)) {
            return ItemStack.EMPTY;
        }
        // 客户端没有共享工具包，和以前一样定位为空。
        InternalInventory toolkit = WcwtToolkitStore.items(player, terminalStack);
        if (toolkit == null || toolkitSlot >= toolkit.size()) {
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

    public ItemStack locateTerminalStack(Player player) {
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
