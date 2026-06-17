package com.lhy.wcwt.client;

import com.lhy.wcwt.WcwtMod;
import com.lhy.wcwt.compat.InventoryProfilesNextCompat;
import com.lhy.wcwt.compat.WcwtPolymorphClientCompat;
import com.lhy.wcwt.init.ModMenus;
import com.lhy.wcwt.menu.WirelessComprehensiveWorkTerminalMenu;
import com.lhy.wcwt.network.CraftingLockPacket;
import com.lhy.wcwt.network.OpenTerminalHotkeyPacket;
import com.lhy.wcwt.network.OpenToolkitHotkeyPacket;
import appeng.init.client.InitScreens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.fml.ModList;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Method;

@EventBusSubscriber(modid = WcwtMod.MOD_ID, value = Dist.CLIENT)
public class ModClientSetup {
    private static boolean ipnCompatInitialized;
    private static final boolean DEBUG_TOOLKIT = Boolean.getBoolean("wcwt.debug.toolkit");

    @SubscribeEvent
    public static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.WCWT_MENU.get(), WirelessComprehensiveWorkTerminalScreen::new);
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
        event.register(WcwtKeybindings.OPEN_TERMINAL);
        event.register(WcwtKeybindings.OPEN_TOOLKIT);
        event.register(WcwtKeybindings.OPEN_RESONATING_LIGHTNING_PATTERN_CODING);
        event.register(WcwtKeybindings.TOGGLE_CRAFTING_GRID_LOCK);
        event.register(WcwtKeybindings.TOGGLE_FAVORITE_ITEM);
    }

    @SubscribeEvent
    public static void onScreenKeyPressedPre(ScreenEvent.KeyPressed.Pre event) {
        var minecraft = Minecraft.getInstance();
        Screen activeScreen = minecraft.screen;
        WirelessComprehensiveWorkTerminalScreen screen =
                activeScreen instanceof WirelessComprehensiveWorkTerminalScreen wcwtScreen ? wcwtScreen : null;

        if (handleCraftingGridLockHotkey(event.getKeyCode(), event.getScanCode(), minecraft, activeScreen)) {
            event.setCanceled(true);
            return;
        }

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

    /**
     * 与 Inventory Profiles Next 相同：{@code Init.Post} 注入控件。使用最低优先级，
     * 保证在 IPN 的 {@link ScreenEvent.Init.Post} 之后执行，再递归隐藏深层子控件。
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onWirelessTerminalScreenInitPost(ScreenEvent.Init.Post event) {
        if (event.getScreen() instanceof WirelessComprehensiveWorkTerminalScreen) {
            InventoryProfilesNextCompat.installRuntimeHints();
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
            while (WcwtKeybindings.OPEN_TERMINAL.consumeClick()) {
            }
            while (WcwtKeybindings.OPEN_TOOLKIT.consumeClick()) {
                if (DEBUG_TOOLKIT) {
                    WcwtMod.LOGGER.info("WCWT toolkit debug: consumed toolkit hotkey while WCWT screen already open");
                }
            }
            return;
        }
        while (WcwtKeybindings.OPEN_TERMINAL.consumeClick()) {
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

    private static boolean matchesCraftingGridLockHotkey(int keyCode, int scanCode) {
        return WcwtKeybindings.TOGGLE_CRAFTING_GRID_LOCK.matches(keyCode, scanCode);
    }

    private static boolean handleCraftingGridLockHotkey(int keyCode, int scanCode, Minecraft minecraft,
                                                        Screen activeScreen) {
        return matchesCraftingGridLockHotkey(keyCode, scanCode)
                && isCraftingGridLockHotkeyContext(minecraft, activeScreen)
                && toggleCraftingGridLock(minecraft);
    }

    private static boolean isCraftingGridLockHotkeyContext(Minecraft minecraft, Screen activeScreen) {
        if (!(minecraft.player != null
                && minecraft.player.containerMenu instanceof WirelessComprehensiveWorkTerminalMenu)) {
            return false;
        }
        if (activeScreen instanceof WirelessComprehensiveWorkTerminalScreen) {
            return true;
        }
        if (activeScreen == null) {
            return false;
        }
        String screenClassName = activeScreen.getClass().getName();
        return screenClassName.startsWith("mezz.jei.")
                || screenClassName.startsWith("mezz.jei.library.")
                || screenClassName.startsWith("dev.emi.emi.screen.");
    }

    private static boolean toggleCraftingGridLock(Minecraft minecraft) {
        if (!(minecraft.player != null
                && minecraft.player.containerMenu instanceof WirelessComprehensiveWorkTerminalMenu menu)) {
            return false;
        }
        var host = menu.getMenuHost();
        if (host == null) {
            return false;
        }
        host.toggleCraftingGridLock();
        refreshRecipeViewerIfPresent(minecraft);
        net.neoforged.neoforge.network.PacketDistributor
                .sendToServer(new CraftingLockPacket(host.isCraftingGridLocked()));
        return true;
    }

    private static void refreshRecipeViewerIfPresent(Minecraft minecraft) {
        Screen activeScreen = minecraft.screen;
        if (activeScreen == null) {
            return;
        }
        String screenClassName = activeScreen.getClass().getName();
        if (screenClassName.equals("mezz.jei.gui.recipes.RecipesGui")) {
            try {
                Method updateLayout = activeScreen.getClass().getDeclaredMethod("updateLayout");
                updateLayout.setAccessible(true);
                updateLayout.invoke(activeScreen);
            } catch (Throwable ignored) {
            }
            return;
        }
        if (screenClassName.equals("dev.emi.emi.screen.RecipeScreen")) {
            activeScreen.resize(minecraft, minecraft.getWindow().getGuiScaledWidth(),
                    minecraft.getWindow().getGuiScaledHeight());
        }
    }
}
