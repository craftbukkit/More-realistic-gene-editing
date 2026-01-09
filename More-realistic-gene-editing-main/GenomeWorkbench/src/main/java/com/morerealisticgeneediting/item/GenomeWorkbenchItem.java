package com.morerealisticgeneediting.item;

import com.morerealisticgeneediting.client.gui.screen.GenomeWorkbenchScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public class GenomeWorkbenchItem extends Item {
    public GenomeWorkbenchItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        if (world.isClient) {
            MinecraftClient.getInstance().setScreen(new GenomeWorkbenchScreen());
        }
        return TypedActionResult.success(user.getStackInHand(hand));
    }
}
