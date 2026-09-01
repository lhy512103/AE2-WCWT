package com.lhy.wcwt.client;

import com.lhy.wcwt.WcwtMod;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

/**
 * WCWT 自定义按键。
 */
public final class WcwtKeybindings {
    public static final String CATEGORY = "key.wcwt.category";

    public static final KeyMapping OPEN_ADVANCED_CODING = create("open_advanced_coding");
    public static final KeyMapping OPEN_COSMETIC_ARMOR = create("open_cosmetic_armor");
    public static final KeyMapping OPEN_CURIOS = create("open_curios");
    public static final KeyMapping OPEN_TOOL_SLOTS_BOX = create("open_tool_slots_box");
    public static final KeyMapping OPEN_INDEPENDENT_TERMINAL = create("open_terminal", KeyConflictContext.IN_GAME);
    public static final KeyMapping OPEN_TOOLKIT = create("open_toolkit", KeyConflictContext.IN_GAME);
    public static final KeyMapping OPEN_RESONATING_LIGHTNING_PATTERN_CODING =
            create("open_resonating_lightning_pattern_coding");
    public static final KeyMapping TOGGLE_FAVORITE_ITEM = create("toggle_favorite_item", KeyConflictContext.IN_GAME);
    public static final KeyMapping TOOLKIT_BAR_LEFT = create("toolkit_bar_left", GLFW.GLFW_KEY_LEFT);
    public static final KeyMapping TOOLKIT_BAR_RIGHT = create("toolkit_bar_right", GLFW.GLFW_KEY_RIGHT);

    private WcwtKeybindings() {
    }

    private static KeyMapping create(String name) {
        return create(name, KeyConflictContext.GUI);
    }

    private static KeyMapping create(String name, KeyConflictContext conflictContext) {
        return create(name, conflictContext, InputConstants.UNKNOWN.getValue());
    }

    private static KeyMapping create(String name, int defaultKey) {
        return create(name, KeyConflictContext.IN_GAME, defaultKey);
    }

    private static KeyMapping create(String name, KeyConflictContext conflictContext, int defaultKey) {
        return new KeyMapping(
                "key." + WcwtMod.MOD_ID + "." + name,
                conflictContext,
                InputConstants.Type.KEYSYM,
                defaultKey,
                CATEGORY);
    }
}
