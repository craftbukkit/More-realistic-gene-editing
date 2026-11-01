package com.morerealisticgeneediting.network.s2c;
import com.morerealisticgeneediting.network.PayloadCodecs;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import java.util.UUID;
public record S2CSendMotifSearchResultsPacket(UUID genomeId,int syncId,long revision,long scanStart,int scannedLength,long[] positions,boolean truncated) implements CustomPayload {
    public static final Id<S2CSendMotifSearchResultsPacket> ID=new Id<>(Identifier.of("morerealisticgeneediting","send_motif_search_results"));
    public static final PacketCodec<RegistryByteBuf,S2CSendMotifSearchResultsPacket> CODEC=PayloadCodecs.of(b->new S2CSendMotifSearchResultsPacket(b.readUuid(),b.readVarInt(),b.readLong(),b.readLong(),b.readVarInt(),b.readLongArray(128),b.readBoolean()),(b,p)->{b.writeUuid(p.genomeId());b.writeVarInt(p.syncId());b.writeLong(p.revision());b.writeLong(p.scanStart());b.writeVarInt(p.scannedLength());b.writeLongArray(p.positions());b.writeBoolean(p.truncated());});
    @Override public Id<S2CSendMotifSearchResultsPacket> getId(){return ID;}
}
