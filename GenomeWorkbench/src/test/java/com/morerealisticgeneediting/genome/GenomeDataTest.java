package com.morerealisticgeneediting.genome;
import com.morerealisticgeneediting.data.*;
import com.morerealisticgeneediting.genome.sequencing.SequencingSimulator.SequencingRead;
import net.minecraft.nbt.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class GenomeDataTest {
    private Genome sample(){return Genome.createFromUnpackedSequence(UUID.randomUUID(),"ACGTACGT");}
    @Test void packedBasesRejectNegativeIndicesAndInvalidSymbols(){
        for(int i=-3;i<0;i++){final int at=i;assertThrows(IndexOutOfBoundsException.class,()->TwoBitEncoding.getBase(new byte[]{0},at));}
        assertThrows(IllegalArgumentException.class,()->TwoBitEncoding.pack("ACNT"));
        assertThrows(IllegalArgumentException.class,()->TwoBitEncoding.pack(new byte[]{4}));
        assertThrows(IllegalArgumentException.class,()->TwoBitEncoding.unpack(new byte[1],5));
        assertEquals("ACGT",String.valueOf(TwoBitEncoding.getBaseAt(TwoBitEncoding.pack("ACGT"),0))+TwoBitEncoding.getBaseAt(TwoBitEncoding.pack("ACGT"),1)+TwoBitEncoding.getBaseAt(TwoBitEncoding.pack("ACGT"),2)+TwoBitEncoding.getBaseAt(TwoBitEncoding.pack("ACGT"),3));
    }
    @Test void insertionIsConsumedOnceAndAllSlicesConcatenate(){
        Genome g=sample().performInsertion(2,"TT").performDeletion(4,2);
        assertEquals("ACTTGTGT",g.getSequence());
        for(int split=0;split<=g.getTotalLength();split++)assertEquals(g.getSequence(),g.getSequence(0,split)+g.getSequence(split,(int)g.getTotalLength()-split));
        assertEquals(8,g.getTotalLength());
    }
    @Test void terminalInsertionAndEmptyReferenceAreWellDefined(){
        assertEquals("ACGTACGTTT",sample().performInsertion(8,"TT").getSequence());
        assertEquals("AC",Genome.createFromUnpackedSequence(UUID.randomUUID(),"").performInsertion(0,"AC").getSequence());
    }
    @Test void samePatchReplacementAndDefensiveSnapshot(){
        Genome g=sample();GenomePatch patch=new GenomePatch(g.getUUID());patch.addDeletion(2,2);patch.addInsertion(2,"GG");
        Genome changed=g.applyPatch(patch);patch.addInsertion(8,"A");
        assertEquals("ACGGACGT",changed.getSequence());assertEquals("ACGTACGT",g.getSequence());
    }
    @Test void overlappingOrOutOfBoundsEditsAreRejected(){
        Genome g=sample().performDeletion(2,3);
        assertThrows(IllegalArgumentException.class,()->g.performDeletion(1,4));
        assertThrows(IllegalArgumentException.class,()->g.performInsertion(3,"AA"));
        assertThrows(IllegalArgumentException.class,()->sample().performInsertion(9,"A"));
        assertThrows(IllegalArgumentException.class,()->sample().performDeletion(7,2));
        Genome once=sample().performInsertion(2,"A");assertThrows(IllegalArgumentException.class,()->once.performInsertion(2,"C"));
    }
    @Test void patchAndGenomeRoundTripsRetainAllEditsAndIdentity(){
        Genome g=sample();GenomePatch p=new GenomePatch(g.getUUID());p.addPointMutation(0,(byte)3);p.addInsertion(2,"TT");p.addDeletion(5,2);
        GenomePatch copy=GenomePatch.fromNbt(p.writeNbt());assertEquals(p.getPatchId(),copy.getPatchId());assertEquals(p.getInsertions(),copy.getInsertions());assertEquals(p.getDeletions(),copy.getDeletions());assertEquals(p.getPointMutations(),copy.getPointMutations());
        Genome changed=g.applyPatch(copy),loaded=Genome.fromNbt(changed.writeNbt());assertEquals(changed.getSequence(),loaded.getSequence());assertEquals(g.getUUID(),loaded.getUUID());assertEquals(g.getOwner(),loaded.getOwner());assertEquals(1,loaded.getRevision());assertEquals(g.getReferenceId(),loaded.getReferenceId());
    }
    @Test void repositoryRoundTripAndOwnerIsolation(){
        Genome a=sample().performInsertion(1,"TT");GenomeRepository store=new GenomeRepository();store.put(a);
        GenomeRepository loaded=GenomeRepository.fromNbt(store.writeNbt(new NbtCompound(),null),null);
        assertEquals(a.getSequence(),loaded.findOwned(a.getUUID(),a.getOwner()).orElseThrow().getSequence());
        assertTrue(loaded.findOwned(a.getUUID(),UUID.randomUUID()).isEmpty());
        assertThrows(IllegalArgumentException.class,()->loaded.put(Genome.fromNbt(withOwner(a.writeNbt(),UUID.randomUUID()))));
    }
    private NbtCompound withOwner(NbtCompound n,UUID owner){n.putUuid("owner",owner);return n;}
    @Test void malformedSavedLengthCannotCreatePaddingBases(){
        NbtCompound n=sample().writeNbt();n.putInt("baseLength",100);assertThrows(IllegalArgumentException.class,()->Genome.fromNbt(n));
    }
    @Test void malformedSavedEditsCannotSilentlyBecomePositionZeroOrAdenine(){
        Genome g=sample();GenomePatch patch=new GenomePatch(g.getUUID());patch.addPointMutation(3,(byte)2);
        NbtCompound saved=patch.writeNbt();saved.getList("points",NbtElement.COMPOUND_TYPE).getCompound(0).remove("position");
        assertThrows(IllegalArgumentException.class,()->GenomePatch.fromNbt(saved));
        NbtCompound missingBase=patch.writeNbt();missingBase.getList("points",NbtElement.COMPOUND_TYPE).getCompound(0).remove("base");
        assertThrows(IllegalArgumentException.class,()->GenomePatch.fromNbt(missingBase));
        NbtCompound wrongList=patch.writeNbt();NbtList strings=new NbtList();strings.add(NbtString.of("broken"));wrongList.put("points",strings);
        assertThrows(IllegalArgumentException.class,()->GenomePatch.fromNbt(wrongList));
        NbtCompound missingReference=g.writeNbt();missingReference.remove("packedBases");
        assertThrows(IllegalArgumentException.class,()->Genome.fromNbt(missingReference));
        assertThrows(IllegalArgumentException.class,()->GenomeRepository.fromNbt(new NbtCompound(),null));
    }
    @Test void deliveryHistoryRetainsRevisionAndRejectsStaleSnapshots(){
        Genome initial=sample(),edited=initial.performInsertion(2,"TT");GenomeRepository repository=new GenomeRepository();repository.put(initial);repository.put(edited);
        assertThrows(IllegalArgumentException.class,()->repository.recordDelivery(UUID.randomUUID(),initial));
        UUID entity=UUID.randomUUID();repository.recordDelivery(entity,edited);
        GenomeRepository loaded=GenomeRepository.fromNbt(repository.writeNbt(new NbtCompound(),null),null);
        NbtCompound record=loaded.writeNbt(new NbtCompound(),null).getList("deliveries",NbtElement.COMPOUND_TYPE).getCompound(0);
        assertEquals(entity,record.getUuid("entity"));assertEquals(edited.getUUID(),record.getUuid("sample"));assertEquals(edited.getRevision(),record.getLong("revision"));
    }
    @Test void editedCoordinatesCannotPretendInsertedBasesHaveAReferencePosition(){
        Genome g=sample().performInsertion(2,"TT");assertEquals(2,g.editedToReference(4));
        assertThrows(IllegalArgumentException.class,()->g.editedToReference(2));assertThrows(IllegalArgumentException.class,()->g.editedToReference(3));
    }
    @Test void frameshiftRequiresExplicitCodingContext(){
        EditOutcome deletion=new EditOutcome(EditOutcome.Type.DELETION,1,1,"","toy");assertFalse(deletion.causesFrameshift());assertTrue(deletion.causesFrameshift(true));assertFalse(deletion.causesFrameshift(false));
    }
    @Test void motifBoundariesAndInvalidInput(){
        Genome g=sample();assertEquals(List.of(0,4),MotifSearch.search(new GenomeSlice(g,g.getSequence(),0),"ACGT"));
        assertThrows(IllegalArgumentException.class,()->MotifSearch.search(new GenomeSlice(g,g.getSequence(),0),""));
    }
    @Test void concordanceUsesActualBothStrandEvidenceAndRejectsUncoveredOrWrongSequence(){
        Genome g=Genome.createFromUnpackedSequence(UUID.randomUUID(),"AAAACCCG");int[] q={40,40,40,40,40,40,40,40};
        SequencingRead reverse=new SequencingRead("reverse","CGGGTTTT",q,0,true,null);
        assertTrue(SequenceVerification.verify(g,List.of(reverse,reverse,reverse),20,3).passed());
        SequencingRead wrong=new SequencingRead("wrong","CCCCGGGT",q,0,false,null);
        assertFalse(SequenceVerification.verify(g,List.of(wrong,wrong,wrong),20,3).passed());
        assertFalse(SequenceVerification.verify(g,List.of(reverse),20,3).passed());
    }
    @Test void cacheKeyMustMatchGenomeAndLruAccessIsSafe(){
        Genome a=sample(),b=sample(),c=sample();GenomeCache cache=new GenomeCache(2);cache.put(a.getUUID(),a);cache.put(b.getUUID(),b);assertSame(a,cache.get(a.getUUID()));cache.put(c.getUUID(),c);assertNull(cache.get(b.getUUID()));
        assertThrows(IllegalArgumentException.class,()->cache.put(UUID.randomUUID(),a));
    }
}
