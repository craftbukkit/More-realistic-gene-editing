package com.morerealisticgeneediting.block;

import com.morerealisticgeneediting.MoreRealisticGeneEditing;
import com.morerealisticgeneediting.genome.Genome;
import com.morerealisticgeneediting.item.GenomeSampleItem;
import com.morerealisticgeneediting.screens.GenomeTerminalScreenHandler;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.UUID;

public class GenomeTerminalBlock extends Block {

    private static final Text TITLE = Text.of("Genome Terminal");

    public GenomeTerminalBlock(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        ItemStack stack = player.getStackInHand(hand);
        if (stack.getItem() instanceof GenomeSampleItem) {
            if (!world.isClient) {
                UUID genomeId = GenomeSampleItem.getGenomeId(stack);
                if (genomeId == null) {
                    // If the sample is new, create a corresponding Genome on the server
                    UUID playerUuid = player.getUuid();
                    Genome newGenome = Genome.createFromUnpackedSequence(playerUuid, ""); // Empty sequence initially
                    genomeId = newGenome.getUUID();
                    MoreRealisticGeneEditing.genomeCache.put(genomeId, newGenome);
                    GenomeSampleItem.setGenomeId(stack, genomeId);
                    player.sendMessage(Text.of("New genome sample initialized."), false);
                }

                // We need to ensure the server-side cache has the genome.
                // This is a fallback, the primary creation should happen when the item is created.
                if (!MoreRealisticGeneEditing.genomeCache.containsKey(genomeId)) {
                    MoreRealisticGeneEditing.LOGGER.warn("Genome with ID {} not found in cache. Re-initializing.", genomeId);
                    Genome newGenome = Genome.createFromUnpackedSequence(player.getUuid(), "");
                    MoreRealisticGeneEditing.genomeCache.put(genomeId, newGenome);
                }

                player.openHandledScreen(new ExtendedScreenHandlerFactory() {
                    @Override
                    public void writeScreenOpeningData(ServerPlayerEntity serverPlayerEntity, PacketByteBuf buf) {
                        buf.writeUuid(genomeId);
                    }

                    @Override
                    public Text getDisplayName() {
                        return TITLE;
                    }

                    @Override
                    public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) {
                        return new GenomeTerminalScreenHandler(syncId, inv, genomeId);
                    }
                });
            }
            return ActionResult.SUCCESS;
        } else {
            if (!world.isClient) {
                player.sendMessage(Text.of("You need to be holding a genome sample."), false);
            }
            return ActionResult.FAIL;
        }
    }
}
