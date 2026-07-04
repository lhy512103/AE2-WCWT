package com.lhy.wcwt.compat.emi;

import com.lhy.wcwt.client.WirelessComprehensiveWorkTerminalScreen;
import com.lhy.wcwt.compat.WcwtOptionalFeatureGates;
import com.lhy.wcwt.init.ModMenus;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.Bounds;

@EmiEntrypoint
public class WcwtEmiPlugin implements EmiPlugin {

    @Override
    public void register(EmiRegistry registry) {
        for (var hiddenStack : WcwtOptionalFeatureGates.hiddenUpgradeCardStacks()) {
            registry.removeEmiStacks(EmiStack.of(hiddenStack));
        }
        registry.addRecipeHandler(ModMenus.WCWT_MENU.get(), new WcwtEmiRecipeHandler());
        registry.addExclusionArea(WirelessComprehensiveWorkTerminalScreen.class,
                (screen, consumer) -> consumer.accept(new Bounds(screen.getGuiLeft(), screen.getGuiTop(),
                        screen.getXSize(), screen.getYSize())));
    }
}
