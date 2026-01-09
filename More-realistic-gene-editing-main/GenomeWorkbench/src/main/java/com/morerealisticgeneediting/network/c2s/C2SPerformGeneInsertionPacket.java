package com.morerealisticgeneediting.network.c2s;

import com.morerealisticgeneediting.MoreRealisticGeneEditing;
import com.morerealisticgeneediting.genome.Genome;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

import java.util.UUID;

public class C2SPerformGeneInsertionPacket {

    public static final Identifier PACKET_ID = new Identifier(MoreRealisticGeneEditing.MOD_ID, "perform_gene_insertion");

    public static void send(UUID genomeId, long knockoutPosition, String geneSequence) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeUuid(genomeId);
        buf.writeLong(knockoutPosition);
        buf.writeString(geneSequence);
        ClientPlayNetworking.send(PACKET_ID, buf);
    }

    public static void handle(net.minecraft.server.MinecraftServer server, net.minecraft.server.network.ServerPlayerEntity player, net.minecraft.server.network.ServerPlayNetworkHandler handler, PacketByteBuf buf, net.fabricmc.fabric.api.networking.v1.PacketSender responseSender) {
        UUID genomeId = buf.readUuid();
        long knockoutPosition = buf.readLong();
        String geneSequence = buf.readString();

        server.execute(() -> {
            Genome genome = MoreRealisticGeneEditing.genomeCache.get(genomeId);
            if (genome != null) {
                MoreRealisticGeneEditing.LOGGER.info("Performing gene insertion for genome {}.", genomeId);
                Genome newGenome = genome.performInsertion(knockoutPosition, geneSequence);
                MoreRealisticGeneEditing.genomeCache.put(genomeId, newGenome);
                MoreRealisticGeneEditing.LOGGER.info("Gene insertion complete. Genome {} is now updated.", genomeId);
            } else {
                MoreRealisticGeneEditing.LOGGER.error("Could not find genome with ID {} to perform insertion.", genomeId);
            }
        });
    }
}
