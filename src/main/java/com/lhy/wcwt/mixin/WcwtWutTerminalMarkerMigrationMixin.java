package com.lhy.wcwt.mixin;

import com.lhy.wcwt.WcwtMod;
import com.lhy.wcwt.init.ModComponents;
import de.mari_023.ae2wtlib.wut.ItemWUT;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ItemWUT.class, remap = false)
public abstract class WcwtWutTerminalMarkerMigrationMixin {
    private static final String CURRENT_TERMINAL_TAG = "currentTerminal";

    @Inject(method = "inventoryTick", at = @At("HEAD"))
    private void wcwt$restoreOverwrittenTerminalMarker(ItemStack stack, Level level, Entity entity, int slot,
            boolean selected, CallbackInfo ci) {
        if (level.isClientSide()) {
            return;
        }

        var tag = stack.getTag();
        if (tag == null || !tag.contains(ModComponents.ROOT_TAG, Tag.TAG_COMPOUND)
                || tag.getBoolean(WcwtMod.WUT_TERMINAL_ID)) {
            return;
        }

        tag.putBoolean(WcwtMod.WUT_TERMINAL_ID, true);
        if (WcwtMod.MOD_ID.equals(tag.getString(CURRENT_TERMINAL_TAG))) {
            tag.putString(CURRENT_TERMINAL_TAG, WcwtMod.WUT_TERMINAL_ID);
        }
    }
}