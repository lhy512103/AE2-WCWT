package com.lhy.wcwt.client;

import com.lhy.wcwt.WcwtMod;
import com.lhy.wcwt.compat.InventoryProfilesNextCompat;
import com.lhy.wcwt.compat.WcwtPolymorphClientCompat;
import com.lhy.wcwt.compat.reflect.WcwtReflect;
import com.lhy.wcwt.init.ModMenus;
import appeng.init.client.InitScreens;
import com.lhy.wcwt.network.OpenTerminalHotkeyPacket;
import com.lhy.wcwt.network.OpenToolkitHotkeyPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = WcwtMod.MOD_ID, value = Dist.CLIENT)
public class ModClientSetup {
    private static boolean ipnCompatInitialized;
    private static final boolean DEBUG_TOOLKIT = Boolean.getBoolean("wcwt.debug.toolkit");

    @SubscribeEvent
    public static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.WCWT_MENU_TYPE, WirelessComprehensiveWorkTerminalScreen::new);
        InitScreens.register(event, ModMenus.WCWT_MAGNET_MENU.get(), WcwtMagnetScreen::new,
                "/screens/wtlib/magnet.json");
        InitScreens.register(event, ModMenus.WCWT_TRASH_MENU.get(), WcwtTrashScreen::new,
                "/screens/wtlib/trash.json");
        WcwtPolymorphClientCompat.registerWidgets();
    }

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(WcwtKeybindings.OPEN_ADVANCED_CODING);
        event.register(WcwtKeybindings.OPEN_COSMETIC_ARMOR);
        event.register(WcwtKeybindings.OPEN_CURIOS);
        event.register(WcwtKeybindings.OPEN_TOOL_SLOTS_BOX);
        event.register(WcwtKeybindings.OPEN_INDEPENDENT_TERMINAL);
        event.register(WcwtKeybindings.OPEN_TOOLKIT);
        event.register(WcwtKeybindings.OPEN_RESONATING_LIGHTNING_PATTERN_CODING);
        event.register(WcwtKeybindings.TOGGLE_FAVORITE_ITEM);
    }

    @SubscribeEvent
    public static void onScreenKeyPressedPre(ScreenEvent.KeyPressed.Pre event) {
        var minecraft = Minecraft.getInstance();
        Screen activeScreen = minecraft.screen;
        WirelessComprehensiveWorkTerminalScreen screen =
                activeScreen instanceof WirelessComprehensiveWorkTerminalScreen wcwtScreen ? wcwtScreen : null;

        if (screen == null) {
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

    @SubscribeEvent
    public static void onClientTickPost(ClientTickEvent.Post event) {
        var minecraft = Minecraft.getInstance();
        if (!ipnCompatInitialized) {
            ipnCompatInitialized = true;
            InventoryProfilesNextCompat.ensureHintsInstalled();
        }
        if (minecraft.player == null) {
            return;
        }
        if (minecraft.screen instanceof WirelessComprehensiveWorkTerminalScreen) {
            while (WcwtKeybindings.OPEN_INDEPENDENT_TERMINAL.consumeClick()) {
            }
            while (WcwtKeybindings.OPEN_TOOLKIT.consumeClick()) {
                if (DEBUG_TOOLKIT) {
                    WcwtMod.LOGGER.info("WCWT toolkit debug: consumed toolkit hotkey while WCWT screen already open");
                }
            }
            return;
        }
        while (WcwtKeybindings.OPEN_INDEPENDENT_TERMINAL.consumeClick()) {
            net.neoforged.neoforge.network.PacketDistributor.sendToServer(new OpenTerminalHotkeyPacket());
        }
        while (WcwtKeybindings.OPEN_TOOLKIT.consumeClick()) {
            if (DEBUG_TOOLKIT) {
                WcwtMod.LOGGER.info("WCWT toolkit debug: sending OpenToolkitHotkeyPacket, screen={}",
                        minecraft.screen == null ? "<null>" : minecraft.screen.getClass().getName());
            }
            net.neoforged.neoforge.network.PacketDistributor.sendToServer(new OpenToolkitHotkeyPacket());
        }
    }

    private static boolean matchesFillSearchHotkey(int keyCode, int scanCode) {
        var keyMapping = WcwtReflect
                .readStaticField("extendedae_plus", "com.extendedae_plus.client.ModKeybindings", "FILL_SEARCH_KEY")
                .orElse(null);
        if (keyMapping != null) {
            var matched = WcwtReflect.findMethod(keyMapping.getClass(), "matches", int.class, int.class)
                    .flatMap(method -> WcwtReflect.invoke(keyMapping, method, keyCode, scanCode))
                    .filter(Boolean.class::isInstance)
                    .map(Boolean.class::cast);
            if (matched.isPresent()) {
                return matched.get();
            }
        }
        return keyCode == GLFW.GLFW_KEY_F;
    }
}
