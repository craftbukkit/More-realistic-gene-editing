package com.morerealisticgeneediting.network.c2s;

import com.morerealisticgeneediting.MoreRealisticGeneEditing;
import com.morerealisticgeneediting.project.ServerProjectManager;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public class C2SStartProjectPacket {

    public static final Identifier ID = new Identifier(MoreRealisticGeneEditing.MOD_ID, "start_project");

    /**
     * Called on the client to send a request to the server to start a project.
     * @param projectId The ID of the project to start.
     */
    public static void send(String projectId) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString(projectId);
        ClientPlayNetworking.send(ID, buf);
    }

    /**
     * Called on the server when a start_project packet is received.
     */
    public static void receive(MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler, PacketByteBuf buf, PacketSender responseSender) {
        String projectId = buf.readString();

        // The server will execute this on the main server thread.
        server.execute(() -> {
            ServerProjectManager.startProject(player, projectId);
        });
    }
}
