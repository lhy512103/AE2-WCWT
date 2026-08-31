package com.lhy.wcwt.compat.emi;

import com.lhy.wcwt.client.WirelessComprehensiveWorkTerminalScreen;
import com.lhy.wcwt.compat.WcwtOptionalFeatureGates;
import com.lhy.wcwt.init.ModMenus;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.widget.Bounds;
import net.minecraft.world.item.Item;

import java.util.HashSet;

@EmiEntrypoint
public class WcwtEmiPlugin implements EmiPlugin {

    @Override
    public void register(EmiRegistry registry) {
        var hiddenItems = new HashSet<Item>(WcwtOptionalFeatureGates.hiddenUpgradeCardItems());
        if (!hiddenItems.isEmpty()) {
            registry.removeEmiStacks(stack -> hiddenItems.contains(stack.getItemStack().getItem()));
            registry.removeRecipes(recipe -> recipe.getOutputs().stream()
                    .anyMatch(output -> hiddenItems.contains(output.getItemStack().getItem())));
        }
        registry.addRecipeHandler(ModMenus.WCWT_MENU_TYPE, new WcwtEmiRecipeHandler());
        registry.addExclusionArea(WirelessComprehensiveWorkTerminalScreen.class,
                (screen, consumer) -> consumer.accept(new Bounds(screen.getGuiLeft(), screen.getGuiTop(),
                        screen.getXSize(), screen.getYSize())));
    }
}
