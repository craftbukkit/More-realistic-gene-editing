package com.morerealisticgeneediting.genome;
import java.util.*;
/** Deterministic fictional 4kb teaching references, not real organism genome assemblies. */
public final class SpeciesGenomeMapping {
    private SpeciesGenomeMapping() { }
    public static String referenceId(String entityId){return "virtual:"+entityId.replace(':','/')+"/v1";}
    public static String sequence(String entityId){
        Random rng=new Random(0x4d524745L ^ entityId.hashCode());StringBuilder sequence=new StringBuilder(4096);
        for(int i=0;i<4096;i++)sequence.append("ACGT".charAt(rng.nextInt(4)));
        // Fixed artificial teaching target, without claims about a natural gene.
        sequence.replace(100,123,"ACGTACGTACGTACGTACGTAGG");return sequence.toString();
    }
    public static String cellType(String species){
        return Set.of("minecraft:player","minecraft:pig","minecraft:cow","minecraft:sheep","minecraft:horse","minecraft:rabbit","minecraft:wolf","minecraft:cat","minecraft:goat","minecraft:llama").contains(species)?"mammalian":"eukaryotic";
    }
}
