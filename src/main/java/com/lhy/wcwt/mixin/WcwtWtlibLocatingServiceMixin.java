package com.lhy.wcwt.mixin;

import appeng.menu.AEBaseMenu;
import appeng.menu.locator.ItemMenuHostLocator;
import com.lhy.wcwt.helpers.WcwtToolkitAccess;
import com.lhy.wcwt.helpers.WcwtToolkitHotbarState;
import com.lhy.wcwt.helpers.WcwtToolkitSync;
import com.lhy.wcwt.menu.locator.WcwtToolkitItemLocator;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import de.mari_023.ae2wtlib.api.registration.WTDefinition;
import de.mari_023.ae2wtlib.api.terminal.Ae2wtlibLocatingService;
import de.mari_023.ae2wtlib.api.terminal.ItemWUT;
import de.mari_023.ae2wtlib.api.terminal.WUTHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = Ae2wtlibLocatingService.class, remap = false)
public abstract class WcwtWtlibLocatingServiceMixin {
    @WrapOperation(method = "run", at = @At(value = "INVOKE",
            target = "Lde/mari_023/ae2wtlib/api/terminal/WUTHandler;findTerminal(Lnet/minecraft/world/entity/player/Player;Lde/mari_023/ae2wtlib/api/registration/WTDefinition;)Lappeng/menu/locator/ItemMenuHostLocator;"))
    @Nullable
    private ItemMenuHostLocator wcwt$findTerminalForHotkey(Player player, WTDefinition terminal,
            Operation<ItemMenuHostLocator> original) {
        var active = player.containerMenu instanceof AEBaseMenu menu
                && menu.getLocator() instanceof WcwtToolkitItemLocator locator
                ? locator : WcwtToolkitItemLocator.forHand(player, InteractionHand.MAIN_HAND);
        if (active != null) {
            var stack = active.locateItem(player);
            if (stack.getItem() instanceof ItemWUT && WUTHandler.hasTerminal(stack, terminal)) {
                return active;
            }
        }
        var carried = original.call(player, terminal);
        if (carried != null || !WcwtToolkitHotbarState.hasToolkitCard(player)) {
            return carried;
        }
        var hotbar = WcwtToolkitAccess.findHotbarTerminal(player, terminal, stack -> true);
        if (hotbar != null && player instanceof ServerPlayer serverPlayer) {
            WcwtToolkitSync.ensureLayoutSynced(serverPlayer);
        }
        return hotbar;
    }
}
