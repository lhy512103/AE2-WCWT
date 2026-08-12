package com.lhy.wcwt.helpers;

import appeng.api.config.Actionable;
import appeng.api.inventories.InternalInventory;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.upgrades.Upgrades;
import appeng.items.contents.NetworkToolMenuHost;
import appeng.items.tools.NetworkToolItem;
import appeng.util.inv.AppEngInternalInventory;
import appeng.util.inv.InternalInventoryHost;
import appeng.util.inv.filter.IAEItemFilter;
import com.google.common.primitives.Ints;
import com.lhy.wcwt.init.ModComponents;
import com.lhy.wcwt.init.ModItems;
import com.lhy.wcwt.menu.locator.WcwtToolkitNetworkToolLocator;
import de.mari_023.ae2wtlib.api.terminal.ItemWT;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import org.jetbrains.annotations.Nullable;

/**
 * 由 WCWT 的卡槽包卡提供的虚拟网络工具宿主。
 * 物品定位器对 AE2 暴露网络工具栈，实际 3×3 内容持久化在终端物品组件中。
 */
public final class WcwtSimulatedNetworkToolMenuHost extends NetworkToolMenuHost<NetworkToolItem> {
    private final ItemStack terminalStack;
    private final WcwtToolkitNetworkToolLocator terminalLocator;
    private final AppEngInternalInventory inventory;

    public WcwtSimulatedNetworkToolMenuHost(NetworkToolItem item, Player player,
            WcwtToolkitNetworkToolLocator locator, ItemStack terminalStack) {
        super(item, player, locator, (IInWorldGridNodeHost) null);
        this.terminalStack = terminalStack;
        this.terminalLocator = locator;
        this.inventory = createInventory(player);
    }

    @Override
    public long insert(Player player, AEKey what, long amount, Actionable mode) {
        if (what instanceof AEItemKey itemKey) {
            var stack = itemKey.toStack(Ints.saturatedCast(amount));
            var overflow = inventory.addItems(stack, mode.isSimulate());
            return stack.getCount() - overflow.getCount();
        }
        return 0;
    }

    @Override
    public InternalInventory getInventory() {
        return inventory;
    }

    @Override
    public boolean isValid() {
        ItemStack current = terminalLocator.locateTerminalStack(getPlayer());
        return !current.isEmpty()
                && current == terminalStack
                && current.getItem() instanceof ItemWT terminalItem
                && terminalItem.getUpgrades(current).isInstalled(ModItems.TOOL_SLOTS_BOX_CARD.get());
    }

    private AppEngInternalInventory createInventory(Player player) {
        var result = new AppEngInternalInventory(new InternalInventoryHost() {
            @Override
            public void saveChangedInventory(AppEngInternalInventory inv) {
                if (!player.level().isClientSide()) {
                    terminalStack.set(ModComponents.NETWORK_TOOL_INV.get(), inv.toItemContainerContents());
                }
            }

            @Override
            public boolean isClientSide() {
                return player.level().isClientSide();
            }
        }, 9);
        result.setEnableClientEvents(true);
        result.setFilter(new NetworkToolInventoryFilter());
        result.fromItemContainerContents(terminalStack.getOrDefault(
                ModComponents.NETWORK_TOOL_INV.get(), ItemContainerContents.EMPTY));
        return result;
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