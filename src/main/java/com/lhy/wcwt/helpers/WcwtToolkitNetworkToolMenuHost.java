package com.lhy.wcwt.helpers;

import appeng.api.inventories.InternalInventory;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.items.tools.NetworkToolItem;
import appeng.menu.locator.ItemMenuHostLocator;
import appeng.util.inv.AppEngInternalInventory;
import appeng.util.inv.InternalInventoryHost;
import appeng.util.inv.filter.IAEItemFilter;
import com.google.common.primitives.Ints;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import org.jetbrains.annotations.Nullable;

import appeng.api.config.Actionable;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.items.contents.NetworkToolMenuHost;

/**
 * 让 AE 网络工具放在 WCWT 工具包中时，内部 9 格升级卡库存的修改能够正确回写到工具包槽位。
 */
public class WcwtToolkitNetworkToolMenuHost extends NetworkToolMenuHost<NetworkToolItem> {
    private static final int FLUSH_INTERVAL_TICKS = Math.max(1,
            Integer.getInteger("wcwt.toolkitNetworkToolFlushTicks", 10));

    private final WirelessComprehensiveWorkTerminalMenuHost terminalHost;
    private final int toolkitSlot;
    private final AppEngInternalInventory toolkitInventory;
    private boolean toolkitInventoryDirty;
    private int ticksUntilFlush = FLUSH_INTERVAL_TICKS;

    public WcwtToolkitNetworkToolMenuHost(NetworkToolItem item, Player player, ItemMenuHostLocator locator,
            @Nullable IInWorldGridNodeHost host, WirelessComprehensiveWorkTerminalMenuHost terminalHost, int toolkitSlot) {
        super(item, player, locator, host);
        this.terminalHost = terminalHost;
        this.toolkitSlot = toolkitSlot;
        this.toolkitInventory = createToolkitAwareNetworkToolInventory(player);
    }

    @Override
    public long insert(Player player, AEKey what, long amount, Actionable mode) {
        if (what instanceof AEItemKey itemKey) {
            var stack = itemKey.toStack(Ints.saturatedCast(amount));
            var overflow = getInventory().addItems(stack, mode.isSimulate());
            return stack.getCount() - overflow.getCount();
        }
        return 0;
    }

    @Override
    public InternalInventory getInventory() {
        return toolkitInventory;
    }

    @Override
    public void tick() {
        super.tick();
        if (isClientSide() || !toolkitInventoryDirty) {
            return;
        }

        if (--ticksUntilFlush <= 0) {
            flushDirtyToolInventory();
        }
    }

    @Override
    public boolean isValid() {
        boolean valid = super.isValid();
        if (!valid) {
            flushDirtyToolInventory();
        }
        return valid;
    }

    private AppEngInternalInventory createToolkitAwareNetworkToolInventory(Player player) {
        var inventory = new AppEngInternalInventory(new InternalInventoryHost() {
            @Override
            public void saveChangedInventory(AppEngInternalInventory inv) {
                markToolInventoryDirty();
            }

            @Override
            public boolean isClientSide() {
                return player.level().isClientSide();
            }
        }, 9);
        inventory.setEnableClientEvents(true);
        inventory.setFilter(new NetworkToolInventoryFilter());
        inventory.fromItemContainerContents(getItemStack().getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY));
        return inventory;
    }

    private void markToolInventoryDirty() {
        if (isClientSide()) {
            return;
        }
        toolkitInventoryDirty = true;
        flushDirtyToolInventory();
        ticksUntilFlush = FLUSH_INTERVAL_TICKS;
    }

    private void flushDirtyToolInventory() {
        if (isClientSide() || !toolkitInventoryDirty) {
            return;
        }

        var toolkit = terminalHost.getSubInventory(WirelessComprehensiveWorkTerminalMenuHost.INV_TOOLKIT);
        if (toolkit == null || toolkitSlot < 0 || toolkitSlot >= toolkit.size()) {
            return;
        }

        ItemStack currentTool = toolkit.getStackInSlot(toolkitSlot);
        if (currentTool.isEmpty() || !(currentTool.getItem() instanceof NetworkToolItem)) {
            return;
        }

        ItemContainerContents contents = toolkitInventory.toItemContainerContents();
        ItemContainerContents currentContents = currentTool.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        if (!contents.equals(currentContents)) {
            currentTool.set(DataComponents.CONTAINER, contents);
            toolkit.setItemDirect(toolkitSlot, currentTool);
        }

        toolkitInventoryDirty = false;
        ticksUntilFlush = FLUSH_INTERVAL_TICKS;
    }

    private static final class NetworkToolInventoryFilter implements IAEItemFilter {
        @Override
        public boolean allowExtract(InternalInventory inv, int slot, int amount) {
            return true;
        }

        @Override
        public boolean allowInsert(InternalInventory inv, int slot, ItemStack stack) {
            return appeng.api.upgrades.Upgrades.isUpgradeCardItem(stack.getItem());
        }
    }
}
