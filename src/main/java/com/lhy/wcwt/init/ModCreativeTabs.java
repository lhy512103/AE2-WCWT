package com.lhy.wcwt.init;

import appeng.api.config.Actionable;
import com.lhy.wcwt.WcwtMod;
import com.lhy.wcwt.item.WirelessComprehensiveWorkTerminalItem;
import com.lhy.wcwt.universal.WcwtItemIds;
import com.lhy.wcwt.universal.WcwtUniversalTerminals;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {
    public static final ResourceKey<CreativeModeTab> WCWT_TAB_KEY = ResourceKey.create(
            Registries.CREATIVE_MODE_TAB,
            ResourceLocation.fromNamespaceAndPath(WcwtMod.MOD_ID, "main"));

    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, WcwtMod.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> WCWT_TAB = CREATIVE_TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.wcwt.main"))
                    .icon(ModCreativeTabs::chargedTerminalStack)
                    .displayItems((parameters, output) -> addWcwtItems(output))
                    .build());

    private ModCreativeTabs() {
    }

    public static void addWcwtItems(CreativeModeTab.Output output) {
        output.accept(new ItemStack(ModItems.WIRELESS_COMPREHENSIVE_WORK_TERMINAL.get()));
        output.accept(chargedTerminalStack());

        var universal = chargedUniversalTerminalStack();
        if (!universal.isEmpty()) {
            output.accept(universal);
        }

        output.accept(ModItems.ADVANCED_CODING_CARD);
        output.accept(ModItems.COSMETIC_ARMOR_CARD);
        output.accept(ModItems.CURIOS_CARD);
        output.accept(ModItems.TOOL_SLOTS_BOX_CARD);
        output.accept(ModItems.TOOLKIT_CARD);
        output.accept(ModItems.RESONATING_LIGHTNING_PATTERN_CODING_CARD);
    }

    private static ItemStack chargedTerminalStack() {
        var terminal = (WirelessComprehensiveWorkTerminalItem) ModItems.WIRELESS_COMPREHENSIVE_WORK_TERMINAL.get();
        var stack = new ItemStack(terminal);
        terminal.injectAEPower(stack, terminal.getAEMaxPower(stack), Actionable.MODULATE);
        return stack;
    }

    private static ItemStack chargedUniversalTerminalStack() {
        var terminal = (WirelessComprehensiveWorkTerminalItem) ModItems.WIRELESS_COMPREHENSIVE_WORK_TERMINAL.get();
        var stack = new ItemStack(terminal);
        for (ResourceLocation id : WcwtItemIds.MERGEABLE_TERMINALS) {
            var item = BuiltInRegistries.ITEM.get(id);
            if (item != Items.AIR) {
                WcwtUniversalTerminals.addTerminal(stack, new ItemStack(item));
            }
        }
        if (!WcwtUniversalTerminals.isUniversal(stack)) {
            return ItemStack.EMPTY;
        }
        terminal.injectAEPower(stack, terminal.getAEMaxPower(stack), Actionable.MODULATE);
        return stack;
    }
}
