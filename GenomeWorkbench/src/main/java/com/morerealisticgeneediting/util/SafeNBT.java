package com.morerealisticgeneediting.util;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Utility class for safely reading and writing NBT compounds.
 * 
 * Features:
 * - Size limit checks to prevent memory exhaustion
 * - Safe getters with default values
 * - Validation of data before writing
 * - Protection against malformed data
 */
public final class SafeNBT {

    // ========== Constants ==========
    public static final int MAX_STRING_LENGTH = 32767;
    public static final int MAX_LIST_SIZE = 512;
    public static final int MAX_NBT_DEPTH = 512;
    public static final long MAX_NBT_SIZE = 2 * 1024 * 1024; // 2MB

    private SafeNBT() {} // Prevent instantiation

    // ========== Safe Writers ==========

    /**
     * Safely puts a string into an NbtCompound, checking its length first.
     *
     * @param nbt The NbtCompound to write to
     * @param key The key to use
     * @param value The string value to write
     * @param maxLength The maximum allowed length
     * @return true if the string was written, false if too long or null
     */
    public static boolean putString(NbtCompound nbt, String key, @Nullable String value, int maxLength) {
        if (nbt == null || key == null || value == null) return false;
        if (value.length() > maxLength) return false;
        nbt.putString(key, value);
        return true;
    }

    /**
     * Safely puts a string with default max length.
     */
    public static boolean putString(NbtCompound nbt, String key, @Nullable String value) {
        return putString(nbt, key, value, MAX_STRING_LENGTH);
    }

    /**
     * Safely puts a truncated string (truncates if too long).
     */
    public static void putStringTruncated(NbtCompound nbt, String key, @Nullable String value, int maxLength) {
        if (nbt == null || key == null || value == null) return;
        String truncated = value.length() > maxLength ? value.substring(0, maxLength) : value;
        nbt.putString(key, truncated);
    }

    /**
     * Safely puts an NbtList into an NbtCompound.
     */
    public static boolean putList(NbtCompound nbt, String key, @Nullable NbtList list, int maxListSize) {
        if (nbt == null || key == null || list == null) return false;
        if (list.size() > maxListSize) return false;
        nbt.put(key, list);
        return true;
    }

    /**
     * Safely puts an NbtList with default max size.
     */
    public static boolean putList(NbtCompound nbt, String key, @Nullable NbtList list) {
        return putList(nbt, key, list, MAX_LIST_SIZE);
    }

    /**
     * Safely puts an integer within a range.
     */
    public static void putIntClamped(NbtCompound nbt, String key, int value, int min, int max) {
        if (nbt == null || key == null) return;
        nbt.putInt(key, Math.max(min, Math.min(max, value)));
    }

    /**
     * Safely puts a long within a range.
     */
    public static void putLongClamped(NbtCompound nbt, String key, long value, long min, long max) {
        if (nbt == null || key == null) return;
        nbt.putLong(key, Math.max(min, Math.min(max, value)));
    }

    /**
     * Safely puts a float within a range.
     */
    public static void putFloatClamped(NbtCompound nbt, String key, float value, float min, float max) {
        if (nbt == null || key == null) return;
        nbt.putFloat(key, Math.max(min, Math.min(max, value)));
    }

    /**
     * Safely puts a double within a range.
     */
    public static void putDoubleClamped(NbtCompound nbt, String key, double value, double min, double max) {
        if (nbt == null || key == null) return;
        nbt.putDouble(key, Math.max(min, Math.min(max, value)));
    }

    // ========== Safe Readers ==========

    /**
     * Safely gets a string with a default value.
     */
    public static String getString(NbtCompound nbt, String key, String defaultValue) {
        if (nbt == null || key == null || !nbt.contains(key, NbtElement.STRING_TYPE)) {
            return defaultValue;
        }
        return nbt.getString(key);
    }

    /**
     * Safely gets a string as Optional.
     */
    public static Optional<String> getStringOptional(NbtCompound nbt, String key) {
        if (nbt == null || key == null || !nbt.contains(key, NbtElement.STRING_TYPE)) {
            return Optional.empty();
        }
        return Optional.of(nbt.getString(key));
    }

    /**
     * Safely gets an integer with bounds checking.
     */
    public static int getIntClamped(NbtCompound nbt, String key, int defaultValue, int min, int max) {
        if (nbt == null || key == null || !nbt.contains(key, NbtElement.INT_TYPE)) {
            return Math.max(min, Math.min(max, defaultValue));
        }
        return Math.max(min, Math.min(max, nbt.getInt(key)));
    }

    /**
     * Safely gets a long with bounds checking.
     */
    public static long getLongClamped(NbtCompound nbt, String key, long defaultValue, long min, long max) {
        if (nbt == null || key == null || !nbt.contains(key, NbtElement.LONG_TYPE)) {
            return Math.max(min, Math.min(max, defaultValue));
        }
        return Math.max(min, Math.min(max, nbt.getLong(key)));
    }

    /**
     * Safely gets a float with bounds checking.
     */
    public static float getFloatClamped(NbtCompound nbt, String key, float defaultValue, float min, float max) {
        if (nbt == null || key == null || !nbt.contains(key, NbtElement.FLOAT_TYPE)) {
            return Math.max(min, Math.min(max, defaultValue));
        }
        return Math.max(min, Math.min(max, nbt.getFloat(key)));
    }

    /**
     * Safely gets a double with bounds checking.
     */
    public static double getDoubleClamped(NbtCompound nbt, String key, double defaultValue, double min, double max) {
        if (nbt == null || key == null || !nbt.contains(key, NbtElement.DOUBLE_TYPE)) {
            return Math.max(min, Math.min(max, defaultValue));
        }
        return Math.max(min, Math.min(max, nbt.getDouble(key)));
    }

    /**
     * Safely gets a boolean with default value.
     */
    public static boolean getBoolean(NbtCompound nbt, String key, boolean defaultValue) {
        if (nbt == null || key == null || !nbt.contains(key, NbtElement.BYTE_TYPE)) {
            return defaultValue;
        }
        return nbt.getBoolean(key);
    }

    /**
     * Safely gets a compound with null check.
     */
    @Nullable
    public static NbtCompound getCompound(NbtCompound nbt, String key) {
        if (nbt == null || key == null || !nbt.contains(key, NbtElement.COMPOUND_TYPE)) {
            return null;
        }
        return nbt.getCompound(key);
    }

    /**
     * Safely gets a compound as Optional.
     */
    public static Optional<NbtCompound> getCompoundOptional(NbtCompound nbt, String key) {
        return Optional.ofNullable(getCompound(nbt, key));
    }

    /**
     * Safely gets a list with null check.
     */
    @Nullable
    public static NbtList getList(NbtCompound nbt, String key, int type) {
        if (nbt == null || key == null || !nbt.contains(key, NbtElement.LIST_TYPE)) {
            return null;
        }
        return nbt.getList(key, type);
    }

    // ========== Validation ==========

    /**
     * Estimates the size of an NBT compound.
     */
    public static long estimateSize(@Nullable NbtCompound nbt) {
        if (nbt == null) return 0;
        return estimateSizeRecursive(nbt, 0);
    }

    private static long estimateSizeRecursive(NbtElement element, int depth) {
        if (depth > MAX_NBT_DEPTH) {
            throw new IllegalArgumentException("NBT depth exceeds maximum");
        }
        
        if (element == null) return 0;
        
        long size = 1; // Type byte
        
        if (element instanceof NbtCompound compound) {
            for (String key : compound.getKeys()) {
                size += 2 + key.length();
                size += estimateSizeRecursive(compound.get(key), depth + 1);
            }
        } else if (element instanceof NbtList list) {
            size += 5;
            for (int i = 0; i < list.size(); i++) {
                size += estimateSizeRecursive(list.get(i), depth + 1);
            }
        } else if (element.getType() == NbtElement.STRING_TYPE) {
            size += 2 + element.asString().length();
        } else {
            size += 8; // For primitives (overestimate)
        }
        
        return size;
    }

    /**
     * Validates that an NBT compound is within size limits.
     */
    public static boolean isValidSize(@Nullable NbtCompound nbt, long maxSize) {
        if (nbt == null) return true;
        try {
            return estimateSize(nbt) <= maxSize;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Validates that an NBT compound is within default size limits.
     */
    public static boolean isValidSize(@Nullable NbtCompound nbt) {
        return isValidSize(nbt, MAX_NBT_SIZE);
    }
}
