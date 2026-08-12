package com.lhy.wcwt.init;

import com.lhy.wcwt.WcwtMod;
import com.lhy.wcwt.item.WirelessComprehensiveWorkTerminalItem;
import appeng.api.upgrades.Upgrades;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(WcwtMod.MOD_ID);

    private static WirelessComprehensiveWorkTerminalItem wirelessComprehensiveWorkTerminal;
    public static final Supplier<WirelessComprehensiveWorkTerminalItem> WIRELESS_COMPREHENSIVE_WORK_TERMINAL =
            ModItems::getWirelessComprehensiveWorkTerminal;

    public static synchronized WirelessComprehensiveWorkTerminalItem registerWirelessComprehensiveWorkTerminal() {
        if (wirelessComprehensiveWorkTerminal == null) {
            wirelessComprehensiveWorkTerminal = Registry.register(
                    BuiltInRegistries.ITEM,
                    ResourceLocation.fromNamespaceAndPath(WcwtMod.MOD_ID, "wireless_comprehensive_work_terminal"),
                    new WirelessComprehensiveWorkTerminalItem());
        }
        return wirelessComprehensiveWorkTerminal;
    }

    private static WirelessComprehensiveWorkTerminalItem getWirelessComprehensiveWorkTerminal() {
        if (wirelessComprehensiveWorkTerminal == null) {
            throw new IllegalStateException("WCWT terminal item has not been registered yet");
        }
        return wirelessComprehensiveWorkTerminal;
    }

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
