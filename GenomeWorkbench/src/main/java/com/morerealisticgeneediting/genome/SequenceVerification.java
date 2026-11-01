package com.morerealisticgeneediting.genome;
import com.morerealisticgeneediting.genome.sequencing.SequencingSimulator.SequencingRead;
import java.util.List;
/** Game-only concordance against the stored edited sample. This is not independent variant discovery or functional validation. */
public final class SequenceVerification {
    private SequenceVerification() { }
    public record Result(int coveredBases,int mismatches,int totalBases,boolean passed) { }
    public static Result verify(Genome sample,List<SequencingRead> reads,int minQuality,int minDepth){
        int length=Math.toIntExact(sample.getTotalLength());
        if(length<1||length>100_000||minQuality<0||minQuality>93||minDepth<1||minDepth>1000)throw new IllegalArgumentException("Invalid concordance bounds");
        if(reads.size()>200_000)throw new IllegalArgumentException("Too many reads");
        int[][] counts=new int[4][length];long total=0;
        for(SequencingRead read:reads){
            total+=read.sequence().length();if(total>10_000_000)throw new IllegalArgumentException("Concordance base budget exceeded");
            long start=read.referencePosition();int n=read.sequence().length();
            if(start<0||start>length||n>length-start)throw new IllegalArgumentException("Read outside sample");
            int[] quality=read.qualityScores();
            for(int i=0;i<n;i++){
                int readIndex=read.isReversed()?n-1-i:i;if(quality[readIndex]<minQuality)continue;
                char base=Character.toUpperCase(read.sequence().charAt(readIndex));
                if(read.isReversed())base=switch(base){case 'A'->'T';case 'C'->'G';case 'G'->'C';case 'T'->'A';default->'N';};
                int allele="ACGT".indexOf(base);if(allele>=0)counts[allele][(int)start+i]++;
            }
        }
        String expected=sample.getSequence(0,length);int covered=0,mismatches=0;
        for(int i=0;i<length;i++){
            int depth=counts[0][i]+counts[1][i]+counts[2][i]+counts[3][i];if(depth<minDepth)continue;
            covered++;int allele="ACGT".indexOf(expected.charAt(i));
            if(allele<0||counts[allele][i]*2<=depth)mismatches++;
        }
        return new Result(covered,mismatches,length,covered==length&&mismatches==0);
    }
}
