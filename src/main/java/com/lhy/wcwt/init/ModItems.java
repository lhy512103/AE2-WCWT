package com.lhy.wcwt.init;

import com.lhy.wcwt.WcwtMod;
import com.lhy.wcwt.item.WirelessComprehensiveWorkTerminalItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(WcwtMod.MOD_ID);

    public static final DeferredItem<Item> WIRELESS_COMPREHENSIVE_WORK_TERMINAL = ITEMS.registerItem("wireless_comprehensive_work_terminal", 
            (properties) -> new WirelessComprehensiveWorkTerminalItem(properties.stacksTo(1)));
}
