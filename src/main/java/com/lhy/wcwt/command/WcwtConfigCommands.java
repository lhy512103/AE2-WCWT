package com.lhy.wcwt.command;

import com.lhy.wcwt.WcwtMod;
import com.lhy.wcwt.config.WcwtServerConfig;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;

public final class WcwtConfigCommands {
    private WcwtConfigCommands() {
    }

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal(WcwtMod.MOD_ID)
                .then(Commands.literal("config")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("toolkitSlotCount")
                                .executes(context -> showToolkitSlotCount(context.getSource()))
                                .then(Commands.argument("value", IntegerArgumentType.integer(
                                                WcwtServerConfig.MIN_TOOLKIT_SLOTS,
                                                WcwtServerConfig.MAX_TOOLKIT_SLOTS))
                                        .executes(context -> setToolkitSlotCount(
                                                context.getSource(),
                                                IntegerArgumentType.getInteger(context, "value")))))
                        .then(Commands.literal("patternProviderActiveRefresh")
                                .executes(context -> showPatternProviderActiveRefresh(context.getSource()))
                                .then(Commands.argument("value", BoolArgumentType.bool())
                                        .executes(context -> setPatternProviderActiveRefresh(
                                                context.getSource(),
                                                BoolArgumentType.getBool(context, "value")))))));
    }

    private static int showToolkitSlotCount(CommandSourceStack source) {
        if (!ensureLoaded(source)) {
            return 0;
        }
        int value = WcwtServerConfig.TOOLKIT_SLOT_COUNT.get();
        source.sendSuccess(() -> Component.literal("WCWT: toolkitSlotCount = " + value), false);
        return value;
    }

    private static int setToolkitSlotCount(CommandSourceStack source, int value) {
        if (!ensureLoaded(source)) {
            return 0;
        }
        WcwtServerConfig.TOOLKIT_SLOT_COUNT.set(value);
        save();
        source.sendSuccess(() -> Component.literal(
                "WCWT: toolkitSlotCount set to " + value + " and saved to config/wcwt-server.toml."), true);
        return value;
    }

    private static int showPatternProviderActiveRefresh(CommandSourceStack source) {
        if (!ensureLoaded(source)) {
            return 0;
        }
        boolean value = WcwtServerConfig.PATTERN_PROVIDER_ACTIVE_REFRESH.get();
        source.sendSuccess(() -> Component.literal("WCWT: patternProviderActiveRefresh = " + value), false);
        return value ? 1 : 0;
    }

    private static int setPatternProviderActiveRefresh(CommandSourceStack source, boolean value) {
        if (!ensureLoaded(source)) {
            return 0;
        }
        WcwtServerConfig.PATTERN_PROVIDER_ACTIVE_REFRESH.set(value);
        save();
        source.sendSuccess(() -> Component.literal(
                "WCWT: patternProviderActiveRefresh set to " + value
                        + " and saved to config/wcwt-server.toml."), true);
        return value ? 1 : 0;
    }

    private static boolean ensureLoaded(CommandSourceStack source) {
        if (!WcwtServerConfig.SPEC.isLoaded()) {
            source.sendFailure(Component.literal("WCWT: server config is not loaded."));
            return false;
        }
        return true;
    }

    private static void save() {
        WcwtServerConfig.SPEC.save();
    }
}
