package com.morerealisticgeneediting.genome;

import com.morerealisticgeneediting.data.GenomePatch;

import java.util.*;

public class Genome {

    // ... (fields remain the same)
    private final UUID uuid;
    private final UUID owner;
    private final byte[] packedBases;
    private final long baseTotalLength;
    private final long sequenceStartOffset;
    private final List<GenomePatch> patches;

    // --- Lazily-initialized caches for performance ---
    private transient NavigableMap<Long, Object> structuralMods = null;
    private transient NavigableMap<Long, Byte> pointMutations = null;
    private transient NavigableMap<Long, Long> cumulativeDeltaMap = null;
    private transient long finalLength = -1;

    private Genome(UUID uuid, UUID owner, byte[] packedBases, long totalLength, long sequenceStartOffset, List<GenomePatch> patches) {
        this.uuid = uuid;
        this.owner = owner;
        this.packedBases = packedBases;
        this.baseTotalLength = totalLength;
        this.sequenceStartOffset = sequenceStartOffset;
        this.patches = Collections.unmodifiableList(new ArrayList<>(patches));
    }

    private void preparePatchesAndIndex() {
        if (structuralMods != null) return; // Already prepared

        TreeMap<Long, String> insertions = new TreeMap<>();
        TreeMap<Long, Integer> deletions = new TreeMap<>();
        this.pointMutations = new TreeMap<>();

        for (GenomePatch patch : patches) {
            insertions.putAll(patch.getInsertions());
            deletions.putAll(patch.getDeletions());
            this.pointMutations.putAll(patch.getPointMutations());
        }

        this.structuralMods = new TreeMap<>();
        this.structuralMods.putAll(insertions);
        this.structuralMods.putAll(deletions);

        // Build the cumulative delta map for fast seeking
        this.cumulativeDeltaMap = new TreeMap<>();
        long runningDelta = 0;
        for (Map.Entry<Long, Object> entry : this.structuralMods.entrySet()) {
            long pos = entry.getKey();
            this.cumulativeDeltaMap.put(pos, runningDelta);
            if (entry.getValue() instanceof String) {
                runningDelta += ((String) entry.getValue()).length();
            } else if (entry.getValue() instanceof Integer) {
                runningDelta -= (Integer) entry.getValue();
            }
        }
    }

    public static Genome createFromUnpackedSequence(UUID owner, String sequence) {
        return new Genome(UUID.randomUUID(), owner, TwoBitEncoding.pack(sequence), sequence.length(), 0, new ArrayList<>());
    }
    
    // ... (performInsertion, performKnockout remain the same) ...
    public Genome performInsertion(long position, String sequence) {
        GenomePatch newPatch = new GenomePatch(this.uuid);
        newPatch.addInsertion(position, sequence);
        List<GenomePatch> newPatches = new ArrayList<>(this.patches);
        newPatches.add(newPatch);
        return new Genome(this.uuid, this.owner, this.packedBases, this.baseTotalLength, this.sequenceStartOffset, newPatches);
    }

    public Genome performKnockout(long pamPosition, int protospacerLength) {
        int knockoutStart = (int) Math.max(0, pamPosition - protospacerLength);
        int knockoutLength = (int) Math.min((int)pamPosition, knockoutStart + protospacerLength) - knockoutStart;
        if (knockoutLength <= 0) return this;
        GenomePatch newPatch = new GenomePatch(this.uuid);
        newPatch.addDeletion(knockoutStart, knockoutLength);
        List<GenomePatch> newPatches = new ArrayList<>(this.patches);
        newPatches.add(newPatch);
        return new Genome(this.uuid, this.owner, this.packedBases, this.baseTotalLength, this.sequenceStartOffset, newPatches);
    }

    public String getSequence(long finalStart, int finalLength) {
        preparePatchesAndIndex();
        long totalLength = getTotalLength();
        if (finalStart >= totalLength || finalLength <= 0) return "";

        SequenceIterator iter = new SequenceIterator(this);
        iter.seek(finalStart);

        long effectiveLength = Math.min(finalLength, totalLength - finalStart);
        StringBuilder sb = new StringBuilder((int)effectiveLength);
        for (int i = 0; i < effectiveLength && iter.hasNext(); i++) {
            sb.append(iter.next());
        }
        return sb.toString();
    }

    public long getTotalLength() {
        if (this.finalLength != -1) return this.finalLength;
        preparePatchesAndIndex();
        
        long length = this.baseTotalLength;
        if (cumulativeDeltaMap.isEmpty()) {
            this.finalLength = length;
            return length;
        }

        Map.Entry<Long, Long> lastEntry = cumulativeDeltaMap.lastEntry();
        Object lastMod = structuralMods.get(lastEntry.getKey());
        long lastDelta = lastEntry.getValue();
        if(lastMod instanceof String) {
            lastDelta += ((String) lastMod).length();
        } else if (lastMod instanceof Integer) {
            lastDelta -= (Integer) lastMod;
        }

        this.finalLength = this.baseTotalLength + lastDelta;
        return this.finalLength;
    }
    
    // ... (other getters) ...
    public UUID getUUID() { return uuid; }

    private static class SequenceIterator implements Iterator<Character> {
        private final Genome genome;
        private long basePos; 
        private long finalPos;
        private String insertionBuffer = null;
        private int insertionIndex = 0;

        public SequenceIterator(Genome genome) {
            this.genome = genome;
            this.basePos = 0;
            this.finalPos = 0;
        }

        public void seek(long targetFinalPos) {
            genome.preparePatchesAndIndex();

            long low = 0;
            long high = genome.baseTotalLength;
            long bestBasePos = 0;

            // Binary search to find the closest basePos _before_ the targetFinalPos
            while (low <= high) {
                long midBasePos = low + (high - low) / 2;
                Map.Entry<Long, Long> floorEntry = genome.cumulativeDeltaMap.floorEntry(midBasePos);
                long delta = (floorEntry != null) ? floorEntry.getValue() : 0;
                long calcFinalPos = midBasePos + delta;

                if (calcFinalPos < targetFinalPos) {
                    bestBasePos = midBasePos;
                    low = midBasePos + 1;
                } else {
                    high = midBasePos - 1;
                }
            }
            
            // Set iterator to the starting point found by the binary search
            this.basePos = bestBasePos;
            Map.Entry<Long, Long> floorEntry = genome.cumulativeDeltaMap.floorEntry(this.basePos);
            long delta = (floorEntry != null) ? floorEntry.getValue() : 0;
            this.finalPos = this.basePos + delta;

            // Linear scan for the small remaining distance
            while (finalPos < targetFinalPos && hasNext()) {
                next();
            }
        }

        @Override
        public boolean hasNext() {
            return finalPos < genome.getTotalLength();
        }

        @Override
        public Character next() {
            if (!hasNext()) throw new NoSuchElementException();

            if (insertionBuffer != null) {
                char c = insertionBuffer.charAt(insertionIndex++);
                if (insertionIndex >= insertionBuffer.length()) {
                    insertionBuffer = null;
                    insertionIndex = 0;
                }
                finalPos++;
                return c;
            }

            // Check for structural modifications at the current base position
            Map.Entry<Long, Object> modEntry = genome.structuralMods.ceilingEntry(basePos);
            while (modEntry != null && modEntry.getKey() == basePos) {
                 Object mod = modEntry.getValue();
                if (mod instanceof String) { // Insertion
                    insertionBuffer = (String) mod;
                    return next(); // Recurse to process the buffer
                } else { // Deletion
                    basePos += (Integer) mod;
                    modEntry = genome.structuralMods.ceilingEntry(basePos);
                }
            }

            // We are now at a non-modified base position
            if (genome.pointMutations.containsKey(basePos)) {
                char c = TwoBitEncoding.decodeBase(genome.pointMutations.get(basePos));
                basePos++;
                finalPos++;
                return c;
            }

            // Default: get base from original packed sequence
            char c = TwoBitEncoding.getBaseAt(genome.packedBases, basePos - genome.sequenceStartOffset);
            basePos++;
            finalPos++;
            return c;
        }
    }
}
