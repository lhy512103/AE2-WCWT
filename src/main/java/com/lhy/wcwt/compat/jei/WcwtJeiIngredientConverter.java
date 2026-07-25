package com.lhy.wcwt.compat.jei;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.ingredients.ITypedIngredient;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;

/**
 * JEI公開APIの材料をAE2のキーへ変換する。
 *
 * <p>このクラスへJEI内部GUI型を追加してはいけない。TMRV環境でもEAEP経由で
 * 安全にロードできることが、この互換層の前提になる。</p>
 */
final class WcwtJeiIngredientConverter {
    // ブックマーク注文ではアイテム種類だけを指定するため、個数を1へ正規化する。
    private static final int BOOKMARK_ITEM_COUNT = 1;
    // 空のChemicalStackをAE2へ渡さないために保証する最小量。
    private static final long MINIMUM_CHEMICAL_AMOUNT = 1L;

    private WcwtJeiIngredientConverter() {
    }

    @Nullable
    static GenericStack toGenericStack(@Nullable ITypedIngredient<?> ingredient) {
        // マウス下に材料がない場合は変換対象もない。
        if (ingredient == null) {
            return null;
        }

        try {
            GenericStack converted = convertWithAe2JeiIntegration(ingredient);
            // AE2のJEI変換器が対応する材料なら、その結果を優先する。
            if (converted != null) {
                return normalizeItemAmount(converted);
            }

            Object raw = ingredient.getIngredient();
            // バニラアイテムはブックマーク注文用に1個へ正規化する。
            if (raw instanceof ItemStack stack && !stack.isEmpty()) {
                return GenericStack.fromItemStack(stack.copyWithCount(BOOKMARK_ITEM_COUNT));
            }
            // Forge液体はJEIが示した量を維持してAE2へ渡す。
            if (raw instanceof FluidStack fluid && !fluid.isEmpty()) {
                return GenericStack.fromFluidStack(fluid.copy());
            }
            return convertMekanismChemical(raw);
        } catch (RuntimeException | LinkageError ignored) {
            // 代替ビューアや連携MODの型が想定外でもクリック処理を停止させない。
            return null;
        }
    }

    private static GenericStack normalizeItemAmount(GenericStack stack) {
        // アイテムだけを1個へ正規化し、液体とChemicalの表示量は維持する。
        if (stack.what() instanceof AEItemKey) {
            return new GenericStack(stack.what(), BOOKMARK_ITEM_COUNT);
        }
        return stack;
    }

    @Nullable
    private static GenericStack convertWithAe2JeiIntegration(ITypedIngredient<?> ingredient) {
        try {
            Class<?> convertersClass =
                    Class.forName("tamaized.ae2jeiintegration.api.integrations.jei.IngredientConverters");
            Method getConverter = convertersClass.getMethod("getConverter", IIngredientType.class);
            Object converter = getConverter.invoke(null, ingredient.getType());
            // AE2 JEI Integrationがこの材料型を扱わない場合は標準変換へ戻す。
            if (converter == null) {
                return null;
            }
            Method getStackFromIngredient = converter.getClass().getMethod("getStackFromIngredient", Object.class);
            Object converted = getStackFromIngredient.invoke(converter, ingredient.getIngredient());
            return converted instanceof GenericStack stack ? stack : null;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            // AE2 JEI Integrationは任意依存なので、未導入時は標準変換へ戻す。
            return null;
        }
    }

    @Nullable
    private static GenericStack convertMekanismChemical(Object raw) {
        try {
            Class<?> chemicalStackClass = Class.forName("mekanism.api.chemical.ChemicalStack");
            // JEI材料がMekanism Chemicalでなければ、この任意連携では処理しない。
            if (!chemicalStackClass.isInstance(raw)) {
                return null;
            }
            Class<?> keyClass = Class.forName("me.ramidzkh.mekae2.ae2.MekanismKey");
            Method of = keyClass.getMethod("of", chemicalStackClass);
            Object key = of.invoke(null, raw);
            // Applied MekanisticsがAE2キーへ変換できなかった場合は注文対象にしない。
            if (!(key instanceof appeng.api.stacks.AEKey aeKey)) {
                return null;
            }
            Method getAmount = chemicalStackClass.getMethod("getAmount");
            long amount = ((Number) getAmount.invoke(raw)).longValue();
            return new GenericStack(aeKey, Math.max(MINIMUM_CHEMICAL_AMOUNT, amount));
        } catch (ReflectiveOperationException | LinkageError ignored) {
            // MekanismまたはApplied Mekanisticsは任意依存なので、未導入時は変換しない。
            return null;
        }
    }
}
