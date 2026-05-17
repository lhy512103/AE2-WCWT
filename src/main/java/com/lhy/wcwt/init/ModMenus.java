package com.lhy.wcwt.init;

import com.lhy.wcwt.WcwtMod;
import com.lhy.wcwt.helpers.WirelessComprehensiveWorkTerminalMenuHost;
import com.lhy.wcwt.menu.WcwtMagnetMenu;
import com.lhy.wcwt.menu.WcwtTrashMenu;
import com.lhy.wcwt.menu.WirelessComprehensiveWorkTerminalMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import appeng.menu.implementations.MenuTypeBuilder;

public class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, WcwtMod.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<WirelessComprehensiveWorkTerminalMenu>> WCWT_MENU = MENUS.register("wcwt", 
            () -> MenuTypeBuilder.create((int id, net.minecraft.world.entity.player.Inventory ip, WirelessComprehensiveWorkTerminalMenuHost host) -> new WirelessComprehensiveWorkTerminalMenu(id, ip, host), WirelessComprehensiveWorkTerminalMenuHost.class).build("wcwt"));
    public static final DeferredHolder<MenuType<?>, MenuType<WcwtMagnetMenu>> WCWT_MAGNET_MENU = MENUS.register("wcwt_magnet",
            () -> MenuTypeBuilder.create((int id, net.minecraft.world.entity.player.Inventory ip, WirelessComprehensiveWorkTerminalMenuHost host) -> new WcwtMagnetMenu(id, ip, host), WirelessComprehensiveWorkTerminalMenuHost.class).build("wcwt_magnet"));
    public static final DeferredHolder<MenuType<?>, MenuType<WcwtTrashMenu>> WCWT_TRASH_MENU = MENUS.register("wcwt_trash",
            () -> MenuTypeBuilder.create((int id, net.minecraft.world.entity.player.Inventory ip, WirelessComprehensiveWorkTerminalMenuHost host) -> new WcwtTrashMenu(id, ip, host), WirelessComprehensiveWorkTerminalMenuHost.class).build("wcwt_trash"));
}
