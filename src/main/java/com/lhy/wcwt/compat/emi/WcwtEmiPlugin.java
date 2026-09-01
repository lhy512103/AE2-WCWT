package com.lhy.wcwt.compat.emi;

import com.lhy.wcwt.client.WirelessComprehensiveWorkTerminalScreen;
import com.lhy.wcwt.compat.WcwtOptionalFeatureGates;
import com.lhy.wcwt.compat.WcwtPullItemsSupport;
import com.lhy.wcwt.init.ModMenus;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.widget.Bounds;
import dev.emi.emi.api.widget.Widget;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.world.item.Item;

import java.util.HashSet;
import java.util.List;

@EmiEntrypoint
public class WcwtEmiPlugin implements EmiPlugin {
    private static final int BUTTON = 12;
    private static final int BUTTON_PITCH = 14;

    @Override
    public void register(EmiRegistry registry) {
        var hiddenItems = new HashSet<Item>(WcwtOptionalFeatureGates.hiddenUpgradeCardItems());
        if (!hiddenItems.isEmpty()) {
            registry.removeEmiStacks(stack -> hiddenItems.contains(stack.getItemStack().getItem()));
            registry.removeRecipes(recipe -> recipe.getOutputs().stream()
                    .anyMatch(output -> hiddenItems.contains(output.getItemStack().getItem())));
        }
        registry.addRecipeHandler(ModMenus.WCWT_MENU_TYPE, new WcwtEmiRecipeHandler());
        registry.addRecipeDecorator(WcwtEmiPlugin::decoratePullItems);
        registry.addExclusionArea(WirelessComprehensiveWorkTerminalScreen.class,
                (screen, consumer) -> consumer.accept(new Bounds(screen.getGuiLeft(), screen.getGuiTop(),
                        screen.getXSize(), screen.getYSize())));
    }

    private static void decoratePullItems(EmiRecipe recipe, WidgetHolder widgets) {
        if (!WcwtPullItemsSupport.shouldShowExtraButton()) {
            return;
        }
        var handler = new WcwtEmiRecipeHandler();
        if (!handler.supportsRecipe(recipe)) {
            return;
        }
        List<Widget> existing = holderWidgets(widgets);
        int[] pos = pullButtonPos(recipe);
        widgets.add(new WcwtEmiPullItemsWidget(pos[0], pos[1], recipe, existing));
    }

    private static int[] pullButtonPos(EmiRecipe recipe) {
        int height = recipe.getDisplayHeight();
        int rows = Math.max(1, (height + 10) / BUTTON_PITCH);
        int rightButtons = 1;
        if (recipe.supportsRecipeTree()) {
            rightButtons += 2;
        }
        int space = Math.min(8, height + 8 - (Math.min(rows, rightButtons) * BUTTON_PITCH - 2));
        int bottom = height + 4 - BUTTON - space / 2;
        int x = recipe.getDisplayWidth() + 5;
        int remaining = rightButtons;
        while (remaining > 0) {
            int used = Math.min(rows, remaining);
            int first = remaining - used;
            if (first == 0) {
                int fillX = x;
                int fillY = bottom;
                if (used == 1) {
                    return new int[] {fillX, fillY - BUTTON_PITCH};
                }
                return new int[] {fillX + BUTTON_PITCH, fillY};
            }
            remaining -= used;
            x += BUTTON_PITCH;
        }
        return new int[] {recipe.getDisplayWidth() + 5, Math.max(0, height - BUTTON)};
    }

    @SuppressWarnings("unchecked")
    private static List<Widget> holderWidgets(WidgetHolder holder) {
        try {
            var field = holder.getClass().getField("widgets");
            Object value = field.get(holder);
            if (value instanceof List<?> list) {
                return (List<Widget>) list;
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return List.of();
    }
}
