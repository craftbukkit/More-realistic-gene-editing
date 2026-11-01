package com.morerealisticgeneediting.equipment;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class MeasurementModelsTest {
    @Test void visibleInstrumentCannotProduceUltravioletConcentration() {
        var reading=MeasurementModels.optical("dsDNA",2,1,.8f,0,0,0,340,1000);
        assertTrue(Float.isNaN(reading.a260()));
        assertTrue(Float.isNaN(reading.a280()));
        assertTrue(Float.isNaN(reading.a230()));
        assertTrue(Float.isNaN(reading.concentration()));
        assertTrue(Float.isNaN(reading.ratio260280()));
    }
    @Test void repeatedMeasurementsDoNotChangeAliquotTruth() {
        var first=MeasurementModels.optical("virtual_dna_aliquot",2,1,.8f,0,0,0,190,850);
        var second=MeasurementModels.optical("virtual_dna_aliquot",2,1,.8f,0,0,0,190,850);
        assertEquals(first,second);
        assertEquals(100,first.concentration());
        assertEquals(2,first.ratio260280());
        assertEquals(2.5,first.ratio260230(),1e-6);
    }
    @Test void unknownOrZeroDenominatorIsNotReportedAsZeroPurity() {
        var reading=MeasurementModels.optical("unknown",2,0,Float.NaN,0,0,0,190,850);
        assertTrue(Float.isNaN(reading.concentration()));
        assertTrue(Float.isNaN(reading.ratio260280()));
        assertTrue(Float.isNaN(reading.ratio260230()));
    }
    @Test void proteinCannotUseGenericDnaConversion() {
        assertTrue(Float.isNaN(MeasurementModels.optical("protein",1,2,1,0,0,0,190,850).concentration()));
    }
    @Test void pairedReadsAllocateBothReadEndsAndKeepTheirQualitySeparate() {
        int[] r1=new int[150],r2=new int[150];java.util.Arrays.fill(r1,40);java.util.Arrays.fill(r2,20);
        float[] qualities=MeasurementModels.cycleQuality(List.of(r1,r2),150,true);
        assertEquals(300,qualities.length);
        assertEquals(40,qualities[149]);assertEquals(20,qualities[150]);assertEquals(20,qualities[299]);
    }
    @Test void unobservedReadCyclesStayUnknown() {
        float[] qualities=MeasurementModels.cycleQuality(List.of(new int[]{30,35}),100,true);
        assertEquals(200,qualities.length);assertEquals(30,qualities[0]);
        assertTrue(Float.isNaN(qualities[2]));assertTrue(Float.isNaN(qualities[199]));
    }
    @Test void zeroDriveCannotCauseDeliveryAndScoresStayBounded() {
        var off=MeasurementModels.delivery(0,1);assertEquals(0,off.deliveryFraction());assertEquals(1,off.survivingFraction());
        for(int pulses=1;pulses<=10;pulses++)for(int knob=0;knob<=100;knob++){
            var result=MeasurementModels.delivery(knob/100f,pulses);
            assertTrue(result.deliveryFraction()>=0&&result.deliveryFraction()<=1);
            assertTrue(result.survivingFraction()>=0&&result.survivingFraction()<=1);
        }
        assertThrows(IllegalArgumentException.class,()->MeasurementModels.delivery(Float.NaN,1));
        assertThrows(IllegalArgumentException.class,()->MeasurementModels.delivery(.5f,0));
    }
}
