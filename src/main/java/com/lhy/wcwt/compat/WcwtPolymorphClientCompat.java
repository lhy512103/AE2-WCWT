package com.lhy.wcwt.compat;

import appeng.menu.SlotSemantics;
import appeng.parts.encoding.EncodingMode;
import com.illusivesoulworks.polymorph.api.PolymorphApi;
import com.illusivesoulworks.polymorph.api.client.base.IRecipesWidget;
import com.illusivesoulworks.polymorph.api.client.widget.AbstractRecipesWidget;
import com.illusivesoulworks.polymorph.api.client.widget.SelectionWidget;
import com.illusivesoulworks.polymorph.api.common.base.IRecipePair;
import com.lhy.wcwt.client.WirelessComprehensiveWorkTerminalScreen;
import com.lhy.wcwt.menu.WcwtSlotSemantics;
import com.lhy.wcwt.menu.WirelessComprehensiveWorkTerminalMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class WcwtPolymorphClientCompat {
    private static final String MOD_ID = "polymorph";
    private static final int MANUAL_BUTTON_X = 132;
    private static final int MANUAL_BUTTON_Y_OFFSET_FROM_OUTPUT = 19;
    private static boolean registered;

    private WcwtPolymorphClientCompat() {
    }

    public static void registerWidgets() {
        if (registered || !ModList.get().isLoaded(MOD_ID)) {
            return;
        }
        registered = true;
        Impl.registerWidgets();
    }

    private static final class Impl {
        private Impl() {
        }

        private static void registerWidgets() {
            PolymorphApi.client().registerWidget(screen -> {
                if (!(screen instanceof WirelessComprehensiveWorkTerminalScreen wcwtScreen)) {
                    return null;
                }
                WirelessComprehensiveWorkTerminalMenu menu = wcwtScreen.getMenu();

                List<ModeAwareRecipesWidget> widgets = new ArrayList<>(3);
                Slot previewSlot = firstSlot(menu.getSlots(WcwtSlotSemantics.WCWT_PATTERN_PREVIEW));
                if (previewSlot != null) {
                    widgets.add(new ModeAwareRecipesWidget(wcwtScreen, menu, previewSlot, Placement.PATTERN_PREVIEW));
                }

                Slot manualCraftingResultSlot = firstSlot(menu.getSlots(SlotSemantics.CRAFTING_RESULT));
                if (manualCraftingResultSlot != null) {
                    widgets.add(new ModeAwareRecipesWidget(wcwtScreen, menu, manualCraftingResultSlot,
                            Placement.MANUAL_CRAFTING));
                }

                Slot manualSmithingResultSlot = firstSlot(menu.getSlots(WcwtSlotSemantics.WCWT_MANUAL_SMITHING_RESULT));
                if (manualSmithingResultSlot != null) {
                    widgets.add(new ModeAwareRecipesWidget(wcwtScreen, menu, manualSmithingResultSlot,
                            Placement.MANUAL_SMITHING));
                }

                return widgets.isEmpty() ? null : new WcwtRecipesWidget(widgets);
            });
        }

        private static Slot firstSlot(List<Slot> slots) {
            return slots.isEmpty() ? null : slots.get(0);
        }
    }

    private static final class WcwtRecipesWidget implements IRecipesWidget {
        private final List<ModeAwareRecipesWidget> widgets;

        private WcwtRecipesWidget(List<ModeAwareRecipesWidget> widgets) {
            this.widgets = widgets;
        }

        @Override
        public void initChildWidgets() {
            widgets.forEach(IRecipesWidget::initChildWidgets);
        }

        @Override
        public void selectRecipe(ResourceLocation resourceLocation) {
            primaryWidget().selectRecipe(resourceLocation);
        }

        @Override
        public void highlightRecipe(ResourceLocation resourceLocation) {
            widgets.forEach(widget -> widget.highlightRecipe(resourceLocation));
        }

        @Override
        public void setRecipesList(Set<IRecipePair> recipesList, ResourceLocation selected) {
            Set<IRecipePair> safeRecipes = recipesList == null ? Set.of() : Set.copyOf(recipesList);
            if (safeRecipes.isEmpty()) {
                widgets.stream()
                        .filter(ModeAwareRecipesWidget::shouldClearOnEmptyRecipes)
                        .forEach(widget -> widget.setRecipesList(safeRecipes, selected));
                return;
            }

            boolean matched = false;
            for (ModeAwareRecipesWidget widget : widgets) {
                if (matchesRecipeList(widget, safeRecipes)) {
                    widget.setRecipesList(safeRecipes, selected);
                    matched = true;
                }
            }

            if (!matched) {
                for (ModeAwareRecipesWidget widget : widgets) {
                    if (widget.isModeActive() && widget.isOutputSlotActive()) {
                        widget.setRecipesList(safeRecipes, selected);
                    }
                }
            }
        }

        @Override
        public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            for (ModeAwareRecipesWidget widget : widgets) {
                if (shouldUseWidget(widget)) {
                    widget.render(guiGraphics, mouseX, mouseY, partialTick);
                }
            }
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            for (ModeAwareRecipesWidget widget : widgets) {
                if (shouldUseWidget(widget) && widget.mouseClicked(mouseX, mouseY, button)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public Slot getOutputSlot() {
            return primaryWidget().getOutputSlot();
        }

        @Override
        public SelectionWidget getSelectionWidget() {
            return primaryWidget().getSelectionWidget();
        }

        @Override
        public int getXPos() {
            return primaryWidget().getXPos();
        }

        @Override
        public int getYPos() {
            return primaryWidget().getYPos();
        }

        private ModeAwareRecipesWidget primaryWidget() {
            for (ModeAwareRecipesWidget widget : widgets) {
                if (widget.isModeActive() && !widget.getOutputSlot().getItem().isEmpty()) {
                    return widget;
                }
            }
            for (ModeAwareRecipesWidget widget : widgets) {
                if (widget.isModeActive()) {
                    return widget;
                }
            }
            for (ModeAwareRecipesWidget widget : widgets) {
                if (shouldUseWidget(widget)) {
                    return widget;
                }
            }
            return widgets.get(0);
        }

        private boolean shouldUseWidget(ModeAwareRecipesWidget widget) {
            if (!widget.isModeActive()) {
                return false;
            }
            Slot outputSlot = widget.getOutputSlot();
            if (!widget.hasMultipleRecipes() || outputSlot == null || !outputSlot.isActive()) {
                return false;
            }
            return widget.hasMatchingOutput();
        }

        private boolean matchesRecipeList(ModeAwareRecipesWidget widget, Set<IRecipePair> recipesList) {
            if (!widget.isModeActive()) {
                return false;
            }
            Slot outputSlot = widget.getOutputSlot();
            if (outputSlot == null || !outputSlot.isActive()) {
                return false;
            }
            ItemStack current = outputSlot.getItem();
            if (current.isEmpty()) {
                return recipesList.isEmpty();
            }
            for (IRecipePair recipePair : recipesList) {
                if (ItemStack.isSameItemSameTags(recipePair.getOutput(), current)) {
                    return true;
                }
            }
            return false;
        }
    }

    private enum Placement {
        PATTERN_PREVIEW,
        MANUAL_CRAFTING,
        MANUAL_SMITHING
    }

    private static final class ModeAwareRecipesWidget extends AbstractRecipesWidget {
        private final WirelessComprehensiveWorkTerminalMenu menu;
        private final Slot outputSlot;
        private final Placement placement;
        private Set<IRecipePair> recipesList = Set.of();

        private ModeAwareRecipesWidget(WirelessComprehensiveWorkTerminalScreen screen,
                                       WirelessComprehensiveWorkTerminalMenu menu,
                                       Slot outputSlot,
                                       Placement placement) {
            super(screen);
            this.menu = menu;
            this.outputSlot = outputSlot;
            this.placement = placement;
        }

        @Override
        public void selectRecipe(ResourceLocation resourceLocation) {
            selectClientRecipe(resourceLocation);
            PolymorphApi.common().getPacketDistributor().sendPlayerRecipeSelectionC2S(resourceLocation);
            if (placement == Placement.PATTERN_PREVIEW) {
                menu.forcePatternPreviewUpdate();
            }
        }

        private void selectClientRecipe(ResourceLocation resourceLocation) {
            var player = Minecraft.getInstance().player;
            if (player == null) {
                return;
            }
            player.level().getRecipeManager().byKey(resourceLocation)
                    .ifPresent(recipe -> PolymorphApi.common().getRecipeData(player)
                            .ifPresent(data -> data.selectRecipe(recipe)));
        }

        @Override
        public Slot getOutputSlot() {
            return outputSlot;
        }

        @Override
        public int getXPos() {
            return switch (placement) {
                case PATTERN_PREVIEW -> getOutputSlot().x;
                case MANUAL_CRAFTING, MANUAL_SMITHING -> MANUAL_BUTTON_X;
            };
        }

        @Override
        public int getYPos() {
            return switch (placement) {
                case PATTERN_PREVIEW -> getOutputSlot().y + patternPreviewYOffset();
                case MANUAL_CRAFTING, MANUAL_SMITHING ->
                        getOutputSlot().y + MANUAL_BUTTON_Y_OFFSET_FROM_OUTPUT;
            };
        }

        private boolean isModeActive() {
            return switch (placement) {
                case PATTERN_PREVIEW -> menu.getPatternEncodingMode() == EncodingMode.CRAFTING
                        || menu.getPatternEncodingMode() == EncodingMode.SMITHING_TABLE;
                case MANUAL_CRAFTING ->
                        menu.getManualWorkspaceMode() == WirelessComprehensiveWorkTerminalMenu.ManualWorkspaceMode.CRAFTING;
                case MANUAL_SMITHING ->
                        menu.getManualWorkspaceMode() == WirelessComprehensiveWorkTerminalMenu.ManualWorkspaceMode.SMITHING;
            };
        }

        private int patternPreviewYOffset() {
            return menu.getPatternEncodingMode() == EncodingMode.CRAFTING ? -20 : -15;
        }

        private boolean hasMultipleRecipes() {
            return recipesList.size() > 1;
        }

        private boolean isOutputSlotActive() {
            return outputSlot != null && outputSlot.isActive();
        }

        private boolean hasMatchingOutput() {
            ItemStack current = outputSlot == null ? ItemStack.EMPTY : outputSlot.getItem();
            if (current.isEmpty()) {
                return false;
            }
            for (IRecipePair recipePair : recipesList) {
                if (ItemStack.isSameItemSameTags(recipePair.getOutput(), current)) {
                    return true;
                }
            }
            return false;
        }

        private boolean shouldClearOnEmptyRecipes() {
            if (!isModeActive() || !isOutputSlotActive()) {
                return true;
            }
            ItemStack current = outputSlot.getItem();
            return current.isEmpty() || !hasMatchingOutput();
        }

        @Override
        public void setRecipesList(Set<IRecipePair> recipesList, ResourceLocation selected) {
            this.recipesList = recipesList == null ? Set.of() : Set.copyOf(recipesList);
            super.setRecipesList(this.recipesList, selected);
        }

        @Override
        public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            resetWidgetOffsets();
            super.render(guiGraphics, mouseX, mouseY, partialTick);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            resetWidgetOffsets();
            return super.mouseClicked(mouseX, mouseY, button);
        }
    }
}
