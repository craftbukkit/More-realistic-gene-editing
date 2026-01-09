package com.morerealisticgeneediting.genome.provider;

import com.morerealisticgeneediting.genome.GenomeSlice;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * A registry for all available GenomeProviders.
 * It dispatches requests to the appropriate provider based on the genome identifier.
 */
public class GenomeProviderRegistry {

    private static final List<GenomeProvider> providers = new ArrayList<>();

    public static void registerDefaults() {
        providers.add(new LocalGenomeProvider());
        providers.add(new EnsemblRestProvider());
        // Future providers can be added here
    }

    /**
     * Asynchronously fetches a genome slice by delegating to the first capable provider.
     * @param identifier The genome identifier (UUID, Ensembl string, etc.).
     * @param start The starting position of the slice.
     * @param length The length of the slice.
     * @return A CompletableFuture that will resolve to the GenomeSlice, or null if no provider can handle it.
     */
    public static CompletableFuture<GenomeSlice> getSlice(String identifier, long start, int length) {
        for (GenomeProvider provider : providers) {
            if (provider.canProvide(identifier)) {
                return provider.getSlice(identifier, start, length);
            }
        }
        return CompletableFuture.completedFuture(null); // No provider found
    }
}
