package com.lhy.wcwt;

import appeng.api.config.Actionable;
import appeng.api.features.HotkeyAction;
import appeng.api.features.GridLinkables;
import appeng.api.ids.AECreativeTabIds;
import appeng.api.upgrades.Upgrades;
import appeng.core.definitions.AEItems;
import appeng.core.localization.GuiText;
import appeng.hotkeys.HotkeyActions;
import appeng.items.tools.powered.WirelessTerminalItem;
import appeng.items.tools.powered.powersink.PoweredItemCapabilities;
import appeng.menu.locator.MenuLocators;
import com.lhy.wcwt.init.ModComponents;
import com.lhy.wcwt.init.ModItems;
import com.lhy.wcwt.init.ModMenus;
import com.lhy.wcwt.hotkeys.WcwtMagnetHotkeyAction;
import com.lhy.wcwt.hotkeys.WcwtStowHotkeyAction;
import com.lhy.wcwt.item.WirelessComprehensiveWorkTerminalItem;
import com.lhy.wcwt.menu.locator.WcwtToolkitNetworkToolLocator;
import com.lhy.wcwt.menu.WcwtSlotSemantics;
import com.lhy.wcwt.config.WcwtClientConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import com.lhy.wcwt.config.WcwtServerConfig;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.InterModComms;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(WcwtMod.MOD_ID)
public class WcwtMod {
    public static final String MOD_ID = "wcwt";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static final ResourceKey<CreativeModeTab> AE2WTLIB_TAB = ResourceKey.create(
            Registries.CREATIVE_MODE_TAB,
            ResourceLocation.fromNamespaceAndPath("ae2wtlib", "main"));

    public WcwtMod(IEventBus modEventBus, ModContainer modContainer) {
        WcwtSlotSemantics.init();
        ModComponents.DATA_COMPONENTS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModMenus.MENUS.register(modEventBus);
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::addCreativeTabItems);
        modEventBus.addListener(this::registerCapabilities);
        modContainer.registerConfig(ModConfig.Type.CLIENT, WcwtClientConfig.SPEC);
        modContainer.registerConfig(ModConfig.Type.SERVER, WcwtServerConfig.SPEC);
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
                    ModItems.WIRELESS_COMPREHENSIVE_WORK_TERMINAL.get(),
                    (player, locator) -> ((WirelessComprehensiveWorkTerminalItem)
                            ModItems.WIRELESS_COMPREHENSIVE_WORK_TERMINAL.get()).openFromInventory(player, locator),
                    HotkeyAction.WIRELESS_TERMINAL);
            HotkeyActions.register(new WcwtMagnetHotkeyAction(), "ae2wtlib_magnet");
            HotkeyActions.register(new WcwtStowHotkeyAction(), "ae2wtlib_stow");
            registerInventorySorterCompat();

            // 给 WCWT 物品注册兼容的升级卡。
            // 不注册的话 ScrollingUpgradesPanel 的"可用升级"为空，槽位 isItemValid 也会拒绝任何卡。
            //
            // ───── 自定义升级卡列表 ─────
            // 想增减升级卡或调整数量，直接改下面这几行：
            //   Upgrades.add(<升级卡 Item>, wcwt, <最大数量>, groupKey);
            // 升级卡 Item 的取法：
            //   - AE2 原生：appeng.core.definitions.AEItems.<名称>（如 ENERGY_CARD / CRAFTING_CARD / INVERTER_CARD / FUZZY_CARD ...）
            //   - WTLib 的卡（QUANTUM_BRIDGE_CARD / MAGNET_CARD）：用 registerExternalUpgradeCard(...) 按 ResourceLocation 查找。
            //   - ae2importexportcard（输入卡 / 输出卡）：同上；模组未安装时自动跳过。
            // 注意：所有升级卡的最大数量之和不能超过 WirelessComprehensiveWorkTerminalItem.UPGRADE_INVENTORY_SIZE（当前 14），
            //       超出会导致后注册的升级显示不全。改这个上限去 WirelessComprehensiveWorkTerminalItem 里改 UPGRADE_INVENTORY_SIZE 常量。
            var wcwt = ModItems.WIRELESS_COMPREHENSIVE_WORK_TERMINAL.get();
            String groupKey = GuiText.WirelessTerminals.getTranslationKey();
            Upgrades.add(AEItems.ENERGY_CARD, wcwt, 10, groupKey);                              // 能源卡 ×10
            registerExternalUpgradeCard(wcwt, "ae2wtlib", "quantum_bridge_card", 1, groupKey, false); // 量子桥卡 ×1
            registerExternalUpgradeCard(wcwt, "ae2wtlib", "magnet_card", 1, groupKey, false); // 磁力卡 ×1
            registerExternalUpgradeCard(wcwt, "ae2importexportcard", "import_card", 1, groupKey, true); // 输入卡 ×1
            registerExternalUpgradeCard(wcwt, "ae2importexportcard", "export_card", 1, groupKey, true); // 输出卡 ×1
        });
    }

    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerItem(Capabilities.EnergyStorage.ITEM,
                (stack, context) -> new PoweredItemCapabilities(stack,
                        (WirelessComprehensiveWorkTerminalItem) ModItems.WIRELESS_COMPREHENSIVE_WORK_TERMINAL.get()),
                ModItems.WIRELESS_COMPREHENSIVE_WORK_TERMINAL.get());
    }

    /**
     * @param quietIfMissing 为 true 时物品缺失只打 debug（可选兼容模组）；为 false 时打 warn（预期存在的资源）。
     */
    private static void registerExternalUpgradeCard(Item host, String namespace, String path, int max, String groupKey,
            boolean quietIfMissing) {
        var card = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(namespace, path));
        if (card != null && card != Items.AIR) {
            Upgrades.add(card, host, max, groupKey);
            LOGGER.info("Registered compatible upgrade '{}:{}' (×{}) for WCWT", namespace, path, max);
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
            LOGGER.info("Registered InvTweaksRefoxed screen blacklist for WCWT");
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
}
