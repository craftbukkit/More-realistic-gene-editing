package com.morerealisticgeneediting.genome;
import java.util.Objects;
/** A window with an explicit source identity; remote whole-reference length may be unknown (-1). */
public final class GenomeSlice {
    private final Genome genome;
    private final String sequence,referenceId;
    private final long start,totalLength;
    public GenomeSlice(Genome genome,String sequence,long start){this(Objects.requireNonNull(genome),sequence,start,genome.getTotalLength(),genome.getUUID().toString());}
    public GenomeSlice(String sequence,long start,long totalLength,String referenceId){this(null,sequence,start,totalLength,referenceId);}
    private GenomeSlice(Genome genome,String sequence,long start,long totalLength,String referenceId){
        Objects.requireNonNull(sequence);Objects.requireNonNull(referenceId);
        if(start<0||sequence.length()>100_000||totalLength< -1||start>Long.MAX_VALUE-sequence.length()||(totalLength>=0&&start+sequence.length()>totalLength))throw new IllegalArgumentException("Invalid sequence window");
        if(!sequence.matches("[ACGTNacgtn]*"))throw new IllegalArgumentException("Unsupported sequence alphabet");
        this.genome=genome;this.sequence=sequence.toUpperCase(java.util.Locale.ROOT);this.start=start;this.totalLength=totalLength;this.referenceId=referenceId;
    }
    /** Null for remote read-only windows; never synthesize a fake complete Genome. */
    public Genome getGenome(){return genome;}
    public String getSequence(){return sequence;} public long getStart(){return start;}
    public long getTotalGenomeLength(){return totalLength;} public String getGenomeIdentifier(){return referenceId;}
    public int getLength(){return sequence.length();}
    public byte getBaseAt(int index){return TwoBitEncoding.encodeBase(sequence.charAt(index));}
}
