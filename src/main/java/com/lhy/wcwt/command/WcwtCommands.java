package com.lhy.wcwt.command;

import com.lhy.wcwt.config.WcwtServerConfig;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class WcwtCommands {
    private WcwtCommands() {
    }

    public static void register(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("wcwt")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("config")
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
        int value = WcwtServerConfig.toolkitSlotCount();
        source.sendSuccess(() -> Component.literal("WCWT toolkitSlotCount = " + value), false);
        return value;
    }

    private static int setToolkitSlotCount(CommandSourceStack source, int value) {
        WcwtServerConfig.setToolkitSlotCount(value);
        source.sendSuccess(() -> Component.literal("WCWT toolkitSlotCount set to " + value), true);
        return value;
    }

    private static int showPatternProviderActiveRefresh(CommandSourceStack source) {
        boolean value = WcwtServerConfig.patternProviderActiveRefresh();
        source.sendSuccess(() -> Component.literal("WCWT patternProviderActiveRefresh = " + value), false);
        return value ? 1 : 0;
    }

    private static int setPatternProviderActiveRefresh(CommandSourceStack source, boolean value) {
        WcwtServerConfig.setPatternProviderActiveRefresh(value);
        source.sendSuccess(() -> Component.literal("WCWT patternProviderActiveRefresh set to " + value), true);
        return value ? 1 : 0;
    }
}
