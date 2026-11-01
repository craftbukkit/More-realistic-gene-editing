package com.morerealisticgeneediting.screens;
import com.morerealisticgeneediting.block.GenomeTerminalBlock;
import com.morerealisticgeneediting.genome.GenomeRepository;
import com.morerealisticgeneediting.item.GenomeSampleItem;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import java.util.UUID;
public abstract class GenomeSessionScreenHandler extends ScreenHandler {
    public final UUID genomeId;
    public final BlockPos terminalPos;
    public final long totalLength;
    public final long revision;
    public final boolean portable;
    protected GenomeSessionScreenHandler(ScreenHandlerType<?> type,int syncId,UUID id,BlockPos pos,long length,long revision,boolean portable) {
        super(type,syncId); this.genomeId=id; this.terminalPos=pos.toImmutable();this.totalLength=length;this.revision=revision;this.portable=portable;
    }
    public boolean holdsSample(PlayerEntity player) {
        return genomeId.equals(GenomeSampleItem.getGenomeId(player.getMainHandStack())) || genomeId.equals(GenomeSampleItem.getGenomeId(player.getOffHandStack()));
    }
    @Override public boolean canUse(PlayerEntity player) {
        if (player.getWorld().isClient) return true;
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return false;
        return player.squaredDistanceTo(terminalPos.getX()+0.5,terminalPos.getY()+0.5,terminalPos.getZ()+0.5)<=64
            && (portable ? player.getMainHandStack().isOf(com.morerealisticgeneediting.item.ModItems.GENOME_WORKBENCH) || player.getOffHandStack().isOf(com.morerealisticgeneediting.item.ModItems.GENOME_WORKBENCH) : player.getWorld().getBlockState(terminalPos).getBlock() instanceof GenomeTerminalBlock)
            && holdsSample(player) && GenomeRepository.get(serverPlayer.getServer()).findOwned(genomeId,player.getUuid()).isPresent();
    }
    @Override public ItemStack quickMove(PlayerEntity player,int slot) { return ItemStack.EMPTY; }
}
