package com.lhy.wcwt.compat;

import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.crafting.pattern.AEProcessingPattern;
import com.lhy.wcwt.helpers.WirelessComprehensiveWorkTerminalMenuHost;
import com.lhy.wcwt.menu.WirelessComprehensiveWorkTerminalMenu;
import com.lhy.wcwt.pull.WcwtIngredientPriorities;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** Adapts the 2.4.6 terminal flow using the upstream catalog, encoder and uploader. */
public final class UselessModCompat {
    private UselessModCompat() {}

    public record Selection(ResourceLocation recipeId, String fingerprint, String sourceId) {}
    public record Transfer(Selection selection, List<GenericStack> inputs, List<GenericStack> outputs) {}
    public enum UploadResult { SKIPPED, UPLOADED, DUPLICATE }

    public static boolean available() {
        return ModList.get().getModContainerById("useless_mod")
                .map(mod -> mod.getModInfo().getVersion().compareTo(
                        new org.apache.maven.artifact.versioning.DefaultArtifactVersion("1.21.1-2.4.6")) >= 0)
                .orElse(false);
    }

    public static boolean isRecipe(Object recipe) {
        return available() && Integration.isRecipe(recipe);
    }

    public static @Nullable Transfer transfer(Object recipe, WirelessComprehensiveWorkTerminalMenu menu) {
        return available() ? Integration.transfer(recipe, menu) : null;
    }

    public static ItemStack encode(ItemStack pattern, Selection selection, Level level) {
        return available() ? Integration.encode(pattern, selection, level) : ItemStack.EMPTY;
    }

    public static UploadResult upload(WirelessComprehensiveWorkTerminalMenuHost host, ItemStack pattern,
                                      ServerPlayer player) {
        return available() ? Integration.upload(host, pattern, player) : UploadResult.SKIPPED;
    }

    // Keep optional-mod types out of the facade's signatures and constant pool resolution path.
    private static final class Integration {
        private static boolean isRecipe(Object recipe) {
            return recipe instanceof com.sorrowmist.useless.content.recipe.AlloyFurnaceRecipeCatalog.Entry;
        }

        private static @Nullable Transfer transfer(Object selected, WirelessComprehensiveWorkTerminalMenu menu) {
            if (!(selected instanceof com.sorrowmist.useless.content.recipe.AlloyFurnaceRecipeCatalog.Entry entry)) {
                return null;
            }
            var recipe = entry.recipe();
            List<GenericStack> inputs = new ArrayList<>();
            var priorities = WcwtIngredientPriorities.createContext(menu, java.util.Map.of());
            for (var input : recipe.inputs()) {
                List<GenericStack> options = new ArrayList<>();
                for (var item : input.ingredient().getItems()) {
                    var key = AEItemKey.of(item);
                    if (key != null) options.add(new GenericStack(key, input.count()));
                }
                if (options.isEmpty()) {
                    var representative = com.sorrowmist.useless.content.recipe.AdapterUtils
                            .itemRepresentative(input.ingredient());
                    var key = representative == null ? null : AEItemKey.of(representative);
                    if (key != null) options.add(new GenericStack(key, input.count()));
                }
                var chosen = WcwtIngredientPriorities.chooseBestGenericStack(priorities, options);
                if (chosen == null || chosen.amount() <= 0) return null;
                inputs.add(chosen);
            }
            for (var input : recipe.inputFluids()) {
                List<GenericStack> options = new ArrayList<>();
                for (var fluid : input.getRepresentatives()) {
                    if (!fluid.isEmpty()) options.add(new GenericStack(AEFluidKey.of(fluid), input.amount()));
                }
                if (options.isEmpty()) {
                    var fluid = com.sorrowmist.useless.content.recipe.AdapterUtils
                            .fluidRepresentative(input.ingredient(), 1);
                    if (fluid != null && !fluid.isEmpty()) {
                        options.add(new GenericStack(AEFluidKey.of(fluid), input.amount()));
                    }
                }
                var chosen = WcwtIngredientPriorities.chooseBestGenericStack(priorities, options);
                if (chosen == null || chosen.amount() <= 0) return null;
                inputs.add(chosen);
            }
            inputs.addAll(recipe.keyInputs());
            List<GenericStack> outputs = new ArrayList<>();
            for (var item : recipe.outputs()) outputs.add(GenericStack.fromItemStack(item));
            for (var fluid : recipe.outputFluids()) outputs.add(GenericStack.fromFluidStack(fluid));
            outputs.addAll(recipe.keyOutputs());
            if (inputs.isEmpty() || outputs.isEmpty()
                    || inputs.stream().anyMatch(s -> s == null || s.amount() <= 0)
                    || outputs.stream().anyMatch(s -> s == null || s.amount() <= 0)) return null;
            var identity = new Selection(recipe.id(),
                    com.sorrowmist.useless.content.recipe.AlloyFurnaceRecipeFingerprint.create(
                            recipe, menu.getPlayer().registryAccess()), entry.sourceId());
            return new Transfer(identity, inputs, outputs);
        }

        private static ItemStack encode(ItemStack pattern, Selection selection, Level level) {
            var details = PatternDetailsHelper.decodePattern(pattern, level);
            if (!(details instanceof AEProcessingPattern)) return ItemStack.EMPTY;
            var identity = new com.sorrowmist.useless.content.recipe.AlloyFurnaceRecipeIdentity(
                    selection.recipeId(), selection.fingerprint());
            var entry = com.sorrowmist.useless.content.recipe.AlloyFurnaceRecipeCatalog.resolvePattern(
                    level, selection.sourceId(), identity, details);
            if (entry.isEmpty() || !com.sorrowmist.useless.content.recipe.AlloyFurnaceRecipeCatalog.matchesRecipe(
                    level, selection.sourceId(), entry.get().recipe(), details)) return ItemStack.EMPTY;
            if (details instanceof com.sorrowmist.useless.content.machines.advanced_alloy_furnace.ae.ProcessingPatternRecipeHolder holder) {
                holder.uselessMod$setRecipeIdentity(identity);
            }
            return com.sorrowmist.useless.content.machines.advanced_alloy_furnace.ae.OmniversalPatternEncoding
                    .encode(pattern, details, entry.get(), level);
        }

        private static UploadResult upload(WirelessComprehensiveWorkTerminalMenuHost host, ItemStack pattern,
                                           ServerPlayer player) {
            if (!com.sorrowmist.useless.core.config.ConfigManager.isOmniversalPatternAutoUploadEnabled()) {
                return UploadResult.SKIPPED;
            }
            var result = com.sorrowmist.useless.content.machines.advanced_alloy_furnace.ae.OmniversalPatternUploader
                    .upload(host, pattern);
            return switch (result.outcome()) {
                case UPLOADED -> {
                    com.sorrowmist.useless.content.machines.advanced_alloy_furnace.ae.PatternUploadNotice
                            .notifyUploaded(player, result.target());
                    yield UploadResult.UPLOADED;
                }
                case DUPLICATE -> {
                    com.sorrowmist.useless.content.machines.advanced_alloy_furnace.ae.PatternUploadNotice
                            .notifyDuplicate(player);
                    yield UploadResult.DUPLICATE;
                }
                case SKIPPED -> UploadResult.SKIPPED;
            };
        }
    }
}
