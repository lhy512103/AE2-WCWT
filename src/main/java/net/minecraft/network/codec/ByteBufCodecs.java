package net.minecraft.network.codec;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceKey;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.ToIntFunction;

public final class ByteBufCodecs {
    private ByteBufCodecs() {
    }

    public static final StreamCodec<ByteBuf, Boolean> BOOL = StreamCodec.of(ByteBuf::writeBoolean, ByteBuf::readBoolean);
    public static final StreamCodec<ByteBuf, Integer> VAR_INT = StreamCodec.of(
            (buf, value) -> asFriendly(buf).writeVarInt(value),
            buf -> asFriendly(buf).readVarInt());
    public static final StreamCodec<ByteBuf, Long> VAR_LONG = StreamCodec.of(
            (buf, value) -> asFriendly(buf).writeVarLong(value),
            buf -> asFriendly(buf).readVarLong());
    public static final StreamCodec<ByteBuf, Integer> INT = StreamCodec.of(ByteBuf::writeInt, ByteBuf::readInt);
    public static final StreamCodec<ByteBuf, Short> SHORT = StreamCodec.of(
            (buf, value) -> buf.writeShort(value),
            buf -> (short) buf.readShort());
    public static final StreamCodec<ByteBuf, String> STRING_UTF8 = StreamCodec.of(
            (buf, value) -> asFriendly(buf).writeUtf(value),
            buf -> asFriendly(buf).readUtf());
    public static final StreamCodec<ByteBuf, CompoundTag> COMPOUND_TAG = StreamCodec.of(
            (buf, tag) -> asFriendly(buf).writeNbt(tag),
            buf -> asFriendly(buf).readNbt());

    public static <B extends ByteBuf, E> StreamCodec<B, E> idMapper(IntFunction<E> byId, ToIntFunction<E> toId) {
        return StreamCodec.of((buf, value) -> asFriendly(buf).writeVarInt(toId.applyAsInt(value)),
                buf -> byId.apply(asFriendly(buf).readVarInt()));
    }

    public static <B extends ByteBuf, E> Function<StreamCodec<? super B, E>, StreamCodec<B, List<E>>> list() {
        return elementCodec -> StreamCodec.of((buf, values) -> {
            FriendlyByteBuf friendly = asFriendly(buf);
            friendly.writeVarInt(values.size());
            for (E value : values) {
                @SuppressWarnings("unchecked")
                StreamCodec<B, E> codec = (StreamCodec<B, E>) elementCodec;
                codec.encode(buf, value);
            }
        }, buf -> {
            FriendlyByteBuf friendly = asFriendly(buf);
            int size = friendly.readVarInt();
            List<E> result = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                @SuppressWarnings("unchecked")
                StreamCodec<B, E> codec = (StreamCodec<B, E>) elementCodec;
                result.add(codec.decode(buf));
            }
            return result;
        });
    }

    public static <B extends ByteBuf, K, V, M extends Map<K, V>> StreamCodec<B, M> map(
            IntFunction<M> factory,
            StreamCodec<? super B, K> keyCodec,
            StreamCodec<? super B, V> valueCodec) {
        return map(factory, keyCodec, valueCodec, Integer.MAX_VALUE);
    }

    public static <B extends ByteBuf, K, V, M extends Map<K, V>> StreamCodec<B, M> map(
            IntFunction<M> factory,
            StreamCodec<? super B, K> keyCodec,
            StreamCodec<? super B, V> valueCodec,
            int ignoredMaxSize) {
        return StreamCodec.of((buf, values) -> {
            FriendlyByteBuf friendly = asFriendly(buf);
            friendly.writeVarInt(values.size());
            @SuppressWarnings("unchecked")
            StreamCodec<B, K> castKeyCodec = (StreamCodec<B, K>) keyCodec;
            @SuppressWarnings("unchecked")
            StreamCodec<B, V> castValueCodec = (StreamCodec<B, V>) valueCodec;
            for (Map.Entry<K, V> entry : values.entrySet()) {
                castKeyCodec.encode(buf, entry.getKey());
                castValueCodec.encode(buf, entry.getValue());
            }
        }, buf -> {
            FriendlyByteBuf friendly = asFriendly(buf);
            int size = friendly.readVarInt();
            M result = factory.apply(size);
            @SuppressWarnings("unchecked")
            StreamCodec<B, K> castKeyCodec = (StreamCodec<B, K>) keyCodec;
            @SuppressWarnings("unchecked")
            StreamCodec<B, V> castValueCodec = (StreamCodec<B, V>) valueCodec;
            for (int i = 0; i < size; i++) {
                result.put(castKeyCodec.decode(buf), castValueCodec.decode(buf));
            }
            return result;
        });
    }

    public static StreamCodec<RegistryFriendlyByteBuf, Holder<net.minecraft.world.item.Item>> holderRegistry(
            ResourceKey<? extends Registry<net.minecraft.world.item.Item>> ignoredRegistryKey) {
        return StreamCodec.of((buf, holder) -> {
            Registry<net.minecraft.world.item.Item> registry = itemRegistry(buf.registryAccess());
            buf.writeResourceLocation(registry.getKey(holder.value()));
        }, buf -> {
            Registry<net.minecraft.world.item.Item> registry = itemRegistry(buf.registryAccess());
            return registry.getHolderOrThrow(ResourceKey.create(Registries.ITEM, buf.readResourceLocation()));
        });
    }

    private static Registry<net.minecraft.world.item.Item> itemRegistry(RegistryAccess access) {
        RegistryAccess actualAccess = access != null ? access : net.minecraft.client.Minecraft.getInstance().level.registryAccess();
        return actualAccess.registryOrThrow(Registries.ITEM);
    }

    private static FriendlyByteBuf asFriendly(ByteBuf buf) {
        return buf instanceof FriendlyByteBuf friendly ? friendly : new FriendlyByteBuf(buf);
    }
}
