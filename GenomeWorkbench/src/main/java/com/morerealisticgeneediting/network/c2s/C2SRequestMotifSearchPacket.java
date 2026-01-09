package com.morerealisticgeneediting.network.c2s;

import com.morerealisticgeneediting.MoreRealisticGeneEditing;
import com.morerealisticgeneediting.genome.Genome;
import com.morerealisticgeneediting.genome.provider.GenomeProvider;
import com.morerealisticgeneediting.genome.provider.GenomeProviderRegistry;
import com.morerealisticgeneediting.genome.provider.LocalGenomeProvider;
import com.morerealisticgeneediting.network.C2SPackets;
import com.morerealisticgeneediting.network.s2c.S2CSendMotifSearchResultsPacket;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class C2SRequestMotifSearchPacket {

    private static final int MAX_MOTIF_LENGTH = 100;
    private static final ExecutorService motifSearchExecutor = Executors.newFixedThreadPool(2);

    public static void send(String genomeIdentifier, String motif) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString(genomeIdentifier);
        buf.writeString(motif);
        ClientPlayNetworking.send(C2SPackets.REQUEST_MOTIF_SEARCH, buf);
    }

    public static void handle(MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler, PacketByteBuf buf, net.fabricmc.fabric.api.networking.v1.PacketSender responseSender) {
        String genomeIdentifier = buf.readString();
        String motif = buf.readString();

        if (motif.length() > MAX_MOTIF_LENGTH) {
            MoreRealisticGeneEditing.LOGGER.warn("Player {} sent a motif that was too long: {}", player.getName().getString(), motif.length());
            return;
        }

        motifSearchExecutor.submit(() -> {
            Optional<GenomeProvider> providerOpt = GenomeProviderRegistry.getProvider(genomeIdentifier);
            if (providerOpt.isPresent() && providerOpt.get() instanceof LocalGenomeProvider) {
                LocalGenomeProvider localProvider = (LocalGenomeProvider) providerOpt.get();
                localProvider.getGenome(genomeIdentifier).thenAccept(genome -> {
                    if (genome != null) {
                        if (!genome.getOwner().equals(player.getUuid())) {
                            MoreRealisticGeneEditing.LOGGER.warn("Player {} tried to search a genome they do not own: {}", player.getName().getString(), genomeIdentifier);
                            return;
                        }
                        List<Long> hits = new ArrayList<>();
                        String sequence = genome.getUnpackedSequence();
                        for (int i = 0; i <= sequence.length() - motif.length(); i++) {
                            if (sequence.substring(i, i + motif.length()).equalsIgnoreCase(motif)) {
                                hits.add((long) i);
                            }
                        }
                        S2CSendMotifSearchResultsPacket.send(player, hits);
                    }
                });
            } else {
                MoreRealisticGeneEditing.LOGGER.warn("Motif search requested for non-local or unknown genome: {}", genomeIdentifier);
            }
        });
    }
}
