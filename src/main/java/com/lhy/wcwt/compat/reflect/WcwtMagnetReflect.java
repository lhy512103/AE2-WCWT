package com.lhy.wcwt.compat.reflect;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/**
 * AE2WTLib 磁力卡设置的反射访问。
 *
 * <p>{@code MAGNET_SETTINGS} 数据组件和 {@code MagnetMode} 枚举都只存在于 AE2WTLib 的内部包
 * （{@code de.mari_023.ae2wtlib.*}），不在 {@code api} 包里，所以拿不到编译期依赖。
 * 服务端补货逻辑、客户端设置界面、网络包处理三处都要读它，这里统一收口，避免三份重复代码。
 *
 * <p>所有方法在反射失效时返回 {@code empty} 或 {@code false}，绝不外抛。
 */
public final class WcwtMagnetReflect {
    @SuppressWarnings("unchecked")
    private static DataComponentType<Object> asObjectComponent(Object o) {
        return (DataComponentType<Object>) o;
    }

    private static final String MOD_ID = "ae2wtlib";
    private static final String ADDITIONAL_COMPONENTS_CLASS =
            "de.mari_023.ae2wtlib.AE2wtlibAdditionalComponents";
    private static final String MAGNET_MODE_CLASS =
            "de.mari_023.ae2wtlib.wct.magnet_card.MagnetMode";
    private static final String OFF = "OFF";

    private WcwtMagnetReflect() {
    }

    /** 磁力设置数据组件。拿不到时返回 empty，此时磁力相关开关整体不可用。 */
    @SuppressWarnings("rawtypes")
    public static Optional<DataComponentType<Object>> settingsComponent() {
        return WcwtReflect.readStaticField(MOD_ID, ADDITIONAL_COMPONENTS_CLASS, "MAGNET_SETTINGS")
                .map(WcwtMagnetReflect::asObjectComponent);
    }

    /** 读取终端当前磁力模式名，失败返回 {@code "OFF"}。 */
    public static String modeName(ItemStack terminal) {
        var component = settingsComponent().orElse(null);
        var fallback = WcwtReflect.enumConstant(MOD_ID, MAGNET_MODE_CLASS, OFF).orElse(null);
        if (component == null || fallback == null) {
            return OFF;
        }
        Object mode = terminal.getOrDefault(component, fallback);
        return mode instanceof Enum<?> enumValue ? enumValue.name() : OFF;
    }

    /** 写入磁力模式，失败返回 false。 */
    public static boolean setMode(ItemStack terminal, String modeName) {
        var component = settingsComponent().orElse(null);
        if (component == null) {
            return false;
        }
        return WcwtReflect.enumConstant(MOD_ID, MAGNET_MODE_CLASS, modeName)
                .map(mode -> {
                    terminal.set(component, mode);
                    return true;
                })
                .orElse(false);
    }

    /** 读取磁力模式上的布尔设置项（如 {@code pickupToME}），失败返回 false。 */
    public static boolean readSetting(ItemStack terminal, String methodName) {
        var component = settingsComponent().orElse(null);
        var fallback = WcwtReflect.enumConstant(MOD_ID, MAGNET_MODE_CLASS, OFF).orElse(null);
        if (component == null || fallback == null) {
            return false;
        }
        Object mode = terminal.getOrDefault(component, fallback);
        return WcwtReflect.findMethod(mode.getClass(), methodName)
                .flatMap(method -> WcwtReflect.invoke(mode, method))
                .filter(Boolean.class::isInstance)
                .map(Boolean.class::cast)
                .orElse(false);
    }

    /**
     * 刷写「磁力 / 拾取到 ME」两个开关。
     * 调用 {@code MagnetMode.set(boolean, boolean)} 得到新模式后写回组件。
     */
    public static boolean applySettings(ItemStack terminal, boolean magnet, boolean pickupToMe) {
        var component = settingsComponent().orElse(null);
        var fallback = WcwtReflect.enumConstant(MOD_ID, MAGNET_MODE_CLASS, OFF).orElse(null);
        if (component == null || fallback == null) {
            return false;
        }
        Object current = terminal.getOrDefault(component, fallback);
        var updated = WcwtReflect.findMethod(current.getClass(), "set", boolean.class, boolean.class)
                .flatMap(method -> WcwtReflect.invoke(current, method, magnet, pickupToMe))
                .orElse(null);
        if (updated == null) {
            return false;
        }
        terminal.set(component, updated);
        return true;
    }

    /** 磁力模式枚举是否可访问（仅用于诊断）。 */
    public static boolean isAvailable() {
        return settingsComponent().isPresent()
                && WcwtReflect.enumConstant(MOD_ID, MAGNET_MODE_CLASS, OFF).isPresent();
    }
}
