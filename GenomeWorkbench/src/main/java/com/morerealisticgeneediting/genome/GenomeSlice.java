package com.morerealisticgeneediting.genome;

/**
 * Represents a slice of a genome sequence. This is a data transfer object (DTO).
 */
public class GenomeSlice {

    private final Genome genome;
    private final String sequence;
    private final long start; // The starting position of this slice within the conceptual genome.

    /**
     * Constructs a new GenomeSlice.
     * @param genome The source Genome object.
     * @param sequence The actual sequence string for this slice.
     * @param start The starting position of this slice within the conceptual genome.
     */
    public GenomeSlice(Genome genome, String sequence, long start) {
        this.genome = genome;
        this.sequence = sequence;
        this.start = start;
    }

    public Genome getGenome() {
        return genome;
    }

    public String getSequence() {
        return sequence;
    }

    public long getStart() {
        return start;
    }
    
    public long getTotalGenomeLength() {
        return genome.getTotalLength();
    }
}
