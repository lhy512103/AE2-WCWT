package com.lhy.wcwt;

import appeng.api.stacks.AEKeyType;
import appeng.api.stacks.AEKeyTypes;
import appeng.api.stacks.AEKeyTypesInternal;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.neoforged.fml.loading.LoadingModList;
import net.neoforged.neoforge.registries.RegistryBuilder;

import java.util.List;
import java.util.Map;

/**
 * Brings up vanilla registries and AE2's item key type for unit tests that touch {@code ItemStack} or
 * {@code AEItemKey}. Safe to call from several test classes in one JVM.
 */
public final class TestBootstrap {
    private static boolean done;

    private TestBootstrap() {
    }

    public static synchronized void ensure() {
        if (done) {
            return;
        }
        SharedConstants.tryDetectVersion();
        // src/test/resources supplies an empty en_us map: unit tests have no Minecraft client assets.
        Bootstrap.bootStrap();
        net.minecraft.core.registries.BuiltInRegistries.ITEM.listElements().forEach(holder -> {
            if (!holder.areComponentsBound()) {
                holder.bindComponents(net.minecraft.core.component.DataComponents.COMMON_ITEM_COMPONENTS);
            }
        });
        boolean keyTypesReady;
        try {
            keyTypesReady = AEKeyTypesInternal.getRegistry() != null;
        } catch (RuntimeException notYet) {
            keyTypesReady = false;
        }
        if (!keyTypesReady) {
            var keyTypes = new RegistryBuilder<>(AEKeyType.REGISTRY_KEY).disableRegistrationCheck().create();
            AEKeyTypesInternal.setRegistry(keyTypes);
            AEKeyTypes.register(AEKeyType.items());
            keyTypes.freeze();
        }
        done = true;
    }
}
