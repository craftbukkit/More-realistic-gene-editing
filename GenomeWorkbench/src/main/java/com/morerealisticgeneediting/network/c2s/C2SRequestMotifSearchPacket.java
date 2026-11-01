package com.morerealisticgeneediting.network.c2s;
import com.morerealisticgeneediting.network.PayloadCodecs;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import java.util.UUID;
public record C2SRequestMotifSearchPacket(UUID genomeId, int syncId, long revision, long start, String motif) implements CustomPayload {
    public static final Id<C2SRequestMotifSearchPacket> ID = new Id<>(Identifier.of("morerealisticgeneediting","request_motif_search"));
    public static final PacketCodec<RegistryByteBuf,C2SRequestMotifSearchPacket> CODEC = PayloadCodecs.of(b -> new C2SRequestMotifSearchPacket(b.readUuid(),b.readVarInt(),b.readLong(),b.readLong(),b.readString(64)), (b,p) -> { b.writeUuid(p.genomeId());b.writeVarInt(p.syncId());b.writeLong(p.revision());b.writeLong(p.start());b.writeString(p.motif(),64); });
    @Override public Id<C2SRequestMotifSearchPacket> getId() { return ID; }
}
