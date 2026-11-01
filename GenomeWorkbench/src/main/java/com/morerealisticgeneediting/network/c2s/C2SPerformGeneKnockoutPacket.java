package com.morerealisticgeneediting.network.c2s;
import com.morerealisticgeneediting.network.PayloadCodecs;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import java.util.UUID;
public record C2SPerformGeneKnockoutPacket(UUID genomeId, int syncId, long revision, long pamPosition) implements CustomPayload {
    public static final Id<C2SPerformGeneKnockoutPacket> ID = new Id<>(Identifier.of("morerealisticgeneediting","perform_gene_knockout"));
    public static final PacketCodec<RegistryByteBuf,C2SPerformGeneKnockoutPacket> CODEC = PayloadCodecs.of(b -> new C2SPerformGeneKnockoutPacket(b.readUuid(),b.readVarInt(),b.readLong(),b.readLong()), (b,p) -> { b.writeUuid(p.genomeId());b.writeVarInt(p.syncId());b.writeLong(p.revision());b.writeLong(p.pamPosition()); });
    @Override public Id<C2SPerformGeneKnockoutPacket> getId() { return ID; }
}
