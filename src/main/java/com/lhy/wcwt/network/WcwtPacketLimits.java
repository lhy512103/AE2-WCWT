package com.lhy.wcwt.network;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.Nullable;

/** Bounds for client-controlled lengths, checked before anything is allocated from them. */
public final class WcwtPacketLimits {
    public static final int MAX_RECIPE_INGREDIENTS = 256;
    public static final int MAX_INGREDIENT_ALTERNATIVES = 1024;
    /** Across all ingredients of one request; a client trims to this before sending. */
    public static final int MAX_TOTAL_INGREDIENT_ALTERNATIVES = 8192;
    public static final int MAX_TRANSFER_STACKS = 256;
    public static final int MAX_SLOT_INDICES = 256;
    /** Provider search text, dimension ids and similar short strings carried in packets. */
    public static final int MAX_SEARCH_TEXT_LENGTH = 256;
    /** Mapping text typed into the pattern management panel. */
    public static final int MAX_MAPPING_TEXT_LENGTH = 1024;

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

    /**
     * An ordinal codec that reports an unknown ordinal as a decoder error instead of an array index error;
     * both disconnect the sender, but only the former says what was wrong.
     */
    public static <E extends Enum<E>> StreamCodec<ByteBuf, E> enumCodec(E[] values, String what) {
        return ByteBufCodecs.VAR_INT.map(ordinal -> {
            if (ordinal < 0 || ordinal >= values.length) {
                throw new DecoderException("Unknown " + what + " ordinal " + ordinal);
            }
            return values[ordinal];
        }, Enum::ordinal);
    }

    /** Clamps a client-supplied string so it fits the codec that carries it. */
    public static String truncate(@Nullable String text, int maxLength) {
        if (text == null) {
            return "";
        }
        return text.length() <= maxLength ? text : text.substring(0, maxLength);
    }
}
