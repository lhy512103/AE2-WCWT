package com.lhy.wcwt.client.gui.widgets;

import java.util.List;
import java.util.function.Supplier;

import net.minecraft.world.inventory.Slot;

import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.IUpgradeableObject;
import appeng.client.gui.WidgetContainer;
import de.mari_023.ae2wtlib.api.gui.ScrollingUpgradesPanel;

/**
 * 当前未启用的占位 wrapper。
 *
 * <p>之前这里放过一个“奇点槽始终占位”的自定义升级面板实现，
 * 但在当前环境下 `Slot.x / Slot.y` 是 final，旧实现会直接编译失败，
 * 所以先注释/停用，保留这个类名给以后继续试验时使用。
 *
 * <p>如果以后想再改自定义逻辑，可以直接在这个文件里重写，
 * 然后在 `WirelessComprehensiveWorkTerminalScreen` 里把
 * `ScrollingUpgradesPanel` 再切回 `WcwtScrollingUpgradesPanel`。
 */
public class WcwtScrollingUpgradesPanel extends ScrollingUpgradesPanel {

    public WcwtScrollingUpgradesPanel(List<Slot> slots, IUpgradeableObject upgradeableObject, WidgetContainer widgets,
            Supplier<IUpgradeInventory> upgrades) {
        super(slots, upgradeableObject, widgets, upgrades);
    }
}
