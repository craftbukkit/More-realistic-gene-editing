package com.morerealisticgeneediting.block.entity;
import com.morerealisticgeneediting.item.ModItems;
import com.morerealisticgeneediting.screen.PcrMachineScreenHandler;
import com.morerealisticgeneediting.util.StackData;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.*;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
public class PcrMachineBlockEntity extends TimedMachineBlockEntity {
    public PcrMachineBlockEntity(BlockPos pos, BlockState state) { super(ModBlockEntities.PCR_MACHINE,pos,state,2,1); }
    @Override protected boolean acceptsInput(int slot,ItemStack stack) { return slot==0 && (stack.isOf(ModItems.GENOME_SAMPLE) || stack.isOf(ModItems.DNA_SAMPLE)) && StackData.has(stack); }
    public void startPcr(PlayerEntity player) { startJob(player,derived(ModItems.AMPLIFIED_GENE_FRAGMENT,getStack(0),"abstract_pcr"),60,0); }
    public static void tick(World world,BlockPos pos,BlockState state,PcrMachineBlockEntity entity) { entity.tickJob(); }
    @Override public Text getDisplayName() { return Text.translatable("block.morerealisticgeneediting.pcr_machine"); }
    @Override public ScreenHandler createMenu(int syncId,PlayerInventory inv,PlayerEntity player) { return new PcrMachineScreenHandler(syncId,inv,this,propertyDelegate); }
}
