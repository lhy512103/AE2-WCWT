package com.lhy.wcwt.mixin;

import com.lhy.wcwt.compat.emi.WcwtEmiPlugin;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.screen.WidgetGroup;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** EMI recipe decorators are disabled by default, so add the button to the completed widget list. */
@Pseudo
@Mixin(targets = "dev.emi.emi.screen.RecipeDisplay", remap = false)
public abstract class WcwtEmiRecipeDisplayMixin {
    @Shadow @Final public EmiRecipe recipe;

    @Inject(method = "getWidgets", at = @At("RETURN"))
    private void wcwt$addPullItemsButton(int x, int y, int availableWidth, int availableHeight,
                                       CallbackInfoReturnable<WidgetGroup> cir) {
        WcwtEmiPlugin.addPullItemsButton(recipe, cir.getReturnValue().widgets);
    }
}
