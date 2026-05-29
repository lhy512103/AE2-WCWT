package com.lhy.wcwt.helpers;

import appeng.api.inventories.InternalInventory;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.upgrades.Upgrades;
import appeng.items.contents.NetworkToolMenuHost;
import appeng.items.tools.NetworkToolItem;
import appeng.util.inv.AppEngInternalInventory;
import appeng.util.inv.InternalInventoryHost;
import appeng.util.inv.filter.IAEItemFilter;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import appeng.api.config.Actionable;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;

/**
 * 让 AE 网络工具放在 WCWT 工具包中时，内部 9 格升级卡库存的修改能够正确回写到工具包槽位。
 */
public class WcwtToolkitNetworkToolMenuHost extends NetworkToolMenuHost {
    private static final int FLUSH_INTERVAL_TICKS = Math.max(1,
            Integer.getInteger("wcwt.toolkitNetworkToolFlushTicks", 10));

    private final WirelessComprehensiveWorkTerminalMenuHost terminalHost;
    private final int toolkitSlot;
    private final AppEngInternalInventory toolkitInventory;
    private boolean toolkitInventoryDirty;
    private int ticksUntilFlush = FLUSH_INTERVAL_TICKS;

    public WcwtToolkitNetworkToolMenuHost(Player player, @Nullable Integer inventorySlot, ItemStack toolStack,
            @Nullable IInWorldGridNodeHost host, WirelessComprehensiveWorkTerminalMenuHost terminalHost, int toolkitSlot) {
        super(player, inventorySlot, toolStack, host);
        this.terminalHost = terminalHost;
        this.toolkitSlot = toolkitSlot;
        this.toolkitInventory = createToolkitAwareNetworkToolInventory(player, toolStack);
    }

    public long insert(Player player, AEKey what, long amount, Actionable mode) {
        if (what instanceof AEItemKey itemKey) {
            int insertCount = (int) Math.min(Integer.MAX_VALUE, amount);
            var stack = itemKey.toStack(insertCount);
            var overflow = getInternalInventory().addItems(stack, mode.isSimulate());
            return stack.getCount() - overflow.getCount();
        }
        return 0;
    }

    @Override
    public InternalInventory getInventory() {
        return toolkitInventory;
    }

    @Override
    public boolean onBroadcastChanges(AbstractContainerMenu menu) {
        boolean keepOpen = super.onBroadcastChanges(menu);
        if (!keepOpen) {
            flushDirtyToolInventory();
            return false;
        }
        if (!isClientSide() && toolkitInventoryDirty && --ticksUntilFlush <= 0) {
            flushDirtyToolInventory();
        }
        return true;
    }

    private AppEngInternalInventory createToolkitAwareNetworkToolInventory(Player player, ItemStack toolStack) {
        var inventory = new AppEngInternalInventory(new InternalInventoryHost() {
            @Override
            public void onChangeInventory(InternalInventory inv, int slot) {
                markToolInventoryDirty();
            }

            @Override
            public void saveChanges() {
                markToolInventoryDirty();
            }

            @Override
            public boolean isClientSide() {
                return player.level().isClientSide();
            }
        }, 9);
        inventory.setEnableClientEvents(true);
        inventory.setFilter(new NetworkToolInventoryFilter());
        inventory.readFromNBT(toolStack.getOrCreateTag(), "inv");
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

        toolkitInventory.writeToNBT(currentTool.getOrCreateTag(), "inv");
        toolkit.setItemDirect(toolkitSlot, currentTool);

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
            return Upgrades.isUpgradeCardItem(stack.getItem());
        }
    }
}
