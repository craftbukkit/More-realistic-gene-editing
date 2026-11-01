package com.morerealisticgeneediting.screen;
import com.morerealisticgeneediting.ethics.EthicsCase;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.util.math.BlockPos;
public record EthicsCaseOpenData(BlockPos pos,EthicsCase ethicsCase) {
    public static final PacketCodec<RegistryByteBuf,EthicsCaseOpenData> CODEC=new PacketCodec<>() {
        public EthicsCaseOpenData decode(RegistryByteBuf buf) { return new EthicsCaseOpenData(buf.readBlockPos(),buf.readBoolean()?EthicsCase.from(buf):null); }
        public void encode(RegistryByteBuf buf,EthicsCaseOpenData value) { buf.writeBlockPos(value.pos());buf.writeBoolean(value.ethicsCase()!=null);if(value.ethicsCase()!=null)value.ethicsCase().write(buf); }
    };
}
