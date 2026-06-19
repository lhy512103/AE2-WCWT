package com.lhy.wcwt.mixin.compat.eaep;

import appeng.api.implementations.menuobjects.ItemMenuHost;
import com.extendedae_plus.menu.locator.CuriosItemLocator;
import com.lhy.wcwt.compat.CuriosBridge;
import com.lhy.wcwt.item.WirelessComprehensiveWorkTerminalItem;
import com.lhy.wcwt.menu.locator.WcwtCurioLocator;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * EAEP 的 {@link CuriosItemLocator#locate} 在饰品槽里命中无线终端时，会把"返回主界面"硬编码成
 * AE2 原版无线终端（{@code MEStorageMenu.WIRELESS_TYPE}）。对于放在饰品槽里的综合工作终端来说，
 * 这会导致 JEI 书签中键下单后退回到原版无线终端界面，而不是 WCWT 综合工作终端。
 *
 * <p>这里在 {@code locate} 入口处拦截：若该饰品槽里实际是 WCWT 终端，则直接返回 WCWT 自己的菜单宿主，
 * 其 {@code returnToMainMenu} 会通过 {@link WcwtCurioLocator} 重新打开 WCWT 菜单，保证下单后退回到正确界面。
 *
 * <p>该兜底在主线程的开菜单流程中执行，Curios 能力可正常解析；即便服务端数据包拦截因线程/能力时序
 * 未能命中饰品槽，这里仍能纠正返回界面。
 */
@Mixin(value = CuriosItemLocator.class, remap = false)
public abstract class WcwtEaepCuriosItemLocatorMixin {
    @Shadow
    @Final
    private String slotId;

    @Shadow
    @Final
    private int index;

    @Inject(method = "locate", at = @At("HEAD"), cancellable = true, remap = false)
    private <T> void wcwt$locateWcwtHost(Player player, Class<T> hostInterface, CallbackInfoReturnable<T> cir) {
        for (var curio : CuriosBridge.getEquippedSlots(player)) {
            if (!slotId.equals(curio.identifier()) || index != curio.slotIndex()) {
                continue;
            }

            ItemStack stack = curio.handler().getStackInSlot(curio.slotIndex());
            if (!(stack.getItem() instanceof WirelessComprehensiveWorkTerminalItem terminal)) {
                return;
            }

            var locator = new WcwtCurioLocator(slotId, index);
            ItemMenuHost menuHost = terminal.getMenuHost(player, locator, stack);
            if (hostInterface.isInstance(menuHost)) {
                cir.setReturnValue(hostInterface.cast(menuHost));
            }
            return;
        }
    }
}
