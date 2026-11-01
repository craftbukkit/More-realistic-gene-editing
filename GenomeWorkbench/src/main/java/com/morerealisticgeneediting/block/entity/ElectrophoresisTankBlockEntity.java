package com.morerealisticgeneediting.block.entity;
import com.morerealisticgeneediting.item.ModItems;
import com.morerealisticgeneediting.screen.ElectrophoresisTankScreenHandler;
import com.morerealisticgeneediting.util.StackData;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.*;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
public class ElectrophoresisTankBlockEntity extends TimedMachineBlockEntity {
    public ElectrophoresisTankBlockEntity(BlockPos pos, BlockState state) { super(ModBlockEntities.ELECTROPHORESIS_TANK,pos,state,2,1); }
    @Override protected boolean acceptsInput(int slot,ItemStack stack) { return slot==0 && stack.isOf(ModItems.RECOMBINANT_PLASMID) && StackData.has(stack); }
    public void startElectrophoresis(PlayerEntity player) { startJob(player,derived(ModItems.PURIFIED_PLASMID,getStack(0),"abstract_gel_separation"),80,0); }
    public static void tick(World world,BlockPos pos,BlockState state,ElectrophoresisTankBlockEntity entity) { entity.tickJob(); }
    @Override public Text getDisplayName() { return Text.translatable("block.morerealisticgeneediting.electrophoresis_tank"); }
    @Override public ScreenHandler createMenu(int syncId,PlayerInventory inv,PlayerEntity player) { return new ElectrophoresisTankScreenHandler(syncId,inv,this,propertyDelegate); }
}
