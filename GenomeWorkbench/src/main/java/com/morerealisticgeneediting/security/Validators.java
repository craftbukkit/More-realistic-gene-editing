package com.morerealisticgeneediting.security;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Pattern;

/**
 * Comprehensive input validation utilities for security.
 * 
 * All user inputs should be validated before processing to prevent:
 * - Buffer overflow attacks
 * - Injection attacks  
 * - Resource exhaustion
 * - Invalid state corruption
 */
public final class Validators {

    // ========== Constants ==========
    public static final int MAX_STRING_LENGTH = 32767;
    public static final int MAX_SEQUENCE_LENGTH = 100000;
    public static final int MAX_SLICE_LENGTH = 10000;
    public static final int MAX_SEARCH_PATTERN_LENGTH = 100;
    public static final int MAX_NBT_DEPTH = 512;
    public static final long MAX_NBT_SIZE = 2 * 1024 * 1024; // 2MB
    public static final long MAX_GENOME_LENGTH = 10_000_000_000L;
    public static final double MAX_PLAYER_REACH = 64.0;

    // ========== Patterns ==========
    private static final Pattern DNA_PATTERN = Pattern.compile("^[ACGTNacgtn]*$");
    private static final Pattern IUPAC_PATTERN = Pattern.compile("^[ACGTURYSWKMBDHVNacgturyswkmbdhvn]*$");
    private static final Pattern SAFE_STRING_PATTERN = Pattern.compile("^[\\w\\s\\-_.,:;!?()\\[\\]{}@#$%&*+=<>/'\"]*$");
    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("^[a-z][a-z0-9_]*$");

    private Validators() {} // Prevent instantiation

    // ========== DNA Sequence Validation ==========

    /**
     * Validates a DNA sequence (A, C, G, T, N only).
     */
    public static boolean isValidDnaSequence(@Nullable String sequence, int maxLength) {
        if (sequence == null || sequence.isEmpty() || sequence.length() > maxLength) {
            return false;
        }
        return DNA_PATTERN.matcher(sequence).matches();
    }

    /**
     * Validates a DNA sequence with IUPAC ambiguity codes.
     */
    public static boolean isValidIupacSequence(@Nullable String sequence, int maxLength) {
        if (sequence == null || sequence.isEmpty() || sequence.length() > maxLength) {
            return false;
        }
        return IUPAC_PATTERN.matcher(sequence).matches();
    }

    /**
     * Validates a search motif.
     */
    public static boolean isValidMotif(@Nullable String motif, int maxLength) {
        if (motif == null || motif.isEmpty() || motif.length() > maxLength) {
            return false;
        }
        return DNA_PATTERN.matcher(motif).matches();
    }

    // ========== Range Validation ==========

    /**
     * Validates a genomic range with overflow protection.
     */
    public static boolean isValidGenomeRange(long start, int length, long maxGenomeLength, int maxLength) {
        if (start < 0 || length <= 0 || length > maxLength) {
            return false;
        }
        // Check for potential overflow
        try {
            long end = Math.addExact(start, length);
            return end <= maxGenomeLength;
        } catch (ArithmeticException e) {
            return false; // Overflow occurred
        }
    }

    /**
     * Validates a slice request with comprehensive checks.
     */
    public static ValidationResult validateSliceRequest(long start, int length, long maxGenomeLength) {
        if (start < 0) {
            return ValidationResult.failure("Start position cannot be negative");
        }
        if (length <= 0) {
            return ValidationResult.failure("Length must be positive");
        }
        if (length > MAX_SLICE_LENGTH) {
            return ValidationResult.failure("Slice length exceeds maximum (" + MAX_SLICE_LENGTH + ")");
        }
        try {
            long end = Math.addExact(start, length);
            if (end > maxGenomeLength) {
                return ValidationResult.failure("Range exceeds genome length");
            }
        } catch (ArithmeticException e) {
            return ValidationResult.failure("Position overflow");
        }
        return ValidationResult.success();
    }

    // ========== String Validation ==========

    /**
     * Validates a general user input string.
     */
    public static boolean isValidUserString(@Nullable String text, int maxLength) {
        if (text == null || text.length() > maxLength) {
            return false;
        }
        return SAFE_STRING_PATTERN.matcher(text).matches();
    }

    /**
     * Validates an identifier (lowercase, alphanumeric with underscores).
     */
    public static boolean isValidIdentifier(@Nullable String id, int maxLength) {
        if (id == null || id.isEmpty() || id.length() > maxLength) {
            return false;
        }
        return IDENTIFIER_PATTERN.matcher(id).matches();
    }

    /**
     * Sanitizes a string by removing potentially dangerous characters.
     */
    public static String sanitizeString(@Nullable String input, int maxLength) {
        if (input == null) return "";
        
        String sanitized = input
            .replaceAll("[<>]", "")  // Remove HTML brackets
            .replaceAll("[\u0000-\u001F\u007F-\u009F]", "")  // Remove control characters
            .trim();
        
        if (sanitized.length() > maxLength) {
            sanitized = sanitized.substring(0, maxLength);
        }
        
        return sanitized;
    }

    // ========== Numeric Validation ==========

    /**
     * Clamps an integer to a valid range.
     */
    public static int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Clamps a long to a valid range.
     */
    public static long clampLong(long value, long min, long max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Safely adds two longs, returning Long.MAX_VALUE on overflow.
     */
    public static long safeAdd(long a, long b) {
        try {
            return Math.addExact(a, b);
        } catch (ArithmeticException e) {
            return Long.MAX_VALUE;
        }
    }

    // ========== Block Position Validation ==========

    /**
     * Validates a block position is within world bounds.
     */
    public static boolean isValidBlockPos(@Nullable BlockPos pos) {
        if (pos == null) return false;
        return pos.getY() >= -64 && pos.getY() <= 320 &&
               Math.abs(pos.getX()) <= 30_000_000 &&
               Math.abs(pos.getZ()) <= 30_000_000;
    }

    /**
     * Validates player can access a block position (distance check).
     */
    public static boolean canPlayerAccessBlock(@Nullable ServerPlayerEntity player, @Nullable BlockPos pos) {
        if (player == null || pos == null) return false;
        if (!isValidBlockPos(pos)) return false;
        
        double distSq = player.squaredDistanceTo(
            pos.getX() + 0.5, 
            pos.getY() + 0.5, 
            pos.getZ() + 0.5
        );
        return distSq <= MAX_PLAYER_REACH * MAX_PLAYER_REACH;
    }

    // ========== NBT Validation ==========

    /**
     * Estimates the size of an NBT compound in bytes.
     */
    public static long estimateNbtSize(@Nullable NbtCompound nbt) {
        if (nbt == null) return 0;
        return estimateNbtSizeRecursive(nbt, 0);
    }

    private static long estimateNbtSizeRecursive(NbtElement element, int depth) {
        if (depth > MAX_NBT_DEPTH) {
            throw new IllegalArgumentException("NBT depth exceeds maximum");
        }
        
        if (element == null) return 0;
        
        long size = 1; // Type byte
        
        if (element instanceof NbtCompound compound) {
            for (String key : compound.getKeys()) {
                size += 2 + key.length(); // Key length + key string
                size += estimateNbtSizeRecursive(compound.get(key), depth + 1);
            }
        } else if (element instanceof NbtList list) {
            size += 5; // Type + length
            for (int i = 0; i < list.size(); i++) {
                size += estimateNbtSizeRecursive(list.get(i), depth + 1);
            }
        } else if (element.getType() == NbtElement.STRING_TYPE) {
            size += 2 + element.asString().length();
        } else if (element.getType() == NbtElement.INT_ARRAY_TYPE) {
            size += 4 + element.asString().length() * 4; // Approximation
        } else if (element.getType() == NbtElement.BYTE_ARRAY_TYPE) {
            size += 4 + element.asString().length();
        } else {
            size += 8; // For primitives
        }
        
        return size;
    }

    /**
     * Validates NBT is within acceptable size limits.
     */
    public static boolean isNbtSizeValid(@Nullable NbtCompound nbt, long maxSize) {
        if (nbt == null) return true;
        try {
            return estimateNbtSize(nbt) <= maxSize;
        } catch (IllegalArgumentException e) {
            return false; // Depth exceeded
        }
    }

    // ========== Result Class ==========

    /**
     * Result of a validation operation.
     */
    public record ValidationResult(boolean valid, String errorMessage) {
        public static ValidationResult success() {
            return new ValidationResult(true, null);
        }
        
        public static ValidationResult failure(String message) {
            return new ValidationResult(false, message);
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public boolean isFailed() {
            return !valid;
        }
    }
}
