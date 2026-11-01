package com.morerealisticgeneediting.security;

import com.morerealisticgeneediting.util.SafeNBT;
import net.minecraft.nbt.NbtCompound;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SafeNBTTest {
    @Test void primitiveArraysCannotBypassSizeBudget() {
        NbtCompound bytes = new NbtCompound();
        bytes.putByteArray("data", new byte[4096]);
        NbtCompound integers = new NbtCompound();
        integers.putIntArray("data", new int[1024]);
        NbtCompound longs = new NbtCompound();
        longs.putLongArray("data", new long[512]);
        for (NbtCompound payload : new NbtCompound[]{bytes, integers, longs}) {
            assertFalse(SafeNBT.isValidSize(payload, 2048));
            assertTrue(SafeNBT.isValidSize(payload, 8192));
        }
    }

    @Test void multibyteNamesAndValuesCountTowardsBudget() {
        NbtCompound payload = new NbtCompound();
        payload.putString("样本".repeat(10), "基因".repeat(100));
        assertFalse(SafeNBT.isValidSize(payload, 512));
        assertTrue(SafeNBT.isValidSize(payload, 1024));
    }

    @Test void ExcessivelyNestedPayloadIsRejectedWithoutCrashing() {
        NbtCompound root = new NbtCompound();
        NbtCompound child = root;
        for (int i = 0; i < SafeNBT.MAX_NBT_DEPTH + 2; i++) {
            NbtCompound next = new NbtCompound();
            child.put("child", next);
            child = next;
        }
        assertFalse(SafeNBT.isValidSize(root));
    }
}
