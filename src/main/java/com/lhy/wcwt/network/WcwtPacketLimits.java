package com.lhy.wcwt.network;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.codec.ByteBufCodecs;

/** Bounds for client-controlled lengths, checked before anything is allocated from them. */
public final class WcwtPacketLimits {
    public static final int MAX_RECIPE_INGREDIENTS = 256;
    public static final int MAX_INGREDIENT_ALTERNATIVES = 1024;
    public static final int MAX_TRANSFER_STACKS = 256;
    public static final int MAX_SLOT_INDICES = 256;

    private WcwtPacketLimits() {
    }

    public static int readCount(ByteBuf buf, int max, String what) {
        return checkCount(ByteBufCodecs.VAR_INT.decode(buf), max, what);
    }

    public static int checkCount(int count, int max, String what) {
        if (count < 0 || count > max) {
            throw new DecoderException(what + " count " + count + " outside 0.." + max);
        }
        return count;
    }

    public static <E extends Enum<E>> E readEnum(ByteBuf buf, E[] values, String what) {
        int ordinal = ByteBufCodecs.VAR_INT.decode(buf);
        if (ordinal < 0 || ordinal >= values.length) {
            throw new DecoderException("Unknown " + what + " ordinal " + ordinal);
        }
        return values[ordinal];
    }
}
