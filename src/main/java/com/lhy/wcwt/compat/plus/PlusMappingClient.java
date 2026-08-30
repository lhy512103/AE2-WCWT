package com.lhy.wcwt.compat.plus;

import appeng.client.gui.style.ScreenStyle;
import com.extendedae_plus.client.screen.RecipeTypeMappingScreen;
import com.extendedae_plus.client.widget.ResizableAETextField;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class PlusMappingClient {
    private PlusMappingClient() {
    }

    public static EditBox createTextField(ScreenStyle style, Font font, int x, int y, int width, int height,
                                          Component placeholder) {
        ResizableAETextField field = new ResizableAETextField(style, font, x, y, width, height);
        field.setBordered(false);
        field.setMaxLength(256);
        field.setPlaceholder(placeholder);
        return field;
    }

    public static Screen openMappingScreen(Screen parent) {
        return new RecipeTypeMappingScreen(parent);
    }
}
