package com.morerealisticgeneediting.genome;
import java.util.*;
public final class MotifSearch {
    public static final int MAX_RESULTS=4096;
    private MotifSearch() { }
    public static List<Integer> search(GenomeSlice slice,String motif){
        Objects.requireNonNull(slice);Objects.requireNonNull(motif);
        motif=motif.toUpperCase(Locale.ROOT);
        if(motif.isEmpty()||motif.length()>256||!motif.matches("[ACGT]+"))throw new IllegalArgumentException("Motif must contain 1-256 A/C/G/T bases");
        List<Integer> found=new ArrayList<>();String sequence=slice.getSequence();
        for(int p=sequence.indexOf(motif);p>=0;p=sequence.indexOf(motif,p+1)){
            if(found.size()==MAX_RESULTS)throw new IllegalArgumentException("Too many motif matches; narrow the window");found.add(p);
        }
        return List.copyOf(found);
    }
}
