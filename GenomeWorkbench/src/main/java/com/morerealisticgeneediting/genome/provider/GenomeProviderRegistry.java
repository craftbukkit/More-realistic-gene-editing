package com.morerealisticgeneediting.genome.provider;
import com.morerealisticgeneediting.genome.*;
import net.minecraft.server.MinecraftServer;
import java.util.*;
import java.util.concurrent.CompletableFuture;
public final class GenomeProviderRegistry {
    private static final GenomeProvider REMOTE=new EnsemblRestProvider();
    private GenomeProviderRegistry() { }
    public static void registerDefaults(){ /* Stateless remote provider; local repositories belong to saves. */ }
    public static CompletableFuture<GenomeSlice> getSlice(MinecraftServer server,UUID owner,String identifier,long start,int length){
        if(identifier!=null&&identifier.startsWith("ensembl:"))return getSlice(identifier,start,length);
        return new LocalGenomeProvider(GenomeRepository.get(server),owner).getSlice(identifier,start,length);
    }
    /** Public references only. Local sample access must use the server/owner overload. */
    public static CompletableFuture<GenomeSlice> getSlice(String identifier,long start,int length){
        if(!REMOTE.canProvide(identifier))return CompletableFuture.failedFuture(new IllegalArgumentException("Local samples require an authenticated world context"));
        return REMOTE.getSlice(identifier,start,length);
    }
}
