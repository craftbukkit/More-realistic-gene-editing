package com.morerealisticgeneediting.block.entity;
import com.morerealisticgeneediting.recipe.CellProcessorRecipe;
import com.morerealisticgeneediting.screen.CellProcessorScreenHandler;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.*;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
public class CellProcessorBlockEntity extends TimedMachineBlockEntity {
    public CellProcessorBlockEntity(BlockPos pos,BlockState state) { super(ModBlockEntities.CELL_PROCESSOR_BLOCK_ENTITY,pos,state,4,2); }
    @Override protected boolean acceptsInput(int slot,ItemStack stack) { return slot>=0 && slot<2 && !stack.isEmpty(); }
    public void startProcessing(PlayerEntity player) {
        if(world==null || world.isClient || !canPlayerUse(player))return;
        var input=new CellProcessorRecipe.Input(getStack(0),getStack(1));
        world.getRecipeManager().getFirstMatch(CellProcessorRecipe.Type.INSTANCE,input,world).ifPresent(recipe -> startJob(player,recipe.value().craft(input,world.getRegistryManager()),50,0,1));
    }
    public static void tick(World world,BlockPos pos,BlockState state,CellProcessorBlockEntity entity) { entity.tickJob(); }
    @Override public Text getDisplayName() { return Text.translatable("block.morerealisticgeneediting.cell_processor"); }
    @Override public ScreenHandler createMenu(int syncId,PlayerInventory inv,PlayerEntity player) { return new CellProcessorScreenHandler(syncId,inv,this,propertyDelegate); }
}
