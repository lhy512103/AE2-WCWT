package com.lhy.wcwt;

import appeng.api.config.Actionable;
import appeng.api.features.GridLinkables;
import appeng.api.features.HotkeyAction;
import appeng.api.ids.AECreativeTabIds;
import appeng.api.upgrades.Upgrades;
import appeng.core.definitions.AEItems;
import appeng.core.localization.GuiText;
import appeng.hotkeys.HotkeyActions;
import appeng.hotkeys.InventoryHotkeyAction;
import appeng.items.tools.powered.WirelessTerminalItem;
import appeng.menu.locator.MenuLocators;
import com.lhy.wcwt.config.WcwtClientConfig;
import com.lhy.wcwt.config.WcwtServerConfig;
import com.lhy.wcwt.hotkeys.WcwtMagnetHotkeyAction;
import com.lhy.wcwt.init.ModItems;
import com.lhy.wcwt.init.ModMenus;
import com.lhy.wcwt.item.WirelessComprehensiveWorkTerminalItem;
import com.lhy.wcwt.menu.WcwtSlotSemantics;
import com.lhy.wcwt.menu.locator.WcwtToolkitNetworkToolLocator;
import com.lhy.wcwt.util.ResourceLocationCompat;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.InterModComms;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(WcwtMod.MOD_ID)
public class WcwtMod {
    public static final String MOD_ID = "wcwt";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final ResourceKey<CreativeModeTab> AE2WTLIB_TAB = ResourceKey.create(
            Registries.CREATIVE_MODE_TAB,
            ResourceLocationCompat.id("ae2wtlib", "main"));

    @SuppressWarnings("removal")
    public WcwtMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        WcwtSlotSemantics.init();
        ModItems.ITEMS.register(modEventBus);
        ModMenus.MENUS.register(modEventBus);

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::addCreativeTabItems);
        MinecraftForge.EVENT_BUS.register(WcwtPacketsBootstrap.class);

        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, WcwtServerConfig.SPEC);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, WcwtClientConfig.SPEC);
        });
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            MenuLocators.register(
                    WcwtToolkitNetworkToolLocator.class,
                    WcwtToolkitNetworkToolLocator::writeToPacket,
                    WcwtToolkitNetworkToolLocator::readFromPacket);
            GridLinkables.register(
                    ModItems.WIRELESS_COMPREHENSIVE_WORK_TERMINAL.get(),
                    WirelessTerminalItem.LINKABLE_HANDLER);
            HotkeyActions.register(
                    new InventoryHotkeyAction(
                            ModItems.WIRELESS_COMPREHENSIVE_WORK_TERMINAL.get(),
                            ((WirelessComprehensiveWorkTerminalItem) ModItems.WIRELESS_COMPREHENSIVE_WORK_TERMINAL.get())::openFromInventory),
                    HotkeyAction.WIRELESS_TERMINAL);
            HotkeyActions.register(new WcwtMagnetHotkeyAction(), "ae2wtlib_magnet");
            registerInventorySorterCompat();

            var wcwt = ModItems.WIRELESS_COMPREHENSIVE_WORK_TERMINAL.get();
            String groupKey = GuiText.WirelessTerminals.getTranslationKey();
            Upgrades.add(AEItems.ENERGY_CARD, wcwt, 10, groupKey);
            registerExternalUpgradeCard(wcwt, "ae2wtlib", "quantum_bridge_card", 1, groupKey, false);
            registerExternalUpgradeCard(wcwt, "ae2wtlib", "magnet_card", 1, groupKey, false);
            registerExternalUpgradeCard(wcwt, "ae2importexportcard", "import_card", 1, groupKey, true);
            registerExternalUpgradeCard(wcwt, "ae2importexportcard", "export_card", 1, groupKey, true);
        });
    }

    private static void registerExternalUpgradeCard(Item host, String namespace, String path, int max, String groupKey,
            boolean quietIfMissing) {
        var card = BuiltInRegistries.ITEM.get(ResourceLocationCompat.id(namespace, path));
        if (card != null && card != Items.AIR) {
            Upgrades.add(card, host, max, groupKey);
            LOGGER.info("Registered compatible upgrade '{}:{}' (x{}) for WCWT", namespace, path, max);
        } else if (quietIfMissing) {
            LOGGER.debug("Optional upgrade '{}:{}' not present; skipping", namespace, path);
        } else {
            LOGGER.warn("Upgrade '{}:{}' not found at runtime; skipping registration", namespace, path);
        }
    }

    private static void registerInventorySorterCompat() {
        if (ModList.get().isLoaded("invtweaks")) {
            InterModComms.sendTo("invtweaks", "blacklist-screen",
                    () -> "com.lhy.wcwt.client.WirelessComprehensiveWorkTerminalScreen");
        }
    }

    private void addCreativeTabItems(BuildCreativeModeTabContentsEvent event) {
        if (AECreativeTabIds.MAIN.equals(event.getTabKey()) || AE2WTLIB_TAB.equals(event.getTabKey())) {
            var terminal = (WirelessComprehensiveWorkTerminalItem) ModItems.WIRELESS_COMPREHENSIVE_WORK_TERMINAL.get();
            event.accept(new ItemStack(terminal));

            var charged = new ItemStack(terminal);
            terminal.injectAEPower(charged, terminal.getAEMaxPower(charged), Actionable.MODULATE);
            event.accept(charged);
        }
    }

    public static final class WcwtPacketsBootstrap {
        @net.minecraftforge.eventbus.api.SubscribeEvent
        public static void register(net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent event) {
            event.enqueueWork(com.lhy.wcwt.network.WcwtPackets::register);
        }
    }
}
