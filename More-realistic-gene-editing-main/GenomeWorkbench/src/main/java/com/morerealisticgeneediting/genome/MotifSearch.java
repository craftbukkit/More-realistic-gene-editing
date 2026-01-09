package com.morerealisticgeneediting.genome;

import java.util.ArrayList;
import java.util.List;

public class MotifSearch {

    /**
     * Searches for a motif within a GenomeSlice.
     *
     * @param slice The GenomeSlice to search in.
     * @param motif The motif to search for, as a string of A, C, G, T.
     * @return A list of starting positions of the motif within the slice.
     */
    public static List<Integer> search(GenomeSlice slice, String motif) {
        List<Integer> foundPositions = new ArrayList<>();
        if (motif == null || motif.isEmpty() || slice.getLength() < motif.length()) {
            return foundPositions;
        }

        byte[] motifBytes = new byte[motif.length()];
        for (int i = 0; i < motif.length(); i++) {
            motifBytes[i] = baseToByte(motif.charAt(i));
        }

        for (int i = 0; i <= slice.getLength() - motif.length(); i++) {
            boolean match = true;
            for (int j = 0; j < motif.length(); j++) {
                if (slice.getBaseAt(i + j) != motifBytes[j]) {
                    match = false;
                    break;
                }
            }
            if (match) {
                foundPositions.add(i);
            }
        }
        return foundPositions;
    }

    private static byte baseToByte(char base) {
        switch (Character.toUpperCase(base)) {
            case 'A': return 0;
            case 'C': return 1;
            case 'G': return 2;
            case 'T': return 3;
            default: throw new IllegalArgumentException("Invalid base: " + base);
        }
    }
}
