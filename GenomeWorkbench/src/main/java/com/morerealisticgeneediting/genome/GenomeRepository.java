package com.morerealisticgeneediting.genome;
import net.minecraft.nbt.*;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;
import java.util.*;
/** Authoritative per-save sample store. Access only on the Minecraft server thread. */
public final class GenomeRepository extends PersistentState {
    private static final String SAVE_KEY="morerealisticgeneediting_genomes";
    private static final Type<GenomeRepository> TYPE=new Type<>(GenomeRepository::new,GenomeRepository::fromNbt,null);
    private static final int MAX_SAMPLES=4096;
    private static final long MAX_TOTAL_BASES=32_000_000;
    private final Map<UUID,Genome> genomes=new LinkedHashMap<>();
    private final Map<UUID,NbtCompound> deliveries=new LinkedHashMap<>();
    public static GenomeRepository get(MinecraftServer server){
        if(!server.isOnThread())throw new IllegalStateException("Genome repository must be accessed on the server thread");
        return server.getOverworld().getPersistentStateManager().getOrCreate(TYPE,SAVE_KEY);
    }
    public Optional<Genome> get(UUID id){return Optional.ofNullable(genomes.get(id));}
    public Optional<Genome> findOwned(UUID id,UUID owner){return get(id).filter(g->g.getOwner().equals(owner));}
    public void put(Genome genome){
        Objects.requireNonNull(genome);Genome previous=genomes.get(genome.getUUID());
        if(previous==null&&genomes.size()>=MAX_SAMPLES)throw new IllegalStateException("World sample capacity reached");
        if(previous!=null&&(!previous.getOwner().equals(genome.getOwner())||genome.getRevision()<previous.getRevision()||(genome!=previous&&genome.getRevision()==previous.getRevision())))throw new IllegalArgumentException("Invalid sample replacement");
        long total=genomes.values().stream().mapToLong(Genome::getStoredBaseCount).sum()-(previous==null?0:previous.getStoredBaseCount())+genome.getStoredBaseCount();
        if(total>MAX_TOTAL_BASES)throw new IllegalStateException("World sequence storage budget reached");
        genomes.put(genome.getUUID(),genome);markDirty();
    }
    public void recordDelivery(UUID entityId,Genome genome){
        if(!genomes.containsKey(genome.getUUID()))throw new IllegalArgumentException("Unknown sample");
        if(!deliveries.containsKey(entityId)&&deliveries.size()>=MAX_SAMPLES)throw new IllegalStateException("World delivery record capacity reached");
        NbtCompound record=new NbtCompound();record.putUuid("entity",entityId);record.putUuid("sample",genome.getUUID());record.putUuid("owner",genome.getOwner());record.putLong("revision",genome.getRevision());deliveries.put(entityId,record);markDirty();
    }
    public static GenomeRepository fromNbt(NbtCompound n,RegistryWrapper.WrapperLookup registries){
        GenomeRepository store=new GenomeRepository();NbtList list=n.getList("genomes",NbtElement.COMPOUND_TYPE);
        if(list.size()>MAX_SAMPLES)throw new IllegalArgumentException("Too many saved samples");
        for(int i=0;i<list.size();i++){Genome g=Genome.fromNbt(list.getCompound(i));if(store.genomes.putIfAbsent(g.getUUID(),g)!=null)throw new IllegalArgumentException("Duplicate sample identity");}
        if(store.genomes.values().stream().mapToLong(Genome::getStoredBaseCount).sum()>MAX_TOTAL_BASES)throw new IllegalArgumentException("Saved sequence storage budget exceeded");
        NbtList delivered=n.getList("deliveries",NbtElement.COMPOUND_TYPE);
        if(delivered.size()>MAX_SAMPLES)throw new IllegalArgumentException("Too many delivery records");
        for(int i=0;i<delivered.size();i++){NbtCompound record=delivered.getCompound(i);store.deliveries.put(record.getUuid("entity"),record.copy());}
        return store;
    }
    @Override public NbtCompound writeNbt(NbtCompound n,RegistryWrapper.WrapperLookup registries){NbtList list=new NbtList();genomes.values().forEach(g->list.add(g.writeNbt()));n.put("genomes",list);NbtList delivered=new NbtList();deliveries.values().forEach(record->delivered.add(record.copy()));n.put("deliveries",delivered);return n;}
}
