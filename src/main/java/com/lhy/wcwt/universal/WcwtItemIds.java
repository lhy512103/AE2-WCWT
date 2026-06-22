package com.lhy.wcwt.universal;

import com.lhy.wcwt.util.ResourceLocationCompat;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Set;

public final class WcwtItemIds {
    public static final ResourceLocation AE2_WIRELESS_TERMINAL =
            ResourceLocationCompat.id("ae2", "wireless_terminal");
    public static final ResourceLocation AE2_WIRELESS_CRAFTING_TERMINAL =
            ResourceLocationCompat.id("ae2", "wireless_crafting_terminal");
    public static final ResourceLocation AE2WTLIB_WIRELESS_PATTERN_ENCODING_TERMINAL =
            ResourceLocationCompat.id("ae2wtlib", "wireless_pattern_encoding_terminal");
    public static final ResourceLocation AE2WTLIB_WIRELESS_PATTERN_ACCESS_TERMINAL =
            ResourceLocationCompat.id("ae2wtlib", "wireless_pattern_access_terminal");
    public static final ResourceLocation ADVANCED_AE_WIRELESS_QUANTUM_CRAFTER_TERMINAL =
            ResourceLocationCompat.id("advanced_ae", "wireless_quantum_crafter_terminal");
    public static final ResourceLocation MEREQUESTER_WIRELESS_REQUESTER_TERMINAL =
            ResourceLocationCompat.id("merequester", "wireless_requester_terminal");
    public static final ResourceLocation AE2WTLIB_QUANTUM_BRIDGE_CARD =
            ResourceLocationCompat.id("ae2wtlib", "quantum_bridge_card");

    public static final List<ResourceLocation> MERGEABLE_TERMINALS = List.of(
            AE2_WIRELESS_TERMINAL,
            AE2_WIRELESS_CRAFTING_TERMINAL,
            AE2WTLIB_WIRELESS_PATTERN_ENCODING_TERMINAL,
            AE2WTLIB_WIRELESS_PATTERN_ACCESS_TERMINAL,
            ADVANCED_AE_WIRELESS_QUANTUM_CRAFTER_TERMINAL,
            MEREQUESTER_WIRELESS_REQUESTER_TERMINAL);

    public static final Set<ResourceLocation> MERGEABLE_TERMINAL_SET = Set.copyOf(MERGEABLE_TERMINALS);

    private WcwtItemIds() {
    }
}
