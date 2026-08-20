package com.lhy.wcwt.compat;

import appeng.api.networking.IGrid;
import appeng.helpers.patternprovider.PatternContainer;
import com.extendedae_plus.content.matrix.PatternCorePlusBlockEntity;
import com.extendedae_plus.util.uploadPattern.ExtendedAEPatternUploadUtil;
import com.glodblock.github.extendedae.common.tileentities.matrix.TileAssemblerMatrixPattern;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

/**
 * ExtendedAE / ExtendedAE Plus assembler-matrix upload using public block-entity
 * and PatternContainer APIs. Duplicate checks stay on WCWT so a matrix miss
 * does not fall through to named pattern providers.
 */
public final class ExtendedAePlusMatrixUploadCompat {
    private static final String EXTENDEDAE = "extendedae";
    private static final String EXTENDEDAE_PLUS = "extendedae_plus";

    private ExtendedAePlusMatrixUploadCompat() {
    }

    public static boolean isAssemblerMatrixAvailable() {
        return ModList.get().isLoaded(EXTENDEDAE) || ModList.get().isLoaded(EXTENDEDAE_PLUS);
    }

    public static boolean isUploadAvailable() {
        return ModList.get().isLoaded(EXTENDEDAE_PLUS);
    }

    public static boolean isAssemblerMatrix(PatternContainer provider) {
        if (provider == null) {
            return false;
        }
        if (ModList.get().isLoaded(EXTENDEDAE) && provider instanceof TileAssemblerMatrixPattern) {
            return true;
        }
        return ModList.get().isLoaded(EXTENDEDAE_PLUS) && provider instanceof PatternCorePlusBlockEntity;
    }

    public static boolean uploadPatternToMatrix(ServerPlayer player, ItemStack pattern, IGrid grid) {
        return isUploadAvailable()
                && ExtendedAEPatternUploadUtil.uploadPatternToMatrix(player, pattern, grid);
    }
}
