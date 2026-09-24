package com.lhy.wcwt.compat.emi;

import java.util.ArrayList;
import java.util.List;

import appeng.client.gui.Icon;
import appeng.core.localization.ItemModText;
import appeng.parts.encoding.EncodingMode;
import com.lhy.wcwt.client.WirelessComprehensiveWorkTerminalScreen;
import com.lhy.wcwt.compat.Ae2HelpersCompat;
import com.lhy.wcwt.compat.WcwtManualWorkspaceRecipeSwitch;
import com.lhy.wcwt.compat.WcwtPullItemsSupport;
import com.lhy.wcwt.menu.WirelessComprehensiveWorkTerminalMenu;
import com.lhy.wcwt.network.WcwtPullRecipeInputsPacket;
import com.lhy.wcwt.network.WcwtPullRecipeInputsPacket.RequestedIngredient;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.handler.EmiRecipeHandler;
import dev.emi.emi.api.widget.Bounds;
import dev.emi.emi.api.widget.Widget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.neoforge.network.PacketDistributor;

final class WcwtEmiPullItemsWidget extends Widget {
    private static final int SIZE = 12;
    private static final int PITCH = 14;
    private static final int MAX_PLACEMENT_ATTEMPTS = 8;
    private static final ResourceLocation EMI_BUTTONS =
            ResourceLocation.fromNamespaceAndPath("emi", "textures/gui/buttons.png");

    private final int x;
    private final int y;
    private final EmiRecipe recipe;
    private final List<Widget> recipeWidgets;

    WcwtEmiPullItemsWidget(int x, int y, EmiRecipe recipe, List<Widget> recipeWidgets) {
        this.x = x;
        this.y = y;
        this.recipe = recipe;
        this.recipeWidgets = recipeWidgets;
    }

    @Override
    public Bounds getBounds() {
        return resolvePlacement();
    }

    /**
     * EMI and addons such as EMI Recipe Sharing add their buttons to the same widget list after the
     * decorators run, so the free spot is only known once the list is complete: move up, then one
     * column right, past anything already there.
     */
    private Bounds resolvePlacement() {
        int candidateX = x;
        int candidateY = y;
        for (int attempt = 0; attempt < MAX_PLACEMENT_ATTEMPTS; attempt++) {
            if (!overlapsOtherWidget(candidateX, candidateY)) {
                return new Bounds(candidateX, candidateY, SIZE, SIZE);
            }
            if (candidateY - PITCH >= 0) {
                candidateY -= PITCH;
            } else {
                candidateX += PITCH;
                candidateY = y;
            }
        }
        return new Bounds(x, y, SIZE, SIZE);
    }

    private boolean overlapsOtherWidget(int left, int top) {
        for (Widget widget : recipeWidgets) {
            if (widget == this) {
                continue;
            }
            Bounds other = widget.getBounds();
            if (other == null || other.empty()) {
                continue;
            }
            if (left < other.right() && other.left() < left + SIZE
                    && top < other.bottom() && other.top() < top + SIZE) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        if (!WcwtPullItemsSupport.shouldShowExtraButton()) {
            return;
        }
        var preview = preview();
        boolean canFill = preview != null && preview.inputCount() > 0 && preview.anyResolved();
        Bounds bounds = getBounds();
        boolean hovered = canFill && bounds.contains(mouseX, mouseY);
        int textureV = canFill ? (hovered ? 12 : 0) : 24;
        graphics.blit(EMI_BUTTONS, bounds.x(), bounds.y(), SIZE, SIZE, 72, textureV, SIZE, SIZE, 256, 256);
        var hammer = Icon.CRAFT_HAMMER.getBlitter().dest(bounds.x() + 1, bounds.y() + 1, 10, 10);
        if (!canFill) {
            hammer.color(0.45F, 0.45F, 0.45F);
        }
        hammer.blit(graphics);
        if (preview != null && hovered) {
            WcwtEmiRecipeHandler.renderMissingAndCraftableSlotOverlays(
                    WcwtEmiRecipeHandler.getRecipeInputSlots(recipe, recipeWidgets),
                    graphics, preview.missingSlots(), preview.craftableSlots());
        }
    }

    @Override
    public List<ClientTooltipComponent> getTooltip(int mouseX, int mouseY) {
        var preview = preview();
        if (preview == null || preview.inputCount() <= 0) {
            return tooltip(List.of(EmiRecipeHandler.NOT_ENOUGH_INGREDIENTS));
        }
        List<Component> lines = new ArrayList<>();
        lines.add(ItemModText.MOVE_ITEMS.text());
        lines.addAll(WcwtEmiRecipeHandler.createLockedGridTooltip(
                preview, WcwtEmiRecipeHandler.getTransferMode(recipe) != EncodingMode.CRAFTING));
        return tooltip(lines);
    }

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        if (button != 0 || !getBounds().contains(mouseX, mouseY) || !WcwtPullItemsSupport.shouldShowExtraButton()) {
            return false;
        }
        var preview = preview();
        if (preview == null || preview.inputCount() <= 0 || !preview.anyResolved()) {
            return false;
        }
        var player = Minecraft.getInstance().player;
        if (player == null || !(player.containerMenu instanceof WirelessComprehensiveWorkTerminalMenu menu)) {
            return false;
        }
        EncodingMode mode = WcwtEmiRecipeHandler.getTransferMode(recipe);
        WcwtManualWorkspaceRecipeSwitch.switchForTransfer(menu, mode);
        List<RequestedIngredient> requested = WcwtEmiRecipeHandler.collectRequestedIngredients(menu, recipe);
        if (mode == EncodingMode.PROCESSING
                && menu.getManualWorkspaceMode() == WirelessComprehensiveWorkTerminalMenu.ManualWorkspaceMode.CRAFTING) {
            requested = WcwtEmiRecipeHandler.mergeProcessingIngredients(requested);
        }
        if (requested.isEmpty()) {
            return false;
        }
        boolean allowShiftMaxTransfer = mode != EncodingMode.CRAFTING;
        boolean maxTransfer = allowShiftMaxTransfer && Screen.hasShiftDown();
        boolean craftMissing = Screen.hasControlDown();
        PacketDistributor.sendToServer(new WcwtPullRecipeInputsPacket(maxTransfer, craftMissing,
                requested, menu.getManualWorkspaceMode().ordinal()));
        if (craftMissing && mode == EncodingMode.CRAFTING && recipe.getBackingRecipe() != null) {
            Ae2HelpersCompat.watchMissingCraftingSlots(menu, recipe.getBackingRecipe().value());
        }
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        Screen screen = Minecraft.getInstance().screen;
        if (screen != null && !(screen instanceof WirelessComprehensiveWorkTerminalScreen)) {
            screen.onClose();
        }
        return true;
    }

    private WcwtEmiRecipeHandler.PreviewResult preview() {
        var player = Minecraft.getInstance().player;
        if (player == null || !(player.containerMenu instanceof WirelessComprehensiveWorkTerminalMenu menu)) {
            return null;
        }
        return WcwtEmiRecipeHandler.buildLockedGridPreview(menu, recipe);
    }

    private static List<ClientTooltipComponent> tooltip(List<Component> lines) {
        return lines.stream()
                .map(Component::getVisualOrderText)
                .map(ClientTooltipComponent::create)
                .toList();
    }
}
