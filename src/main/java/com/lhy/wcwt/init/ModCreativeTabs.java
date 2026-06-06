package com.lhy.wcwt.init;

import com.lhy.wcwt.WcwtMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, WcwtMod.MOD_ID);

    public static final RegistryObject<CreativeModeTab> MAIN =
            CREATIVE_MODE_TABS.register("main", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.wcwt"))
                    .icon(WcwtMod::chargedTerminalStack)
                    .displayItems((parameters, output) -> WcwtMod.acceptTerminalVariants(output))
                    .build());

    private ModCreativeTabs() {
    }
}
