package com.lhy.wcwt.client.gui.widgets;

import appeng.client.gui.style.PaletteColor;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.AETextField;
import com.lhy.wcwt.client.gui.WcwtTextRendering;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/** AE2 text field with WCWT's language-aware placeholder rendering. */
public final class WcwtScaledPlaceholderTextField extends AETextField {
    private final int placeholderColor;
    private @Nullable Component scaledPlaceholder;

    public WcwtScaledPlaceholderTextField(ScreenStyle style, Font font, int x, int y, int width, int height) {
        super(style, font, x, y, width, height);
        this.placeholderColor = style.getColor(PaletteColor.TEXTFIELD_PLACEHOLDER).toARGB();
    }

    @Override
    public void setPlaceholder(Component placeholder) {
        this.scaledPlaceholder = placeholder;
    }

    @Override
    public Component getPlaceholder() {
        return scaledPlaceholder;
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderWidget(graphics, mouseX, mouseY, partialTick);
        if (scaledPlaceholder != null && !isFocused() && getValue().isEmpty()) {
            WcwtTextRendering.drawString(graphics, net.minecraft.client.Minecraft.getInstance().font,
                    scaledPlaceholder, getX(), getY(), placeholderColor, false);
        }
    }
}
