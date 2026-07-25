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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Field;
import java.util.Optional;

/**
 * フルJEIの内部GUIクラスを使うブックマーク操作。
 *
 * <p>TMRVはJEI公開APIだけを提供するため、このクラスはJEI内部GUIクラスが
 * 実在するときだけMixinから読み込まれる。</p>
 */
public final class WcwtJeiBookmarkOrder {
    private static final boolean DEBUG = Boolean.getBoolean("wcwt.debug.jeiBookmark");
    private static Field focusSourceField;

    private WcwtJeiBookmarkOrder() {
    }

    @SuppressWarnings("unchecked")
    public static void handleBookmarkMiddleClick(BookmarkInputHandler handler, Screen screen, UserInput input,
                                                 IInternalKeyMappings keyBindings,
                                                 CallbackInfoReturnable<Optional<IUserInputHandler>> cir) {
        Minecraft minecraft = Minecraft.getInstance();
        // ピック操作以外、またはJEIのブックマーク操作と競合する入力は処理しない。
        if (minecraft.player == null
                || minecraft.options == null
                || !input.is(minecraft.options.keyPickItem)
                || input.is(keyBindings.getBookmark())) {
            return;
        }
        // WCWT端末を所持していない場合はJEI本来の操作へ戻す。
        if (!WcwtWirelessFeatures.hasAnyTerminal(minecraft.player)) {
            debug("JEI bookmark handler skipped: no WCWT terminal, screen={}, mouse={},{}",
                    screen == null ? null : screen.getClass().getName(), input.getMouseX(), input.getMouseY());
            return;
        }

        try {
            CombinedRecipeFocusSource focusSource = getFocusSource(handler);
            // JEI内部フィールドを取得できない版では入力を奪わない。
            if (focusSource == null) {
                debug("JEI bookmark handler skipped: no focusSource field on {}", handler.getClass().getName());
                return;
            }
            Optional<IClickableIngredientInternal<?>> clickedOptional =
                    focusSource.getIngredientUnderMouse(input, keyBindings).findFirst();
            // マウス下に材料がない場合はJEI本来の入力処理へ戻す。
            if (clickedOptional.isEmpty()) {
                return;
            }

            IClickableIngredientInternal<?> clicked = clickedOptional.get();
            ITypedIngredient<?> typedIngredient = clicked.getTypedIngredient();
            GenericStack stack = WcwtJeiIngredientConverter.toGenericStack(typedIngredient);
            // AE2へ変換できない材料は注文パケットを送らない。
            if (stack == null || stack.what() == null) {
                return;
            }

            // シミュレーション呼び出しでは入力消費とパケット送信を行わない。
            if (!input.isSimulate()) {
                debug("JEI bookmark handler sending WCWT craft packet stack={}", stack);
                ModNetworking.sendToServer(
                        new WcwtJeiBookmarkOrderPacket(stack, WcwtJeiBookmarkOrderPacket.Action.OPEN_CRAFT));
            }

            IUserInputHandler sameElement = new SameElementInputHandler(handler, clicked::isMouseOver);
            cir.setReturnValue(Optional.of(sameElement));
        } catch (RuntimeException | LinkageError ignored) {
            // JEI内部実装が想定外の版では、クリックを壊さずJEI本来の処理へ戻す。
        }
    }

    static void debug(String message, Object... args) {
        // デバッグフラグが無効な通常起動ではクリックごとのログを出さない。
        if (DEBUG) {
            WcwtMod.LOGGER.info("WCWT JEI bookmark debug: " + message, args);
        }
    }

    private static CombinedRecipeFocusSource getFocusSource(BookmarkInputHandler handler) {
        try {
            Field field = focusSourceField;
            // 初回だけJEI内部フィールドを探索し、以降のクリックでは再利用する。
            if (field == null) {
                field = findField(BookmarkInputHandler.class, "focusSource", CombinedRecipeFocusSource.class);
                // 対応フィールドがないJEI版ではフルJEI連携を無効として扱う。
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
            // 既知のフィールド名と型が一致する場合は最短経路で採用する。
            if (expectedType.isAssignableFrom(field.getType())) {
                field.setAccessible(true);
                return field;
            }
        } catch (NoSuchFieldException ignored) {
            // フィールド名が変わったJEI版では、下の型検索へフォールバックする。
        }

        // 難読化や名称変更に備え、同じ型のフィールドだけを候補として探索する。
        for (Field field : owner.getDeclaredFields()) {
            if (expectedType.isAssignableFrom(field.getType())) {
                field.setAccessible(true);
                return field;
            }
        }
        return null;
    }
}
