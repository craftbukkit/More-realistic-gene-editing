package com.morerealisticgeneediting.item.custom;

import com.morerealisticgeneediting.item.ModItems;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;

public class GeneSyringe extends Item {
    public GeneSyringe(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {
        if (!user.world.isClient) {
            // Check if the syringe contains a recombinant plasmid
            // For now, we'll just check if the player is holding one in their offhand
            ItemStack offhandStack = user.getOffHandStack();
            if (offhandStack.getItem() == ModItems.RECOMBINANT_PLASMID) {
                // TODO: Read NBT data from the plasmid and apply the genetic modification

                // For now, as a placeholder, we'll just change the entity's name
                NbtCompound plasmidNbt = offhandStack.getNbt();
                if (plasmidNbt != null && plasmidNbt.contains("gene")) {
                    String gene = plasmidNbt.getString("gene");
                    entity.setCustomName(Text.of("Modified " + entity.getName().getString() + " with " + gene));
                    entity.setCustomNameVisible(true);

                    // Consume the plasmid and damage the syringe
                    offhandStack.decrement(1);
                    stack.damage(1, user, (p) -> p.sendToolBreakStatus(hand));

                    return ActionResult.SUCCESS;
                }
            }
        }
        return ActionResult.PASS;
    }
}
