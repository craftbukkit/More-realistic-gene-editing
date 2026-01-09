package com.morerealisticgeneediting.security;

import com.morerealisticgeneediting.MoreRealisticGeneEditing;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Thread-safe rate limiter with sliding window and burst protection.
 * 
 * Features:
 * - Configurable cooldown between requests
 * - Burst protection with request counting
 * - Automatic cleanup of stale entries
 * - Per-player tracking
 * - Statistics tracking
 */
public final class RateLimiters {

    // ========== Configuration ==========
    private static final long CLEANUP_INTERVAL_MS = 60 * 1000; // 1 minute
    private static final long ENTRY_EXPIRY_MS = 10 * 60 * 1000; // 10 minutes
    private static final int DEFAULT_BURST_LIMIT = 10;
    private static final long DEFAULT_BURST_WINDOW_MS = 10 * 1000; // 10 seconds

    // ========== State ==========
    private final Map<UUID, RateLimitEntry> entries = new ConcurrentHashMap<>();
    private final long cooldownMillis;
    private final int burstLimit;
    private final long burstWindowMs;
    private volatile long lastCleanup = System.currentTimeMillis();
    
    // ========== Statistics ==========
    private final AtomicInteger totalAllowed = new AtomicInteger(0);
    private final AtomicInteger totalBlocked = new AtomicInteger(0);

    /**
     * Rate limit entry tracking requests per player.
     */
    private static class RateLimitEntry {
        volatile long lastRequestTime = 0;
        volatile long windowStart = 0;
        final AtomicInteger requestsInWindow = new AtomicInteger(0);
        
        void reset(long now) {
            windowStart = now;
            requestsInWindow.set(0);
        }
        
        boolean isExpired() {
            return System.currentTimeMillis() - lastRequestTime > ENTRY_EXPIRY_MS;
        }
    }

    /**
     * Create a rate limiter with default burst settings.
     * 
     * @param cooldownMillis Minimum time between allowed requests
     */
    public RateLimiters(long cooldownMillis) {
        this(cooldownMillis, DEFAULT_BURST_LIMIT, DEFAULT_BURST_WINDOW_MS);
    }

    /**
     * Create a rate limiter with custom burst settings.
     * 
     * @param cooldownMillis Minimum time between allowed requests
     * @param burstLimit Maximum requests in burst window
     * @param burstWindowMs Duration of burst window in milliseconds
     */
    public RateLimiters(long cooldownMillis, int burstLimit, long burstWindowMs) {
        this.cooldownMillis = Math.max(0, cooldownMillis);
        this.burstLimit = Math.max(1, burstLimit);
        this.burstWindowMs = Math.max(1000, burstWindowMs);
    }

    /**
     * Checks if a player is allowed to make a request.
     * If allowed, updates the timestamp and returns true.
     * If rate-limited, returns false.
     *
     * @param playerUuid The UUID of the player making the request
     * @return true if the request is allowed, false if rate-limited
     */
    public boolean tryAcquire(UUID playerUuid) {
        if (playerUuid == null) return false;
        
        maybeCleanup();
        
        long now = System.currentTimeMillis();
        RateLimitEntry entry = entries.computeIfAbsent(playerUuid, k -> new RateLimitEntry());
        
        // Check cooldown
        if (now - entry.lastRequestTime < cooldownMillis) {
            totalBlocked.incrementAndGet();
            return false;
        }
        
        // Check burst limit
        if (now - entry.windowStart > burstWindowMs) {
            entry.reset(now);
        }
        
        if (entry.requestsInWindow.get() >= burstLimit) {
            totalBlocked.incrementAndGet();
            return false;
        }
        
        // Allow request
        entry.lastRequestTime = now;
        entry.requestsInWindow.incrementAndGet();
        totalAllowed.incrementAndGet();
        return true;
    }

    /**
     * Check if a player is currently rate-limited without consuming a request.
     */
    public boolean isRateLimited(UUID playerUuid) {
        if (playerUuid == null) return true;
        
        RateLimitEntry entry = entries.get(playerUuid);
        if (entry == null) return false;
        
        long now = System.currentTimeMillis();
        
        // Check cooldown
        if (now - entry.lastRequestTime < cooldownMillis) {
            return true;
        }
        
        // Check burst limit
        if (now - entry.windowStart <= burstWindowMs && 
            entry.requestsInWindow.get() >= burstLimit) {
            return true;
        }
        
        return false;
    }

    /**
     * Get remaining cooldown time for a player in milliseconds.
     * Returns 0 if not rate-limited.
     */
    public long getRemainingCooldown(UUID playerUuid) {
        if (playerUuid == null) return cooldownMillis;
        
        RateLimitEntry entry = entries.get(playerUuid);
        if (entry == null) return 0;
        
        long elapsed = System.currentTimeMillis() - entry.lastRequestTime;
        return Math.max(0, cooldownMillis - elapsed);
    }

    /**
     * Reset rate limit for a specific player.
     */
    public void reset(UUID playerUuid) {
        if (playerUuid != null) {
            entries.remove(playerUuid);
        }
    }

    /**
     * Clear all rate limit entries.
     */
    public void clear() {
        entries.clear();
        totalAllowed.set(0);
        totalBlocked.set(0);
    }

    /**
     * Get statistics for this rate limiter.
     */
    public RateLimitStats getStats() {
        int allowed = totalAllowed.get();
        int blocked = totalBlocked.get();
        int total = allowed + blocked;
        double blockRate = total > 0 ? (double) blocked / total : 0;
        return new RateLimitStats(entries.size(), allowed, blocked, blockRate);
    }

    /**
     * Rate limiter statistics.
     */
    public record RateLimitStats(
        int activeEntries,
        int totalAllowed,
        int totalBlocked,
        double blockRate
    ) {
        @Override
        public String toString() {
            return String.format("RateLimitStats[active=%d, allowed=%d, blocked=%d, blockRate=%.2f%%]",
                activeEntries, totalAllowed, totalBlocked, blockRate * 100);
        }
    }

    // ========== Private Methods ==========

    /**
     * Perform cleanup of expired entries if needed.
     */
    private void maybeCleanup() {
        long now = System.currentTimeMillis();
        if (now - lastCleanup < CLEANUP_INTERVAL_MS) return;
        
        lastCleanup = now;
        
        Iterator<Map.Entry<UUID, RateLimitEntry>> iter = entries.entrySet().iterator();
        int removed = 0;
        while (iter.hasNext()) {
            if (iter.next().getValue().isExpired()) {
                iter.remove();
                removed++;
            }
        }
        
        if (removed > 0) {
            MoreRealisticGeneEditing.LOGGER.debug("RateLimiters cleanup: removed {} expired entries", removed);
        }
    }

    // ========== Pre-configured Instances ==========

    /** Rate limiter for genome slice requests: 500ms cooldown, 10 per 10s */
    public static final RateLimiters GENOME_SLICE = new RateLimiters(500, 10, 10000);
    
    /** Rate limiter for motif search requests: 1s cooldown, 5 per 30s */
    public static final RateLimiters MOTIF_SEARCH = new RateLimiters(1000, 5, 30000);
    
    /** Rate limiter for gene editing operations: 2s cooldown, 3 per 30s */
    public static final RateLimiters GENE_EDITING = new RateLimiters(2000, 3, 30000);
    
    /** Rate limiter for project operations: 500ms cooldown, 20 per 60s */
    public static final RateLimiters PROJECT_OPS = new RateLimiters(500, 20, 60000);
}
