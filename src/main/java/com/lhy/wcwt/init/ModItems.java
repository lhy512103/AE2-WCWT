package com.lhy.wcwt.init;

import com.lhy.wcwt.WcwtMod;
import com.lhy.wcwt.item.WirelessComprehensiveWorkTerminalItem;
import appeng.api.upgrades.Upgrades;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, WcwtMod.MOD_ID);

    public static final RegistryObject<Item> WIRELESS_COMPREHENSIVE_WORK_TERMINAL =
            ITEMS.register("wireless_comprehensive_work_terminal",
                    () -> new WirelessComprehensiveWorkTerminalItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> ADVANCED_CODING_CARD = registerUpgradeCard("advanced_coding_card");
    public static final RegistryObject<Item> COSMETIC_ARMOR_CARD = registerUpgradeCard("cosmetic_armor_card");
    public static final RegistryObject<Item> CURIOS_CARD = registerUpgradeCard("curios_card");
    public static final RegistryObject<Item> TOOL_SLOTS_BOX_CARD = registerUpgradeCard("tool_slots_box_card");
    public static final RegistryObject<Item> TOOLKIT_CARD = registerUpgradeCard("toolkit_card");
    public static final RegistryObject<Item> RESONATING_LIGHTNING_PATTERN_CODING_CARD =
            registerUpgradeCard("resonating_lightning_pattern_coding_card");

    private static RegistryObject<Item> registerUpgradeCard(String id) {
        return ITEMS.register(id, () -> Upgrades.createUpgradeCardItem(new Item.Properties().stacksTo(1)));
    }
}
