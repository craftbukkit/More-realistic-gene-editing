package com.morerealisticgeneediting.screens;
import com.morerealisticgeneediting.network.PayloadCodecs;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.util.math.BlockPos;
import java.util.UUID;
public record GeneInsertionOpenData(UUID genomeId, BlockPos pos, long totalLength, long revision, long pamPosition,boolean portable) {
    public static final PacketCodec<RegistryByteBuf,GeneInsertionOpenData> CODEC = PayloadCodecs.of(b -> new GeneInsertionOpenData(b.readUuid(),b.readBlockPos(),b.readLong(),b.readLong(),b.readLong(),b.readBoolean()),(b,p)->{b.writeUuid(p.genomeId());b.writeBlockPos(p.pos());b.writeLong(p.totalLength());b.writeLong(p.revision());b.writeLong(p.pamPosition());b.writeBoolean(p.portable());});
}
