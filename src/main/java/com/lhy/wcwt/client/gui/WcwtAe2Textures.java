package com.lhy.wcwt.client.gui;

import com.lhy.wcwt.util.ResourceLocationCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

public final class WcwtAe2Textures {
    public static final int STATES_WIDTH = 256;
    public static final int STATES_HEIGHT = 256;
    public static final int CHECKBOX_WIDTH = 64;
    public static final int CHECKBOX_HEIGHT = 64;
    public static final int EXTRA_PANELS_WIDTH = 128;
    public static final int EXTRA_PANELS_HEIGHT = 128;

    private static final String GUI_PREFIX = "textures/guis/wcwt/ae2_1_21/";

    private static final ResourceLocation STATES =
            ResourceLocationCompat.id("ae2", GUI_PREFIX + "states.png");
    private static final ResourceLocation CHECKBOX =
            ResourceLocationCompat.id("ae2", GUI_PREFIX + "checkbox.png");
    private static final ResourceLocation PATTERN_MODES =
            ResourceLocationCompat.id("ae2", GUI_PREFIX + "pattern_modes.png");
    private static final ResourceLocation EXTRA_PANELS =
            ResourceLocationCompat.id("ae2", GUI_PREFIX + "extra_panels.png");
    private static final ResourceLocation TERMINAL =
            ResourceLocationCompat.id("ae2", GUI_PREFIX + "terminal.png");
    private static final ResourceLocation MOLECULAR_ASSEMBLER_LIGHTS =
            ResourceLocationCompat.id("ae2", GUI_PREFIX + "molecular_assembler_lights.png");

    private static final ResourceLocation FALLBACK_STATES =
            ResourceLocationCompat.id("ae2", "textures/guis/states.png");
    private static final ResourceLocation FALLBACK_CHECKBOX =
            ResourceLocationCompat.id("ae2", "textures/guis/checkbox.png");
    private static final ResourceLocation FALLBACK_PATTERN_MODES =
            ResourceLocationCompat.id("ae2", "textures/guis/pattern_modes.png");
    private static final ResourceLocation FALLBACK_EXTRA_PANELS =
            ResourceLocationCompat.id("ae2", "textures/guis/extra_panels.png");
    private static final ResourceLocation FALLBACK_TERMINAL =
            ResourceLocationCompat.id("ae2", "textures/guis/terminal.png");
    private static final ResourceLocation FALLBACK_MOLECULAR_ASSEMBLER_LIGHTS =
            ResourceLocationCompat.id("ae2", "textures/block/molecular_assembler_lights.png");

    private static boolean hasStates;
    private static boolean hasCheckbox;
    private static boolean hasPatternModes;
    private static boolean hasExtraPanels;
    private static boolean hasTerminal;
    private static boolean hasMolecularAssemblerLights;

    private WcwtAe2Textures() {
    }

    public static ResourceLocation states() {
        return themedOrFallback(STATES, FALLBACK_STATES);
    }

    public static ResourceLocation checkbox() {
        return themedOrFallback(CHECKBOX, FALLBACK_CHECKBOX);
    }

    public static ResourceLocation patternModes() {
        return themedOrFallback(PATTERN_MODES, FALLBACK_PATTERN_MODES);
    }

    public static boolean usingThemedPatternModes() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            return false;
        }
        updateCache(minecraft.getResourceManager());
        return hasPatternModes;
    }

    public static boolean usingThemedExtraPanels() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            return false;
        }
        updateCache(minecraft.getResourceManager());
        return hasExtraPanels;
    }

    public static ResourceLocation extraPanels() {
        return themedOrFallback(EXTRA_PANELS, FALLBACK_EXTRA_PANELS);
    }

    public static ResourceLocation terminal() {
        return themedOrFallback(TERMINAL, FALLBACK_TERMINAL);
    }

    public static ResourceLocation molecularAssemblerLights() {
        return themedOrFallback(MOLECULAR_ASSEMBLER_LIGHTS, FALLBACK_MOLECULAR_ASSEMBLER_LIGHTS);
    }

    private static ResourceLocation themedOrFallback(ResourceLocation themed, ResourceLocation fallback) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            return fallback;
        }
        updateCache(minecraft.getResourceManager());
        if (themed.equals(STATES)) {
            return hasStates ? themed : fallback;
        } else if (themed.equals(CHECKBOX)) {
            return hasCheckbox ? themed : fallback;
        } else if (themed.equals(PATTERN_MODES)) {
            return hasPatternModes ? themed : fallback;
        } else if (themed.equals(EXTRA_PANELS)) {
            return hasExtraPanels ? themed : fallback;
        } else if (themed.equals(TERMINAL)) {
            return hasTerminal ? themed : fallback;
        } else if (themed.equals(MOLECULAR_ASSEMBLER_LIGHTS)) {
            return hasMolecularAssemblerLights ? themed : fallback;
        }
        return fallback;
    }

    private static void updateCache(ResourceManager resourceManager) {
        hasStates = resourceManager.getResource(STATES).isPresent();
        hasCheckbox = resourceManager.getResource(CHECKBOX).isPresent();
        hasPatternModes = resourceManager.getResource(PATTERN_MODES).isPresent();
        hasExtraPanels = resourceManager.getResource(EXTRA_PANELS).isPresent();
        hasTerminal = resourceManager.getResource(TERMINAL).isPresent();
        hasMolecularAssemblerLights = resourceManager.getResource(MOLECULAR_ASSEMBLER_LIGHTS).isPresent();
    }
}
