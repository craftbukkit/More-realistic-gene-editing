package com.morerealisticgeneediting.network.c2s;
import com.morerealisticgeneediting.network.PayloadCodecs;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import java.util.UUID;
public record C2SCompleteProjectPacket(String projectId) implements CustomPayload {
    public static final Id<C2SCompleteProjectPacket> ID = new Id<>(Identifier.of("morerealisticgeneediting","complete_project"));
    public static final PacketCodec<RegistryByteBuf,C2SCompleteProjectPacket> CODEC = PayloadCodecs.of(b -> new C2SCompleteProjectPacket(b.readString(128)), (b,p) -> { b.writeString(p.projectId(),128); });
    @Override public Id<C2SCompleteProjectPacket> getId() { return ID; }
}
