package com.lhy.wcwt.compat.extendedae;

import com.glodblock.github.extendedae.client.render.EAEHighlightHandler;
import com.glodblock.github.extendedae.util.FCClientUtil;
import com.glodblock.github.extendedae.util.MessageUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

public final class ExtendedAeHighlight {
    private ExtendedAeHighlight() {
    }

    public static boolean highlight(Player player, BlockPos pos, ResourceKey<Level> dimension,
                                    @Nullable Direction face) {
        if (player == null || pos == null || dimension == null) {
            return false;
        }
        double multiplier = Math.min(30.0, Math.max(1.0, pos.distSqr(player.getOnPos())));
        long until = System.currentTimeMillis() + (long) (600 * multiplier);
        if (face == null) {
            EAEHighlightHandler.highlight(pos, dimension, until);
        } else {
            EAEHighlightHandler.highlight(pos, face, dimension, until, rotateFaceBox(pos, face));
        }
        player.displayClientMessage(
                MessageUtil.createEnhancedHighlightMessage(player, pos, dimension,
                        "chat.ex_pattern_access_terminal.pos"),
                false);
        return true;
    }

    private static AABB rotateFaceBox(BlockPos pos, Direction face) {
        AABB origin = new AABB(2 / 16D, 2 / 16D, 0, 14 / 16D, 14 / 16D, 2 / 16D).move(pos);
        var center = new AABB(pos).getCenter();
        return switch (face) {
            case WEST -> FCClientUtil.rotor(origin, center, Direction.Axis.Y, (float) (Math.PI / 2));
            case SOUTH -> FCClientUtil.rotor(origin, center, Direction.Axis.Y, (float) Math.PI);
            case EAST -> FCClientUtil.rotor(origin, center, Direction.Axis.Y, (float) (-Math.PI / 2));
            case UP -> FCClientUtil.rotor(origin, center, Direction.Axis.X, (float) (-Math.PI / 2));
            case DOWN -> FCClientUtil.rotor(origin, center, Direction.Axis.X, (float) (Math.PI / 2));
            default -> origin;
        };
    }
}
