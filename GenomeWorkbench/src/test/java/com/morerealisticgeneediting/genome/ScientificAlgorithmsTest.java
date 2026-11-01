package com.morerealisticgeneediting.genome;

import com.morerealisticgeneediting.data.EditOutcome;
import com.morerealisticgeneediting.genome.crispr.CrisprEngine;
import com.morerealisticgeneediting.genome.pcr.PcrSimulator;
import com.morerealisticgeneediting.genome.sequencing.SequencingSimulator;
import com.morerealisticgeneediting.genome.analysis.GelElectrophoresisSimulator;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ScientificAlgorithmsTest {
    private Genome genome(String s) { return Genome.createFromUnpackedSequence(new UUID(0,1),s); }
    private SequencingSimulator.SequencingRead read(String sequence, boolean reversed) {
        int[] q=new int[sequence.length()]; Arrays.fill(q,40);
        return new SequencingSimulator.SequencingRead("toy",sequence,q,0,reversed,null);
    }
    @Test void includesPamAtLastValidStartAndRecognizesBothStrands() {
        CrisprEngine c=new CrisprEngine(0);
        var forward=c.findPamSites(genome("A".repeat(20)+"AGG"),0,23,CrisprEngine.SPCAS9_PAM);
        assertEquals(1,forward.size()); assertEquals(20,forward.get(0).position()); assertEquals(17,forward.get(0).cutPosition());
        var reverse=c.findPamSites(genome("CCT"+"T".repeat(20)),0,23,CrisprEngine.SPCAS9_PAM);
        assertEquals(1,reverse.size()); assertEquals(CrisprEngine.Strand.REVERSE,reverse.get(0).strand());
        assertEquals(6,reverse.get(0).cutPosition());
    }
    @Test void cas12aUsesUpstreamPamAndLongerGuide() {
        CrisprEngine c=new CrisprEngine(0);
        String sequence="TTTA"+"C".repeat(23);
        var sites=c.findPamSites(genome(sequence),0,sequence.length(),CrisprEngine.CPF1_PAM);
        assertEquals(1,sites.size()); assertEquals(4,sites.get(0).protospacerStart());
        assertEquals(23,sites.get(0).protospacer().length()); assertEquals(22,sites.get(0).cutPosition());
        assertEquals(CrisprEngine.Nuclease.CAS12A,sites.get(0).nuclease());
    }
    @Test void everyRepairSizeIsBoundedAndNoEditHasNoByproducts() {
        Genome g=genome("A".repeat(20)+"AGG"+"A".repeat(100));
        var target=new CrisprEngine(0).findPamSites(g,0,23,CrisprEngine.SPCAS9_PAM).get(0);
        CrisprEngine c=new CrisprEngine(0); int noEdit=0,insertions=0;
        for(int i=0;i<10_000;i++) {
            var result=c.performEditing(g,target,null); var outcome=result.primaryOutcome();
            assertTrue(result.byproducts().isEmpty()); assertTrue(Double.isNaN(result.qualityMetrics().mosaicismLevel()));
            assertTrue(Double.isNaN(result.qualityMetrics().frameshiftProbability()));
            if(outcome.getType()==EditOutcome.Type.NO_CHANGE) {noEdit++;assertEquals(0,result.editingEfficiency());assertEquals(0,result.qualityMetrics().indelFrequency());}
            if(outcome.getType()==EditOutcome.Type.INSERTION) {insertions++;assertTrue(outcome.size()>=1&&outcome.size()<=10);assertEquals(outcome.size(),outcome.getSequence().length());}
            if(outcome.getType()==EditOutcome.Type.DELETION) assertTrue(outcome.size()>=1&&outcome.size()<=30);
        }
        assertTrue(noEdit>0);assertTrue(insertions>0);
    }
    @Test void rejectsForgedTargetGeometryAndInvalidTemplate() {
        Genome g=genome("A".repeat(20)+"AGG"); CrisprEngine c=new CrisprEngine(0);
        var target=c.findPamSites(g,0,23,CrisprEngine.SPCAS9_PAM).get(0);
        var forged=new CrisprEngine.TargetSite(target.position(),target.protospacer(),target.pamSequence(),0.5,0,
            target.strand(),target.nuclease(),target.protospacerStart(),0);
        assertThrows(IllegalArgumentException.class,()->c.performEditing(g,forged,null));
        assertThrows(IllegalArgumentException.class,()->c.performEditing(g,target,"NNN"));
        assertThrows(IllegalArgumentException.class,()->c.findPamSites(g,-1,23,CrisprEngine.SPCAS9_PAM));
    }
    @Test void primerFormulaNoLongerDoubleSubtractsLengthAndDefaultCandidatesExist() {
        assertEquals(51.78,PcrSimulator.Primer.design("ACGT".repeat(5),true).meltingTemperature(),1e-9);
        var highGc=PcrSimulator.Primer.design("GC".repeat(6)+"AT".repeat(4),true);
        assertTrue(highGc.meltingTemperature()>=55&&highGc.meltingTemperature()<=65);
        String upstream="GC".repeat(6)+"AT".repeat(4);
        Genome g=genome(upstream+"A".repeat(10)+upstream);
        var pair=new PcrSimulator(0).designPrimers(g,20,30,20);
        assertEquals(2,pair.size()); assertTrue(pair.get(0).isForward()); assertFalse(pair.get(1).isForward());
    }
    @Test void primerDesignHandlesSampleBoundaryAndRejectsInvalidInputs() {
        PcrSimulator p=new PcrSimulator(0); Genome g=genome("ACGT".repeat(30));
        assertDoesNotThrow(()->p.designPrimers(g,0,10,20));
        assertThrows(IllegalArgumentException.class,()->p.designPrimers(g,1,10,10_000));
        assertThrows(IllegalArgumentException.class,()->PcrSimulator.Primer.design("N",true));
        assertThrows(IllegalArgumentException.class,()->new PcrSimulator.ReactionParameters(-1,55,60,false,1,1));
    }
    @Test void pcrDoesNotAmplifyWithoutRequiredComponents() {
        var forward=PcrSimulator.Primer.design("GC".repeat(6)+"AT".repeat(4),true);
        var reverse=PcrSimulator.Primer.design("GC".repeat(6)+"AT".repeat(4),false);
        Genome template=genome(forward.sequence()+"AAAACCCC"+"AT".repeat(4)+"GC".repeat(6));
        assertTrue(new PcrSimulator(0).runPcr(template,forward,reverse,PcrSimulator.ReactionParameters.standard()).success());
        var result=new PcrSimulator(0).runPcr(template,forward,reverse,
                new PcrSimulator.ReactionParameters(30,55,60,false,1.5,0));
        assertFalse(result.success());assertEquals(0,result.totalCopies());
        assertFalse(new PcrSimulator(0).runPcr(template,forward,reverse,
                new PcrSimulator.ReactionParameters(30,55,60,false,0,200)).success());
    }
    @Test void perfectReverseReadDoesNotCreateVariants() {
        assertTrue(new SequencingSimulator(0).callVariants(List.of(read("CGGGTTTT",true)),genome("AAAACCCG"),1,20).isEmpty());
    }
    @Test void reverseReadQualityFollowsReverseComplementedBase() {
        // Original reference AC. Reverse read AC -> forward GT. Only forward position 0 has high quality.
        var r=new SequencingSimulator.SequencingRead("r","AC",new int[]{2,40},0,true,null);
        var variants=new SequencingSimulator(0).callVariants(List.of(r),genome("AC"),1,20);
        assertEquals(1,variants.size());assertEquals(0,variants.get(0).position());assertEquals("G",variants.get(0).alternateAllele());
    }
    @Test void highQualityReferenceReadsDoNotRescueLowQualityAlt() {
        var low=new SequencingSimulator.SequencingRead("low","C",new int[]{2},0,false,null);
        List<SequencingSimulator.SequencingRead> reads=new ArrayList<>();reads.add(low);
        for(int i=0;i<10;i++) reads.add(read("A",false));
        assertTrue(new SequencingSimulator(0).callVariants(reads,genome("A"),1,20).isEmpty());
    }
    @Test void shortRegionProducesValidFastqAndReadsStayInRequestedRegion() {
        SequencingSimulator s=new SequencingSimulator(0);
        var result=s.runSequencing(genome("ACGT".repeat(100)),SequencingSimulator.Technology.ILLUMINA_PE150,3,100,100);
        assertFalse(result.reads().isEmpty());
        for(var r:result.reads()) {assertEquals(r.sequence().length(),r.qualityScores().length);assertTrue(r.referencePosition()>=100);assertTrue(r.referencePosition()+r.sequence().length()<=200);assertNotNull(r.mateId());assertEquals(r.sequence().length(),r.toFastq().split("\n")[3].length());}
        assertEquals(-1,result.stats().estimatedVariants());
    }
    @Test void pairedSettingsOverloadUsesActualReadLength() {
        var result=new SequencingSimulator(0).runSequencing(genome("ACGT".repeat(100)),SequencingSimulator.Technology.ILLUMINA_PE150,0L,400,2,50,true);
        assertEquals(50,result.stats().meanReadLength());assertEquals(16,result.reads().size());assertEquals(800,result.stats().totalBases());
    }
    @Test void sequencingRejectsUnsafeOrInvalidRequestsBeforeAllocation() {
        Genome g=genome("ACGT".repeat(100));SequencingSimulator s=new SequencingSimulator(0);
        assertThrows(IllegalArgumentException.class,()->s.runSequencing(g,SequencingSimulator.Technology.ILLUMINA_SE150,-1,0,400));
        assertThrows(IllegalArgumentException.class,()->s.runSequencing(g,SequencingSimulator.Technology.ILLUMINA_SE150,1,399,2));
        assertThrows(IllegalArgumentException.class,()->s.runSequencing(g,SequencingSimulator.Technology.ILLUMINA_SE150,1001,0,400));
        assertThrows(IllegalArgumentException.class,()->new SequencingSimulator.SequencingRead("bad","AC",new int[]{30},0,false,null));
    }
    @Test void n50UsesCeilingOfHalfTotalLength() {
        SequencingSimulator s=new SequencingSimulator(0);
        assertEquals(1,s.calculateN50(List.of(read("AA",false),read("A",false),read("A",false),read("A",false))));
        assertEquals(0,s.calculateN50(List.of()));
    }
    private GelElectrophoresisSimulator.GelResult gel(double time,int volts) {
        return new GelElectrophoresisSimulator(0).runGel(List.of(GelElectrophoresisSimulator.DnaSample.single("toy",1000,100)),GelElectrophoresisSimulator.GelConcentration.STANDARD_1_0,time,volts);
    }
    @Test void gelRequiresBothTimeAndDriveAndIncreasesBeforeBoundary() {
        assertEquals(0,gel(0,100).lanes().get(0).get(0).migrationDistance());
        assertEquals(0,gel(60,0).lanes().get(0).get(0).migrationDistance());
        assertTrue(gel(60,100).lanes().get(0).get(0).migrationDistance()>gel(30,100).lanes().get(0).get(0).migrationDistance());
        assertEquals(-1,gel(60,100).lanes().get(0).get(0).estimatedSize());
    }
    @Test void gelDoesNotInferLadderFromFirstSampleOrCalibrateStationaryBands() {
        var s=new GelElectrophoresisSimulator(0);
        var result=s.runGel(List.of(GelElectrophoresisSimulator.DnaSample.ladder("standard",new int[]{100,1000}),GelElectrophoresisSimulator.DnaSample.single("sample",500,100)),GelElectrophoresisSimulator.GelConcentration.STANDARD_1_0,0,100);
        assertEquals(-1,result.lanes().get(1).get(0).estimatedSize());
        assertThrows(IllegalArgumentException.class,()->gel(-1,100));
    }
}
