package com.morerealisticgeneediting.genome;

/**
 * A utility class for handling 2-bit encoding of DNA bases.
 * This allows for storing 4 bases in a single byte.
 * A = 00, C = 01, G = 10, T = 11
 */
public final class TwoBitEncoding {

    private TwoBitEncoding() {}

    /**
     * Packs an array of bases into a 2-bit encoded byte array.
     *
     * @param bases The array of bases (0-3).
     * @return A byte array containing the packed bases.
     */
    public static byte[] pack(byte[] bases) {
        int packedLength = (bases.length + 3) / 4;
        byte[] packed = new byte[packedLength];
        for (int i = 0; i < bases.length; i++) {
            int packedIndex = i / 4;
            int shift = (i % 4) * 2;
            packed[packedIndex] |= (bases[i] & 0b11) << shift;
        }
        return packed;
    }

    /**
     * Unpacks a 2-bit encoded byte array into an array of bases.
     *
     * @param packed The packed byte array.
     * @param originalLength The original number of bases.
     * @return An array of bases (0-3).
     */
    public static byte[] unpack(byte[] packed, int originalLength) {
        byte[] bases = new byte[originalLength];
        for (int i = 0; i < originalLength; i++) {
            int packedIndex = i / 4;
            int shift = (i % 4) * 2;
            bases[i] = (byte) ((packed[packedIndex] >> shift) & 0b11);
        }
        return bases;
    }

    /**
     * Gets a single base from a packed array.
     *
     * @param packed The packed byte array.
     * @param index The index of the base to retrieve.
     * @return The base at the specified index.
     */
    public static byte getBase(byte[] packed, int index) {
        int packedIndex = index / 4;
        if (packedIndex < 0 || packedIndex >= packed.length) {
            throw new IndexOutOfBoundsException("Index " + index + " is out of bounds for packed array of length " + packed.length * 4);
        }
        int shift = (index % 4) * 2;
        return (byte) ((packed[packedIndex] >> shift) & 0b11);
    }
}
