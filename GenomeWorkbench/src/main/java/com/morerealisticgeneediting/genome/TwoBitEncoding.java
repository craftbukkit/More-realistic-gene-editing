package com.morerealisticgeneediting.genome;
import java.util.Objects;
/** Packs unambiguous A/C/G/T only; the caller must retain the logical base count. */
public final class TwoBitEncoding {
    private TwoBitEncoding() { }
    public static byte encodeBase(char base) {
        return switch (Character.toUpperCase(base)) {
            case 'A' -> 0; case 'C' -> 1; case 'G' -> 2; case 'T' -> 3;
            default -> throw new IllegalArgumentException("Unsupported DNA base: " + base);
        };
    }
    public static char decodeBase(byte base) {
        if (base < 0 || base > 3) throw new IllegalArgumentException("Base must be in [0,3]");
        return "ACGT".charAt(base);
    }
    public static byte[] pack(String sequence) {
        Objects.requireNonNull(sequence, "sequence");
        byte[] bases = new byte[sequence.length()];
        for (int i=0;i<bases.length;i++) bases[i]=encodeBase(sequence.charAt(i));
        return pack(bases);
    }
    public static byte[] pack(byte[] bases) {
        Objects.requireNonNull(bases, "bases");
        byte[] packed = new byte[(int)((bases.length + 3L) / 4)];
        for (int i=0;i<bases.length;i++) {
            if (bases[i]<0 || bases[i]>3) throw new IllegalArgumentException("Base must be in [0,3]");
            packed[i/4] |= (byte)(bases[i] << ((i%4)*2));
        }
        return packed;
    }
    public static byte[] unpack(byte[] packed, int originalLength) {
        Objects.requireNonNull(packed, "packed");
        if (originalLength<0 || originalLength > (long)packed.length*4) throw new IllegalArgumentException("Invalid logical length");
        byte[] result=new byte[originalLength];
        for (int i=0;i<originalLength;i++) result[i]=getBase(packed,i);
        return result;
    }
    public static byte getBase(byte[] packed,int index) {
        Objects.requireNonNull(packed, "packed");
        if (index<0 || (long)index >= (long)packed.length*4) throw new IndexOutOfBoundsException("Invalid base index: "+index);
        return (byte)((packed[index/4] >>> ((index%4)*2)) & 3);
    }
    public static char getBaseAt(byte[] packed,long index) {
        if(index<0 || index>Integer.MAX_VALUE) throw new IndexOutOfBoundsException("Invalid base index: "+index);
        return decodeBase(getBase(packed,(int)index));
    }
}
