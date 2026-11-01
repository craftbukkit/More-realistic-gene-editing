package com.morerealisticgeneediting.util;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Bounded per-server background work. Suppliers must operate on immutable snapshots. */
public final class AsyncJobs {
    private static final Map<MinecraftServer,ExecutorService> EXECUTORS=new WeakHashMap<>();
    static { ServerLifecycleEvents.SERVER_STOPPING.register(AsyncJobs::shutdown); }
    private AsyncJobs(){}
    private static synchronized ExecutorService executor(MinecraftServer server){
        return EXECUTORS.computeIfAbsent(server,key->new ThreadPoolExecutor(1,1,0,TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(32),task->{Thread t=new Thread(task,"mrge-background");t.setDaemon(true);return t;},new ThreadPoolExecutor.AbortPolicy()));
    }
    /** Rejection is explicit; no work is silently lost and no world state runs in the worker. */
    public static <T> void submit(MinecraftServer server,Supplier<T> work,Consumer<T> callback){
        if(!server.isRunning())throw new RejectedExecutionException("Server stopped");
        ExecutorService executor=executor(server);
        executor.execute(()->{
            try {
                T result=work.get();
                if(!executor.isShutdown() && server.isRunning())server.execute(()->{if(!executor.isShutdown() && server.isRunning())callback.accept(result);});
            }catch(RuntimeException error){org.slf4j.LoggerFactory.getLogger(AsyncJobs.class).error("Background laboratory job failed",error);}
        });
    }
    public static synchronized void shutdown(MinecraftServer server){ExecutorService worker=EXECUTORS.remove(server);if(worker!=null)worker.shutdownNow();}
    public static synchronized void shutdown(){for(ExecutorService worker:EXECUTORS.values())worker.shutdownNow();EXECUTORS.clear();}
}
