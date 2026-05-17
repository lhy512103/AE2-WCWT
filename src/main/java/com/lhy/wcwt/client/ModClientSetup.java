package com.lhy.wcwt.client;

import com.lhy.wcwt.WcwtMod;
import com.lhy.wcwt.init.ModMenus;
import com.lhy.wcwt.network.OpenToolkitHotkeyPacket;
import appeng.init.client.InitScreens;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.fml.ModList;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Method;

@EventBusSubscriber(modid = WcwtMod.MOD_ID, value = Dist.CLIENT)
public class ModClientSetup {
    @SubscribeEvent
    public static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.WCWT_MENU.get(), WirelessComprehensiveWorkTerminalScreen::new);
        InitScreens.register(event, ModMenus.WCWT_MAGNET_MENU.get(), WcwtMagnetScreen::new,
                "/screens/wtlib/magnet.json");
        InitScreens.register(event, ModMenus.WCWT_TRASH_MENU.get(), WcwtTrashScreen::new,
                "/screens/wtlib/trash.json");
    }

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(WcwtKeybindings.OPEN_ADVANCED_CODING);
        event.register(WcwtKeybindings.OPEN_COSMETIC_ARMOR);
        event.register(WcwtKeybindings.OPEN_CURIOS);
        event.register(WcwtKeybindings.OPEN_TOOL_SLOTS_BOX);
        event.register(WcwtKeybindings.OPEN_TOOLKIT);
        event.register(WcwtKeybindings.OPEN_RESONATING_LIGHTNING_PATTERN_CODING);
    }

    @SubscribeEvent
    public static void onScreenKeyPressedPre(ScreenEvent.KeyPressed.Pre event) {
        if (!(Minecraft.getInstance().screen instanceof WirelessComprehensiveWorkTerminalScreen screen)) {
            return;
        }
        if (screen.handleExtendedUiHotkey(event.getKeyCode(), event.getScanCode())) {
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
        if (minecraft.player == null) {
            return;
        }
        if (minecraft.screen instanceof WirelessComprehensiveWorkTerminalScreen) {
            while (WcwtKeybindings.OPEN_TOOLKIT.consumeClick()) {
            }
            return;
        }
        while (WcwtKeybindings.OPEN_TOOLKIT.consumeClick()) {
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
}
