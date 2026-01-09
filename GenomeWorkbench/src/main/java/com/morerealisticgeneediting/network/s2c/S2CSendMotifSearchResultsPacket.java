package com.morerealisticgeneediting.network.s2c;

import com.morerealisticgeneediting.network.PacketIdentifiers;
import com.morerealisticgeneediting.screens.GenomeTerminalScreen;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

public class S2CSendMotifSearchResultsPacket {

    public static void send(ServerPlayerEntity player, List<Long> results) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeLongArray(results.stream().mapToLong(l -> l).toArray());
        ServerPlayNetworking.send(player, PacketIdentifiers.SEND_MOTIF_SEARCH_RESULTS, buf);
    }

    public static void registerClientListener() {
        ClientPlayNetworking.registerGlobalReceiver(PacketIdentifiers.SEND_MOTIF_SEARCH_RESULTS, (client, handler, buf, responseSender) -> {
            long[] resultsArray = buf.readLongArray();
            List<Long> results = LongStream.of(resultsArray).boxed().collect(Collectors.toList());

            client.execute(() -> {
                Screen currentScreen = MinecraftClient.getInstance().currentScreen;
                if (currentScreen instanceof GenomeTerminalScreen) {
                    ((GenomeTerminalScreen) currentScreen).setMotifHits(results);
                }
            });
        });
    }
}
