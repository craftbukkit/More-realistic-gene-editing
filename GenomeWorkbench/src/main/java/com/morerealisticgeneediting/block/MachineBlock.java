package com.morerealisticgeneediting.block;
import com.morerealisticgeneediting.block.entity.*;
import net.minecraft.block.*;
import net.minecraft.block.entity.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import java.util.function.Supplier;
/** Common interactions and server-only ticks for the core processing machines. */
public abstract class MachineBlock extends BlockWithEntity {
 private final Supplier<? extends BlockEntityType<?>> type;
 protected MachineBlock(Settings settings,Supplier<? extends BlockEntityType<?>> type) {super(settings);this.type=type;}
 @Override public BlockRenderType getRenderType(BlockState state) {return BlockRenderType.MODEL;}
 @Override public BlockEntity createBlockEntity(BlockPos pos,BlockState state) {return type.get().instantiate(pos,state);}
 @Override protected ActionResult onUse(BlockState state,World world,BlockPos pos,PlayerEntity player,BlockHitResult hit) {
  if(!world.isClient && world.getBlockEntity(pos) instanceof NamedScreenHandlerFactory factory) player.openHandledScreen(factory);
  return ActionResult.success(world.isClient);
 }
 @Override protected void onStateReplaced(BlockState state,World world,BlockPos pos,BlockState next,boolean moved) {
  if(!state.isOf(next.getBlock())) {
   if(world.getBlockEntity(pos) instanceof Inventory inventory) ItemScatterer.spawn(world,pos,inventory);
   world.updateComparators(pos,this);
  }
  super.onStateReplaced(state,world,pos,next,moved);
 }
 @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world,BlockState state,BlockEntityType<T> actual) {
  if(world.isClient || actual!=type.get())return null;
  return (w,p,s,e)-> {
   if(e instanceof CellProcessorBlockEntity x)CellProcessorBlockEntity.tick(w,p,s,x);
   else if(e instanceof SequencerBlockEntity x)SequencerBlockEntity.tick(w,p,s,x);
   else if(e instanceof PcrMachineBlockEntity x)PcrMachineBlockEntity.tick(w,p,s,x);
   else if(e instanceof IncubatorBlockEntity x)IncubatorBlockEntity.tick(w,p,s,x);
   else if(e instanceof ElectrophoresisTankBlockEntity x)ElectrophoresisTankBlockEntity.tick(w,p,s,x);
  };
 }
}
