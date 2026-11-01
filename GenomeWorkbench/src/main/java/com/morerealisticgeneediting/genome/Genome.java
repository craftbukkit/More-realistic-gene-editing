package com.morerealisticgeneediting.genome;
import com.morerealisticgeneediting.data.GenomePatch;
import net.minecraft.nbt.*;
import java.util.*;
/** Immutable bounded sample. Edits use original-reference coordinates; reads use edited coordinates. */
public final class Genome {
    public static final int MAX_BASES=2_000_000;
    public static final int MAX_PATCHES=256;
    private final UUID uuid,owner;
    private final byte[] packedBases;
    private final int baseLength;
    private final String referenceId;
    private final long revision;
    private final List<GenomePatch> patches;
    private final NavigableMap<Long,Byte> points=new TreeMap<>();
    private final NavigableMap<Long,String> insertions=new TreeMap<>();
    private final NavigableMap<Long,Integer> deletions=new TreeMap<>();
    private final int editedLength;
    private transient volatile String editedSequence;
    private Genome(UUID id,UUID owner,byte[] packed,int length,String referenceId,long revision,List<GenomePatch> patches){
        this.uuid=Objects.requireNonNull(id);this.owner=Objects.requireNonNull(owner);
        if(length<0||length>MAX_BASES||packed.length!=(length+3)/4)throw new IllegalArgumentException("Invalid packed reference length");
        if(referenceId==null||referenceId.isBlank()||referenceId.length()>256||revision!=patches.size()||patches.size()>MAX_PATCHES)throw new IllegalArgumentException("Invalid genome metadata");
        this.packedBases=packed.clone();this.baseLength=length;this.referenceId=referenceId;this.revision=revision;
        this.patches=patches.stream().map(GenomePatch::copy).toList();
        for(GenomePatch patch:this.patches)mergeValidated(patch);
        long finalLength=length;for(String s:insertions.values())finalLength+=s.length();for(int n:deletions.values())finalLength-=n;
        if(finalLength<0||finalLength>MAX_BASES)throw new IllegalArgumentException("Edited sample exceeds supported size");
        this.editedLength=(int)finalLength;
    }
    private void mergeValidated(GenomePatch patch){
        if(!uuid.equals(patch.getBaseGenomeId()))throw new IllegalArgumentException("Patch belongs to another sample");
        if(points.size()+insertions.size()+deletions.size()+patch.size()>GenomePatch.MAX_MUTATIONS)throw new IllegalArgumentException("Too many sample edits");
        for(var e:patch.getPointMutations().entrySet()){
            long p=e.getKey();if(p>=baseLength||points.containsKey(p)||isDeleted(p))throw new IllegalArgumentException("Point edit out of bounds or conflicts");
        }
        for(var e:patch.getInsertions().entrySet()){
            long p=e.getKey();if(p>baseLength||insertions.containsKey(p)||isDeleted(p))throw new IllegalArgumentException("Insertion out of bounds or conflicts");
        }
        for(var e:patch.getDeletions().entrySet()){
            long p=e.getKey(),end=p+e.getValue();
            if(end>baseLength||isDeleted(p))throw new IllegalArgumentException("Deletion out of bounds or conflicts");
            var next=deletions.ceilingEntry(p);
            if(next!=null&&next.getKey()<end)throw new IllegalArgumentException("Overlapping deletion");
            if(!points.subMap(p,true,end,false).isEmpty()||!insertions.subMap(p,true,end,false).isEmpty())throw new IllegalArgumentException("Deletion overlaps earlier edits");
        }
        points.putAll(patch.getPointMutations());insertions.putAll(patch.getInsertions());deletions.putAll(patch.getDeletions());
    }
    private boolean isDeleted(long p){var e=deletions.floorEntry(p);return e!=null&&p<e.getKey()+e.getValue();}
    public static Genome createFromUnpackedSequence(UUID owner,String sequence){return createFromUnpackedSequence(owner,sequence,"virtual:unannotated/v1");}
    public static Genome createFromUnpackedSequence(UUID owner,String sequence,String referenceId){
        Objects.requireNonNull(sequence);if(sequence.length()>MAX_BASES)throw new IllegalArgumentException("Reference too long");
        return new Genome(UUID.randomUUID(),owner,TwoBitEncoding.pack(sequence),sequence.length(),referenceId,0,List.of());
    }
    /** For local client demonstrations only, never a player-owned server sample. */
    public static Genome createFromUnpackedSequence(String sequence){return createFromUnpackedSequence(new UUID(0,0),sequence);}
    public UUID getUUID(){return uuid;} public UUID getOwner(){return owner;}
    public String getReferenceId(){return referenceId;} public long getRevision(){return revision;}
    public long getTotalLength(){return editedLength;} public long getReferenceLength(){return baseLength;}
    public long getStoredBaseCount(){return baseLength+insertions.values().stream().mapToLong(String::length).sum();}
    public List<GenomePatch> getPatches(){return patches.stream().map(GenomePatch::copy).toList();}
    public Genome applyPatch(GenomePatch patch){
        if(patch.isEmpty())return this;
        List<GenomePatch> next=new ArrayList<>(patches);next.add(patch);
        return new Genome(uuid,owner,packedBases,baseLength,referenceId,Math.addExact(revision,1),next);
    }
    public Genome performInsertion(long referencePosition,String sequence){GenomePatch p=new GenomePatch(uuid);p.addInsertion(referencePosition,sequence);return applyPatch(p);}
    public Genome performDeletion(long referenceStart,int length){GenomePatch p=new GenomePatch(uuid);p.addDeletion(referenceStart,length);return applyPatch(p);}
    /** Deprecated exact interval deletion; callers should use a nuclease-specific modeled outcome. */
    @Deprecated public Genome performKnockout(long end,int length){return performDeletion(Math.subtractExact(end,length),length);}
    public String getReferenceSequence(){StringBuilder s=new StringBuilder(baseLength);for(int i=0;i<baseLength;i++)s.append(TwoBitEncoding.getBaseAt(packedBases,i));return s.toString();}
    private String materialize(){
        String cached=editedSequence;if(cached!=null)return cached;
        StringBuilder out=new StringBuilder(editedLength);long p=0;
        while(p<=baseLength){
            String added=insertions.get(p);if(added!=null)out.append(added);
            if(p==baseLength)break;
            Integer removed=deletions.get(p);if(removed!=null){p+=removed;continue;}
            Byte changed=points.get(p);out.append(changed==null?TwoBitEncoding.getBaseAt(packedBases,p):TwoBitEncoding.decodeBase(changed));p++;
        }
        if(out.length()!=editedLength)throw new IllegalStateException("Inconsistent genome edit index");
        editedSequence=out.toString();return editedSequence;
    }
    public String getSequence(long start,int length){
        if(start<0||length<0||start>editedLength)throw new IndexOutOfBoundsException("Invalid edited slice");
        int end=(int)Math.min((long)editedLength,start+length);return materialize().substring((int)start,end);
    }
    public String getSequence(){return materialize();} public String getUnpackedSequence(){return materialize();}
    /** Map an edited base to the original reference; inserted bases have no reference position. */
    public long editedToReference(long editedPosition){
        if(editedPosition<0||editedPosition>=editedLength)throw new IndexOutOfBoundsException("Invalid edited base");
        long edited=0;
        for(long p=0;p<baseLength;){
            String added=insertions.get(p);if(added!=null){if(editedPosition<edited+added.length())throw new IllegalArgumentException("Target lies in an earlier insertion");edited+=added.length();}
            Integer deleted=deletions.get(p);if(deleted!=null){p+=deleted;continue;}
            if(edited++==editedPosition)return p;p++;
        }
        throw new IllegalArgumentException("Target lies in a terminal insertion");
    }
    public NbtCompound writeNbt(){
        NbtCompound n=new NbtCompound();n.putInt("schemaVersion",1);n.putUuid("id",uuid);n.putUuid("owner",owner);n.putString("referenceId",referenceId);n.putLong("revision",revision);n.putInt("baseLength",baseLength);n.putByteArray("packedBases",packedBases);
        NbtList ps=new NbtList();patches.forEach(p->ps.add(p.writeNbt()));n.put("patches",ps);return n;
    }
    public static Genome fromNbt(NbtCompound n){
        if(n.getInt("schemaVersion")!=1||!n.containsUuid("id")||!n.containsUuid("owner"))throw new IllegalArgumentException("Invalid genome save schema");
        NbtList ps=n.getList("patches",NbtElement.COMPOUND_TYPE);if(ps.size()>MAX_PATCHES)throw new IllegalArgumentException("Too many saved patches");
        List<GenomePatch> patches=new ArrayList<>();for(int i=0;i<ps.size();i++)patches.add(GenomePatch.fromNbt(ps.getCompound(i)));
        return new Genome(n.getUuid("id"),n.getUuid("owner"),n.getByteArray("packedBases"),n.getInt("baseLength"),n.getString("referenceId"),n.getLong("revision"),patches);
    }
}
