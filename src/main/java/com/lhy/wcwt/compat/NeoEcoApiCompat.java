package com.lhy.wcwt.compat;

import appeng.api.networking.IGrid;
import appeng.helpers.patternprovider.PatternContainer;
import cn.dancingsnow.neoecoae.api.IECOPatternStorage;
import cn.dancingsnow.neoecoae.api.IECOPatternStorageService;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

/** Direct Neo ECO AE API integration. This class is only called on the server side. */
public final class NeoEcoApiCompat {
    private NeoEcoApiCompat() {
    }

    public static boolean isAvailable() {
        var mods = ModList.get();
        return mods != null && mods.isLoaded("neoecoae");
    }

    public static UploadResult uploadPatternToEcoStorage(IGrid grid, ItemStack pattern) {
        IECOPatternStorage storage = getPatternStorage(grid);
        if (storage == null) {
            return UploadResult.REJECTED;
        }
        return switch (storage.insertPatternWithResult(pattern)) {
            case INSERTED -> UploadResult.INSERTED;
            case ALREADY_PRESENT -> UploadResult.DUPLICATE;
            default -> UploadResult.REJECTED;
        };
    }

    public static Object patternStorageSystem(IGrid grid) {
        return getPatternStorage(grid);
    }

    private static IECOPatternStorage getPatternStorage(IGrid grid) {
        if (!isAvailable()) {
            return null;
        }
        IECOPatternStorageService service = grid.getService(IECOPatternStorageService.class);
        return service == null ? null : service.getPatternStorage();
    }

    public static boolean isEcoPatternProvider(PatternContainer provider) {
        return isAvailable() && provider instanceof IECOPatternStorage;
    }

    public enum UploadResult {
        INSERTED,
        DUPLICATE,
        REJECTED
    }
}
