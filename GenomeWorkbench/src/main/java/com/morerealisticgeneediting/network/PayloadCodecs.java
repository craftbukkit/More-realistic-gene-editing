package com.morerealisticgeneediting.network;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import java.util.function.BiConsumer;
import java.util.function.Function;
public final class PayloadCodecs {
    private PayloadCodecs() {}
    public static <T> PacketCodec<RegistryByteBuf,T> of(Function<RegistryByteBuf,T> read, BiConsumer<RegistryByteBuf,T> write) {
        return new PacketCodec<>() {
            public T decode(RegistryByteBuf buf) { return read.apply(buf); }
            public void encode(RegistryByteBuf buf, T value) { write.accept(buf,value); }
        };
    }
}
