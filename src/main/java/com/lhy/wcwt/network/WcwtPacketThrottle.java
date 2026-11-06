package com.lhy.wcwt.network;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.server.level.ServerPlayer;

/**
 * Per-player, per-tick budgets for packets that a hand-paced client sends at most a few times per tick.
 * Anything beyond the budget is dropped; a legitimate client never gets there. Only used from the server
 * main thread.
 */
public final class WcwtPacketThrottle {
    private static final Map<ServerPlayer, Map<String, long[]>> BUDGETS = new WeakHashMap<>();

    private WcwtPacketThrottle() {
    }

    /** @return whether this packet may be handled; counts one use of the budget either way. */
    public static boolean tryAcquire(ServerPlayer player, String key, int perTick) {
        long tick = player.level().getGameTime();
        var budgets = BUDGETS.computeIfAbsent(player, ignored -> new HashMap<>());
        long[] budget = budgets.computeIfAbsent(key, ignored -> new long[] {Long.MIN_VALUE, 0L});
        if (budget[0] != tick) {
            budget[0] = tick;
            budget[1] = 0L;
        }
        return budget[1]++ < perTick;
    }
}
