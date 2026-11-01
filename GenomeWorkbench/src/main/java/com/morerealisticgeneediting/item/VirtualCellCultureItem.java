package com.morerealisticgeneediting.item;

import com.morerealisticgeneediting.genome.SpeciesGenomeMapping;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import java.util.List;
import java.util.UUID;

/** Explicit fictional cell material, distinct from the virtual purified DNA aliquot. */
public final class VirtualCellCultureItem extends Item {
    public VirtualCellCultureItem(Settings settings) { super(settings); }

    @Override public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {
        if (GenomeSampleItem.readData(stack).containsUuid("CellCultureId")) return ActionResult.PASS;
        if (!(user instanceof ServerPlayerEntity player)) return ActionResult.SUCCESS;
        String species = Registries.ENTITY_TYPE.getId(entity.getType()).toString();
        NbtCompound data = new NbtCompound();
        data.putUuid("CellCultureId", UUID.randomUUID());
        data.putUuid("Owner", player.getUuid());
        data.putUuid("SourceEntity", entity.getUuid());
        data.putString("Species", species);
        data.putString("SampleType", "virtual_cell_culture");
        data.putString("CellType", SpeciesGenomeMapping.cellType(species));
        data.putBoolean("Simulation", true);
        data.putString("SimulationModel", "fictional-cell-culture-v1");
        // Initial conditions of a game model, not measured biological counts or viability.
        data.putFloat("CellDensity", 100_000);
        data.putFloat("Viability", 1);
        data.putString("MeasurementStatus", "model_initial_conditions");
        ItemStack filled = stack.copyWithCount(1);
        GenomeSampleItem.writeData(filled, data);
        stack.decrement(1);
        if (stack.isEmpty()) player.setStackInHand(hand, filled);
        else if (!player.getInventory().insertStack(filled)) player.dropItem(filled, false);
        player.sendMessage(Text.literal("Virtual cell culture collected; growth and delivery are game simulations."), true);
        return ActionResult.SUCCESS;
    }

    @Override public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        NbtCompound data = GenomeSampleItem.readData(stack);
        tooltip.add(Text.literal(data.containsUuid("CellCultureId")
            ? "Virtual cell culture: " + data.getString("Species")
            : "Use on an entity to collect fictional cells for teaching")
            .formatted(Formatting.GRAY));
        tooltip.add(Text.literal("Not a DNA aliquot; no measured viability or editing result")
            .formatted(Formatting.DARK_GRAY));
    }
}
