package com.morerealisticgeneediting.util;

import java.util.Random;

/**
 * Optimized utilities for DNA sequence manipulation.
 * 
 * Features:
 * - Efficient string building using StringBuilder
 * - Complement and reverse complement operations
 * - GC content calculation
 * - Sequence validation
 * - Random sequence generation
 */
public final class DnaUtils {

    private static final char[] DNA_BASES = {'A', 'C', 'G', 'T'};
    private static final char[] COMPLEMENT = new char[128];
    
    static {
        // Initialize complement lookup table
        COMPLEMENT['A'] = 'T';
        COMPLEMENT['T'] = 'A';
        COMPLEMENT['G'] = 'C';
        COMPLEMENT['C'] = 'G';
        COMPLEMENT['a'] = 't';
        COMPLEMENT['t'] = 'a';
        COMPLEMENT['g'] = 'c';
        COMPLEMENT['c'] = 'g';
        COMPLEMENT['N'] = 'N';
        COMPLEMENT['n'] = 'n';
    }

    private DnaUtils() {} // Prevent instantiation

    // ========== Complement Operations ==========

    /**
     * Get the complement of a single base.
     */
    public static char complement(char base) {
        if (base < COMPLEMENT.length && COMPLEMENT[base] != 0) {
            return COMPLEMENT[base];
        }
        return 'N';
    }

    /**
     * Get the complement of a sequence.
     * Uses StringBuilder for efficiency.
     */
    public static String complement(String sequence) {
        if (sequence == null || sequence.isEmpty()) return "";
        
        StringBuilder sb = new StringBuilder(sequence.length());
        for (int i = 0; i < sequence.length(); i++) {
            sb.append(complement(sequence.charAt(i)));
        }
        return sb.toString();
    }

    /**
     * Get the reverse complement of a sequence.
     * This is the most common operation in molecular biology.
     */
    public static String reverseComplement(String sequence) {
        if (sequence == null || sequence.isEmpty()) return "";
        
        StringBuilder sb = new StringBuilder(sequence.length());
        for (int i = sequence.length() - 1; i >= 0; i--) {
            sb.append(complement(sequence.charAt(i)));
        }
        return sb.toString();
    }

    /**
     * Reverse a sequence without complementing.
     */
    public static String reverse(String sequence) {
        if (sequence == null || sequence.isEmpty()) return "";
        return new StringBuilder(sequence).reverse().toString();
    }

    // ========== GC Content ==========

    /**
     * Calculate GC content as a fraction (0.0 to 1.0).
     */
    public static double gcContent(String sequence) {
        if (sequence == null || sequence.isEmpty()) return 0.0;
        
        int gc = 0;
        int total = 0;
        
        for (int i = 0; i < sequence.length(); i++) {
            char c = Character.toUpperCase(sequence.charAt(i));
            if (c == 'G' || c == 'C') {
                gc++;
                total++;
            } else if (c == 'A' || c == 'T') {
                total++;
            }
            // Ignore N and other characters
        }
        
        return total > 0 ? (double) gc / total : 0.0;
    }

    /**
     * Calculate GC content as a percentage (0 to 100).
     */
    public static double gcContentPercent(String sequence) {
        return gcContent(sequence) * 100.0;
    }

    // ========== Validation ==========

    /**
     * Check if a sequence contains only valid DNA bases (A, C, G, T, N).
     */
    public static boolean isValidDna(String sequence) {
        if (sequence == null || sequence.isEmpty()) return false;
        
        for (int i = 0; i < sequence.length(); i++) {
            char c = Character.toUpperCase(sequence.charAt(i));
            if (c != 'A' && c != 'C' && c != 'G' && c != 'T' && c != 'N') {
                return false;
            }
        }
        return true;
    }

    /**
     * Check if a sequence contains only strict DNA bases (A, C, G, T).
     */
    public static boolean isValidDnaStrict(String sequence) {
        if (sequence == null || sequence.isEmpty()) return false;
        
        for (int i = 0; i < sequence.length(); i++) {
            char c = Character.toUpperCase(sequence.charAt(i));
            if (c != 'A' && c != 'C' && c != 'G' && c != 'T') {
                return false;
            }
        }
        return true;
    }

    /**
     * Sanitize a sequence by removing invalid characters.
     */
    public static String sanitize(String sequence) {
        if (sequence == null) return "";
        
        StringBuilder sb = new StringBuilder(sequence.length());
        for (int i = 0; i < sequence.length(); i++) {
            char c = Character.toUpperCase(sequence.charAt(i));
            if (c == 'A' || c == 'C' || c == 'G' || c == 'T' || c == 'N') {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    // ========== Generation ==========

    /**
     * Generate a random DNA sequence.
     */
    public static String randomSequence(int length, Random random) {
        if (length <= 0) return "";
        
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(DNA_BASES[random.nextInt(4)]);
        }
        return sb.toString();
    }

    /**
     * Generate a random DNA sequence with target GC content.
     * 
     * @param length Sequence length
     * @param targetGc Target GC content (0.0 to 1.0)
     * @param random Random number generator
     */
    public static String randomSequence(int length, double targetGc, Random random) {
        if (length <= 0) return "";
        
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            if (random.nextDouble() < targetGc) {
                sb.append(random.nextBoolean() ? 'G' : 'C');
            } else {
                sb.append(random.nextBoolean() ? 'A' : 'T');
            }
        }
        return sb.toString();
    }

    // ========== Sequence Analysis ==========

    /**
     * Count occurrences of a pattern in a sequence.
     */
    public static int countPattern(String sequence, String pattern) {
        if (sequence == null || pattern == null || pattern.isEmpty()) return 0;
        
        int count = 0;
        int index = 0;
        while ((index = sequence.indexOf(pattern, index)) != -1) {
            count++;
            index += 1; // Allow overlapping matches
        }
        return count;
    }

    /**
     * Find all positions of a pattern in a sequence.
     */
    public static int[] findPattern(String sequence, String pattern) {
        if (sequence == null || pattern == null || pattern.isEmpty()) return new int[0];
        
        java.util.List<Integer> positions = new java.util.ArrayList<>();
        int index = 0;
        while ((index = sequence.indexOf(pattern, index)) != -1) {
            positions.add(index);
            index += 1;
        }
        return positions.stream().mapToInt(Integer::intValue).toArray();
    }

    /**
     * Calculate the Hamming distance between two sequences.
     * Sequences must be the same length.
     */
    public static int hammingDistance(String seq1, String seq2) {
        if (seq1 == null || seq2 == null || seq1.length() != seq2.length()) {
            return -1;
        }
        
        int distance = 0;
        for (int i = 0; i < seq1.length(); i++) {
            if (Character.toUpperCase(seq1.charAt(i)) != Character.toUpperCase(seq2.charAt(i))) {
                distance++;
            }
        }
        return distance;
    }

    /**
     * Calculate melting temperature using the Wallace rule (for short oligos).
     * Tm = 4(G+C) + 2(A+T)
     */
    public static double meltingTemperature(String sequence) {
        if (sequence == null || sequence.isEmpty()) return 0.0;
        
        int gc = 0, at = 0;
        for (int i = 0; i < sequence.length(); i++) {
            char c = Character.toUpperCase(sequence.charAt(i));
            if (c == 'G' || c == 'C') gc++;
            else if (c == 'A' || c == 'T') at++;
        }
        return 4 * gc + 2 * at;
    }

    /**
     * Get a subsequence with bounds checking.
     */
    public static String safeSubstring(String sequence, int start, int length) {
        if (sequence == null || start < 0 || length <= 0) return "";
        
        int end = Math.min(start + length, sequence.length());
        if (start >= sequence.length()) return "";
        
        return sequence.substring(start, end);
    }

    // ========== Formatting ==========

    /**
     * Format a sequence with line breaks every N bases (FASTA style).
     */
    public static String formatFasta(String sequence, int lineWidth) {
        if (sequence == null || sequence.isEmpty()) return "";
        if (lineWidth <= 0) lineWidth = 60; // Default FASTA width
        
        StringBuilder sb = new StringBuilder(sequence.length() + sequence.length() / lineWidth);
        for (int i = 0; i < sequence.length(); i += lineWidth) {
            int end = Math.min(i + lineWidth, sequence.length());
            sb.append(sequence, i, end).append('\n');
        }
        return sb.toString();
    }

    /**
     * Format a sequence with spaces every N bases for readability.
     */
    public static String formatSpaced(String sequence, int groupSize) {
        if (sequence == null || sequence.isEmpty()) return "";
        if (groupSize <= 0) groupSize = 10;
        
        StringBuilder sb = new StringBuilder(sequence.length() + sequence.length() / groupSize);
        for (int i = 0; i < sequence.length(); i++) {
            if (i > 0 && i % groupSize == 0) {
                sb.append(' ');
            }
            sb.append(sequence.charAt(i));
        }
        return sb.toString();
    }
}
