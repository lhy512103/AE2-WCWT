package com.lhy.wcwt.client;

import appeng.init.client.InitScreens;
import com.lhy.wcwt.WcwtMod;
import com.lhy.wcwt.compat.InventoryProfilesNextCompat;
import com.lhy.wcwt.init.ModMenus;
import com.lhy.wcwt.menu.WirelessComprehensiveWorkTerminalMenu;
import com.lhy.wcwt.network.CraftingLockPacket;
import com.lhy.wcwt.network.ModNetworking;
import com.lhy.wcwt.network.OpenToolkitHotkeyPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Method;

@Mod.EventBusSubscriber(modid = WcwtMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ModClientSetup {
    private static boolean ipnCompatInitialized;
    private static final boolean DEBUG_TOOLKIT = Boolean.getBoolean("wcwt.debug.toolkit");

    public static void init(IEventBus modBus) {
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((minecraft, parent) -> new WcwtConfigScreen(parent)));
        modBus.addListener(ModClientSetup::onRegisterKeyMappings);
        modBus.addListener(ModClientSetup::onClientSetup);
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            WcwtMod.LOGGER.info("Registering WCWT client screens");
            net.minecraft.client.gui.screens.MenuScreens.register(ModMenus.WCWT_MENU.get(), ModClientSetup::createMainScreen);
            InitScreens.register(ModMenus.WCWT_MAGNET_MENU.get(), WcwtMagnetScreen::new, "/screens/wtlib/magnet.json");
            InitScreens.register(ModMenus.WCWT_TRASH_MENU.get(), WcwtTrashScreen::new, "/screens/wtlib/trash.json");
        });
    }

    private static WirelessComprehensiveWorkTerminalScreen createMainScreen(
            com.lhy.wcwt.menu.WirelessComprehensiveWorkTerminalMenu menu, Inventory inv, Component title) {
        try {
            return new WirelessComprehensiveWorkTerminalScreen(menu, inv, title);
        } catch (Throwable t) {
            WcwtMod.LOGGER.error("Failed to construct WCWT main screen", t);
            return null;
        }
    }

    private static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(WcwtKeybindings.OPEN_ADVANCED_CODING);
        event.register(WcwtKeybindings.OPEN_COSMETIC_ARMOR);
        event.register(WcwtKeybindings.OPEN_CURIOS);
        event.register(WcwtKeybindings.OPEN_TOOL_SLOTS_BOX);
        event.register(WcwtKeybindings.OPEN_TOOLKIT);
        event.register(WcwtKeybindings.OPEN_RESONATING_LIGHTNING_PATTERN_CODING);
        event.register(WcwtKeybindings.TOGGLE_FAVORITE_ITEM);
        event.register(WcwtKeybindings.TOGGLE_CRAFTING_LOCK);
    }

    @SubscribeEvent
    public static void onScreenKeyPressedPre(ScreenEvent.KeyPressed.Pre event) {
        if (WcwtKeybindings.TOGGLE_CRAFTING_LOCK.matches(event.getKeyCode(), event.getScanCode())
                && toggleCurrentWcwtCraftingLock()) {
            event.setCanceled(true);
            return;
        }
        if (!(Minecraft.getInstance().screen instanceof WirelessComprehensiveWorkTerminalScreen screen)) {
            return;
        }
        if (!screen.isTypingInPatternManagementField()
                && screen.handleExtendedUiHotkey(event.getKeyCode(), event.getScanCode())) {
            event.setCanceled(true);
            return;
        }
        if (WcwtKeybindings.TOGGLE_FAVORITE_ITEM.matches(event.getKeyCode(), event.getScanCode())
                && screen.toggleFavoriteForHoveredRepoSlot()) {
            event.setCanceled(true);
            return;
        }
        if (!matchesFillSearchHotkey(event.getKeyCode(), event.getScanCode())) {
            return;
        }
        if (screen.fillProviderSearchFromJeiIngredient()) {
            event.setCanceled(true);
        }
    }

    private static boolean toggleCurrentWcwtCraftingLock() {
        var minecraft = Minecraft.getInstance();
        if (minecraft.player == null
                || !(minecraft.player.containerMenu instanceof WirelessComprehensiveWorkTerminalMenu menu)) {
            return false;
        }
        var host = menu.getMenuHost();
        if (host == null) {
            return false;
        }

        boolean locked = !host.isCraftingGridLocked();
        host.setCraftingGridLocked(locked);
        ModNetworking.sendToServer(new CraftingLockPacket(locked));
        return true;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onWirelessTerminalScreenInitPost(ScreenEvent.Init.Post event) {
        if (event.getScreen() instanceof WirelessComprehensiveWorkTerminalScreen) {
            InventoryProfilesNextCompat.installRuntimeHints();
        }
    }

    @SubscribeEvent
    public static void onClientTickPost(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        var minecraft = Minecraft.getInstance();
        if (!ipnCompatInitialized) {
            ipnCompatInitialized = true;
            InventoryProfilesNextCompat.ensureHintsInstalled();
        }
        if (minecraft.player == null) {
            return;
        }
        if (minecraft.screen instanceof WirelessComprehensiveWorkTerminalScreen) {
            while (WcwtKeybindings.OPEN_TOOLKIT.consumeClick()) {
                if (DEBUG_TOOLKIT) {
                    WcwtMod.LOGGER.info("WCWT toolkit debug: consumed toolkit hotkey while WCWT screen already open");
                }
            }
            return;
        }
        while (WcwtKeybindings.OPEN_TOOLKIT.consumeClick()) {
            if (DEBUG_TOOLKIT) {
                WcwtMod.LOGGER.info("WCWT toolkit debug: sending OpenToolkitHotkeyPacket, screen={}",
                        minecraft.screen == null ? "<null>" : minecraft.screen.getClass().getName());
            }
            ModNetworking.sendToServer(new OpenToolkitHotkeyPacket());
        }
    }

    private static boolean matchesFillSearchHotkey(int keyCode, int scanCode) {
        if (ModList.get().isLoaded("extendedae_plus")) {
            try {
                Class<?> kb = Class.forName("com.extendedae_plus.client.ModKeybindings");
                Object km = kb.getField("FILL_SEARCH_KEY").get(null);
                Method m = km.getClass().getMethod("matches", int.class, int.class);
                Object r = m.invoke(km, keyCode, scanCode);
                if (r instanceof Boolean b) {
                    return b;
                }
            } catch (Throwable ignored) {
            }
        }
        return keyCode == GLFW.GLFW_KEY_F;
    }
}
