package com.lhy.wcwt.compat;

import appeng.api.networking.IGrid;
import appeng.helpers.patternprovider.PatternContainer;
import cn.dancingsnow.neoecoae.api.IECOPatternStorage;
import cn.dancingsnow.neoecoae.api.IECOPatternStorageService;
import net.minecraft.world.item.ItemStack;

/** Direct Neo ECO AE API integration. This class is only called on the server side. */
public final class NeoEcoApiCompat {
    private NeoEcoApiCompat() {
    }

    public static boolean uploadPatternToEcoStorage(IGrid grid, ItemStack pattern) {
        IECOPatternStorageService service = grid.getService(IECOPatternStorageService.class);
        if (service == null) {
            return false;
        }
        IECOPatternStorage storage = service.getPatternStorage();
        return storage != null && storage.insertPattern(pattern);
    }

    public static boolean isEcoPatternProvider(PatternContainer provider) {
        return provider instanceof IECOPatternStorage;
    }
}
