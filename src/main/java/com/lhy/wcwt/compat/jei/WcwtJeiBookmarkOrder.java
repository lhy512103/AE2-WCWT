package com.lhy.wcwt.compat.jei;

import appeng.api.stacks.GenericStack;
import com.lhy.wcwt.WcwtMod;
import com.lhy.wcwt.helpers.WcwtWirelessFeatures;
import com.lhy.wcwt.network.ModNetworking;
import com.lhy.wcwt.network.WcwtJeiBookmarkOrderPacket;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.common.input.IInternalKeyMappings;
import mezz.jei.gui.input.CombinedRecipeFocusSource;
import mezz.jei.gui.input.IClickableIngredientInternal;
import mezz.jei.gui.input.IUserInputHandler;
import mezz.jei.gui.input.UserInput;
import mezz.jei.gui.input.handlers.BookmarkInputHandler;
import mezz.jei.gui.input.handlers.SameElementInputHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.client.event.ScreenEvent;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Optional;

public final class WcwtJeiBookmarkOrder {
    private static Field focusSourceField;
    private static final String EAEP_JEI_RUNTIME_PROXY = "com.extendedae_plus.integration.jei.JeiRuntimeProxy";
    private static final boolean DEBUG = Boolean.getBoolean("wcwt.debug.jeiBookmark");

    private WcwtJeiBookmarkOrder() {
    }

    @SuppressWarnings("unchecked")
    public static void handleBookmarkMiddleClick(BookmarkInputHandler handler, Screen screen, UserInput input,
                                                 IInternalKeyMappings keyBindings,
                                                 CallbackInfoReturnable<Optional<IUserInputHandler>> cir) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null
                || minecraft.options == null
                || !input.is(minecraft.options.keyPickItem)
                || input.is(keyBindings.getBookmark())) {
            return;
        }
        if (!WcwtWirelessFeatures.hasAnyTerminal(minecraft.player)) {
            debug("JEI bookmark handler skipped: no WCWT terminal, screen={}, mouse={},{}",
                    screen == null ? null : screen.getClass().getName(), input.getMouseX(), input.getMouseY());
            return;
        }

        try {
            CombinedRecipeFocusSource focusSource = getFocusSource(handler);
            if (focusSource == null) {
                debug("JEI bookmark handler skipped: no focusSource field on {}", handler.getClass().getName());
                return;
            }
            Optional<IClickableIngredientInternal<?>> clickedOptional =
                    focusSource.getIngredientUnderMouse(input, keyBindings).findFirst();
            if (clickedOptional.isEmpty()) {
                return;
            }

            IClickableIngredientInternal<?> clicked = clickedOptional.get();
            ITypedIngredient<?> typedIngredient = clicked.getTypedIngredient();
            GenericStack stack = WcwtRecipeTransferHandler.toGenericStackForBookmark(
                    typedIngredient);
            if (stack == null || stack.what() == null) {
                return;
            }

            if (!input.isSimulate()) {
                debug("JEI bookmark handler sending WCWT craft packet stack={}", stack);
                ModNetworking.sendToServer(
                        new WcwtJeiBookmarkOrderPacket(stack, WcwtJeiBookmarkOrderPacket.Action.OPEN_CRAFT));
            }

            IUserInputHandler sameElement = new SameElementInputHandler(handler, clicked::isMouseOver);
            cir.setReturnValue(Optional.of(sameElement));
        } catch (RuntimeException | LinkageError ignored) {
        }
    }

    public static boolean handleEaepMouseButtonPre(ScreenEvent.MouseButtonPressed.Pre event) {
        int button = event.getButton();
        boolean eaepOpenCraftClick = button == 2;
        boolean eaepPullOrCraftClick = button == 0 && Screen.hasControlDown();
        if (!eaepOpenCraftClick && !eaepPullOrCraftClick) {
            return false;
        }

        Minecraft minecraft = Minecraft.getInstance();
        debug("EAEP JEI mouse pre entered: button={}, mouse={},{} player={}, screen={}",
                button, event.getMouseX(), event.getMouseY(), minecraft.player,
                minecraft.screen == null ? null : minecraft.screen.getClass().getName());
        if (minecraft.player == null) {
            debug("EAEP JEI mouse pre skipped: no client player");
            return false;
        }
        if (!WcwtWirelessFeatures.hasAnyTerminal(minecraft.player)) {
            debug("EAEP JEI mouse pre skipped: no WCWT terminal");
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

    @SuppressWarnings("unchecked")
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
            WcwtMod.LOGGER.info("WCWT JEI bookmark debug: " + message, args);
        }
    }

    private static CombinedRecipeFocusSource getFocusSource(BookmarkInputHandler handler) {
        try {
            Field field = focusSourceField;
            if (field == null) {
                field = findField(BookmarkInputHandler.class, "focusSource", CombinedRecipeFocusSource.class);
                if (field == null) {
                    return null;
                }
                focusSourceField = field;
            }
            Object value = field.get(handler);
            return value instanceof CombinedRecipeFocusSource focusSource ? focusSource : null;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static Field findField(Class<?> owner, String preferredName, Class<?> expectedType) {
        try {
            Field field = owner.getDeclaredField(preferredName);
            if (expectedType.isAssignableFrom(field.getType())) {
                field.setAccessible(true);
                return field;
            }
        } catch (NoSuchFieldException ignored) {
        }

        for (Field field : owner.getDeclaredFields()) {
            if (expectedType.isAssignableFrom(field.getType())) {
                field.setAccessible(true);
                return field;
            }
        }
        return null;
    }
}
