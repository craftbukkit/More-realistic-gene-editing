package com.morerealisticgeneediting.item.custom;
import com.morerealisticgeneediting.genome.*;
import com.morerealisticgeneediting.item.*;
import net.minecraft.entity.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.*;
/** Records a game delivery event. Delivery is not evidence of editing, expression or phenotype. */
public class GeneSyringe extends Item {
    public GeneSyringe(Settings settings){super(settings);}
    @Override public ActionResult useOnEntity(ItemStack stack,PlayerEntity user,LivingEntity entity,Hand hand){
        if(!(user instanceof ServerPlayerEntity player))return ActionResult.SUCCESS;
        ItemStack plasmid=user.getStackInHand(hand==Hand.MAIN_HAND?Hand.OFF_HAND:Hand.MAIN_HAND);
        if(!plasmid.isOf(ModItems.RECOMBINANT_PLASMID))return ActionResult.PASS;
        java.util.UUID id=GenomeSampleItem.getGenomeId(plasmid);GenomeRepository store=GenomeRepository.get(player.getServer());
        Genome genome=id==null?null:store.findOwned(id,player.getUuid()).orElse(null);
        if(genome==null||genome.getRevision()<1){player.sendMessage(Text.literal("The construct must reference an owned, edited virtual sample."),true);return ActionResult.FAIL;}
        try{store.recordDelivery(entity.getUuid(),genome);}catch(IllegalStateException failure){player.sendMessage(Text.literal(failure.getMessage()),true);return ActionResult.FAIL;}
        plasmid.decrement(1);stack.damage(1,player,hand==Hand.MAIN_HAND?EquipmentSlot.MAINHAND:EquipmentSlot.OFFHAND);
        player.sendMessage(Text.literal("Virtual delivery recorded. Expression and phenotype remain unverified."),true);
        return ActionResult.SUCCESS;
    }
}
