package com.morerealisticgeneediting.network.c2s;

import com.morerealisticgeneediting.MoreRealisticGeneEditing;
import com.morerealisticgeneediting.network.C2SPackets;
import com.morerealisticgeneediting.screens.GeneInsertionScreenHandler;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.world.World;

import java.util.UUID;

public class C2SOpenGeneInsertionScreenPacket {

    public static void send(String genomeId, long knockoutPosition) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString(genomeId);
        buf.writeLong(knockoutPosition);
        ClientPlayNetworking.send(C2SPackets.OPEN_GENE_INSERTION_SCREEN, buf);
    }

    public static void handle(MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler, PacketByteBuf buf, net.fabricmc.fabric.api.networking.v1.PacketSender responseSender) {
        String genomeIdStr = buf.readString();
        long knockoutPosition = buf.readLong();
        UUID genomeId = UUID.fromString(genomeIdStr);

        server.execute(() -> {
            NamedScreenHandlerFactory screenHandlerFactory = new ExtendedScreenHandlerFactory((syncId, inv, playerEntity) -> {
                // Create a new buffer for the ScreenHandler
                PacketByteBuf data = new PacketByteBuf(Unpooled.buffer());
                data.writeUuid(genomeId);
                data.writeLong(knockoutPosition);
                return new GeneInsertionScreenHandler(syncId, inv, data);
            }, (buf2) -> {
                buf2.writeUuid(genomeId);
                buf2.writeLong(knockoutPosition);
            });

            player.openHandledScreen(screenHandlerFactory);
        });
    }
}
