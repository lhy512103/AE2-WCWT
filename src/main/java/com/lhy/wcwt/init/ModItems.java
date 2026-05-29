package com.lhy.wcwt.init;

import com.lhy.wcwt.WcwtMod;
import com.lhy.wcwt.item.WirelessComprehensiveWorkTerminalItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, WcwtMod.MOD_ID);

    public static final RegistryObject<Item> WIRELESS_COMPREHENSIVE_WORK_TERMINAL =
            ITEMS.register("wireless_comprehensive_work_terminal",
                    () -> new WirelessComprehensiveWorkTerminalItem(new Item.Properties().stacksTo(1)));
}
