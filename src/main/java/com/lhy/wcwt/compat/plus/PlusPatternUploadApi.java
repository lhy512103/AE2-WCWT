package com.lhy.wcwt.compat.plus;

import appeng.api.networking.IGrid;
import appeng.helpers.patternprovider.PatternContainer;
import appeng.parts.encoding.EncodingMode;
import appeng.util.inv.filter.IAEItemFilter;
import com.lhy.wcwt.compat.reflect.WcwtReflect;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.List;
import java.util.Optional;

/** Runtime-only bridge for EAEP's pre-1.6.4 and split upload APIs. */
public final class PlusPatternUploadApi {
    private static final String LEGACY = "com.extendedae_plus.util.uploadPattern.ExtendedAEPatternUploadUtil";
    private static final String MAPPING = "com.extendedae_plus.util.uploadPattern.RecipeTypeNameConfig";
    private static final String PROVIDER = "com.extendedae_plus.util.uploadPattern.ProviderUploadUtil";
    private static final String MATRIX = "com.extendedae_plus.util.uploadPattern.MatrixUploadUtil";

    private PlusPatternUploadApi() {
    }

    public static void presetCraftingProviderSearchKey() {
        invokeVoid(new String[]{LEGACY, MAPPING}, "presetCraftingProviderSearchKey", new Class<?>[0]);
    }

    public static void captureRecipeSearchKey(@Nullable Object recipeBase, @Nullable Recipe<?> recipe,
                                              EncodingMode mode) {
        if (mode != EncodingMode.PROCESSING) {
            invokeVoid(new String[]{LEGACY, MAPPING}, "presetCraftingProviderSearchKey", new Class<?>[0]);
            return;
        }
        String name = mapRecipeTypeToSearchKey(recipe);
        if ((name == null || name.isBlank()) && recipeBase != null) {
            name = deriveSearchKeyFromUnknownRecipe(recipeBase);
        }
        if (name != null && !name.isBlank()) {
            setLastProviderSearchKey(name);
        }
    }

    @Nullable
    public static String consumeLastProviderSearchKey() {
        return call(new String[]{LEGACY, MAPPING}, "consumeLastProviderSearchKey", new Class<?>[0])
                .filter(String.class::isInstance).map(String.class::cast).orElse(null);
    }

    public static String resolveProviderSearchKey(@Nullable String rawKey) {
        if (rawKey == null) return "";
        return call(new String[]{LEGACY, MAPPING}, "resolveProviderSearchKey",
                new Class<?>[]{String.class}, rawKey).filter(String.class::isInstance)
                .map(String.class::cast).orElse("").trim();
    }

    @Nullable
    public static String mapRecipeTypeToSearchKey(@Nullable Recipe<?> recipe) {
        if (recipe == null) return null;
        return call(new String[]{LEGACY, MAPPING}, "mapRecipeTypeToSearchKey",
                new Class<?>[]{Recipe.class}, recipe).filter(String.class::isInstance)
                .map(String.class::cast).orElse(null);
    }

    @Nullable
    public static String resolveRecipeTypeSearchKey(@Nullable net.minecraft.resources.ResourceLocation id,
                                                    @Nullable String title) {
        if (id == null) return null;
        return call(new String[]{LEGACY, MAPPING}, "resolveRecipeTypeSearchKey",
                new Class<?>[]{net.minecraft.resources.ResourceLocation.class, String.class}, id, title)
                .filter(String.class::isInstance).map(String.class::cast).orElse(null);
    }

    @Nullable
    public static String deriveSearchKeyFromUnknownRecipe(@Nullable Object recipeBase) {
        if (recipeBase == null) return null;
        return call(new String[]{LEGACY, MAPPING}, "deriveSearchKeyFromUnknownRecipe",
                new Class<?>[]{Object.class}, recipeBase).filter(String.class::isInstance)
                .map(String.class::cast).orElse(null);
    }

    static Class<?> jeiRuntimeCompatClass() {
        return findClass("com.extendedae_plus.compat.JeiRuntimeCompat")
                .or(() -> findClass("com.extendedae_plus.compat.jei.JeiRuntimeCompat"))
                .orElse(null);
    }

    @Nullable
    public static String getJeiRecipeCategorySearchKey(@Nullable Object recipe) {
        return call(new String[]{"com.extendedae_plus.compat.JeiRuntimeCompat",
                        "com.extendedae_plus.compat.jei.JeiRuntimeCompat"},
                "getRecipeCategorySearchKey", new Class<?>[]{Object.class}, recipe)
                .filter(String.class::isInstance).map(String.class::cast).orElse(null);
    }

    public static void setLastProviderSearchKey(String key) {
        invokeVoid(new String[]{LEGACY, MAPPING}, "setLastProviderSearchKey",
                new Class<?>[]{String.class}, key);
    }

    public static boolean addOrUpdateRecipeTypeMapping(String key, String value) {
        return call(new String[]{LEGACY, MAPPING}, "addOrUpdateRecipeTypeMapping",
                new Class<?>[]{String.class, String.class}, key, value).filter(Boolean.class::isInstance)
                .map(Boolean.class::cast).orElse(false);
    }

    public static boolean removeRecipeTypeMapping(String key) {
        return call(new String[]{LEGACY, MAPPING}, "removeRecipeTypeMapping",
                new Class<?>[]{String.class}, key).filter(Boolean.class::isInstance)
                .map(Boolean.class::cast).orElse(false);
    }

    public static void loadRecipeTypeNames() {
        invokeVoid(new String[]{LEGACY, MAPPING}, "loadRecipeTypeNames", new Class<?>[0]);
    }

    public static int removeMappingsByCnValue(String value) {
        if (value == null || value.isBlank()) return 0;
        Optional<Method> legacyRemove = resolveMethod(new String[]{LEGACY}, "removeMappingsByCnValue",
                new Class<?>[]{String.class});
        if (legacyRemove.isPresent()) {
            return invoke(legacyRemove.get(), null, value).filter(Integer.class::isInstance)
                    .map(Integer.class::cast).orElse(0);
        }
        Optional<Object> mappings = call(new String[]{MAPPING}, "getRecipeTypeMappings", new Class<?>[0]);
        if (mappings.isEmpty() || !(mappings.get() instanceof List<?> entries)) return 0;
        int removed = 0;
        for (Object entry : entries) {
            Optional<Object> mappedValue = invokeInstance(entry, "value", new Class<?>[0]);
            Optional<Object> key = invokeInstance(entry, "key", new Class<?>[0]);
            if (key.filter(String.class::isInstance).isPresent()
                    && mappedValue.filter(String.class::isInstance).map(String.class::cast)
                    .filter(value::equals).isPresent()
                    && call(new String[]{MAPPING}, "removeRecipeTypeMapping", new Class<?>[]{String.class}, key.get())
                    .filter(Boolean.class::isInstance).map(Boolean.class::cast).orElse(false)) {
                removed++;
            }
        }
        return removed;
    }

    public static String getProviderDisplayName(@Nullable PatternContainer provider) {
        if (provider == null) return "";
        return call(new String[]{LEGACY, PROVIDER}, "getProviderDisplayNameComponent",
                new Class<?>[]{PatternContainer.class}, provider).filter(Component.class::isInstance)
                .map(Component.class::cast).map(Component::getString).orElse("");
    }

    public static List<PatternContainer> listAvailableProvidersFromGrid(IGrid grid) {
        return call(new String[]{LEGACY, PROVIDER}, "listAvailableProvidersFromGrid",
                new Class<?>[]{IGrid.class}, grid).filter(List.class::isInstance)
                .map(value -> (List<PatternContainer>) value).orElseGet(List::of);
    }

    public static ItemStack insertIntoAccessiblePatternSlots(PatternContainer provider, ItemStack pattern,
                                                              IAEItemFilter filter) {
        return call(new String[]{LEGACY, PROVIDER}, "insertIntoAccessiblePatternSlots",
                new Class<?>[]{PatternContainer.class, ItemStack.class, IAEItemFilter.class}, provider, pattern, filter)
                .filter(ItemStack.class::isInstance).map(ItemStack.class::cast).orElse(pattern);
    }

    public static void recordProviderUpload(ServerPlayer player, long providerId, PatternContainer provider, int slot) {
        invokeVoid(new String[]{LEGACY, PROVIDER}, "recordProviderUpload",
                new Class<?>[]{ServerPlayer.class, long.class, PatternContainer.class, int.class},
                player, providerId, provider, slot);
    }

    public static void recordMatrixUpload(ServerPlayer player, BlockPos pos, String dimension, boolean superMatrix,
                                          int slot) {
        invokeVoid(new String[]{LEGACY, PROVIDER}, "recordMatrixUpload",
                new Class<?>[]{ServerPlayer.class, BlockPos.class, String.class, boolean.class, int.class},
                player, pos, dimension, superMatrix, slot);
    }

    public static boolean uploadPatternToMatrix(ServerPlayer player, ItemStack pattern, IGrid grid) {
        return call(new String[]{LEGACY, MATRIX}, "uploadPatternToMatrix",
                new Class<?>[]{ServerPlayer.class, ItemStack.class, IGrid.class}, player, pattern, grid)
                .filter(Boolean.class::isInstance).map(Boolean.class::cast).orElse(false);
    }

    static boolean readModernUniqueUploadSetting(Class<?> configClass) {
        Optional<Object> value = readPublicStaticField(configClass, "UNIQUE_PROVIDER_AUTO_UPLOAD_ENABLE");
        if (value.isEmpty()) return false;
        return invokeInstance(value.get(), "get", new Class<?>[0]).filter(Boolean.class::isInstance)
                .map(Boolean.class::cast).orElse(false);
    }

    public static boolean effectiveUniqueUploadEnabled() {
        Optional<Class<?>> screen = findClass("com.extendedae_plus.client.screen.ProviderSelectScreen");
        if (screen.isPresent()) {
            Optional<Object> oldSetting = readDeclaredStaticField(screen.get(), "autoUploadUniqueMatchEnabled");
            if (oldSetting.filter(Boolean.class::isInstance).isPresent()) {
                return (Boolean) oldSetting.get();
            }
        }
        Optional<Class<?>> config = findClass("com.extendedae_plus.config.ModConfigs");
        if (config.isEmpty()) return false;
        return readModernUniqueUploadSetting(config.get());
    }

    private static Optional<Object> call(String[] owners, String name, Class<?>[] parameterTypes, Object... args) {
        return resolveMethod(owners, name, parameterTypes).flatMap(method -> invoke(method, null, args));
    }

    private static boolean invokeVoid(String[] owners, String name, Class<?>[] parameterTypes, Object... args) {
        Optional<Method> method = resolveMethod(owners, name, parameterTypes);
        if (method.isEmpty()) return false;
        invoke(method.get(), null, args);
        return true;
    }

    private static Optional<Method> resolveMethod(String[] owners, String name, Class<?>[] parameterTypes) {
        for (String ownerName : owners) {
            Optional<Class<?>> owner = findClass(ownerName);
            if (owner.isEmpty()) continue;
            Optional<Method> method = WcwtReflect.findMethod(owner.get(), name, parameterTypes);
            if (method.isPresent()) return method;
        }
        return Optional.empty();
    }

    /** Misses are cached too: on EAEP 1.6.4+ the legacy class is asked for on every call and never exists. */
    private static final Map<String, Optional<Class<?>>> CLASS_CACHE = new ConcurrentHashMap<>();

    private static Optional<Class<?>> findClass(String name) {
        return CLASS_CACHE.computeIfAbsent(name, className -> {
            try {
                return Optional.of(Class.forName(className, false, PlusPatternUploadApi.class.getClassLoader()));
            } catch (Throwable missing) {
                return Optional.empty();
            }
        });
    }

    private static Optional<Object> readDeclaredStaticField(Class<?> owner, String name) {
        return WcwtReflect.findDeclaredField(owner, name).flatMap(field -> WcwtReflect.readField(null, field));
    }

    private static Optional<Object> readPublicStaticField(Class<?> owner, String name) {
        return WcwtReflect.findField(owner, name).flatMap(field -> WcwtReflect.readField(null, field));
    }

    private static Optional<Object> invokeInstance(Object instance, String name, Class<?>[] parameterTypes) {
        return WcwtReflect.findMethod(instance.getClass(), name, parameterTypes)
                .flatMap(method -> invoke(method, instance));
    }

    private static Optional<Object> invoke(Method method, @Nullable Object target, Object... args) {
        try {
            return Optional.ofNullable(method.invoke(target, args));
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Cannot access EAEP API method " + method, e);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException runtime) throw runtime;
            if (cause instanceof Error error) throw error;
            throw new IllegalStateException("EAEP API method failed: " + method, cause);
        }
    }
}
