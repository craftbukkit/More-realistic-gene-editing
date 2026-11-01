package com.morerealisticgeneediting.item;
import com.morerealisticgeneediting.block.GenomeTerminalBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.*;
import net.minecraft.world.World;
/** Opens an authenticated server menu; never references client classes from common code. */
public class GenomeWorkbenchItem extends Item {
    public GenomeWorkbenchItem(Settings settings){super(settings);}
    @Override public TypedActionResult<ItemStack> use(World world,PlayerEntity user,Hand hand){
        ItemStack stack=user.getStackInHand(hand);
        if(user instanceof ServerPlayerEntity player){
            ItemStack sample=user.getStackInHand(hand==Hand.MAIN_HAND?Hand.OFF_HAND:Hand.MAIN_HAND);
            if(!GenomeTerminalBlock.open(player,sample,player.getBlockPos(),true))return TypedActionResult.fail(stack);
        }
        return TypedActionResult.success(stack,world.isClient);
    }
}
