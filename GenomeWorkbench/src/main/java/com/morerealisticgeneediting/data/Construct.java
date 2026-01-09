package com.morerealisticgeneediting.data;

import com.morerealisticgeneediting.util.SafeNBT;
import net.minecraft.nbt.NbtCompound;

import java.util.UUID;

/**
 * Represents a genetic construct designed to perform an edit.
 */
public class Construct {

    private static final int MAX_GRNA_LENGTH = 100;

    public enum EditType {
        KNOCKOUT,
        BASE_EDIT,
        PRIME_EDIT
    }

    private final UUID constructId;
    private final EditType editType;
    private final String guideRNA;

    public Construct(EditType editType, String guideRNA) {
        this.constructId = UUID.randomUUID();
        this.editType = editType;
        this.guideRNA = guideRNA;
    }

    private Construct(UUID constructId, EditType editType, String guideRNA) {
        this.constructId = constructId;
        this.editType = editType;
        this.guideRNA = guideRNA;
    }

    // Getters...

    public NbtCompound writeNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putUuid("constructId", constructId);
        nbt.putString("editType", editType.name());
        SafeNBT.putString(nbt, "guideRNA", guideRNA, MAX_GRNA_LENGTH);
        return nbt;
    }

    public static Construct fromNbt(NbtCompound nbt) {
        UUID id = nbt.getUuid("constructId");
        EditType type = EditType.valueOf(nbt.getString("editType"));
        String gRNA = nbt.getString("guideRNA");
        if (gRNA.length() > MAX_GRNA_LENGTH) {
            throw new IllegalArgumentException("guideRNA exceeds max length in NBT");
        }
        return new Construct(id, type, gRNA);
    }
}
