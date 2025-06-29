package org.featurehouse.mcmod.speedrun.alphabeta.util;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;

import java.util.function.Function;

public final class CodecUtil {
    private CodecUtil() {}

    public static <A, E> Codec<E> conditionalDispatch(Codec<A> codec, Function<? super A, ? extends Codec<E>> decodeMap, Function<? super E, ? extends A> encodeMap) {
        return new Codec<E>() {
            @Override
            public <T> DataResult<Pair<E, T>> decode(DynamicOps<T> ops, T input) {
                return codec.decode(ops, input).flatMap(pair -> {
                    A first = pair.getFirst();
                    T second = pair.getSecond();
                    return decodeMap.apply(first).decode(ops, second);
                });
            }

            @Override
            public <T> DataResult<T> encode(E input, DynamicOps<T> ops, T prefix) {
                A a = encodeMap.apply(input);
                return codec.encode(a, ops, prefix);
            }
        };
    }
}
