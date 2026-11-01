package com.morerealisticgeneediting.block.entity;

import com.morerealisticgeneediting.equipment.EquipmentTier;
import com.morerealisticgeneediting.util.StackData;
import com.morerealisticgeneediting.workflow.LabWorkflowManager;
import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class InventorySafetyTest {
    @BeforeAll static void bootstrap(){SharedConstants.createGameVersion();Bootstrap.initialize();}
    @Test void outputMergeNeverOverwritesDifferentItemOrSampleIdentity(){
        ItemStack current=new ItemStack(Items.PAPER,1),other=new ItemStack(Items.PAPER,1);
        NbtCompound a=new NbtCompound();a.putString("SampleId","first");StackData.write(current,a);
        NbtCompound b=new NbtCompound();b.putString("SampleId","second");StackData.write(other,b);
        assertFalse(TimedMachineBlockEntity.canMerge(current,other));
        assertFalse(TimedMachineBlockEntity.canMerge(current,new ItemStack(Items.STONE)));
        assertTrue(TimedMachineBlockEntity.canMerge(current,current.copy()));
    }
    @Test void outputMergeRespectsCapacity(){
        assertFalse(TimedMachineBlockEntity.canMerge(new ItemStack(Items.PAPER,64),new ItemStack(Items.PAPER)));
        assertFalse(TimedMachineBlockEntity.canMerge(ItemStack.EMPTY,new ItemStack(Items.PAPER,65)));
        assertTrue(TimedMachineBlockEntity.canMerge(new ItemStack(Items.PAPER,63),new ItemStack(Items.PAPER)));
        assertFalse(TimedMachineBlockEntity.canMerge(ItemStack.EMPTY,ItemStack.EMPTY));
    }
    @Test void componentSnapshotRequiresExplicitWriteback(){
        ItemStack sample=new ItemStack(Items.PAPER);NbtCompound data=new NbtCompound();data.putInt("Revision",1);StackData.write(sample,data);
        NbtCompound copy=StackData.read(sample);copy.putInt("Revision",2);
        assertEquals(1,StackData.read(sample).getInt("Revision"));
        StackData.write(sample,copy);assertEquals(2,StackData.read(sample).getInt("Revision"));
    }
    @Test void duplicateRecipeRequirementsCannotReuseOneProvidedStack(){
        var recipe=new LabWorkflowManager.LabRecipe("test","test",LabWorkflowManager.WorkflowStage.FRACTIONATION,
                List.of(new ItemStack(Items.PAPER,2),new ItemStack(Items.PAPER,2)),List.of(),20,EquipmentTier.BASIC,1,"test");
        assertFalse(recipe.matches(List.of(new ItemStack(Items.PAPER,3))));
        assertTrue(recipe.matches(List.of(new ItemStack(Items.PAPER,4))));
        assertTrue(recipe.matches(List.of(new ItemStack(Items.PAPER,1),new ItemStack(Items.PAPER,3))));
    }
}
