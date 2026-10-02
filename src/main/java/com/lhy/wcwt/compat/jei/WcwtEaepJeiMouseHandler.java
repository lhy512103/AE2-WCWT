package com.lhy.wcwt.compat.jei;

import appeng.api.stacks.GenericStack;
import com.lhy.wcwt.WcwtMod;
import com.lhy.wcwt.helpers.WcwtWirelessFeatures;
import com.lhy.wcwt.network.ModNetworking;
import com.lhy.wcwt.network.WcwtJeiBookmarkOrderPacket;
import mezz.jei.api.ingredients.ITypedIngredient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.client.event.ScreenEvent;

import java.lang.reflect.Method;
import java.util.Optional;

/**
 * EAEP mouse hook. Only uses the public JEI API, so it also works with JEI API providers such as
 * TooManyRecipeViewers that do not ship JEI's internal GUI classes.
 */
public final class WcwtEaepJeiMouseHandler {
    private static final String EAEP_JEI_RUNTIME_PROXY = "com.extendedae_plus.integration.jei.JeiRuntimeProxy";
    private static final boolean DEBUG = Boolean.getBoolean("wcwt.debug.jeiBookmark");

    private WcwtEaepJeiMouseHandler() {
    }

    public static boolean handleEaepMouseButtonPre(ScreenEvent.MouseButtonPressed.Pre event) {
        int button = event.getButton();
        boolean eaepOpenCraftClick = button == 2;
        boolean eaepPullOrCraftClick = button == 0 && Screen.hasControlDown();
        if (!eaepOpenCraftClick && !eaepPullOrCraftClick) {
            return false;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !WcwtWirelessFeatures.hasAnyTerminal(minecraft.player)) {
            return false;
        }

        GenericStack stack = findEaepHoveredGenericStack(event.getMouseX(), event.getMouseY());
        if (stack == null || stack.what() == null) {
            debug("EAEP JEI mouse pre skipped: no hovered generic stack");
            return false;
        }

        WcwtJeiBookmarkOrderPacket.Action action = eaepOpenCraftClick
                ? WcwtJeiBookmarkOrderPacket.Action.OPEN_CRAFT
                : WcwtJeiBookmarkOrderPacket.Action.PULL_OR_CRAFT;
        debug("EAEP JEI mouse pre sending WCWT packet action={} and canceling EAEP stack={}", action, stack);
        ModNetworking.sendToServer(new WcwtJeiBookmarkOrderPacket(stack, action));
        event.setCanceled(true);
        return true;
    }

    private static GenericStack findEaepHoveredGenericStack(double mouseX, double mouseY) {
        try {
            Class<?> proxyClass = Class.forName(EAEP_JEI_RUNTIME_PROXY);
            Method method = proxyClass.getMethod("getIngredientUnderMouse", double.class, double.class);
            Object result = method.invoke(null, mouseX, mouseY);
            if (result instanceof Optional<?> optional
                    && optional.orElse(null) instanceof ITypedIngredient<?> typedIngredient) {
                return WcwtRecipeTransferHandler.toGenericStackForBookmark(typedIngredient);
            }
        } catch (ReflectiveOperationException | LinkageError ignored) {
        }
        return null;
    }

    private static void debug(String message, Object... args) {
        if (DEBUG) {
            WcwtMod.LOGGER.info("WCWT EAEP JEI debug: " + message, args);
        }
    }
}
