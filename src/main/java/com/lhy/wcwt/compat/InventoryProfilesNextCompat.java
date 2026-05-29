package com.lhy.wcwt.compat;

import com.lhy.wcwt.WcwtMod;
import com.lhy.wcwt.client.WirelessComprehensiveWorkTerminalScreen;
import com.lhy.wcwt.menu.WirelessComprehensiveWorkTerminalMenu;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.List;
import java.util.Queue;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.IdentityHashMap;
import java.util.Set;

/**
 * 为 Inventory Profiles Next 自动写入 WCWT 专用 hints。
 * 不直接依赖 IPN API，避免把 IPN 变成编译期强依赖。
 */
public final class InventoryProfilesNextCompat {
    private static final String IPN_ID_SORT_MOVE_MATCHING = "inventoryprofiles.injected.ui.element.sort_move_all_button";
    private static final String IPN_ID_SORT_CONTINUOUS_CRAFTING = "inventoryprofiles.injected.ui.element.sort_crafting_checkbox";
    /** 防止病态图上的过多反射 */
    private static final int IPN_WIDGET_TRAVERSE_BURST_LIMIT = 2048;

    private static final String IPN_MOD_ID = "inventoryprofilesnext";
    private static final String HINT_FILE_NAME = "wcwt-auto.json";
    private static final int PLAYER_SIDE_BUTTON_X_OFFSET = 180;
    private static final int PLAYER_SIDE_BUTTON_BOTTOM_OFFSET = 1;
    private static final Set<String> IGNORED_SLOT_TYPES = Set.of(
            "appeng.menu.slot.AppEngSlot",
            "appeng.menu.slot.FakeSlot",
            "appeng.menu.slot.OptionalRestrictedInputSlot",
            "appeng.menu.slot.PatternTermSlot",
            "appeng.menu.slot.RestrictedInputSlot",
            "com.lhy.wcwt.menu.WirelessComprehensiveWorkTerminalMenu$ToolkitSlot",
            "com.lhy.wcwt.menu.WirelessComprehensiveWorkTerminalMenu$WcwtCurioSlot",
            "net.neoforged.neoforge.items.SlotItemHandler");
    /**
     * integrationHints 下单个 JSON 的顶层必须为「FQCN → HintClassData」，不能多包一层 mod id（否则会只生成名为 wcwt 的无效条目）。
     */
    private static final String HINTS_JSON = """
            {
              "com.lhy.wcwt.client.WirelessComprehensiveWorkTerminalScreen": {
                "force": true,
                "playerSideOnly": true,
                "slotIgnoreInventoryTypes": [
                  "appeng.menu.slot.AppEngSlot",
                  "appeng.menu.slot.FakeSlot",
                  "appeng.menu.slot.OptionalRestrictedInputSlot",
                  "appeng.menu.slot.PatternTermSlot",
                  "appeng.menu.slot.RestrictedInputSlot",
                  "com.lhy.wcwt.menu.WirelessComprehensiveWorkTerminalMenu$ToolkitSlot",
                  "com.lhy.wcwt.menu.WirelessComprehensiveWorkTerminalMenu$WcwtCurioSlot",
                  "net.neoforged.neoforge.items.SlotItemHandler"
                ],
                "buttonHints": {
                  "SORT": {
                    "horizontalOffset": 148,
                    "bottom": -1
                  },
                  "SORT_COLUMNS": {
                    "horizontalOffset": 148,
                    "bottom": -1
                  },
                  "SORT_ROWS": {
                    "horizontalOffset": 148,
                    "bottom": -1
                  },
                  "SORT_PLAYER": {
                    "horizontalOffset": 148,
                    "bottom": -1
                  },
                  "SORT_COLUMNS_PLAYER": {
                    "horizontalOffset": 148,
                    "bottom": -1
                  },
                  "SORT_ROWS_PLAYER": {
                    "horizontalOffset": 148,
                    "bottom": -1
                  },
                  "MOVE_TO_CONTAINER": {
                    "hide": true
                  },
                  "MOVE_TO_PLAYER": {
                    "hide": true
                  },
                  "CONTINUOUS_CRAFTING": {
                    "hide": true
                  }
                }
              },
              "com.lhy.wcwt.menu.WirelessComprehensiveWorkTerminalMenu": {
                "force": true,
                "playerSideOnly": true,
                "slotIgnoreInventoryTypes": [
                  "appeng.menu.slot.AppEngSlot",
                  "appeng.menu.slot.FakeSlot",
                  "appeng.menu.slot.OptionalRestrictedInputSlot",
                  "appeng.menu.slot.PatternTermSlot",
                  "appeng.menu.slot.RestrictedInputSlot",
                  "com.lhy.wcwt.menu.WirelessComprehensiveWorkTerminalMenu$ToolkitSlot",
                  "com.lhy.wcwt.menu.WirelessComprehensiveWorkTerminalMenu$WcwtCurioSlot",
                  "net.neoforged.neoforge.items.SlotItemHandler"
                ]
              }
            }
            """;

    private InventoryProfilesNextCompat() {
    }

    public static void ensureHintsInstalled() {
        if (!ModList.get().isLoaded(IPN_MOD_ID)) {
            return;
        }

        try {
            Path hintsDir = FMLPaths.CONFIGDIR.get()
                    .resolve(IPN_MOD_ID)
                    .resolve("integrationHints");
            Files.createDirectories(hintsDir);
            Path hintFile = hintsDir.resolve(HINT_FILE_NAME);

            String existing = Files.exists(hintFile)
                    ? Files.readString(hintFile, StandardCharsets.UTF_8)
                    : null;
            if (HINTS_JSON.equals(existing)) {
                return;
            }

            Files.writeString(hintFile, HINTS_JSON, StandardCharsets.UTF_8);
            WcwtMod.LOGGER.info("Installed WCWT compatibility hints for Inventory Profiles Next at {}", hintFile);
        } catch (IOException e) {
            WcwtMod.LOGGER.warn("Failed to install Inventory Profiles Next compatibility hints for WCWT", e);
        }

        installRuntimeHints();
    }

    public static void installRuntimeHints() {
        if (!ModList.get().isLoaded(IPN_MOD_ID)) {
            return;
        }

        try {
            Class<?> hintsManagerClass = Class.forName("org.anti_ad.mc.ipnext.integration.HintsManagerNG");
            Object hintsManager = hintsManagerClass.getField("INSTANCE").get(null);
            Method getHints = hintsManagerClass.getMethod("getHints", Class.class);

            applyHints(getHints.invoke(hintsManager, WirelessComprehensiveWorkTerminalScreen.class), true);
            applyHints(getHints.invoke(hintsManager, WirelessComprehensiveWorkTerminalMenu.class), false);
            hideInjectedButtons();
            WcwtMod.LOGGER.debug(
                    "Applied WCWT runtime hints for Inventory Profiles Next (sort offset={}, sort bottom={}).",
                    PLAYER_SIDE_BUTTON_X_OFFSET, PLAYER_SIDE_BUTTON_BOTTOM_OFFSET);
        } catch (Throwable e) {
            WcwtMod.LOGGER.debug("Failed to inject WCWT runtime hints into Inventory Profiles Next", e);
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static void applyHints(Object hintData, boolean configureButtons) throws ReflectiveOperationException {
        setBooleanField(hintData, "force", true);
        setBooleanField(hintData, "playerSideOnly", true);

        Field slotIgnoreField = hintData.getClass().getDeclaredField("slotIgnoreInventoryTypes");
        slotIgnoreField.setAccessible(true);
        Object slotIgnoreValue = slotIgnoreField.get(hintData);
        if (slotIgnoreValue instanceof Set slotIgnoreTypes) {
            slotIgnoreTypes.addAll(IGNORED_SLOT_TYPES);
        }

        if (!configureButtons) {
            return;
        }

        Class<?> buttonEnumClass = Class.forName("org.anti_ad.mc.ipn.api.IPNButton");
        Method hintFor = hintData.getClass().getMethod("hintFor", buttonEnumClass);

        configureButtonHint(hintFor, hintData, buttonEnumClass, "SORT",
                PLAYER_SIDE_BUTTON_X_OFFSET, PLAYER_SIDE_BUTTON_BOTTOM_OFFSET, false);
        configureButtonHint(hintFor, hintData, buttonEnumClass, "SORT_COLUMNS",
                PLAYER_SIDE_BUTTON_X_OFFSET, PLAYER_SIDE_BUTTON_BOTTOM_OFFSET, false);
        configureButtonHint(hintFor, hintData, buttonEnumClass, "SORT_ROWS",
                PLAYER_SIDE_BUTTON_X_OFFSET, PLAYER_SIDE_BUTTON_BOTTOM_OFFSET, false);
        configureButtonHint(hintFor, hintData, buttonEnumClass, "SORT_PLAYER",
                PLAYER_SIDE_BUTTON_X_OFFSET, PLAYER_SIDE_BUTTON_BOTTOM_OFFSET, false);
        configureButtonHint(hintFor, hintData, buttonEnumClass, "SORT_COLUMNS_PLAYER",
                PLAYER_SIDE_BUTTON_X_OFFSET, PLAYER_SIDE_BUTTON_BOTTOM_OFFSET, false);
        configureButtonHint(hintFor, hintData, buttonEnumClass, "SORT_ROWS_PLAYER",
                PLAYER_SIDE_BUTTON_X_OFFSET, PLAYER_SIDE_BUTTON_BOTTOM_OFFSET, false);
        configureButtonHint(hintFor, hintData, buttonEnumClass, "MOVE_TO_CONTAINER", 0, 0, true);
        configureButtonHint(hintFor, hintData, buttonEnumClass, "MOVE_TO_PLAYER", 0, 0, true);
        configureButtonHint(hintFor, hintData, buttonEnumClass, "CONTINUOUS_CRAFTING", 0, 0, true);
    }

    private static void configureButtonHint(Method hintFor, Object hintData, Class<?> buttonEnumClass,
                                            String buttonName, int horizontalOffset, int bottom, boolean hide)
            throws ReflectiveOperationException {
        Object button = Enum.valueOf((Class<? extends Enum>) buttonEnumClass.asSubclass(Enum.class), buttonName);
        Object buttonHint = hintFor.invoke(hintData, button);
        setIntField(buttonHint, "horizontalOffset", horizontalOffset);
        setIntField(buttonHint, "bottom", bottom);
        setBooleanField(buttonHint, "hide", hide);
    }

    private static void setBooleanField(Object target, String fieldName, boolean value) throws ReflectiveOperationException {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.setBoolean(target, value);
    }

    private static void setIntField(Object target, String fieldName, int value) throws ReflectiveOperationException {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.setInt(target, value);
    }

    /**
     * 隐藏 IPN 注入的「移动相符/移动到容器」（两颗控件共用同一 id）与「连续合成」勾选框；
     * 它们挂在 SortingButtonCollectionWidget 子树深处，不能只扫一层 {@code children}。
     */
    private static void hideInjectedButtons() {
        try {
            Class<?> handlerClass = Class.forName("org.anti_ad.mc.ipnext.gui.inject.ContainerScreenEventHandler");
            Field currentWidgetsField = handlerClass.getField("currentWidgets");
            Object widgets = currentWidgetsField.get(null);
            if (!(widgets instanceof List<?> widgetList)) {
                return;
            }

            Queue<Object> queue = new ArrayDeque<>();
            IdentityHashMap<Object, Boolean> visited = new IdentityHashMap<>();
            for (Object root : widgetList) {
                if (root != null && visited.put(root, Boolean.TRUE) == null) {
                    queue.add(root);
                }
            }

            int budget = IPN_WIDGET_TRAVERSE_BURST_LIMIT;
            while (!queue.isEmpty() && budget-- > 0) {
                Object current = queue.remove();
                if (current != null && shouldHideIpnInjectedLeaf(current)) {
                    setVisible(current, false);
                }
                enqueuePotentialIpnChildWidgets(queue, current, visited);
            }
        } catch (Throwable e) {
            WcwtMod.LOGGER.debug("Failed to hide WCWT-specific Inventory Profiles Next buttons", e);
        }
    }

    private static boolean shouldHideIpnInjectedLeaf(Object widget) {
        String id = readWidgetId(widget);
        return IPN_ID_SORT_MOVE_MATCHING.equals(id) || IPN_ID_SORT_CONTINUOUS_CRAFTING.equals(id);
    }

    private static void enqueuePotentialIpnChildWidgets(Queue<Object> sink, Object node,
                                                        IdentityHashMap<Object, Boolean> visited) {
        if (node == null) {
            return;
        }

        if (!enqueueIterableFromAccessibleZeroArgMethods(sink, node, visited,
                "getChildren", "getChildWidgets")) {
            enqueueIterableFromDeclaredFieldsNamed(sink, node, visited,
                    "children", "_children", "childWidgets", "widgets");
        }

        enqueueIterableFromIterableFieldsScan(sink, node, visited);
    }

    /** 兜底：逐级扫描声明在类型上的 Iterable 字段（libIPN 小部件私有命名不固定时仍能下潜）。 */
    private static void enqueueIterableFromIterableFieldsScan(Queue<Object> sink, Object node,
                                                              IdentityHashMap<Object, Boolean> visited) {
        for (Class<?> c = node.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field field : c.getDeclaredFields()) {
                if (!Iterable.class.isAssignableFrom(field.getType())) {
                    continue;
                }
                try {
                    field.setAccessible(true);
                    Object raw = field.get(node);
                    if (!(raw instanceof Iterable<?> iterable)) {
                        continue;
                    }
                    for (Object child : iterable) {
                        enqueueIfFresh(sink, child, visited);
                    }
                } catch (Throwable ignored) {
                }
            }
        }
    }

    /** @return {@code true} 若任一方法成功返回了一串子节点 */
    private static boolean enqueueIterableFromDeclaredFieldsNamed(Queue<Object> sink, Object node,
                                                                  IdentityHashMap<Object, Boolean> visited,
                                                                  String... fieldNames) {
        for (String name : fieldNames) {
            Field field = findField(node.getClass(), name);
            if (field == null || !Iterable.class.isAssignableFrom(field.getType())) {
                continue;
            }
            try {
                field.setAccessible(true);
                Object raw = field.get(node);
                if (!(raw instanceof Iterable<?> iterable)) {
                    continue;
                }
                boolean anyChild = false;
                for (Object child : iterable) {
                    enqueueIfFresh(sink, child, visited);
                    anyChild = true;
                }
                if (anyChild) {
                    return true;
                }
            } catch (Throwable ignored) {
            }
        }
        return false;
    }

    private static boolean enqueueIterableFromAccessibleZeroArgMethods(Queue<Object> sink, Object node,
                                                                         IdentityHashMap<Object, Boolean> visited,
                                                                         String... methodNames) {
        for (String name : methodNames) {
            Method method = findAccessibleZeroArgMethod(node.getClass(), name);
            if (method == null) {
                continue;
            }
            try {
                Object raw = method.invoke(node);
                if (!(raw instanceof Iterable<?> iterable)) {
                    continue;
                }
                boolean anyChild = false;
                for (Object child : iterable) {
                    enqueueIfFresh(sink, child, visited);
                    anyChild = true;
                }
                if (anyChild) {
                    return true;
                }
            } catch (Throwable ignored) {
            }
        }
        return false;
    }

    private static void enqueueIfFresh(Queue<Object> sink, Object child, IdentityHashMap<Object, Boolean> visited) {
        if (child == null || visited.put(child, Boolean.TRUE) != null) {
            return;
        }
        sink.add(child);
    }

    private static Method findAccessibleZeroArgMethod(Class<?> clazz, String name) {
        for (Class<?> c = clazz; c != null; c = c.getSuperclass()) {
            Method method;
            try {
                method = c.getDeclaredMethod(name);
            } catch (NoSuchMethodException e) {
                continue;
            }
            if (method.getParameterCount() != 0) {
                continue;
            }
            method.setAccessible(true);
            return method;
        }
        return null;
    }

    private static String readWidgetId(Object widget) {
        try {
            Method getId = widget.getClass().getMethod("getId");
            Object value = getId.invoke(widget);
            return value instanceof String str ? str : null;
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static void setVisible(Object widget, boolean visible) {
        try {
            Method setVisible = widget.getClass().getMethod("setVisible", boolean.class);
            setVisible.invoke(widget, visible);
            return;
        } catch (Throwable ignored) {
        }

        try {
            Field visibleField = findField(widget.getClass(), "visible");
            if (visibleField != null) {
                visibleField.setAccessible(true);
                visibleField.setBoolean(widget, visible);
            }
        } catch (Throwable ignored) {
        }
    }

    private static Field findField(Class<?> type, String name) {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }
}
