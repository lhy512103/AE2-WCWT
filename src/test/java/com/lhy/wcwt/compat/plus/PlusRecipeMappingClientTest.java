package com.lhy.wcwt.compat.plus;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLPaths;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class PlusRecipeMappingClientTest {
    private static final ResourceLocation CATEGORY = ResourceLocation.fromNamespaceAndPath("wcwt_test", "pressing");

    @org.junit.jupiter.api.BeforeAll
    static void bootstrap() {
        net.minecraft.SharedConstants.tryDetectVersion();
        if (net.neoforged.fml.loading.LoadingModList.get() == null) {
            net.neoforged.fml.loading.LoadingModList.of(java.util.List.of(), java.util.List.of(),
                    java.util.List.of(), java.util.List.of(), java.util.Map.of());
        }
        net.minecraft.server.Bootstrap.bootStrap();
    }

    @AfterEach
    void clearSearchKey() {
        PlusPatternUploadApi.consumeLastProviderSearchKey();
    }

    @Test
    void jeiEntryUsesTheRuntimeCategoryBeforeRecipeIdFallback() throws ReflectiveOperationException {
        var type = mezz.jei.api.recipe.RecipeType.create("wcwt_test", "pressing", Object.class);
        var category = proxy(mezz.jei.api.recipe.category.IRecipeCategory.class,
                method -> net.minecraft.network.chat.Component.literal("压印器"));
        var helpers = proxy(mezz.jei.api.helpers.IJeiHelpers.class,
                method -> java.util.stream.Stream.of(type));
        var manager = proxy(mezz.jei.api.recipe.IRecipeManager.class, method -> category);
        var runtime = proxy(mezz.jei.api.runtime.IJeiRuntime.class, method -> switch (method) {
            case "getRecipeManager" -> manager;
            case "getJeiHelpers" -> helpers;
            default -> throw new AssertionError("Unexpected runtime call: " + method);
        });
        var compat = PlusPatternUploadApi.jeiRuntimeCompatClass();
        assertNotNull(compat);
        var previous = compat.getMethod("getRuntime").invoke(null);
        var setRuntime = compat.getMethod("setRuntime", mezz.jei.api.runtime.IJeiRuntime.class);
        setRuntime.invoke(null, runtime);
        try {
            PlusRecipeMappingClient.captureJei(new Object(), null, appeng.parts.encoding.EncodingMode.PROCESSING);
            assertEquals("压印器", PlusPatternUploadApi.consumeLastProviderSearchKey());
        } finally {
            setRuntime.invoke(null, previous);
        }
    }

    private static <T> T proxy(Class<T> type, java.util.function.Function<String, Object> methods) {
        return type.cast(java.lang.reflect.Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (instance, method, arguments) -> methods.apply(method.getName())));
    }

    @Test
    void keepsClientCategoryTitleInsteadOfRegistryPath() {
        assertTrue(PlusRecipeMappingClient.captureLocalizedCategory(CATEGORY, "压印器"));
        assertEquals("压印器", PlusPatternUploadApi.consumeLastProviderSearchKey());
        assertTrue(PlusRecipeMappingClient.captureLocalizedCategory(CATEGORY, "Inscriber"));
        assertEquals("Inscriber", PlusPatternUploadApi.consumeLastProviderSearchKey());
    }

    @Test
    void publicMappingWriteAndRemoveApisControlLocalizedCategoryResolution(@TempDir Path gameDir) {
        FMLPaths.loadAbsolutePaths(gameDir);
        String key = CATEGORY.toString();
        assertTrue(PlusPatternUploadApi.addOrUpdateRecipeTypeMapping(key, "专用压印器"));
        try {
            assertTrue(PlusRecipeMappingClient.captureLocalizedCategory(CATEGORY, "压印器"));
            assertEquals("专用压印器", PlusPatternUploadApi.consumeLastProviderSearchKey());
        } finally {
            PlusPatternUploadApi.removeRecipeTypeMapping(key);
        }
        assertTrue(PlusRecipeMappingClient.captureLocalizedCategory(CATEGORY, "压印器"));
        assertEquals("压印器", PlusPatternUploadApi.consumeLastProviderSearchKey());
    }

    @Test
    void removeMappingsByValueDeletesAllMatchesAndPreservesOtherValues(@TempDir Path gameDir) {
        FMLPaths.loadAbsolutePaths(gameDir);
        ResourceLocation first = ResourceLocation.fromNamespaceAndPath("wcwt_test", "pressing_a");
        ResourceLocation second = ResourceLocation.fromNamespaceAndPath("wcwt_test", "pressing_b");
        ResourceLocation kept = ResourceLocation.fromNamespaceAndPath("wcwt_test", "pressing_c");
        String shared = "共享压印器";
        assertTrue(PlusPatternUploadApi.addOrUpdateRecipeTypeMapping(first.toString(), shared));
        assertTrue(PlusPatternUploadApi.addOrUpdateRecipeTypeMapping(second.toString(), shared));
        assertTrue(PlusPatternUploadApi.addOrUpdateRecipeTypeMapping(kept.toString(), "保留压印器"));
        try {
            assertEquals(2, PlusPatternUploadApi.removeMappingsByCnValue(shared));
            assertTrue(PlusRecipeMappingClient.captureLocalizedCategory(first, "压印器甲"));
            assertEquals("压印器甲", PlusPatternUploadApi.consumeLastProviderSearchKey());
            assertTrue(PlusRecipeMappingClient.captureLocalizedCategory(second, "压印器乙"));
            assertEquals("压印器乙", PlusPatternUploadApi.consumeLastProviderSearchKey());
            assertTrue(PlusRecipeMappingClient.captureLocalizedCategory(kept, "压印器丙"));
            assertEquals("保留压印器", PlusPatternUploadApi.consumeLastProviderSearchKey());
        } finally {
            PlusPatternUploadApi.removeRecipeTypeMapping(first.toString());
            PlusPatternUploadApi.removeRecipeTypeMapping(second.toString());
            PlusPatternUploadApi.removeRecipeTypeMapping(kept.toString());
        }
    }

    @Test
    void emptyMappingDeletionIsAConservativeNoOp() {
        assertEquals(0, PlusPatternUploadApi.removeMappingsByCnValue(null));
        assertEquals(0, PlusPatternUploadApi.removeMappingsByCnValue(""));
        assertEquals(0, PlusPatternUploadApi.removeMappingsByCnValue("   "));
    }

    @Test
    void missingTitleFallsBackToIdAndMissingCategoryKeepsFallbackAvailable() {
        assertTrue(PlusRecipeMappingClient.captureLocalizedCategory(CATEGORY, ""));
        assertEquals("pressing", PlusPatternUploadApi.consumeLastProviderSearchKey());
        assertFalse(PlusRecipeMappingClient.captureLocalizedCategory(null, "压印器"));
        assertNull(PlusPatternUploadApi.consumeLastProviderSearchKey());
    }
}
