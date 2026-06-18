package com.lhy.wcwt.client;

import appeng.core.localization.ButtonToolTips;
import com.lhy.wcwt.WcwtMod;
import com.lhy.wcwt.universal.WcwtItemIds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.List;

@EventBusSubscriber(modid = WcwtMod.MOD_ID, value = Dist.CLIENT)
public final class WcwtTooltipEvents {
    private WcwtTooltipEvents() {
    }

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        var stack = event.getItemStack();
        if (BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(WcwtItemIds.AE2WTLIB_QUANTUM_BRIDGE_CARD)) {
            appendWcwtQuantumBridgeTargets(event.getToolTip());
        }
    }

    private static void appendWcwtQuantumBridgeTargets(List<Component> lines) {
        if (BuiltInRegistries.ITEM.get(WcwtItemIds.AE2WTLIB_QUANTUM_BRIDGE_CARD) == Items.AIR) {
            return;
        }

        int supportedByIndex = findSupportedByIndex(lines);
        if (supportedByIndex < 0) {
            lines.add(ButtonToolTips.SupportedBy.text());
            supportedByIndex = lines.size() - 1;
        }

        MutableComponent baseName = Component.translatable("item.wcwt.wireless_comprehensive_work_terminal")
                .withStyle(ChatFormatting.GRAY);
        MutableComponent universalName = Component.translatable("item.wcwt.wireless_universal_comprehensive_work_terminal")
                .withStyle(ChatFormatting.GRAY);

        int baseIndex = addAfterSupportedByIfMissing(lines, supportedByIndex, baseName);
        addAfterIndexIfMissing(lines, baseIndex, universalName);
    }

    private static int findSupportedByIndex(List<Component> lines) {
        String supportedBy = ButtonToolTips.SupportedBy.text().getString();
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).getString().equals(supportedBy)) {
                return i;
            }
        }
        return -1;
    }

    private static int addAfterSupportedByIfMissing(List<Component> lines, int supportedByIndex, Component line) {
        return addAfterIndexIfMissing(lines, supportedByIndex, line);
    }

    private static int addAfterIndexIfMissing(List<Component> lines, int index, Component line) {
        String text = line.getString();
        for (int i = 0; i < lines.size(); i++) {
            Component existing = lines.get(i);
            if (existing.getString().equals(text)) {
                return i;
            }
        }
        int insertIndex = Math.min(lines.size(), index + 1);
        lines.add(insertIndex, line);
        return insertIndex;
    }
}
