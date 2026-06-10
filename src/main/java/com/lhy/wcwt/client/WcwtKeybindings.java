package com.lhy.wcwt.client;

import com.lhy.wcwt.WcwtMod;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

/**
 * WCWT 自定义按键。
 */
public final class WcwtKeybindings {
    public static final String CATEGORY = "key.wcwt.category";

    public static final KeyMapping OPEN_TERMINAL = create("open_terminal", KeyConflictContext.IN_GAME);
    public static final KeyMapping OPEN_ADVANCED_CODING = create("open_advanced_coding");
    public static final KeyMapping OPEN_COSMETIC_ARMOR = create("open_cosmetic_armor");
    public static final KeyMapping OPEN_CURIOS = create("open_curios");
    public static final KeyMapping OPEN_TOOL_SLOTS_BOX = create("open_tool_slots_box");
    public static final KeyMapping OPEN_TOOLKIT = create("open_toolkit", KeyConflictContext.IN_GAME);
    public static final KeyMapping OPEN_RESONATING_LIGHTNING_PATTERN_CODING =
            create("open_resonating_lightning_pattern_coding");
    public static final KeyMapping TOGGLE_FAVORITE_ITEM = create("toggle_favorite_item", KeyConflictContext.IN_GAME);
    public static final KeyMapping TOGGLE_CRAFTING_LOCK = create("toggle_crafting_lock", KeyConflictContext.GUI);
    public static final KeyMapping FILL_RECIPE_VIEWER_SEARCH =
            create("fill_recipe_viewer_search", KeyConflictContext.GUI, GLFW.GLFW_KEY_F);

    private WcwtKeybindings() {
    }

    private static KeyMapping create(String name) {
        return create(name, KeyConflictContext.GUI);
    }

    private static KeyMapping create(String name, KeyConflictContext conflictContext) {
        return create(name, conflictContext, InputConstants.UNKNOWN.getValue());
    }

    private static KeyMapping create(String name, KeyConflictContext conflictContext, int keyCode) {
        return new KeyMapping(
                "key." + WcwtMod.MOD_ID + "." + name,
                conflictContext,
                InputConstants.Type.KEYSYM,
                keyCode,
                CATEGORY);
    }
}
