package com.morerealisticgeneediting.network.c2s;

import com.morerealisticgeneediting.MoreRealisticGeneEditing;
import com.morerealisticgeneediting.genome.provider.GenomeProvider;
import com.morerealisticgeneediting.genome.provider.GenomeProviderRegistry;
import com.morerealisticgeneediting.network.C2SPackets;
import com.morerealisticgeneediting.network.s2c.S2CSendGenomeSlicePacket;
import com.morerealisticgeneediting.security.RateLimiters;
import com.morerealisticgeneediting.security.Validators;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Optional;

/**
 * Client-to-Server packet for requesting a genome slice.
 * 
 * Security features:
 * - Rate limiting (500ms cooldown, burst protection)
 * - Input validation (position, length)
 * - Identifier sanitization
 * - Async processing to prevent server lag
 */
public class C2SRequestGenomeSlicePacket {

    // ========== Constants ==========
    private static final int MAX_IDENTIFIER_LENGTH = 256;
    private static final int MAX_SLICE_LENGTH = Validators.MAX_SLICE_LENGTH;
    private static final long MAX_POSITION = Validators.MAX_GENOME_LENGTH;

    /**
     * Send a genome slice request to the server.
     * 
     * @param genomeIdentifier The genome identifier
     * @param start Start position (0-based)
     * @param length Length of the slice
     */
    public static void send(String genomeIdentifier, long start, int length) {
        // Client-side validation
        if (genomeIdentifier == null || genomeIdentifier.length() > MAX_IDENTIFIER_LENGTH) {
            MoreRealisticGeneEditing.LOGGER.warn("Invalid genome identifier");
            return;
        }
        if (start < 0 || length <= 0 || length > MAX_SLICE_LENGTH) {
            MoreRealisticGeneEditing.LOGGER.warn("Invalid slice parameters: start={}, length={}", start, length);
            return;
        }

        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString(genomeIdentifier, MAX_IDENTIFIER_LENGTH);
        buf.writeLong(start);
        buf.writeInt(length);

        ClientPlayNetworking.send(C2SPackets.REQUEST_GENOME_SLICE, buf);
    }

    /**
     * Handle incoming genome slice request on the server.
     */
    public static void handle(MinecraftServer server, ServerPlayerEntity player, 
                              ServerPlayNetworkHandler handler, PacketByteBuf buf, 
                              net.fabricmc.fabric.api.networking.v1.PacketSender responseSender) {
        
        // ========== Rate Limiting ==========
        if (!RateLimiters.GENOME_SLICE.tryAcquire(player.getUuid())) {
            MoreRealisticGeneEditing.LOGGER.debug("Rate limited genome slice request from {}", 
                player.getName().getString());
            return;
        }

        // ========== Parse Packet Data ==========
        String genomeIdentifier;
        long start;
        int length;
        
        try {
            genomeIdentifier = buf.readString(MAX_IDENTIFIER_LENGTH);
            start = buf.readLong();
            length = buf.readInt();
        } catch (Exception e) {
            MoreRealisticGeneEditing.LOGGER.warn("Malformed genome slice packet from {}: {}", 
                player.getName().getString(), e.getMessage());
            return;
        }

        // ========== Input Validation ==========
        
        // Validate identifier
        if (genomeIdentifier == null || genomeIdentifier.isEmpty()) {
            MoreRealisticGeneEditing.LOGGER.warn("Empty genome identifier from {}", 
                player.getName().getString());
            return;
        }
        
        // Sanitize identifier (remove potentially dangerous characters)
        String sanitizedId = Validators.sanitizeString(genomeIdentifier, MAX_IDENTIFIER_LENGTH);
        if (!sanitizedId.equals(genomeIdentifier)) {
            MoreRealisticGeneEditing.LOGGER.warn("Sanitized genome identifier from {}: '{}' -> '{}'", 
                player.getName().getString(), genomeIdentifier, sanitizedId);
        }

        // Validate range
        Validators.ValidationResult rangeValidation = Validators.validateSliceRequest(start, length, MAX_POSITION);
        if (rangeValidation.isFailed()) {
            MoreRealisticGeneEditing.LOGGER.warn("Invalid slice range from {}: {}", 
                player.getName().getString(), rangeValidation.errorMessage());
            return;
        }

        // ========== Process Request (Async) ==========
        final String finalIdentifier = sanitizedId;
        final long finalStart = start;
        final int finalLength = length;

        server.execute(() -> {
            try {
                Optional<GenomeProvider> providerOpt = GenomeProviderRegistry.getProvider(finalIdentifier);
                
                if (providerOpt.isEmpty()) {
                    MoreRealisticGeneEditing.LOGGER.debug("No provider found for genome: {}", finalIdentifier);
                    return;
                }

                providerOpt.get().getSlice(finalIdentifier, finalStart, finalLength)
                    .thenAccept(slice -> {
                        if (slice != null && player.networkHandler != null) {
                            S2CSendGenomeSlicePacket.send(player, slice);
                        }
                    })
                    .exceptionally(ex -> {
                        MoreRealisticGeneEditing.LOGGER.error("Failed to get genome slice for {}: {}", 
                            player.getName().getString(), ex.getMessage());
                        return null;
                    });
                    
            } catch (Exception e) {
                MoreRealisticGeneEditing.LOGGER.error("Error processing genome slice request: {}", e.getMessage());
            }
        });
    }
}
