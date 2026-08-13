package com.lhy.wcwt.mixin;

import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.UpgradeInventories;
import com.lhy.wcwt.WcwtMod;
import com.lhy.wcwt.item.WirelessComprehensiveWorkTerminalItem;
import de.mari_023.ae2wtlib.wut.ItemWUT;
import de.mari_023.ae2wtlib.wut.WUTHandler;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ItemWUT.class, remap = false)
public abstract class WcwtWutUpgradeCountMixin {
    @Inject(method = "getUpgrades", at = @At("HEAD"), cancellable = true)
    private void wcwt$expandUpgradeInventory(ItemStack stack, CallbackInfoReturnable<IUpgradeInventory> cir) {
        if (WUTHandler.hasTerminal(stack, WcwtMod.WUT_TERMINAL_ID)) {
            var self = (ItemWUT) (Object) this;
            cir.setReturnValue(UpgradeInventories.forItem(
                    stack,
                    WirelessComprehensiveWorkTerminalItem.UPGRADE_INVENTORY_SIZE,
                    self::onUpgradesChanged));
        }
    }
}
