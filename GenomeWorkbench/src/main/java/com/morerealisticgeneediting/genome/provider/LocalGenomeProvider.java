package com.morerealisticgeneediting.genome.provider;

import com.morerealisticgeneediting.MoreRealisticGeneEditing;
import com.morerealisticgeneediting.genome.Genome;
import com.morerealisticgeneediting.genome.GenomeSlice;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class LocalGenomeProvider implements GenomeProvider {

    @Override
    public boolean canProvide(String identifier) {
        try {
            UUID.fromString(identifier);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    @Override
    public CompletableFuture<GenomeSlice> getSlice(String identifier, long start, int length) {
        try {
            UUID genomeId = UUID.fromString(identifier);
            Genome genome = MoreRealisticGeneEditing.genomeCache.get(genomeId);
            if (genome != null) {
                String sequence = genome.getSequence(start, length);
                GenomeSlice slice = new GenomeSlice(genome, sequence, start);
                return CompletableFuture.completedFuture(slice);
            } else {
                return CompletableFuture.completedFuture(null);
            }
        } catch (Exception e) {
            // Catch broader exceptions to be safe
            return CompletableFuture.failedFuture(e);
        }
    }
}
