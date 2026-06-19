package com.lhy.wcwt.mixin.compat.jei;

import com.lhy.wcwt.compat.jei.WcwtJeiBookmarkOrder;
import mezz.jei.common.input.IInternalKeyMappings;
import mezz.jei.gui.input.IUserInputHandler;
import mezz.jei.gui.input.UserInput;
import mezz.jei.gui.input.handlers.BookmarkInputHandler;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(value = BookmarkInputHandler.class, remap = false)
public abstract class WcwtJeiBookmarkInputHandlerMixin {
    @Inject(method = "handleUserInput", at = @At("HEAD"), cancellable = true)
    private void wcwt$orderBookmark(Screen screen, UserInput input, IInternalKeyMappings keyBindings,
                                    CallbackInfoReturnable<Optional<IUserInputHandler>> cir) {
        WcwtJeiBookmarkOrder.handleBookmarkMiddleClick((BookmarkInputHandler) (Object) this, screen, input,
                keyBindings, cir);
    }
}
