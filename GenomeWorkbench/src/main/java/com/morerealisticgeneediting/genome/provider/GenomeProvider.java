package com.morerealisticgeneediting.genome.provider;

import com.morerealisticgeneediting.genome.GenomeSlice;

import java.util.concurrent.CompletableFuture;

/**
 * An interface for providing genome data from various sources (local cache, remote API, etc.).
 * This allows for a pluggable architecture, making the system highly extensible.
 */
public interface GenomeProvider {

    /**
     * Checks if this provider can handle a request for the given identifier.
     * @param identifier The genome identifier (e.g., a UUID string, or "ensembl:homo_sapiens:GRCh38:1").
     * @return true if this provider can handle the identifier, false otherwise.
     */
    boolean canProvide(String identifier);

    /**
     * Asynchronously fetches a slice of a genome.
     * @param identifier The genome identifier.
     * @param start The starting position of the slice.
     * @param length The length of the slice.
     * @return A CompletableFuture that will resolve to the requested GenomeSlice, or null if it cannot be provided.
     */
    CompletableFuture<GenomeSlice> getSlice(String identifier, long start, int length);
}
