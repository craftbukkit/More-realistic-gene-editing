package com.morerealisticgeneediting.network.c2s;

import com.morerealisticgeneediting.MoreRealisticGeneEditing;
import com.morerealisticgeneediting.genome.Genome;
import com.morerealisticgeneediting.network.C2SPackets;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.UUID;

public class C2SPerformGeneKnockoutPacket {

    public static void send(String genomeIdentifier, long pamSiteAbsolutePosition, int protospacerLength) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString(genomeIdentifier);
        buf.writeLong(pamSiteAbsolutePosition);
        buf.writeInt(protospacerLength);

        ClientPlayNetworking.send(C2SPackets.PERFORM_GENE_KNOCKOUT, buf);
    }

    public static void handle(MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler, PacketByteBuf buf, net.fabricmc.fabric.api.networking.v1.PacketSender responseSender) {
        String genomeIdentifier = buf.readString();
        long pamPosition = buf.readLong();
        int protospacerLength = buf.readInt();

        server.execute(() -> {
            try {
                UUID genomeUUID = UUID.fromString(genomeIdentifier);
                if (MoreRealisticGeneEditing.genomeCache.containsKey(genomeUUID)) {
                    Genome genome = MoreRealisticGeneEditing.genomeCache.get(genomeUUID);

                    if (!genome.getOwner().equals(player.getUuid())) {
                        MoreRealisticGeneEditing.LOGGER.warn("Player {} tried to perform knockout on a genome they do not own: {}", player.getName().getString(), genomeUUID);
                        return;
                    }

                    Genome newGenome = genome.performKnockout(pamPosition, protospacerLength);
                    UUID newId = newGenome.getUUID();
                    MoreRealisticGeneEditing.genomeCache.put(newId, newGenome);
                    MoreRealisticGeneEditing.LOGGER.info("Performed knockout on genome {}, created new genome {}", genomeUUID, newId);
                } else {
                    MoreRealisticGeneEditing.LOGGER.warn("Player {} tried to perform knockout on unknown genome {}", player.getName().getString(), genomeUUID);
                }
            } catch (IllegalArgumentException e) {
                MoreRealisticGeneEditing.LOGGER.warn("Player {} sent invalid UUID for knockout: {}", player.getName().getString(), genomeIdentifier);
            }
        });
    }
}
