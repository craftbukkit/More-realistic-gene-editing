package com.morerealisticgeneediting.network.s2c;
import com.morerealisticgeneediting.network.PayloadCodecs;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import java.util.UUID;
public record S2CSendGenomeSlicePacket(UUID genomeId,int syncId,long revision,long totalLength,long start,String sequence) implements CustomPayload {
    public static final Id<S2CSendGenomeSlicePacket> ID=new Id<>(Identifier.of("morerealisticgeneediting","send_genome_slice"));
    public static final PacketCodec<RegistryByteBuf,S2CSendGenomeSlicePacket> CODEC=PayloadCodecs.of(b->new S2CSendGenomeSlicePacket(b.readUuid(),b.readVarInt(),b.readLong(),b.readLong(),b.readLong(),b.readString(4096)),(b,p)->{b.writeUuid(p.genomeId());b.writeVarInt(p.syncId());b.writeLong(p.revision());b.writeLong(p.totalLength());b.writeLong(p.start());b.writeString(p.sequence(),4096);});
    @Override public Id<S2CSendGenomeSlicePacket> getId(){return ID;}
}
