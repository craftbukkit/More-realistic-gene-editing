package com.morerealisticgeneediting.network.c2s;
import com.morerealisticgeneediting.network.PayloadCodecs;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import java.util.UUID;
public record C2SOpenGeneInsertionScreenPacket(UUID genomeId, int syncId, long revision, long pamPosition) implements CustomPayload {
    public static final Id<C2SOpenGeneInsertionScreenPacket> ID = new Id<>(Identifier.of("morerealisticgeneediting","open_gene_insertion_screen"));
    public static final PacketCodec<RegistryByteBuf,C2SOpenGeneInsertionScreenPacket> CODEC = PayloadCodecs.of(b -> new C2SOpenGeneInsertionScreenPacket(b.readUuid(),b.readVarInt(),b.readLong(),b.readLong()), (b,p) -> { b.writeUuid(p.genomeId());b.writeVarInt(p.syncId());b.writeLong(p.revision());b.writeLong(p.pamPosition()); });
    @Override public Id<C2SOpenGeneInsertionScreenPacket> getId() { return ID; }
}
