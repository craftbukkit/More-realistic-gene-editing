package com.morerealisticgeneediting.network.c2s;
import com.morerealisticgeneediting.network.PayloadCodecs;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import java.util.UUID;
public record C2SRequestGenomeSlicePacket(UUID genomeId, int syncId, long revision, long start, int length) implements CustomPayload {
    public static final Id<C2SRequestGenomeSlicePacket> ID = new Id<>(Identifier.of("morerealisticgeneediting","request_genome_slice"));
    public static final PacketCodec<RegistryByteBuf,C2SRequestGenomeSlicePacket> CODEC = PayloadCodecs.of(b -> new C2SRequestGenomeSlicePacket(b.readUuid(),b.readVarInt(),b.readLong(),b.readLong(),b.readVarInt()), (b,p) -> { b.writeUuid(p.genomeId());b.writeVarInt(p.syncId());b.writeLong(p.revision());b.writeLong(p.start());b.writeVarInt(p.length()); });
    @Override public Id<C2SRequestGenomeSlicePacket> getId() { return ID; }
}
