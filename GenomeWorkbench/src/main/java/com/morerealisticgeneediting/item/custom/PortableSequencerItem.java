package com.morerealisticgeneediting.item.custom;
import com.morerealisticgeneediting.genome.*;
import com.morerealisticgeneediting.genome.sequencing.SequencingSimulator;
import com.morerealisticgeneediting.item.*;
import com.morerealisticgeneediting.project.ServerProjectManager;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.world.World;
import java.util.*;
/** Bounded virtual nanopore-like read simulation; no instrument accuracy or gene-discovery claims. */
public class PortableSequencerItem extends Item {
    public PortableSequencerItem(Settings settings){super(settings.maxDamage(50));}
    @Override public TypedActionResult<ItemStack> use(World world,PlayerEntity user,Hand hand){
        ItemStack instrument=user.getStackInHand(hand);
        if(!(user instanceof ServerPlayerEntity player))return TypedActionResult.success(instrument,true);
        ItemStack sample=user.getStackInHand(hand==Hand.MAIN_HAND?Hand.OFF_HAND:Hand.MAIN_HAND);
        UUID id=GenomeSampleItem.getGenomeId(sample);Genome genome=id==null?null:GenomeRepository.get(player.getServer()).findOwned(id,player.getUuid()).orElse(null);
        if(genome==null||!GenomeSampleItem.matchesSnapshot(sample,genome)||genome.getTotalLength()<1||genome.getTotalLength()>100_000){player.sendMessage(Text.literal("Hold an owned virtual DNA sample matching the current revision (1–100,000 bases); stale aliquots cannot verify later edits."),true);return TypedActionResult.fail(instrument);}
        long seed=world.getRandom().nextLong();
        var result=new SequencingSimulator(seed).runSequencing(genome,SequencingSimulator.Technology.NANOPORE_R10,10,0,genome.getTotalLength());
        var check=SequenceVerification.verify(genome,result.reads(),10,3);
        ItemStack output=new ItemStack(ModItems.READ_SET_ITEM);NbtCompound n=GenomeSampleItem.readData(sample);
        n.putString("Type","simulated_read_set");n.putString("Technology","nanopore-like teaching preset");n.putLong("ReferenceRevision",genome.getRevision());n.putLong("ReadSeed",seed);n.putInt("ReadCount",result.stats().totalReads());n.putLong("TotalBases",result.stats().totalBases());n.putDouble("MeanQuality",result.stats().meanQuality());n.putDouble("Coverage",result.stats().coverageDepth());n.putInt("CoveredBases",check.coveredBases());n.putInt("ConcordanceMismatches",check.mismatches());n.putBoolean("SequenceConcordant",check.passed());n.putString("VariantAssessment","not performed");
        GenomeSampleItem.writeData(output,n);
        if(!player.getInventory().insertStack(output))player.dropItem(output,false);
        if(check.passed())ServerProjectManager.recordVerified(player,genome.getUUID(),genome.getRevision());
        instrument.damage(1,player,hand==Hand.MAIN_HAND?EquipmentSlot.MAINHAND:EquipmentSlot.OFFHAND);
        player.sendMessage(Text.literal("Simulated reads: "+result.stats().totalReads()+"; sequence concordance: "+(check.passed()?"passed":"insufficient/discordant evidence")),true);
        return TypedActionResult.success(instrument,false);
    }
    @Override public void appendTooltip(ItemStack stack,TooltipContext context,List<Text> tooltip,TooltipType type){tooltip.add(Text.literal("Virtual long-read simulator; use with an owned sample in the other hand.").formatted(Formatting.GRAY));tooltip.add(Text.literal("Reports measured simulated reads; does not discover arbitrary genes.").formatted(Formatting.DARK_GRAY));}
}
