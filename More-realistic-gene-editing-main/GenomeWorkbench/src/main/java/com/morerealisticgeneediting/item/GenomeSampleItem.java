package com.morerealisticgeneediting.item;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class GenomeSampleItem extends Item {

    private static final String GENOME_IDENTIFIER_KEY = "GenomeIdentifier";

    public GenomeSampleItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {
        // Prevent overwriting a sample
        if (getGenomeIdentifier(stack) != null) {
            return ActionResult.PASS;
        }

        // The interaction should only happen on the server side.
        if (!user.getWorld().isClient()) {
            // Get the official identifier for the entity type (e.g., "minecraft:pig")
            Identifier entityId = Registries.ENTITY_TYPE.getId(entity.getType());
            String genomeIdentifier = entityId.toString();

            // Store this identifier in the item's NBT
            setGenomeIdentifier(stack, genomeIdentifier);

            // Provide feedback to the player
            user.getWorld().playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 0.5F, 1.0F);
        }

        return ActionResult.SUCCESS;
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        // Make the item glow if it contains a sample
        return getGenomeIdentifier(stack) != null;
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        String genomeId = getGenomeIdentifier(stack);
        if (genomeId != null) {
            tooltip.add(Text.translatable("item.morerealisticgeneediting.genome_sample.tooltip_filled", genomeId).formatted(Formatting.GRAY));
        } else {
            tooltip.add(Text.translatable("item.morerealisticgeneediting.genome_sample.tooltip_empty").formatted(Formatting.GRAY));
        }
    }

    public static String getGenomeIdentifier(ItemStack stack) {
        if (stack.hasNbt() && stack.getNbt().contains(GENOME_IDENTIFIER_KEY)) {
            return stack.getNbt().getString(GENOME_IDENTIFIER_KEY);
        }
        return null;
    }

    public static void setGenomeIdentifier(ItemStack stack, String genomeIdentifier) {
        NbtCompound nbt = stack.getOrCreateNbt();
        nbt.putString(GENOME_IDENTIFIER_KEY, genomeIdentifier);
    }
}
