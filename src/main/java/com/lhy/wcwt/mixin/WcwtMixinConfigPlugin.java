package com.lhy.wcwt.mixin;

import net.minecraftforge.fml.loading.LoadingModList;
import org.objectweb.asm.tree.ClassNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public final class WcwtMixinConfigPlugin implements IMixinConfigPlugin {
    private static final Logger LOGGER = LoggerFactory.getLogger("wcwt");
    private static final String COMPAT_PACKAGE = "com.lhy.wcwt.mixin.compat.";
    private static final String EAEP_COMPAT_PACKAGE = COMPAT_PACKAGE + "eaep.";
    private static final String JEI_COMPAT_PACKAGE = COMPAT_PACKAGE + "jei.";

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (!mixinClassName.startsWith(COMPAT_PACKAGE)) {
            return true;
        }
        if (mixinClassName.startsWith(EAEP_COMPAT_PACKAGE)) {
            return shouldApplyCompatMixin("extendedae_plus", targetClassName, mixinClassName);
        }
        if (mixinClassName.startsWith(JEI_COMPAT_PACKAGE)) {
            return shouldApplyCompatMixin("jei", targetClassName, mixinClassName);
        }
        return shouldApplyCompatMixin(null, targetClassName, mixinClassName);
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    private static boolean isClassPresent(String className) {
        try {
            Class.forName(className, false, WcwtMixinConfigPlugin.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException | LinkageError ignored) {
            return false;
        }
    }

    private static boolean isModLoaded(String modId) {
        try {
            var modList = LoadingModList.get();
            return modList != null && modList.getModFileById(modId) != null;
        } catch (RuntimeException | LinkageError ignored) {
            return false;
        }
    }

    private static boolean shouldApplyCompatMixin(String modId, String targetClassName, String mixinClassName) {
        boolean modLoaded = modId != null && isModLoaded(modId);
        boolean classPresent = !modLoaded && isClassPresent(targetClassName);
        boolean apply = modLoaded || classPresent;
        LOGGER.info("WCWT compat mixin check: mixin={}, target={}, modId={}, modLoaded={}, classPresent={}, apply={}",
                mixinClassName, targetClassName, modId, modLoaded, classPresent, apply);
        return apply;
    }
}
