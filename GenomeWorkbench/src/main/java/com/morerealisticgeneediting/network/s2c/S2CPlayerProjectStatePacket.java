package com.morerealisticgeneediting.network.s2c;

import com.morerealisticgeneediting.MoreRealisticGeneEditing;
import com.morerealisticgeneediting.project.PlayerProjectState;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.HashSet;
import java.util.Set;

public class S2CPlayerProjectStatePacket {

    public static final Identifier ID = new Identifier(MoreRealisticGeneEditing.MOD_ID, "player_project_state");

    public static void send(ServerPlayerEntity player, PlayerProjectState state) {
        PacketByteBuf buf = PacketByteBufs.create();
        writeState(buf, state);
        ServerPlayNetworking.send(player, ID, buf);
    }

    public static PlayerProjectState readState(PacketByteBuf buf) {
        Set<String> active = new HashSet<>();
        int activeCount = buf.readVarInt();
        for (int i = 0; i < activeCount; i++) {
            active.add(buf.readString());
        }

        Set<String> completed = new HashSet<>();
        int completedCount = buf.readVarInt();
        for (int i = 0; i < completedCount; i++) {
            completed.add(buf.readString());
        }

        return new PlayerProjectState(active, completed);
    }

    private static void writeState(PacketByteBuf buf, PlayerProjectState state) {
        buf.writeVarInt(state.activeProjectIds().size());
        for (String id : state.activeProjectIds()) {
            buf.writeString(id);
        }

        buf.writeVarInt(state.completedProjectIds().size());
        for (String id : state.completedProjectIds()) {
            buf.writeString(id);
        }
    }
}
