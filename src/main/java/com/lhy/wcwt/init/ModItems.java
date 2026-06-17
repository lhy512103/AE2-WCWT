package com.lhy.wcwt.init;

import com.lhy.wcwt.WcwtMod;
import com.lhy.wcwt.item.WirelessComprehensiveWorkTerminalItem;
import appeng.api.upgrades.Upgrades;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(WcwtMod.MOD_ID);

    public static final DeferredItem<Item> WIRELESS_COMPREHENSIVE_WORK_TERMINAL = ITEMS.registerItem("wireless_comprehensive_work_terminal", 
            (properties) -> new WirelessComprehensiveWorkTerminalItem(properties.stacksTo(1)));

    public static final DeferredItem<Item> ADVANCED_CODING_CARD = registerUpgradeCard("advanced_coding_card");
    public static final DeferredItem<Item> COSMETIC_ARMOR_CARD = registerUpgradeCard("cosmetic_armor_card");
    public static final DeferredItem<Item> CURIOS_CARD = registerUpgradeCard("curios_card");
    public static final DeferredItem<Item> TOOL_SLOTS_BOX_CARD = registerUpgradeCard("tool_slots_box_card");
    public static final DeferredItem<Item> TOOLKIT_CARD = registerUpgradeCard("toolkit_card");
    public static final DeferredItem<Item> RESONATING_LIGHTNING_PATTERN_CODING_CARD =
            registerUpgradeCard("resonating_lightning_pattern_coding_card");

    private static DeferredItem<Item> registerUpgradeCard(String id) {
        return ITEMS.registerItem(id, properties -> Upgrades.createUpgradeCardItem(properties.stacksTo(1)));
    }
}
