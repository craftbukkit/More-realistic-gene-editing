package com.morerealisticgeneediting.data;

import net.minecraft.nbt.NbtCompound;
import java.util.UUID;

/**
 * Represents a quality control (QC) report for a ReadSet.
 */
public class QCReport {

    private final UUID qcReportId;
    private final UUID readSetId;
    private final double qualityScore;
    private final double gcContent;

    public QCReport(UUID readSetId, double qualityScore, double gcContent) {
        this.qcReportId = UUID.randomUUID();
        this.readSetId = readSetId;
        this.qualityScore = qualityScore;
        this.gcContent = gcContent;
    }

    private QCReport(UUID qcReportId, UUID readSetId, double qualityScore, double gcContent) {
        this.qcReportId = qcReportId;
        this.readSetId = readSetId;
        this.qualityScore = qualityScore;
        this.gcContent = gcContent;
    }

    // Getters...

    public NbtCompound writeNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putUuid("qcReportId", qcReportId);
        nbt.putUuid("readSetId", readSetId);
        nbt.putDouble("qualityScore", qualityScore);
        nbt.putDouble("gcContent", gcContent);
        return nbt;
    }

    public static QCReport fromNbt(NbtCompound nbt) {
        return new QCReport(
            nbt.getUuid("qcReportId"),
            nbt.getUuid("readSetId"),
            nbt.getDouble("qualityScore"),
            nbt.getDouble("gcContent")
        );
    }
}
