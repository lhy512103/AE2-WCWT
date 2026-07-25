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
 * ExtendedAE Plusの入力イベントとWCWTを接続する軽量な互換経路。
 *
 * <p>TMRVが提供しないJEI内部GUIクラスを参照せず、JEI公開APIだけを使う。
 * フルJEI専用処理は{@link WcwtJeiBookmarkOrder}へ隔離する。</p>
 */
public final class WcwtEaepBookmarkOrder {
    // ForgeのMouseButtonPressedはGLFWと同じ番号を渡す。0は左クリック。
    private static final int LEFT_MOUSE_BUTTON = 0;
    // ForgeのMouseButtonPressedはGLFWと同じ番号を渡す。2は中クリック。
    private static final int MIDDLE_MOUSE_BUTTON = 2;
    private static final String EAEP_JEI_RUNTIME_PROXY =
            "com.extendedae_plus.integration.jei.JeiRuntimeProxy";
    private static final boolean DEBUG = Boolean.getBoolean("wcwt.debug.jeiBookmark");

    private WcwtEaepBookmarkOrder() {
    }

    public static boolean handleMouseButtonPre(ScreenEvent.MouseButtonPressed.Pre event) {
        int button = event.getButton();
        boolean openCraftClick = button == MIDDLE_MOUSE_BUTTON;
        boolean pullOrCraftClick = button == LEFT_MOUSE_BUTTON && Screen.hasControlDown();
        // WCWTが扱う中クリックまたはCtrl+左クリック以外はEAEPへ渡す。
        if (!openCraftClick && !pullOrCraftClick) {
            return false;
        }

        Minecraft minecraft = Minecraft.getInstance();
        debug("EAEP JEI mouse pre entered: button={}, mouse={},{} player={}, screen={}",
                button, event.getMouseX(), event.getMouseY(), minecraft.player,
                minecraft.screen == null ? null : minecraft.screen.getClass().getName());
        // ワールド未参加中の画面では注文を送れない。
        if (minecraft.player == null) {
            debug("EAEP JEI mouse pre skipped: no client player");
            return false;
        }
        // WCWT端末を所持していない場合はEAEP本来の操作へ戻す。
        if (!WcwtWirelessFeatures.hasAnyTerminal(minecraft.player)) {
            debug("EAEP JEI mouse pre skipped: no WCWT terminal");
            return false;
        }

        GenericStack stack = findHoveredGenericStack(event.getMouseX(), event.getMouseY());
        // TMRV/EAEPから材料を取得またはAE2へ変換できない場合は入力を奪わない。
        if (stack == null || stack.what() == null) {
            debug("EAEP JEI mouse pre skipped: no hovered generic stack");
            return false;
        }

        WcwtJeiBookmarkOrderPacket.Action action = openCraftClick
                ? WcwtJeiBookmarkOrderPacket.Action.OPEN_CRAFT
                : WcwtJeiBookmarkOrderPacket.Action.PULL_OR_CRAFT;
        debug("EAEP JEI mouse pre sending WCWT packet action={} and canceling EAEP stack={}", action, stack);
        ModNetworking.sendToServer(new WcwtJeiBookmarkOrderPacket(stack, action));
        event.setCanceled(true);
        return true;
    }

    private static GenericStack findHoveredGenericStack(double mouseX, double mouseY) {
        try {
            Class<?> proxyClass = Class.forName(EAEP_JEI_RUNTIME_PROXY);
            Method method = proxyClass.getMethod("getIngredientUnderMouse", double.class, double.class);
            Object result = method.invoke(null, mouseX, mouseY);
            // EAEPが公開JEI材料を返した場合だけ、WCWTの注文キーへ変換する。
            if (result instanceof Optional<?> optional
                    && optional.orElse(null) instanceof ITypedIngredient<?> typedIngredient) {
                return WcwtJeiIngredientConverter.toGenericStack(typedIngredient);
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            // EAEP/TMRVの版差がある場合はクラッシュさせず、元の入力処理へ戻す。
        }
        return null;
    }

    private static void debug(String message, Object... args) {
        // デバッグフラグが無効な通常起動ではクリックごとのログを出さない。
        if (DEBUG) {
            WcwtMod.LOGGER.info("WCWT EAEP bookmark debug: " + message, args);
        }
    }
}
