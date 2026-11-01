package com.lhy.wcwt.compat;

import appeng.api.networking.IGrid;
import appeng.helpers.patternprovider.PatternContainer;
import appeng.helpers.patternprovider.PatternProviderLogicHost;
import com.extendedae_plus.content.matrix.PatternCorePlusBlockEntity;
import com.extendedae_plus.content.matrix.supermatrix.SuperAssemblerMatrixPart;
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
        return ModList.get().isLoaded(EXTENDEDAE_PLUS)
                && (provider instanceof PatternCorePlusBlockEntity || provider instanceof SuperAssemblerMatrixPart);
    }

    public static Object assemblerMatrixSystem(PatternContainer provider) {
        if (!isUploadAvailable() || provider == null) {
            return null;
        }
        if (provider instanceof SuperAssemblerMatrixPart part) {
            var superCluster = part.eap$getSuperMatrixCluster();
            if (superCluster != null) {
                return superCluster;
            }
        }
        if (provider instanceof TileAssemblerMatrixPattern matrix
                && matrix.isFormed() && matrix.getMainNode().isActive()) {
            var cluster = matrix.getCluster();
            if (cluster != null) {
                var members = cluster.getBlockEntities();
                while (members.hasNext()) {
                    if (members.next() instanceof com.extendedae_plus.content.matrix.UploadCoreBlockEntity) {
                        return cluster;
                    }
                }
            }
            return null;
        }
        // EAEP's matrix uploader also accepts AE2CS hosts selected by this class-name check.
        if (isUploadAvailable() && provider.getClass().getName().contains("MeteoritePatternProvider")
                && provider instanceof PatternProviderLogicHost host) {
            return host.getLogic();
        }
        return null;
    }

    public static boolean uploadPatternToMatrix(ServerPlayer player, ItemStack pattern, IGrid grid) {
        return isUploadAvailable()
                && ExtendedAEPatternUploadUtil.uploadPatternToMatrix(player, pattern, grid);
    }
}
