package com.lhy.wcwt.item;

import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.UpgradeInventories;
import appeng.api.upgrades.Upgrades;
import appeng.api.implementations.menuobjects.ItemMenuHost;
import appeng.core.localization.PlayerMessages;
import appeng.items.tools.powered.WirelessCraftingTerminalItem;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocator;
import com.lhy.wcwt.helpers.WirelessComprehensiveWorkTerminalMenuHost;
import com.lhy.wcwt.init.ModMenus;
import com.lhy.wcwt.menu.locator.WcwtInventoryLocator;
import com.lhy.wcwt.menu.locator.WcwtItemLocator;
import com.lhy.wcwt.migration.WcwtLegacyUniversalTerminalMigration;
import de.mari_023.ae2wtlib.AE2wtlib;
import de.mari_023.ae2wtlib.terminal.IUniversalWirelessTerminalItem;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public class WirelessComprehensiveWorkTerminalItem extends WirelessCraftingTerminalItem
        implements IUniversalWirelessTerminalItem {

    /**
     * 综合工作终端兼容的升级卡总槽位上限。
     * 父类 {@link appeng.items.tools.powered.WirelessTerminalItem} 写死 2 槽，无法满足 WCWT 的需求。
     *
     * <p>该值必须 ≥ 在 {@code WcwtMod.commonSetup} 里所有 {@code Upgrades.add(card, wcwt, max, ...)}
     * 第三参数 {@code max} 之和。
     * 想加更多升级卡时，先在 {@code WcwtMod} 里加 {@code Upgrades.add(...)}，再把这里的总数同步加大。
     *
     * <p>当前注册：能源卡(10) + 量子桥卡(1) + 磁力卡(1) + 导入卡(1) + 导出卡(1)
     * + 六张扩展 UI 卡(6) = 20。
     */
    public static final int UPGRADE_INVENTORY_SIZE = 20;

    public WirelessComprehensiveWorkTerminalItem(Item.Properties properties) {
        super(() -> 1600000.0, properties);
    }

    @Override
    public MenuType<?> getMenuType() {
        return ModMenus.WCWT_MENU.get();
    }

    @Override
    public MenuType<?> getMenuType(ItemStack stack) {
        return getMenuType();
    }

    @Override
    public IUpgradeInventory getUpgrades(ItemStack stack) {
        // 父类 WirelessTerminalItem.onUpgradesChanged 是 private，子类访问不到，
        // 这里复制其能源卡倍率计算逻辑。
        return UpgradeInventories.forItem(stack, UPGRADE_INVENTORY_SIZE, (st, upgrades) -> {
            updatePowerMultiplier(st);
        });
    }

    public void updatePowerMultiplier(ItemStack stack) {
        setAEMaxPowerMultiplier(stack, 1 + Upgrades.getEnergyCardMultiplier(getUpgrades(stack)));
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        if (!level.isClientSide() && entity instanceof Player player) {
            WcwtLegacyUniversalTerminalMigration.migrate(player, stack);
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            return new InteractionResultHolder<>(
                    stack.isEmpty() || stack.getItem() != this ? InteractionResult.FAIL : InteractionResult.SUCCESS,
                    stack);
        }
        int slot = findHeldInventorySlot(player, stack);
        if (slot >= 0 && openFromLocator(player, new WcwtInventoryLocator(slot), false)) {
            return new InteractionResultHolder<>(InteractionResult.sidedSuccess(false), stack);
        }
        return new InteractionResultHolder<>(InteractionResult.FAIL, stack);
    }

    private static int findHeldInventorySlot(Player player, ItemStack stack) {
        int size = player.getInventory().getContainerSize();
        for (int i = 0; i < size; i++) {
            if (player.getInventory().getItem(i) == stack) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 这里不能像 AE2 原版无线终端那样预先要求 {@code getLinkedGrid()} 非空，
     * 否则纯量子桥链路（无范围内 WAP、跨维度/超距离）会在打开前就被直接拦掉。
     *
     * <p>与 WTLib 的 {@code ItemWT#checkPreconditions} 对齐，只校验物品本身；
     * 实际链路状态交给 {@link WirelessComprehensiveWorkTerminalMenuHost} 在菜单宿主里判断。
     */
    @Override
    protected boolean checkPreconditions(ItemStack item, Player player) {
        if (player.level().isClientSide()) {
            return false;
        }
        if (item.isEmpty() || (item.getItem() != this && item.getItem() != AE2wtlib.UNIVERSAL_TERMINAL)) {
            return false;
        }
        if (!hasPower(player, 0.5, item)) {
            player.displayClientMessage(PlayerMessages.DeviceNotPowered.text(), true);
            return false;
        }

        if (getLinkedGrid(item, player.level(), null) != null) {
            return true;
        }

        var host = new WirelessComprehensiveWorkTerminalMenuHost(player, null, item, (p, sm) -> {
        });
        boolean canOpen = host.canOpenFromAnyLink();
        if (!canOpen) {
            var description = host.getCurrentLinkStatusDescription();
            if (description != null) {
                player.displayClientMessage(description, true);
            } else {
                getLinkedGrid(item, player.level(), player);
            }
        }
        return canOpen;
    }

    @Nullable
    @Override
    public ItemMenuHost getMenuHost(Player player, int inventorySlot, ItemStack stack, @Nullable BlockPos pos) {
        return new WirelessComprehensiveWorkTerminalMenuHost(player, inventorySlot, stack,
                (p, sm) -> openFromLocator(p, new WcwtInventoryLocator(inventorySlot, pos), true));
    }

    @Nullable
    public ItemMenuHost getMenuHost(Player player, MenuLocator locator, ItemStack stack) {
        return new WirelessComprehensiveWorkTerminalMenuHost(player, null, stack,
                (p, sm) -> openFromCurio(p, locator, stack, true));
    }

    public boolean openFromUniversalLocator(Player player, MenuLocator locator, ItemStack stack,
            boolean returningFromSubmenu) {
        if (locator instanceof WcwtItemLocator itemLocator) {
            return openFromLocator(player, itemLocator, returningFromSubmenu);
        }
        return openFromCurio(player, locator, stack, returningFromSubmenu);
    }

    public boolean openFromLocator(Player player, WcwtItemLocator locator, boolean returningFromSubmenu) {
        ItemStack stack = locator.locateItem(player);
        if (checkPreconditions(stack, player)) {
            return MenuOpener.open(getMenuType(), player, locator, returningFromSubmenu);
        }
        return false;
    }

    public boolean openFromCurio(Player player, MenuLocator locator, ItemStack stack, boolean returningFromSubmenu) {
        if (locator instanceof WcwtItemLocator itemLocator) {
            return openFromLocator(player, itemLocator, returningFromSubmenu);
        }
        if (checkPreconditions(stack, player)) {
            return MenuOpener.open(getMenuType(), player, locator, returningFromSubmenu);
        }
        return false;
    }

    @Override
    protected boolean openFromInventory(Player player, int inventorySlot, boolean returningFromSubmenu) {
        return openFromLocator(player, new WcwtInventoryLocator(inventorySlot), returningFromSubmenu);
    }
}
