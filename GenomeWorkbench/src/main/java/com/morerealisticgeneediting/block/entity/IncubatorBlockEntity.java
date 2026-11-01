package com.morerealisticgeneediting.block.entity;
import com.morerealisticgeneediting.item.ModItems;
import com.morerealisticgeneediting.screen.IncubatorScreenHandler;
import com.morerealisticgeneediting.util.StackData;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.*;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
public class IncubatorBlockEntity extends TimedMachineBlockEntity {
    public IncubatorBlockEntity(BlockPos pos, BlockState state) { super(ModBlockEntities.INCUBATOR,pos,state,4,3); }
    @Override protected boolean acceptsInput(int slot,ItemStack stack) { return switch(slot) { case 0 -> stack.isOf(ModItems.AMPLIFIED_GENE_FRAGMENT) && StackData.has(stack); case 1 -> stack.isOf(ModItems.PLASMID_VECTOR); case 2 -> stack.isOf(ModItems.DNA_LIGASE); default -> false; }; }
    public void startIncubation(PlayerEntity player) { startJob(player,derived(ModItems.RECOMBINANT_PLASMID,getStack(0),"abstract_ligation"),200,0,1,2); }
    public static void tick(World world,BlockPos pos,BlockState state,IncubatorBlockEntity entity) { entity.tickJob(); }
    @Override public Text getDisplayName() { return Text.translatable("block.morerealisticgeneediting.incubator"); }
    @Override public ScreenHandler createMenu(int syncId,PlayerInventory inv,PlayerEntity player) { return new IncubatorScreenHandler(syncId,inv,this,propertyDelegate); }
}
