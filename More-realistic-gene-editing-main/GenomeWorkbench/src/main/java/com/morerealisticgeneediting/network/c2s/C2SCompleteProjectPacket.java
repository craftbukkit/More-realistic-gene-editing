package com.morerealisticgeneediting.network.c2s;

import com.morerealisticgeneediting.project.ServerProjectManager;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public class C2SCompleteProjectPacket {
    public static final Identifier ID = new Identifier("morerealisticgeneediting", "complete_project");

    public static void send(String projectId) {
        PacketByteBuf buf = new PacketByteBuf(io.netty.buffer.Unpooled.buffer());
        buf.writeString(projectId);
        ServerPlayNetworking.send(ID, buf);
    }

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(ID, C2SCompleteProjectPacket::receive);
    }

    private static void receive(MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler, PacketByteBuf buf, PacketSender responseSender) {
        String projectId = buf.readString();
        server.execute(() -> {
            ServerProjectManager.completeProject(player, projectId);
        });
    }
}
