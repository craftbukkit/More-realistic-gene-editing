package com.morerealisticgeneediting.genome.provider;
import com.morerealisticgeneediting.genome.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
/** Local requests require an explicit per-world repository and requesting owner. */
public final class LocalGenomeProvider implements GenomeProvider {
    private final GenomeRepository repository;
    private final UUID owner;
    public LocalGenomeProvider(GenomeRepository repository,UUID owner){this.repository=Objects.requireNonNull(repository);this.owner=Objects.requireNonNull(owner);}
    @Override public boolean canProvide(String identifier){try{UUID.fromString(identifier);return true;}catch(IllegalArgumentException|NullPointerException e){return false;}}
    @Override public CompletableFuture<GenomeSlice> getSlice(String identifier,long start,int length){
        try{
            if(length<0||length>100_000)throw new IllegalArgumentException("Window exceeds 100,000 bases");
            Genome genome=repository.findOwned(UUID.fromString(identifier),owner).orElseThrow(()->new IllegalArgumentException("Owned sample not found"));
            return CompletableFuture.completedFuture(new GenomeSlice(genome,genome.getSequence(start,length),start));
        }catch(RuntimeException failure){return CompletableFuture.failedFuture(failure);}
    }
}
