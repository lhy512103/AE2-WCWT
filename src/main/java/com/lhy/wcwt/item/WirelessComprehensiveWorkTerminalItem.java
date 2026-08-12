package com.lhy.wcwt.item;

import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.UpgradeInventories;
import appeng.api.upgrades.Upgrades;
import appeng.menu.locator.ItemMenuHostLocator;
import appeng.menu.locator.MenuLocators;
import com.lhy.wcwt.helpers.WirelessComprehensiveWorkTerminalMenuHost;
import com.lhy.wcwt.init.ModMenus;
import de.mari_023.ae2wtlib.api.terminal.ItemWT;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class WirelessComprehensiveWorkTerminalItem extends ItemWT {

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

    public WirelessComprehensiveWorkTerminalItem() {
        super();
    }

    @Override
    public MenuType<?> getMenuType(ItemMenuHostLocator locator, Player player) {
        return ModMenus.WCWT_MENU.get();
    }

    @Override
    public IUpgradeInventory getUpgrades(ItemStack stack) {
        // 父类 WirelessTerminalItem.onUpgradesChanged 是 private，子类访问不到，
        // 这里复制其能源卡倍率计算逻辑。
        return UpgradeInventories.forItem(stack, UPGRADE_INVENTORY_SIZE, (st, upgrades) -> {
            updatePowerMultiplier(st, upgrades);
        });
    }

    public void updatePowerMultiplier(ItemStack stack) {
        updatePowerMultiplier(stack, getUpgrades(stack));
    }

    private void updatePowerMultiplier(ItemStack stack, IUpgradeInventory upgrades) {
        setAEMaxPowerMultiplier(stack, getUniversalPowerMultiplier(stack, upgrades));
    }

    private int getUniversalPowerMultiplier(ItemStack stack, IUpgradeInventory upgrades) {
        return 1 + Upgrades.getEnergyCardMultiplier(upgrades);
    }

    @Override
    public double getChargeRate(ItemStack stack) {
        return 800.0 * getUniversalPowerMultiplier(stack, getUpgrades(stack));
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
    }

    @Nullable
    @Override
    public WirelessComprehensiveWorkTerminalMenuHost getMenuHost(Player player, ItemMenuHostLocator locator,
            @Nullable BlockHitResult hitResult) {
        return new WirelessComprehensiveWorkTerminalMenuHost(this, player, locator,
                (p, sm) -> openFromInventory(p, locator, true));
    }

    public boolean openFromInventory(Player player, ItemMenuHostLocator locator) {
        return open(player, locator, false);
    }

    public boolean openFromInventory(Player player, ItemMenuHostLocator locator, boolean returningFromSubmenu) {
        return open(player, locator, returningFromSubmenu);
    }

    @Override
    public boolean open(Player player, ItemMenuHostLocator locator, boolean returningFromSubmenu) {
        return super.open(player, locator, returningFromSubmenu);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide() && openFromInventory(player, MenuLocators.forHand(player, hand))) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }
        return new InteractionResultHolder<>(InteractionResult.FAIL, stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> lines,
            TooltipFlag advancedTooltips) {
        super.appendHoverText(stack, context, lines, advancedTooltips);
    }
}
