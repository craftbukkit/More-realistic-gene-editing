package com.morerealisticgeneediting.util;

import com.morerealisticgeneediting.MoreRealisticGeneEditing;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * Performance monitoring utilities for tracking execution times and counts.
 * 
 * Features:
 * - Thread-safe timing measurements
 * - Automatic statistics calculation (avg, min, max)
 * - Memory usage tracking
 * - Periodic reporting
 */
public final class PerformanceMonitor {

    // ========== Constants ==========
    private static final long REPORT_INTERVAL_MS = 60 * 1000; // 1 minute
    private static volatile boolean enabled = true;

    // ========== State ==========
    private static final Map<String, TimingStats> timings = new ConcurrentHashMap<>();
    private static final Map<String, LongAdder> counters = new ConcurrentHashMap<>();
    private static volatile long lastReportTime = System.currentTimeMillis();

    private PerformanceMonitor() {} // Prevent instantiation

    // ========== Timing ==========

    /**
     * Timing statistics for an operation.
     */
    public static class TimingStats {
        private final AtomicLong count = new AtomicLong(0);
        private final AtomicLong totalNanos = new AtomicLong(0);
        private final AtomicLong minNanos = new AtomicLong(Long.MAX_VALUE);
        private final AtomicLong maxNanos = new AtomicLong(0);

        void record(long nanos) {
            count.incrementAndGet();
            totalNanos.addAndGet(nanos);
            minNanos.updateAndGet(current -> Math.min(current, nanos));
            maxNanos.updateAndGet(current -> Math.max(current, nanos));
        }

        public long getCount() { return count.get(); }
        public long getTotalNanos() { return totalNanos.get(); }
        public long getMinNanos() { return count.get() > 0 ? minNanos.get() : 0; }
        public long getMaxNanos() { return maxNanos.get(); }
        public double getAvgNanos() { 
            long c = count.get();
            return c > 0 ? (double) totalNanos.get() / c : 0; 
        }
        public double getAvgMs() { return getAvgNanos() / 1_000_000.0; }
        public double getMinMs() { return getMinNanos() / 1_000_000.0; }
        public double getMaxMs() { return getMaxNanos() / 1_000_000.0; }

        @Override
        public String toString() {
            if (count.get() == 0) return "no data";
            return String.format("count=%d, avg=%.2fms, min=%.2fms, max=%.2fms",
                count.get(), getAvgMs(), getMinMs(), getMaxMs());
        }

        void reset() {
            count.set(0);
            totalNanos.set(0);
            minNanos.set(Long.MAX_VALUE);
            maxNanos.set(0);
        }
    }

    /**
     * Timer handle for measuring execution time.
     */
    public static class Timer implements AutoCloseable {
        private final String key;
        private final long startNanos;

        private Timer(String key) {
            this.key = key;
            this.startNanos = System.nanoTime();
        }

        @Override
        public void close() {
            if (!enabled) return;
            long elapsed = System.nanoTime() - startNanos;
            timings.computeIfAbsent(key, k -> new TimingStats()).record(elapsed);
            maybeReport();
        }

        /**
         * Get elapsed time so far in milliseconds.
         */
        public double elapsedMs() {
            return (System.nanoTime() - startNanos) / 1_000_000.0;
        }
    }

    /**
     * Start timing an operation.
     * Use with try-with-resources:
     * 
     * try (var timer = PerformanceMonitor.startTimer("operation")) {
     *     // code to time
     * }
     */
    public static Timer startTimer(String key) {
        return new Timer(key);
    }

    /**
     * Record a timing manually.
     */
    public static void recordTiming(String key, long nanos) {
        if (!enabled) return;
        timings.computeIfAbsent(key, k -> new TimingStats()).record(nanos);
    }

    /**
     * Get timing statistics for an operation.
     */
    public static TimingStats getTimingStats(String key) {
        return timings.get(key);
    }

    // ========== Counters ==========

    /**
     * Increment a counter.
     */
    public static void increment(String key) {
        if (!enabled) return;
        counters.computeIfAbsent(key, k -> new LongAdder()).increment();
    }

    /**
     * Add to a counter.
     */
    public static void add(String key, long delta) {
        if (!enabled) return;
        counters.computeIfAbsent(key, k -> new LongAdder()).add(delta);
    }

    /**
     * Get counter value.
     */
    public static long getCount(String key) {
        LongAdder adder = counters.get(key);
        return adder != null ? adder.sum() : 0;
    }

    // ========== Memory ==========

    /**
     * Get current memory usage in MB.
     */
    public static double getMemoryUsageMB() {
        Runtime runtime = Runtime.getRuntime();
        long used = runtime.totalMemory() - runtime.freeMemory();
        return used / (1024.0 * 1024.0);
    }

    /**
     * Get memory usage summary.
     */
    public static String getMemorySummary() {
        Runtime runtime = Runtime.getRuntime();
        long total = runtime.totalMemory();
        long free = runtime.freeMemory();
        long used = total - free;
        long max = runtime.maxMemory();
        
        return String.format("Memory: %.1f/%.1f MB (max: %.1f MB, %.1f%% used)",
            used / (1024.0 * 1024.0),
            total / (1024.0 * 1024.0),
            max / (1024.0 * 1024.0),
            (double) used / total * 100);
    }

    // ========== Reporting ==========

    /**
     * Check if it's time to report and do so if needed.
     */
    private static void maybeReport() {
        long now = System.currentTimeMillis();
        if (now - lastReportTime >= REPORT_INTERVAL_MS) {
            lastReportTime = now;
            reportStats();
        }
    }

    /**
     * Report all statistics to the log.
     */
    public static void reportStats() {
        if (timings.isEmpty() && counters.isEmpty()) return;
        
        StringBuilder sb = new StringBuilder("\n=== Performance Report ===\n");
        
        if (!timings.isEmpty()) {
            sb.append("Timings:\n");
            timings.forEach((key, stats) -> {
                sb.append("  ").append(key).append(": ").append(stats).append("\n");
            });
        }
        
        if (!counters.isEmpty()) {
            sb.append("Counters:\n");
            counters.forEach((key, adder) -> {
                sb.append("  ").append(key).append(": ").append(adder.sum()).append("\n");
            });
        }
        
        sb.append(getMemorySummary()).append("\n");
        sb.append("==========================");
        
        MoreRealisticGeneEditing.LOGGER.info(sb.toString());
    }

    // ========== Control ==========

    /**
     * Enable or disable performance monitoring.
     */
    public static void setEnabled(boolean enabled) {
        PerformanceMonitor.enabled = enabled;
    }

    /**
     * Check if monitoring is enabled.
     */
    public static boolean isEnabled() {
        return enabled;
    }

    /**
     * Reset all statistics.
     */
    public static void reset() {
        timings.values().forEach(TimingStats::reset);
        counters.values().forEach(LongAdder::reset);
    }

    /**
     * Clear all data.
     */
    public static void clear() {
        timings.clear();
        counters.clear();
    }
}
