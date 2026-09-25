package com.lhy.wcwt.mixin;

import com.lhy.wcwt.compat.emi.WcwtEmiPlugin;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.screen.RecipeDisplay;
import dev.emi.emi.screen.WidgetGroup;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * EMI only runs EmiRecipeDecorators when the dev-only show-recipe-decorators config is enabled, so
 * the pull-items button is added here, into the widget list the recipe screen renders.
 */
@Pseudo
@Mixin(RecipeDisplay.class)
public class RecipeDisplayMixin {
    @Shadow
    @Final
    public EmiRecipe recipe;

    @Inject(method = "getWidgets", at = @At("RETURN"))
    private void wcwt$addPullItemsButton(int x, int y, int availableWidth, int availableHeight,
            CallbackInfoReturnable<WidgetGroup> cir) {
        WcwtEmiPlugin.addPullItemsButton(recipe, cir.getReturnValue().widgets);
    }
}
