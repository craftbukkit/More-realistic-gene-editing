package com.morerealisticgeneediting.data;
import com.morerealisticgeneediting.genome.TwoBitEncoding;
import net.minecraft.nbt.*;
import java.util.*;
/** Edits in zero-based, half-open original-reference coordinates. Insertions occur before a base/boundary. */
public final class GenomePatch {
    public static final int MAX_MUTATIONS=8192;
    public static final int MAX_INSERTION_LENGTH=1024;
    private final UUID patchId;
    private final UUID baseGenomeId;
    private final NavigableMap<Long,Byte> points=new TreeMap<>();
    private final NavigableMap<Long,String> insertions=new TreeMap<>();
    private final NavigableMap<Long,Integer> deletions=new TreeMap<>();
    public GenomePatch(UUID baseGenomeId) { this(UUID.randomUUID(),baseGenomeId); }
    private GenomePatch(UUID patchId, UUID baseGenomeId) { this.patchId=Objects.requireNonNull(patchId);this.baseGenomeId=Objects.requireNonNull(baseGenomeId); }
    public UUID getPatchId(){return patchId;}
    public UUID getBaseGenomeId(){return baseGenomeId;}
    public NavigableMap<Long,Byte> getPointMutations(){return Collections.unmodifiableNavigableMap(points);}
    public NavigableMap<Long,String> getInsertions(){return Collections.unmodifiableNavigableMap(insertions);}
    public NavigableMap<Long,Integer> getDeletions(){return Collections.unmodifiableNavigableMap(deletions);}
    private void capacity(long position){if(position<0)throw new IllegalArgumentException("Negative reference coordinate");if(size()>=MAX_MUTATIONS)throw new IllegalArgumentException("Too many edits");}
    public int size(){return points.size()+insertions.size()+deletions.size();}
    public boolean isEmpty(){return size()==0;}
    private boolean deleted(long p){var e=deletions.floorEntry(p);return e!=null && p<e.getKey()+e.getValue();}
    public void addPointMutation(long position,byte base){
        capacity(position);TwoBitEncoding.decodeBase(base);
        if(points.containsKey(position)||deleted(position))throw new IllegalArgumentException("Conflicting point edit");
        points.put(position,base);
    }
    public void addInsertion(long position,String sequence){
        capacity(position);Objects.requireNonNull(sequence);
        if(sequence.isEmpty()||sequence.length()>MAX_INSERTION_LENGTH)throw new IllegalArgumentException("Invalid insertion length");
        sequence=sequence.toUpperCase(Locale.ROOT);TwoBitEncoding.pack(sequence);
        if(insertions.containsKey(position))throw new IllegalArgumentException("Duplicate insertion boundary");
        var e=deletions.floorEntry(position);
        if(e!=null && position>e.getKey() && position<e.getKey()+e.getValue())throw new IllegalArgumentException("Insertion inside deletion");
        insertions.put(position,sequence);
    }
    public void addDeletion(long position,int length){
        capacity(position);if(length<=0 || position>Long.MAX_VALUE-length)throw new IllegalArgumentException("Invalid deletion length");
        long end=position+length;var prev=deletions.floorEntry(position);var next=deletions.ceilingEntry(position);
        if((prev!=null && prev.getKey()+prev.getValue()>position)||(next!=null && next.getKey()<end))throw new IllegalArgumentException("Overlapping deletions");
        if(!points.subMap(position,true,end,false).isEmpty()||!insertions.subMap(position,false,end,false).isEmpty())throw new IllegalArgumentException("Deletion overlaps another edit");
        deletions.put(position,length);
    }
    public long getFirstModificationPosition(){return java.util.stream.Stream.of(points,insertions,deletions).filter(m->!m.isEmpty()).mapToLong(m->m.firstKey()).min().orElse(0);}
    public int getTotalModificationSize(){long n=points.size();for(String s:insertions.values())n+=s.length();for(int x:deletions.values())n+=x;return Math.toIntExact(n);}
    public GenomePatch copy(){GenomePatch copy=new GenomePatch(patchId,baseGenomeId);copy.points.putAll(points);copy.insertions.putAll(insertions);copy.deletions.putAll(deletions);return copy;}
    public NbtCompound writeNbt(){
        NbtCompound n=new NbtCompound();n.putUuid("patchId",patchId);n.putUuid("baseGenomeId",baseGenomeId);
        NbtList p=new NbtList(),i=new NbtList(),d=new NbtList();
        points.forEach((pos,base)->{NbtCompound x=new NbtCompound();x.putLong("position",pos);x.putByte("base",base);p.add(x);});
        insertions.forEach((pos,seq)->{NbtCompound x=new NbtCompound();x.putLong("position",pos);x.putString("sequence",seq);i.add(x);});
        deletions.forEach((pos,len)->{NbtCompound x=new NbtCompound();x.putLong("position",pos);x.putInt("length",len);d.add(x);});
        n.put("points",p);n.put("insertions",i);n.put("deletions",d);return n;
    }
    public static GenomePatch fromNbt(NbtCompound n){
        if(!n.containsUuid("patchId")||!n.containsUuid("baseGenomeId"))throw new IllegalArgumentException("Missing patch identity");
        GenomePatch p=new GenomePatch(n.getUuid("patchId"),n.getUuid("baseGenomeId"));
        NbtList points=n.getList("points",NbtElement.COMPOUND_TYPE),ins=n.getList("insertions",NbtElement.COMPOUND_TYPE),del=n.getList("deletions",NbtElement.COMPOUND_TYPE);
        if((long)points.size()+ins.size()+del.size()>MAX_MUTATIONS)throw new IllegalArgumentException("Too many edits in saved patch");
        for(int j=0;j<points.size();j++){var x=points.getCompound(j);p.addPointMutation(x.getLong("position"),x.getByte("base"));}
        for(int j=0;j<ins.size();j++){var x=ins.getCompound(j);p.addInsertion(x.getLong("position"),x.getString("sequence"));}
        for(int j=0;j<del.size();j++){var x=del.getCompound(j);p.addDeletion(x.getLong("position"),x.getInt("length"));}
        return p;
    }
}
