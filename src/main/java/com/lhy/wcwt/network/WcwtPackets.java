package com.lhy.wcwt.network;

import com.lhy.wcwt.WcwtMod;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * 网络数据包注册类
 */
@EventBusSubscriber(modid = WcwtMod.MOD_ID)
public class WcwtPackets {
    
    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(WcwtMod.MOD_ID);
        
        // 注册各种数据包
        registrar.playToServer(
            ExtendedUIPacket.TYPE,
            ExtendedUIPacket.STREAM_CODEC,
            ExtendedUIPacket::handle
        );
        
        registrar.playToServer(
            CraftingLockPacket.TYPE,
            CraftingLockPacket.STREAM_CODEC,
            CraftingLockPacket::handle
        );
        
        registrar.playToServer(
            PatternSelectionPacket.TYPE,
            PatternSelectionPacket.STREAM_CODEC,
            PatternSelectionPacket::handle
        );
        
        registrar.playToServer(
            PatternMultiplierPacket.TYPE,
            PatternMultiplierPacket.STREAM_CODEC,
            PatternMultiplierPacket::handle
        );

        registrar.playToServer(
            PatternModePacket.TYPE,
            PatternModePacket.STREAM_CODEC,
            PatternModePacket::handle
        );

        registrar.playToServer(
            EncodePatternPacket.TYPE,
            EncodePatternPacket.STREAM_CODEC,
            EncodePatternPacket::handle
        );

        registrar.playToServer(
            PatternEncodingModePacket.TYPE,
            PatternEncodingModePacket.STREAM_CODEC,
            PatternEncodingModePacket::handle
        );

        registrar.playToServer(
            PatternEncodingOptionPacket.TYPE,
            PatternEncodingOptionPacket.STREAM_CODEC,
            PatternEncodingOptionPacket::handle
        );

        registrar.playToServer(
            StonecuttingRecipeSelectionPacket.TYPE,
            StonecuttingRecipeSelectionPacket.STREAM_CODEC,
            StonecuttingRecipeSelectionPacket::handle
        );

        registrar.playToServer(
            CycleProcessingOutputPacket.TYPE,
            CycleProcessingOutputPacket.STREAM_CODEC,
            CycleProcessingOutputPacket::handle
        );

        registrar.playToServer(
            TopActionPacket.TYPE,
            TopActionPacket.STREAM_CODEC,
            TopActionPacket::handle
        );

        registrar.playToServer(
            WirelessSettingsPacket.TYPE,
            WirelessSettingsPacket.STREAM_CODEC,
            WirelessSettingsPacket::handle
        );

        registrar.playToServer(
            WcwtPickBlockPacket.TYPE,
            WcwtPickBlockPacket.STREAM_CODEC,
            WcwtPickBlockPacket::handle
        );

        registrar.playToClient(
            WcwtRestockAmountsPacket.TYPE,
            WcwtRestockAmountsPacket.STREAM_CODEC,
            WcwtRestockAmountsPacket::handle
        );

        registrar.playToServer(
            JeiCraftingTransferPacket.TYPE,
            JeiCraftingTransferPacket.STREAM_CODEC,
            JeiCraftingTransferPacket::handle
        );

        registrar.playToServer(
            WcwtPullRecipeInputsPacket.TYPE,
            WcwtPullRecipeInputsPacket.STREAM_CODEC,
            WcwtPullRecipeInputsPacket::handle
        );
        
        registrar.playToServer(
            DirectionChangePacket.TYPE,
            DirectionChangePacket.STREAM_CODEC,
            DirectionChangePacket::handle
        );

        registrar.playToServer(
            CopyPatternPacket.TYPE,
            CopyPatternPacket.STREAM_CODEC,
            CopyPatternPacket::handle
        );

        registrar.playToServer(
            ReplacePatternPacket.TYPE,
            ReplacePatternPacket.STREAM_CODEC,
            ReplacePatternPacket::handle
        );

        registrar.playToServer(
            CellWorkbenchActionPacket.TYPE,
            CellWorkbenchActionPacket.STREAM_CODEC,
            CellWorkbenchActionPacket::handle
        );

        registrar.playToServer(
            CellConfigSetPacket.TYPE,
            CellConfigSetPacket.STREAM_CODEC,
            CellConfigSetPacket::handle
        );

        registrar.playToServer(
            PatternProviderListPacket.Request.TYPE,
            PatternProviderListPacket.Request.STREAM_CODEC,
            PatternProviderListPacket.Request::handle
        );

        registrar.playToClient(
            PatternProviderListPacket.TYPE,
            PatternProviderListPacket.STREAM_CODEC,
            PatternProviderListPacket::handle
        );

        registrar.playToClient(
            PatternProviderFocusPacket.TYPE,
            PatternProviderFocusPacket.STREAM_CODEC,
            PatternProviderFocusPacket::handle
        );

        registrar.playToServer(
            PatternManagementActionPacket.TYPE,
            PatternManagementActionPacket.STREAM_CODEC,
            PatternManagementActionPacket::handle
        );

        registrar.playToServer(
            PatternManagementUploadSettingPacket.TYPE,
            PatternManagementUploadSettingPacket.STREAM_CODEC,
            PatternManagementUploadSettingPacket::handle
        );

        registrar.playToServer(
            ToolkitNetworkToolDepositPacket.TYPE,
            ToolkitNetworkToolDepositPacket.STREAM_CODEC,
            ToolkitNetworkToolDepositPacket::handle
        );

        registrar.playToServer(
            OpenToolkitHotkeyPacket.TYPE,
            OpenToolkitHotkeyPacket.STREAM_CODEC,
            OpenToolkitHotkeyPacket::handle
        );

        registrar.playToServer(
            ResonatingLightningPatternActionPacket.TYPE,
            ResonatingLightningPatternActionPacket.STREAM_CODEC,
            ResonatingLightningPatternActionPacket::handle
        );
    }
}
