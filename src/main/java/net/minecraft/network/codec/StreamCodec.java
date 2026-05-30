package com.lhy.wcwt.compat.minecraft.network.codec;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

public interface StreamCodec<B, V> {
    void encode(B buffer, V value);

    V decode(B buffer);

    default <T> StreamCodec<B, T> map(Function<V, T> decoderMapper, Function<T, V> encoderMapper) {
        return of((buffer, value) -> this.encode(buffer, encoderMapper.apply(value)),
                buffer -> decoderMapper.apply(this.decode(buffer)));
    }

    default <T> T apply(Function<StreamCodec<B, V>, T> applier) {
        return applier.apply(this);
    }

    @SuppressWarnings("unchecked")
    default <T> StreamCodec<B, T> cast() {
        return (StreamCodec<B, T>) this;
    }

    static <B, V> StreamCodec<B, V> of(BiConsumer<B, V> encoder, Function<B, V> decoder) {
        return new StreamCodec<>() {
            @Override
            public void encode(B buffer, V value) {
                encoder.accept(buffer, value);
            }

            @Override
            public V decode(B buffer) {
                return decoder.apply(buffer);
            }
        };
    }

    static <B, V> StreamCodec<B, V> ofMember(BiConsumer<V, B> encoder, Function<B, V> decoder) {
        return of((buffer, value) -> encoder.accept(value, buffer), decoder);
    }

    static <B, V> StreamCodec<B, V> unit(V value) {
        return of((buffer, ignored) -> {
        }, buffer -> value);
    }

    static <B, A, V> StreamCodec<B, V> composite(
            StreamCodec<? super B, A> codecA,
            Function<V, A> getterA,
            Function<A, V> factory) {
        return of((buffer, value) -> codecA.encode(buffer, getterA.apply(value)),
                buffer -> factory.apply(codecA.decode(buffer)));
    }

    static <B, A, C, V> StreamCodec<B, V> composite(
            StreamCodec<? super B, A> codecA,
            Function<V, A> getterA,
            StreamCodec<? super B, C> codecC,
            Function<V, C> getterC,
            BiFunction<A, C, V> factory) {
        return of((buffer, value) -> {
            codecA.encode(buffer, getterA.apply(value));
            codecC.encode(buffer, getterC.apply(value));
        }, buffer -> factory.apply(codecA.decode(buffer), codecC.decode(buffer)));
    }

    static <B, A, C, D, V> StreamCodec<B, V> composite(
            StreamCodec<? super B, A> codecA,
            Function<V, A> getterA,
            StreamCodec<? super B, C> codecC,
            Function<V, C> getterC,
            StreamCodec<? super B, D> codecD,
            Function<V, D> getterD,
            TriFunction<A, C, D, V> factory) {
        return of((buffer, value) -> {
            codecA.encode(buffer, getterA.apply(value));
            codecC.encode(buffer, getterC.apply(value));
            codecD.encode(buffer, getterD.apply(value));
        }, buffer -> factory.apply(codecA.decode(buffer), codecC.decode(buffer), codecD.decode(buffer)));
    }

    static <B, A, C, D, E, V> StreamCodec<B, V> composite(
            StreamCodec<? super B, A> codecA,
            Function<V, A> getterA,
            StreamCodec<? super B, C> codecC,
            Function<V, C> getterC,
            StreamCodec<? super B, D> codecD,
            Function<V, D> getterD,
            StreamCodec<? super B, E> codecE,
            Function<V, E> getterE,
            QuadFunction<A, C, D, E, V> factory) {
        return of((buffer, value) -> {
            codecA.encode(buffer, getterA.apply(value));
            codecC.encode(buffer, getterC.apply(value));
            codecD.encode(buffer, getterD.apply(value));
            codecE.encode(buffer, getterE.apply(value));
        }, buffer -> factory.apply(codecA.decode(buffer), codecC.decode(buffer), codecD.decode(buffer), codecE.decode(buffer)));
    }

    @FunctionalInterface
    interface QuadFunction<A, B, C, D, R> {
        R apply(A a, B b, C c, D d);
    }

    @FunctionalInterface
    interface TriFunction<A, B, C, R> {
        R apply(A a, B b, C c);
    }
}
