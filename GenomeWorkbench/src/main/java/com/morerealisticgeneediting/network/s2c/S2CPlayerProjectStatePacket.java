package com.morerealisticgeneediting.network.s2c;
import com.morerealisticgeneediting.network.PayloadCodecs;
import com.morerealisticgeneediting.project.PlayerProjectState;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import java.util.Set;
import java.util.HashSet;
public record S2CPlayerProjectStatePacket(PlayerProjectState state) implements CustomPayload {
    public static final Id<S2CPlayerProjectStatePacket> ID=new Id<>(Identifier.of("morerealisticgeneediting","player_project_state"));
    public static final PacketCodec<RegistryByteBuf,S2CPlayerProjectStatePacket> CODEC=PayloadCodecs.of(b->new S2CPlayerProjectStatePacket(new PlayerProjectState(readSet(b),readSet(b))),(b,p)->{writeSet(b,p.state.activeProjectIds());writeSet(b,p.state.completedProjectIds());});
    private static Set<String> readSet(RegistryByteBuf b) {
        int count=b.readVarInt();if(count<0||count>256)throw new IllegalArgumentException("Too many projects");
        Set<String> result=new HashSet<>();for(int i=0;i<count;i++)result.add(b.readString(128));return Set.copyOf(result);
    }
    private static void writeSet(RegistryByteBuf b,Set<String> values) {if(values.size()>256)throw new IllegalArgumentException("Too many projects");b.writeVarInt(values.size());for(String s:values)b.writeString(s,128);}
    @Override public Id<S2CPlayerProjectStatePacket> getId(){return ID;}
    public static void send(ServerPlayerEntity player,PlayerProjectState state) {ServerPlayNetworking.send(player,new S2CPlayerProjectStatePacket(state));}
}
