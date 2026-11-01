package com.lhy.wcwt.compat.plus;

import com.extendedae_plus.util.uploadPattern.ExtendedAEPatternUploadUtil;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PlusRecipeMappingClientTest {
    private static final ResourceLocation CATEGORY = ResourceLocation.fromNamespaceAndPath("wcwt_test", "pressing");

    @AfterEach
    void clearSearchKey() {
        ExtendedAEPatternUploadUtil.consumeLastProviderSearchKey();
    }

    @Test
    void jeiEntryUsesTheRuntimeCategoryBeforeRecipeIdFallback() {
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
        var previous = com.extendedae_plus.compat.JeiRuntimeCompat.getRuntime();
        com.extendedae_plus.compat.JeiRuntimeCompat.setRuntime(runtime);
        try {
            PlusRecipeMappingClient.captureJei(new Object(), null, appeng.parts.encoding.EncodingMode.PROCESSING);
            assertEquals("压印器", ExtendedAEPatternUploadUtil.consumeLastProviderSearchKey());
        } finally {
            com.extendedae_plus.compat.JeiRuntimeCompat.setRuntime(previous);
        }
    }

    private static <T> T proxy(Class<T> type, java.util.function.Function<String, Object> methods) {
        return type.cast(java.lang.reflect.Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (instance, method, arguments) -> methods.apply(method.getName())));
    }

    @Test
    void keepsClientCategoryTitleInsteadOfRegistryPath() {
        assertTrue(PlusRecipeMappingClient.captureLocalizedCategory(CATEGORY, "压印器"));
        assertEquals("压印器", ExtendedAEPatternUploadUtil.consumeLastProviderSearchKey());
        assertTrue(PlusRecipeMappingClient.captureLocalizedCategory(CATEGORY, "Inscriber"));
        assertEquals("Inscriber", ExtendedAEPatternUploadUtil.consumeLastProviderSearchKey());
    }

    @Test
    void explicitEaepMappingStillOverridesLocalizedTitle() throws ReflectiveOperationException {
        Field field = ExtendedAEPatternUploadUtil.class.getDeclaredField("CUSTOM_NAMES");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<ResourceLocation, String> mappings = (Map<ResourceLocation, String>) field.get(null);
        String previous = mappings.put(CATEGORY, "专用压印器");
        try {
            assertTrue(PlusRecipeMappingClient.captureLocalizedCategory(CATEGORY, "压印器"));
            assertEquals("专用压印器", ExtendedAEPatternUploadUtil.consumeLastProviderSearchKey());
        } finally {
            if (previous == null) mappings.remove(CATEGORY);
            else mappings.put(CATEGORY, previous);
        }
    }

    @Test
    void missingTitleFallsBackToIdAndMissingCategoryKeepsFallbackAvailable() {
        assertTrue(PlusRecipeMappingClient.captureLocalizedCategory(CATEGORY, ""));
        assertEquals("pressing", ExtendedAEPatternUploadUtil.consumeLastProviderSearchKey());
        assertFalse(PlusRecipeMappingClient.captureLocalizedCategory(null, "压印器"));
        assertNull(ExtendedAEPatternUploadUtil.consumeLastProviderSearchKey());
    }
}
