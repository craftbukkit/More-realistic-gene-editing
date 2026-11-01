package com.morerealisticgeneediting.item;
import com.morerealisticgeneediting.genome.*;
import com.morerealisticgeneediting.project.ServerProjectManager;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import java.util.*;

/** Sample identity points to authoritative per-world data; the item is not a genome database. */
public class GenomeSampleItem extends Item {
    public GenomeSampleItem(Settings settings){super(settings);}
    @Override public ActionResult useOnEntity(ItemStack stack,PlayerEntity user,LivingEntity entity,Hand hand){
        if(getGenomeId(stack)!=null)return ActionResult.PASS;
        if(!(user instanceof ServerPlayerEntity player))return ActionResult.SUCCESS;
        String species=Registries.ENTITY_TYPE.getId(entity.getType()).toString();
        Genome genome=Genome.createFromUnpackedSequence(player.getUuid(),SpeciesGenomeMapping.sequence(species),SpeciesGenomeMapping.referenceId(species));
        try{GenomeRepository.get(player.getServer()).put(genome);}catch(IllegalStateException failure){player.sendMessage(Text.literal(failure.getMessage()),true);return ActionResult.FAIL;}
        NbtCompound n=readData(stack);n.putUuid("GenomeId",genome.getUUID());n.putUuid("SampleId",genome.getUUID());n.putString("GenomeIdentifier",genome.getUUID().toString());
        n.putUuid("Owner",player.getUuid());n.putUuid("SourceEntity",entity.getUuid());n.putString("Species",species);n.putString("ReferenceId",genome.getReferenceId());n.putLong("Revision",0);
        n.putBoolean("Simulation",true);n.putBoolean("SimulationPhysicalState",true);n.putString("SimulationModel","virtual-aliquot-v1");n.putString("SampleType","virtual_dna_aliquot");n.putString("CellType",SpeciesGenomeMapping.cellType(species));n.putLong("GenomeLength",genome.getTotalLength());
        // Fixed fictional aliquot state, assigned once, never refreshed by observation.
        Random physical=new Random(entity.getUuid().getMostSignificantBits()^entity.getUuid().getLeastSignificantBits());
        double concentration=20+physical.nextInt(61),a260=concentration/50.0;
        n.putDouble("ConcentrationNgPerUl",concentration);n.putDouble("Absorbance260",a260);n.putDouble("Absorbance280",a260/1.8);n.putDouble("Absorbance230",a260/2.0);n.putDouble("VisibleAbsorbance",0.1+physical.nextDouble()*0.1);
        writeData(stack,n);ServerProjectManager.recordSampled(player,genome.getUUID(),genome.getRevision());
        player.sendMessage(Text.literal("Virtual teaching sample captured: "+species),true);return ActionResult.SUCCESS;
    }
    @Override public boolean hasGlint(ItemStack stack){return getGenomeId(stack)!=null;}
    @Override public void appendTooltip(ItemStack stack,TooltipContext context,List<Text> tooltip,TooltipType type){
        UUID id=getGenomeId(stack);tooltip.add(Text.literal(id==null?"Use on an entity to collect a virtual teaching sample":"Virtual sample: "+id).formatted(Formatting.GRAY));
        if(id!=null)tooltip.add(Text.literal(readData(stack).getString("ReferenceId")).formatted(Formatting.DARK_GRAY));
    }
    public static NbtCompound readData(ItemStack stack){return stack.getOrDefault(DataComponentTypes.CUSTOM_DATA,NbtComponent.DEFAULT).copyNbt();}
    public static void writeData(ItemStack stack,NbtCompound nbt){stack.set(DataComponentTypes.CUSTOM_DATA,NbtComponent.of(nbt));}
    public static UUID getGenomeId(ItemStack stack){
        NbtCompound n=readData(stack);if(n.containsUuid("GenomeId"))return n.getUuid("GenomeId");
        try{return UUID.fromString(n.getString("GenomeIdentifier"));}catch(IllegalArgumentException ex){return null;}
    }
    public static void setGenomeId(ItemStack stack,UUID id){NbtCompound n=readData(stack);n.putUuid("GenomeId",id);n.putString("GenomeIdentifier",id.toString());writeData(stack,n);}
    public static String getGenomeIdentifier(ItemStack stack){UUID id=getGenomeId(stack);return id==null?null:id.toString();}
    public static void setGenomeIdentifier(ItemStack stack,String identifier){setGenomeId(stack,UUID.fromString(identifier));}
}
