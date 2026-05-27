package com.lhy.wcwt.compat.jei;

import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import com.lhy.wcwt.WcwtMod;
import com.lhy.wcwt.client.WirelessComprehensiveWorkTerminalScreen;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.runtime.IJeiRuntime;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@JeiPlugin
public class WcwtJeiPlugin implements IModPlugin {
    private static final ResourceLocation UID =
            ResourceLocation.fromNamespaceAndPath(WcwtMod.MOD_ID, "jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGuiContainerHandler(WirelessComprehensiveWorkTerminalScreen.class,
                new IGuiContainerHandler<>() {
                    @Override
                    public List<Rect2i> getGuiExtraAreas(WirelessComprehensiveWorkTerminalScreen screen) {
                        return screen.getExclusionZones();
                    }
                });
        registration.addGhostIngredientHandler(WirelessComprehensiveWorkTerminalScreen.class,
                new CellConfigGhostHandler());
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        registration.addRecipeTransferHandler(
                new WcwtCraftingRecipeTransferHandler(registration.getTransferHelper()),
                RecipeTypes.CRAFTING);
        registration.addUniversalRecipeTransferHandler(new WcwtRecipeTransferHandler(
                registration.getTransferHelper()));
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        WcwtJeiBookmarkKeys.setRuntime(jeiRuntime);
    }

    @Override
    public void onRuntimeUnavailable() {
        WcwtJeiBookmarkKeys.setRuntime(null);
    }

    private static class CellConfigGhostHandler
            implements IGhostIngredientHandler<WirelessComprehensiveWorkTerminalScreen> {

        @Override
        public <I> List<Target<I>> getTargetsTyped(WirelessComprehensiveWorkTerminalScreen screen,
                                                   ITypedIngredient<I> ingredient,
                                                   boolean doStart) {
            var panel = screen.getAdvancedCodingPanel();
            if (panel == null || !panel.isVisible()) {
                return List.of();
            }

            // 把 JEI 的 ITypedIngredient 转换成 AEKey（同时支持物品/流体）。
            // 以前只接 getItemStack() → 流体被忽略。改用通用 toAEKey()。
            AEKey key = toAEKey(ingredient);
            if (key == null) {
                return List.of();
            }

            var targets = new ArrayList<Target<I>>();
            targets.add(new CellConfigTarget<>(panel.getReplaceInputArea(),
                    accepted -> panel.setReplaceInputFromKey(key)));
            targets.add(new CellConfigTarget<>(panel.getReplaceOutputArea(),
                    accepted -> panel.setReplaceOutputFromKey(key)));

            int total = panel.getCellConfigRows() * panel.getCellConfigCols();
            int firstSlot = panel.getCellConfigScroll() * panel.getCellConfigCols();
            for (int i = 0; i < total; i++) {
                int slotIdx = firstSlot + i;
                var area = panel.getCellConfigSlotArea(slotIdx);
                targets.add(new CellConfigTarget<>(area,
                        accepted -> panel.setCellConfigSlotFromKey(slotIdx, key)));
            }
            return targets;
        }

        @Override
        public void onComplete() {
        }
    }

    @Nullable
    private static AEKey toAEKey(ITypedIngredient<?> ingredient) {
        Object raw = ingredient.getIngredient();
        if (raw instanceof net.minecraft.world.item.ItemStack stack && !stack.isEmpty()) {
            return AEItemKey.of(stack);
        }
        if (raw instanceof FluidStack fluid && !fluid.isEmpty()) {
            return AEFluidKey.of(fluid);
        }
        if (raw instanceof SizedFluidIngredient sized) {
            for (var fs : sized.getFluids()) {
                if (!fs.isEmpty()) return AEFluidKey.of(fs);
            }
        }
        return null;
    }

    private record CellConfigTarget<I>(Rect2i area, java.util.function.Consumer<I> acceptor)
            implements IGhostIngredientHandler.Target<I> {
        @Override
        public Rect2i getArea() {
            return area;
        }

        @Override
        public void accept(I ingredient) {
            acceptor.accept(ingredient);
        }
    }
}
