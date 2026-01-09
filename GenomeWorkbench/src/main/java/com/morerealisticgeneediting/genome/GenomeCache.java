package com.morerealisticgeneediting.genome;

import com.morerealisticgeneediting.MoreRealisticGeneEditing;

import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Thread-safe, LRU-evicting cache for Genome objects with TTL support.
 * 
 * Features:
 * - Configurable maximum size with LRU eviction
 * - Time-to-live (TTL) for cached entries
 * - Thread-safe concurrent access using ReentrantReadWriteLock
 * - Automatic cleanup of expired entries
 * - Cache statistics tracking
 */
public class GenomeCache {

    // ========== Configuration ==========
    private static final long DEFAULT_TTL_MS = 5 * 60 * 1000; // 5 minutes
    private static final long CLEANUP_INTERVAL_MS = 60 * 1000; // 1 minute

    // ========== State ==========
    private final int capacity;
    private final long ttlMs;
    private final Map<UUID, CacheEntry> cache;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private long lastCleanup = System.currentTimeMillis();
    
    // ========== Statistics ==========
    private long hits = 0;
    private long misses = 0;
    private long evictions = 0;

    /**
     * Cache entry with timestamp for TTL.
     */
    private record CacheEntry(Genome genome, long createdAt) {
        boolean isExpired(long ttlMs) {
            return System.currentTimeMillis() - createdAt > ttlMs;
        }
        
        long getAge() {
            return System.currentTimeMillis() - createdAt;
        }
    }

    /**
     * Create a cache with default TTL.
     */
    public GenomeCache(int capacity) {
        this(capacity, DEFAULT_TTL_MS);
    }

    /**
     * Create a cache with custom TTL.
     */
    public GenomeCache(int capacity, long ttlMs) {
        this.capacity = Math.max(1, capacity);
        this.ttlMs = ttlMs;
        // Access-ordered LinkedHashMap for LRU behavior
        this.cache = new LinkedHashMap<>(capacity, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<UUID, CacheEntry> eldest) {
                boolean shouldRemove = size() > GenomeCache.this.capacity;
                if (shouldRemove) {
                    evictions++;
                }
                return shouldRemove;
            }
        };
    }

    /**
     * Get a genome from the cache.
     * 
     * @param genomeId The genome UUID
     * @return The genome, or null if not found or expired
     */
    public Genome get(UUID genomeId) {
        if (genomeId == null) return null;
        
        lock.readLock().lock();
        try {
            CacheEntry entry = cache.get(genomeId);
            if (entry == null) {
                misses++;
                return null;
            }
            
            if (entry.isExpired(ttlMs)) {
                misses++;
                // Mark for removal (will be cleaned up later)
                return null;
            }
            
            hits++;
            return entry.genome();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Get a genome wrapped in Optional.
     */
    public Optional<Genome> getOptional(UUID genomeId) {
        return Optional.ofNullable(get(genomeId));
    }

    /**
     * Put a genome into the cache.
     */
    public void put(UUID genomeId, Genome genome) {
        if (genomeId == null || genome == null) return;
        
        lock.writeLock().lock();
        try {
            // Periodic cleanup
            maybeCleanup();
            
            cache.put(genomeId, new CacheEntry(genome, System.currentTimeMillis()));
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Remove a genome from the cache.
     */
    public void remove(UUID genomeId) {
        if (genomeId == null) return;
        
        lock.writeLock().lock();
        try {
            cache.remove(genomeId);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Clear all cached genomes.
     */
    public void clear() {
        lock.writeLock().lock();
        try {
            cache.clear();
            hits = 0;
            misses = 0;
            evictions = 0;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Check if a genome is cached and not expired.
     */
    public boolean contains(UUID genomeId) {
        if (genomeId == null) return false;
        
        lock.readLock().lock();
        try {
            CacheEntry entry = cache.get(genomeId);
            return entry != null && !entry.isExpired(ttlMs);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Get current cache size (including potentially expired entries).
     */
    public int size() {
        lock.readLock().lock();
        try {
            return cache.size();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Get the capacity of this cache.
     */
    public int getCapacity() {
        return capacity;
    }

    /**
     * Force cleanup of expired entries.
     */
    public void cleanup() {
        lock.writeLock().lock();
        try {
            doCleanup();
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Get cache statistics.
     */
    public CacheStats getStats() {
        lock.readLock().lock();
        try {
            long total = hits + misses;
            double hitRate = total > 0 ? (double) hits / total : 0;
            return new CacheStats(cache.size(), capacity, hits, misses, evictions, hitRate);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Cache statistics record.
     */
    public record CacheStats(
        int currentSize,
        int maxSize,
        long hits,
        long misses,
        long evictions,
        double hitRate
    ) {
        @Override
        public String toString() {
            return String.format("CacheStats[size=%d/%d, hits=%d, misses=%d, evictions=%d, hitRate=%.2f%%]",
                currentSize, maxSize, hits, misses, evictions, hitRate * 100);
        }
    }

    // ========== Private Methods ==========

    /**
     * Perform cleanup if enough time has passed.
     * Must be called under write lock.
     */
    private void maybeCleanup() {
        long now = System.currentTimeMillis();
        if (now - lastCleanup >= CLEANUP_INTERVAL_MS) {
            doCleanup();
            lastCleanup = now;
        }
    }

    /**
     * Remove all expired entries.
     * Must be called under write lock.
     */
    private void doCleanup() {
        Iterator<Map.Entry<UUID, CacheEntry>> iter = cache.entrySet().iterator();
        int removed = 0;
        while (iter.hasNext()) {
            Map.Entry<UUID, CacheEntry> entry = iter.next();
            if (entry.getValue().isExpired(ttlMs)) {
                iter.remove();
                removed++;
            }
        }
        if (removed > 0) {
            MoreRealisticGeneEditing.LOGGER.debug("GenomeCache cleanup: removed {} expired entries", removed);
        }
    }
}
