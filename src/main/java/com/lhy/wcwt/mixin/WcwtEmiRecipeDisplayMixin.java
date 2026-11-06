package com.lhy.wcwt.mixin;

import com.lhy.wcwt.compat.emi.WcwtEmiPlugin;
import com.lhy.wcwt.compat.reflect.WcwtReflect;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.widget.Widget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * EMI recipe decorators are disabled by default, so add the button to the completed widget list.
 *
 * <p>{@code RecipeDisplay} and {@code WidgetGroup} are EMI internals, not API. The injection is optional and
 * both members are read reflectively, so an EMI build that moves or renames them only loses the button
 * instead of failing to apply the mixin when a recipe page opens.
 */
@Pseudo
@Mixin(targets = "dev.emi.emi.screen.RecipeDisplay", remap = false)
public abstract class WcwtEmiRecipeDisplayMixin {
    @Inject(method = "getWidgets", at = @At("RETURN"), require = 0)
    private void wcwt$addPullItemsButton(int x, int y, int availableWidth, int availableHeight,
                                       CallbackInfoReturnable<?> cir) {
        Object self = this;
        Object group = cir.getReturnValue();
        if (group == null) {
            return;
        }
        EmiRecipe recipe = WcwtReflect.findField(self.getClass(), "recipe")
                .flatMap(field -> WcwtReflect.readField(self, field))
                .filter(EmiRecipe.class::isInstance)
                .map(EmiRecipe.class::cast)
                .orElse(null);
        Object widgets = WcwtReflect.findField(group.getClass(), "widgets")
                .flatMap(field -> WcwtReflect.readField(group, field))
                .orElse(null);
        if (recipe == null || !(widgets instanceof List<?> widgetList)) {
            return;
        }
        @SuppressWarnings("unchecked")
        List<Widget> typedWidgets = (List<Widget>) widgetList;
        WcwtEmiPlugin.addPullItemsButton(recipe, typedWidgets);
    }
}
