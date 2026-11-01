package com.morerealisticgeneediting.network.c2s;
import com.morerealisticgeneediting.network.PayloadCodecs;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import java.util.UUID;
public record C2SPerformGeneInsertionPacket(UUID genomeId, int syncId, long revision, long pamPosition, String sequence) implements CustomPayload {
    public static final Id<C2SPerformGeneInsertionPacket> ID = new Id<>(Identifier.of("morerealisticgeneediting","perform_gene_insertion"));
    public static final PacketCodec<RegistryByteBuf,C2SPerformGeneInsertionPacket> CODEC = PayloadCodecs.of(b -> new C2SPerformGeneInsertionPacket(b.readUuid(),b.readVarInt(),b.readLong(),b.readLong(),b.readString(1024)), (b,p) -> { b.writeUuid(p.genomeId());b.writeVarInt(p.syncId());b.writeLong(p.revision());b.writeLong(p.pamPosition());b.writeString(p.sequence(),1024); });
    @Override public Id<C2SPerformGeneInsertionPacket> getId() { return ID; }
}
