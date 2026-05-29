package com.lhy.wcwt.init;

import appeng.menu.implementations.MenuTypeBuilder;
import com.lhy.wcwt.WcwtMod;
import com.lhy.wcwt.helpers.WirelessComprehensiveWorkTerminalMenuHost;
import com.lhy.wcwt.menu.WcwtMagnetMenu;
import com.lhy.wcwt.menu.WcwtTrashMenu;
import com.lhy.wcwt.menu.WirelessComprehensiveWorkTerminalMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, WcwtMod.MOD_ID);

    public static final RegistryObject<MenuType<WirelessComprehensiveWorkTerminalMenu>> WCWT_MENU = MENUS.register(
            "wcwt",
            () -> MenuTypeBuilder.create(
                    (int id, net.minecraft.world.entity.player.Inventory ip, WirelessComprehensiveWorkTerminalMenuHost host) ->
                            new WirelessComprehensiveWorkTerminalMenu(id, ip, host),
                    WirelessComprehensiveWorkTerminalMenuHost.class).build("wcwt"));

    public static final RegistryObject<MenuType<WcwtMagnetMenu>> WCWT_MAGNET_MENU = MENUS.register(
            "wcwt_magnet",
            () -> MenuTypeBuilder.create(
                    (int id, net.minecraft.world.entity.player.Inventory ip, WirelessComprehensiveWorkTerminalMenuHost host) ->
                            new WcwtMagnetMenu(id, ip, host),
                    WirelessComprehensiveWorkTerminalMenuHost.class).build("wcwt_magnet"));

    public static final RegistryObject<MenuType<WcwtTrashMenu>> WCWT_TRASH_MENU = MENUS.register(
            "wcwt_trash",
            () -> MenuTypeBuilder.create(
                    (int id, net.minecraft.world.entity.player.Inventory ip, WirelessComprehensiveWorkTerminalMenuHost host) ->
                            new WcwtTrashMenu(id, ip, host),
                    WirelessComprehensiveWorkTerminalMenuHost.class).build("wcwt_trash"));
}
