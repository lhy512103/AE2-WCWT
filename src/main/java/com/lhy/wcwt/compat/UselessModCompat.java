package com.lhy.wcwt.compat;

import appeng.api.stacks.GenericStack;
import com.lhy.wcwt.helpers.WirelessComprehensiveWorkTerminalMenuHost;
import com.lhy.wcwt.menu.WirelessComprehensiveWorkTerminalMenu;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Compatibility facade for Some Useless Things (not yet available on Minecraft 26.1.2).
 */
public final class UselessModCompat {
    private UselessModCompat() {
    }

    public record Selection(Identifier recipeId, String fingerprint, String sourceId) {
    }

    public record Transfer(Selection selection, List<GenericStack> inputs, List<GenericStack> outputs) {
    }

    public enum UploadResult { SKIPPED, UPLOADED, DUPLICATE }

    public static boolean available() {
        return false;
    }

    public static boolean isRecipe(Object recipe) {
        return false;
    }

    public static @Nullable Transfer transfer(Object recipe, WirelessComprehensiveWorkTerminalMenu menu) {
        return null;
    }

    public static ItemStack encode(ItemStack pattern, Selection selection, Level level) {
        return ItemStack.EMPTY;
    }

    public static UploadResult upload(WirelessComprehensiveWorkTerminalMenuHost host, ItemStack pattern,
                                      ServerPlayer player) {
        return UploadResult.SKIPPED;
    }
}
