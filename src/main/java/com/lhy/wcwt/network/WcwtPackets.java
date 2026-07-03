package com.lhy.wcwt.network;

public class WcwtPackets {

    private WcwtPackets() {
    }

    public static void register() {
        int id = 0;

        ModNetworking.registerServerbound(id++, ExtendedUIPacket.class, ExtendedUIPacket.TYPE,
                ExtendedUIPacket.STREAM_CODEC, ExtendedUIPacket::handle);
        ModNetworking.registerServerbound(id++, CraftingLockPacket.class, CraftingLockPacket.TYPE,
                CraftingLockPacket.STREAM_CODEC, CraftingLockPacket::handle);
        ModNetworking.registerServerbound(id++, PatternSelectionPacket.class, PatternSelectionPacket.TYPE,
                PatternSelectionPacket.STREAM_CODEC, PatternSelectionPacket::handle);
        ModNetworking.registerServerbound(id++, PatternMultiplierPacket.class, PatternMultiplierPacket.TYPE,
                PatternMultiplierPacket.STREAM_CODEC, PatternMultiplierPacket::handle);
        ModNetworking.registerServerbound(id++, PatternModePacket.class, PatternModePacket.TYPE,
                PatternModePacket.STREAM_CODEC, PatternModePacket::handle);
        ModNetworking.registerServerbound(id++, ManualWorkspaceModePacket.class, ManualWorkspaceModePacket.TYPE,
                ManualWorkspaceModePacket.STREAM_CODEC, ManualWorkspaceModePacket::handle);
        ModNetworking.registerServerbound(id++, ManualAnvilNamePacket.class, ManualAnvilNamePacket.TYPE,
                ManualAnvilNamePacket.STREAM_CODEC, ManualAnvilNamePacket::handle);
        ModNetworking.registerServerbound(id++, EncodePatternPacket.class, EncodePatternPacket.TYPE,
                EncodePatternPacket.STREAM_CODEC, EncodePatternPacket::handle);
        ModNetworking.registerServerbound(id++, PatternEncodingModePacket.class, PatternEncodingModePacket.TYPE,
                PatternEncodingModePacket.STREAM_CODEC, PatternEncodingModePacket::handle);
        ModNetworking.registerServerbound(id++, PatternEncodingOptionPacket.class, PatternEncodingOptionPacket.TYPE,
                PatternEncodingOptionPacket.STREAM_CODEC, PatternEncodingOptionPacket::handle);
        ModNetworking.registerServerbound(id++, StonecuttingRecipeSelectionPacket.class,
                StonecuttingRecipeSelectionPacket.TYPE, StonecuttingRecipeSelectionPacket.STREAM_CODEC,
                StonecuttingRecipeSelectionPacket::handle);
        ModNetworking.registerServerbound(id++, CycleProcessingOutputPacket.class, CycleProcessingOutputPacket.TYPE,
                CycleProcessingOutputPacket.STREAM_CODEC, CycleProcessingOutputPacket::handle);
        ModNetworking.registerServerbound(id++, TopActionPacket.class, TopActionPacket.TYPE,
                TopActionPacket.STREAM_CODEC, TopActionPacket::handle);
        ModNetworking.registerServerbound(id++, WirelessSettingsPacket.class, WirelessSettingsPacket.TYPE,
                WirelessSettingsPacket.STREAM_CODEC, WirelessSettingsPacket::handle);
        ModNetworking.registerServerbound(id++, WcwtPickBlockPacket.class, WcwtPickBlockPacket.TYPE,
                WcwtPickBlockPacket.STREAM_CODEC, WcwtPickBlockPacket::handle);
        ModNetworking.registerServerbound(id++, WcwtJeiBookmarkOrderPacket.class, WcwtJeiBookmarkOrderPacket.TYPE,
                WcwtJeiBookmarkOrderPacket.STREAM_CODEC, WcwtJeiBookmarkOrderPacket::handle);
        ModNetworking.registerClientbound(id++, WcwtRestockAmountsPacket.class, WcwtRestockAmountsPacket.TYPE,
                WcwtRestockAmountsPacket.STREAM_CODEC, WcwtRestockAmountsPacket::handle);
        ModNetworking.registerClientbound(id++, WcwtUpdateRestockPacket.class, WcwtUpdateRestockPacket.TYPE,
                WcwtUpdateRestockPacket.STREAM_CODEC, WcwtUpdateRestockPacket::handle);
        ModNetworking.registerServerbound(id++, JeiCraftingTransferPacket.class, JeiCraftingTransferPacket.TYPE,
                JeiCraftingTransferPacket.STREAM_CODEC, JeiCraftingTransferPacket::handle);
        ModNetworking.registerServerbound(id++, WcwtPullRecipeInputsPacket.class, WcwtPullRecipeInputsPacket.TYPE,
                WcwtPullRecipeInputsPacket.STREAM_CODEC, WcwtPullRecipeInputsPacket::handle);
        ModNetworking.registerServerbound(id++, DirectionChangePacket.class, DirectionChangePacket.TYPE,
                DirectionChangePacket.STREAM_CODEC, DirectionChangePacket::handle);
        ModNetworking.registerServerbound(id++, CopyPatternPacket.class, CopyPatternPacket.TYPE,
                CopyPatternPacket.STREAM_CODEC, CopyPatternPacket::handle);
        ModNetworking.registerServerbound(id++, ReplacePatternPacket.class, ReplacePatternPacket.TYPE,
                ReplacePatternPacket.STREAM_CODEC, ReplacePatternPacket::handle);
        ModNetworking.registerServerbound(id++, CellWorkbenchActionPacket.class, CellWorkbenchActionPacket.TYPE,
                CellWorkbenchActionPacket.STREAM_CODEC, CellWorkbenchActionPacket::handle);
        ModNetworking.registerServerbound(id++, CellConfigSetPacket.class, CellConfigSetPacket.TYPE,
                CellConfigSetPacket.STREAM_CODEC, CellConfigSetPacket::handle);
        ModNetworking.registerServerbound(id++, PatternProviderListPacket.Request.class,
                PatternProviderListPacket.Request.TYPE, PatternProviderListPacket.Request.STREAM_CODEC,
                PatternProviderListPacket.Request::handle);
        ModNetworking.registerClientbound(id++, PatternProviderListPacket.class, PatternProviderListPacket.TYPE,
                PatternProviderListPacket.STREAM_CODEC, PatternProviderListPacket::handle);
        ModNetworking.registerClientbound(id++, PatternProviderFocusPacket.class, PatternProviderFocusPacket.TYPE,
                PatternProviderFocusPacket.STREAM_CODEC, PatternProviderFocusPacket::handle);
        ModNetworking.registerClientbound(id++, OpenEaepProviderSelectScreenPacket.class,
                OpenEaepProviderSelectScreenPacket.TYPE, OpenEaepProviderSelectScreenPacket.STREAM_CODEC,
                OpenEaepProviderSelectScreenPacket::handle);
        ModNetworking.registerServerbound(id++, PatternManagementActionPacket.class,
                PatternManagementActionPacket.TYPE, PatternManagementActionPacket.STREAM_CODEC,
                PatternManagementActionPacket::handle);
        ModNetworking.registerServerbound(id++, PatternManagementUploadSettingPacket.class,
                PatternManagementUploadSettingPacket.TYPE, PatternManagementUploadSettingPacket.STREAM_CODEC,
                PatternManagementUploadSettingPacket::handle);
        ModNetworking.registerServerbound(id++, ToolkitNetworkToolDepositPacket.class,
                ToolkitNetworkToolDepositPacket.TYPE, ToolkitNetworkToolDepositPacket.STREAM_CODEC,
                ToolkitNetworkToolDepositPacket::handle);
        ModNetworking.registerServerbound(id++, ToolkitMemorySlotPacket.class,
                ToolkitMemorySlotPacket.TYPE, ToolkitMemorySlotPacket.STREAM_CODEC,
                ToolkitMemorySlotPacket::handle);
        ModNetworking.registerServerbound(id++, OpenTerminalHotkeyPacket.class, OpenTerminalHotkeyPacket.TYPE,
                OpenTerminalHotkeyPacket.STREAM_CODEC, OpenTerminalHotkeyPacket::handle);
        ModNetworking.registerServerbound(id++, OpenToolkitHotkeyPacket.class, OpenToolkitHotkeyPacket.TYPE,
                OpenToolkitHotkeyPacket.STREAM_CODEC, OpenToolkitHotkeyPacket::handle);
        ModNetworking.registerServerbound(id++, ResonatingLightningPatternActionPacket.class,
                ResonatingLightningPatternActionPacket.TYPE, ResonatingLightningPatternActionPacket.STREAM_CODEC,
                ResonatingLightningPatternActionPacket::handle);
        ModNetworking.registerServerbound(id++, SwitchUniversalTerminalPacket.class,
                SwitchUniversalTerminalPacket.TYPE, SwitchUniversalTerminalPacket.STREAM_CODEC,
                SwitchUniversalTerminalPacket::handle);
        ModNetworking.registerServerbound(id++, SplitUniversalTerminalPacket.class,
                SplitUniversalTerminalPacket.TYPE, SplitUniversalTerminalPacket.STREAM_CODEC,
                SplitUniversalTerminalPacket::handle);
    }
}
