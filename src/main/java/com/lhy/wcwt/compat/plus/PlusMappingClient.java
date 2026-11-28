package com.lhy.wcwt.compat.plus;

import appeng.client.gui.style.ScreenStyle;
import com.extendedae_plus.client.screen.RecipeTypeMappingScreen;
import com.extendedae_plus.client.widget.ResizableAETextField;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
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
        return new MappingScreen(parent);
    }

    public static void requestReturnLastPattern() {
        PlusEncodingUpload.requestReturnLastPattern();
    }

    private static final class MappingScreen extends RecipeTypeMappingScreen {
        private boolean backgroundExtracted;

        private MappingScreen(Screen parent) {
            super(parent);
        }

        @Override
        public boolean isInGameUi() {
            return true;
        }

        @Override
        public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            backgroundExtracted = false;
            super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        }

        @Override
        public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            // ExtendedAE Plus 1.6.3 extracts the background both directly and through Screen.
            if (!backgroundExtracted) {
                backgroundExtracted = true;
                super.extractBackground(graphics, mouseX, mouseY, partialTick);
            }
        }
    }
}
