package com.morerealisticgeneediting.screens;
import com.morerealisticgeneediting.network.PayloadCodecs;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.util.math.BlockPos;
import java.util.UUID;
public record TerminalOpenData(UUID genomeId, BlockPos pos, long totalLength, long revision, boolean portable) {
    public TerminalOpenData(UUID id,BlockPos pos,long length,long revision){this(id,pos,length,revision,false);}
    public static final PacketCodec<RegistryByteBuf,TerminalOpenData> CODEC = PayloadCodecs.of(b -> new TerminalOpenData(b.readUuid(),b.readBlockPos(),b.readLong(),b.readLong(),b.readBoolean()),(b,p)->{b.writeUuid(p.genomeId());b.writeBlockPos(p.pos());b.writeLong(p.totalLength());b.writeLong(p.revision());b.writeBoolean(p.portable());});
}
