package com.lhy.wcwt.helpers;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

/** Keeps a validated provider target menu open when the player is using WCWT remotely. */
public final class WcwtRemoteMenuAccess {
    private static final Map<UUID, Session> SESSIONS = new HashMap<>();

    private WcwtRemoteMenuAccess() {
    }

    public static boolean open(ServerPlayer player, MenuProvider provider, ServerLevel level, BlockPos pos) {
        var openedId = player.openMenu(provider, pos);
        if (openedId.isEmpty() || player.containerMenu.containerId != openedId.getAsInt()) {
            return false;
        }
        track(player, level, pos);
        return true;
    }

    public static boolean trackOpenedMenu(ServerPlayer player, AbstractContainerMenu previousMenu,
                                          ServerLevel level, BlockPos pos) {
        if (player.containerMenu == previousMenu) {
            return false;
        }
        track(player, level, pos);
        return true;
    }

    private static void track(ServerPlayer player, ServerLevel level, BlockPos pos) {
        var state = level.getBlockState(pos);
        SESSIONS.put(player.getUUID(), new Session(
                player.containerMenu,
                level.dimension(),
                pos.immutable(),
                state.getBlock(),
                level.getBlockEntity(pos)));
    }

    public static boolean keepsMenuValid(ServerPlayer player, AbstractContainerMenu menu) {
        Session session = SESSIONS.get(player.getUUID());
        if (session == null || session.menu != menu) {
            if (session != null) {
                SESSIONS.remove(player.getUUID());
            }
            return false;
        }

        ServerLevel level = player.server.getLevel(session.dimension);
        if (level == null || !level.isLoaded(session.pos)) {
            SESSIONS.remove(player.getUUID());
            return false;
        }

        var state = level.getBlockState(session.pos);
        if (state.isAir() || state.getBlock() != session.block) {
            SESSIONS.remove(player.getUUID());
            return false;
        }
        if (session.blockEntity != null
                && (session.blockEntity.isRemoved() || level.getBlockEntity(session.pos) != session.blockEntity)) {
            SESSIONS.remove(player.getUUID());
            return false;
        }
        return true;
    }

    public static void clear(ServerPlayer player, AbstractContainerMenu menu) {
        Session session = SESSIONS.get(player.getUUID());
        if (session != null && session.menu == menu) {
            SESSIONS.remove(player.getUUID());
        }
    }

    private record Session(AbstractContainerMenu menu, ResourceKey<Level> dimension, BlockPos pos,
                           Block block, @Nullable BlockEntity blockEntity) {
    }
}
