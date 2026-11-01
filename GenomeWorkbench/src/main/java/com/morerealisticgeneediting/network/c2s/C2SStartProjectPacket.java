package com.morerealisticgeneediting.network.c2s;
import com.morerealisticgeneediting.network.PayloadCodecs;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import java.util.UUID;
public record C2SStartProjectPacket(String projectId) implements CustomPayload {
    public static final Id<C2SStartProjectPacket> ID = new Id<>(Identifier.of("morerealisticgeneediting","start_project"));
    public static final PacketCodec<RegistryByteBuf,C2SStartProjectPacket> CODEC = PayloadCodecs.of(b -> new C2SStartProjectPacket(b.readString(128)), (b,p) -> { b.writeString(p.projectId(),128); });
    @Override public Id<C2SStartProjectPacket> getId() { return ID; }
}
