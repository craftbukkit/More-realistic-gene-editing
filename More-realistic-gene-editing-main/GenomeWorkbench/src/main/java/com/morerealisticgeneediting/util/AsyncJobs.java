package com.morerealisticgeneediting.util;

import net.minecraft.server.MinecraftServer;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A utility for running tasks asynchronously off the main server thread
 * to avoid blocking and causing TPS drops.
 */
public final class AsyncJobs {

    // A single thread executor is often enough to handle sequential, heavy tasks.
    // For parallel tasks, a cached or fixed thread pool might be better.
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    /**
     * Submits a task to be run on a background thread.
     * The result of the task is then passed to a consumer on the main server thread.
     *
     * @param server The MinecraftServer instance, used to execute the callback on the main thread.
     * @param backgroundTask The task to run asynchronously. It should not modify game state directly.
     * @param mainThreadCallback The callback to run on the main thread with the result of the background task.
     * @param <T> The type of the result.
     */
    public static <T> void submit(MinecraftServer server, Supplier<T> backgroundTask, Consumer<T> mainThreadCallback) {
        executor.submit(() -> {
            final T result = backgroundTask.get();
            // Use the server's main execution loop to run the callback, ensuring thread safety.
            server.execute(() -> mainThreadCallback.accept(result));
        });
    }

    /**
     * Shuts down the executor service. Should be called when the server is stopping.
     */
    public static void shutdown() {
        try {
            executor.shutdown();
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
    }
}
