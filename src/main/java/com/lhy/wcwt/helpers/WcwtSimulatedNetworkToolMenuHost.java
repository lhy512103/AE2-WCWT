package com.lhy.wcwt.helpers;

import appeng.api.inventories.InternalInventory;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.upgrades.Upgrades;
import appeng.items.contents.NetworkToolMenuHost;
import appeng.util.inv.AppEngInternalInventory;
import appeng.util.inv.InternalInventoryHost;
import appeng.util.inv.filter.IAEItemFilter;
import com.lhy.wcwt.menu.locator.WcwtToolkitNetworkToolLocator;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * 用 WCWT 的网络工具卡槽包卡模拟 AE2 网络工具。
 *
 * <p>只提供网络工具自己的升级卡库存；网络操作仍由 AE2 的原始菜单和宿主流程处理，
 * 因此其它模组对网络工具的扩展不需要在 WCWT 中重复适配。</p>
 */
public final class WcwtSimulatedNetworkToolMenuHost extends NetworkToolMenuHost
        implements WcwtNetworkToolSourceHost {
    private static final String INVENTORY_TAG = "wcwtNetworkToolInventory";
    private static final int FLUSH_INTERVAL_TICKS = 10;

    private final ItemStack terminalStack;
    private final WcwtToolkitNetworkToolLocator locator;
    private final AppEngInternalInventory inventory;
    private boolean dirty;
    private int ticksUntilFlush = FLUSH_INTERVAL_TICKS;

    public WcwtSimulatedNetworkToolMenuHost(Player player, @Nullable Integer terminalInventorySlot,
            ItemStack terminalStack, WcwtToolkitNetworkToolLocator locator) {
        super(player, terminalInventorySlot, terminalStack, (IInWorldGridNodeHost) null);
        this.terminalStack = terminalStack;
        this.locator = locator;
        this.inventory = createInventory(player);
        this.inventory.readFromNBT(terminalStack.getOrCreateTag(), INVENTORY_TAG);
    }

    public boolean isCuriosBacked() {
        return getSlot() == null;
    }

    public boolean isSourceStillPresent() {
        ItemStack current = locator.locateTerminalStack(getPlayer());
        return !current.isEmpty() && ItemStack.isSameItemSameTags(current, terminalStack);
    }

    @Override
    public InternalInventory getInventory() {
        return inventory;
    }

    @Override
    public InternalInventory getInternalInventory() {
        return inventory;
    }

    @Override
    public boolean onBroadcastChanges(AbstractContainerMenu menu) {
        boolean keepOpen = super.onBroadcastChanges(menu);
        if (!keepOpen) {
            flush();
            return false;
        }
        if (!isClientSide() && dirty && --ticksUntilFlush <= 0) {
            flush();
        }
        return true;
    }

    private AppEngInternalInventory createInventory(Player player) {
        var result = new AppEngInternalInventory(new InternalInventoryHost() {
            @Override
            public void onChangeInventory(InternalInventory inv, int slot) {
                markDirtyAndFlush();
            }

            @Override
            public void saveChanges() {
                markDirtyAndFlush();
            }

            @Override
            public boolean isClientSide() {
                return player.level().isClientSide();
            }
        }, 9);
        result.setEnableClientEvents(true);
        result.setFilter(new NetworkToolInventoryFilter());
        return result;
    }

    private void markDirtyAndFlush() {
        dirty = true;
        ticksUntilFlush = FLUSH_INTERVAL_TICKS;
        flush();
    }

    private void flush() {
        if (isClientSide() || !dirty || !isSourceStillPresent()) {
            return;
        }
        inventory.writeToNBT(terminalStack.getOrCreateTag(), INVENTORY_TAG);
        dirty = false;
        ticksUntilFlush = FLUSH_INTERVAL_TICKS;
    }

    private static final class NetworkToolInventoryFilter implements IAEItemFilter {
        @Override
        public boolean allowExtract(InternalInventory inv, int slot, int amount) {
            return true;
        }

        @Override
        public boolean allowInsert(InternalInventory inv, int slot, ItemStack stack) {
            return Upgrades.isUpgradeCardItem(stack.getItem());
        }
    }
}