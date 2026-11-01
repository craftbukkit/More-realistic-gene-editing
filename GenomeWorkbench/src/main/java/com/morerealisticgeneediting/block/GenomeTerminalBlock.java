package com.morerealisticgeneediting.block;
import com.mojang.serialization.MapCodec;
import com.morerealisticgeneediting.genome.*;
import com.morerealisticgeneediting.item.GenomeSampleItem;
import com.morerealisticgeneediting.screens.*;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.*;
import net.minecraft.entity.player.*;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import java.util.UUID;
public class GenomeTerminalBlock extends Block {
    public static final MapCodec<GenomeTerminalBlock> CODEC=createCodec(GenomeTerminalBlock::new);
    public GenomeTerminalBlock(Settings settings){super(settings);}
    @Override protected MapCodec<GenomeTerminalBlock> getCodec(){return CODEC;}
    @Override protected ActionResult onUse(BlockState state,World world,BlockPos pos,PlayerEntity user,BlockHitResult hit){
        if(!(user instanceof ServerPlayerEntity player))return ActionResult.SUCCESS;
        ItemStack sample=GenomeSampleItem.getGenomeId(player.getMainHandStack())!=null?player.getMainHandStack():player.getOffHandStack();
        return open(player,sample,pos,false)?ActionResult.SUCCESS:ActionResult.FAIL;
    }
    public static boolean open(ServerPlayerEntity player,ItemStack sample,BlockPos pos,boolean portable){
        UUID id=GenomeSampleItem.getGenomeId(sample);Genome genome=id==null?null:GenomeRepository.get(player.getServer()).findOwned(id,player.getUuid()).orElse(null);
        if(genome==null){player.sendMessage(Text.literal("Hold an owned sample with saved genome data. Missing samples are not regenerated."),true);return false;}
        TerminalOpenData data=new TerminalOpenData(id,pos,genome.getTotalLength(),genome.getRevision(),portable);
        player.openHandledScreen(new ExtendedScreenHandlerFactory<TerminalOpenData>(){
            @Override public TerminalOpenData getScreenOpeningData(ServerPlayerEntity p){return data;}
            @Override public Text getDisplayName(){return Text.literal("Genome terminal — virtual sample");}
            @Override public ScreenHandler createMenu(int syncId,PlayerInventory inventory,PlayerEntity p){return new GenomeTerminalScreenHandler(syncId,inventory,data);}
        });return true;
    }
}
