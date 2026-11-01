package com.morerealisticgeneediting.util;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
public final class NetGuards {
    public static boolean isPlayerClose(PlayerEntity player, World world, BlockPos pos) {
        return player!=null && world!=null && pos!=null && player.isAlive() && player.getWorld()==world
            && world.isChunkLoaded(pos) && player.squaredDistanceTo(pos.getX()+0.5,pos.getY()+0.5,pos.getZ()+0.5)<=64;
    }
}
