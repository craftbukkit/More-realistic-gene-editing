package com.morerealisticgeneediting.network.c2s;

import com.morerealisticgeneediting.data.EditOutcome;
import com.morerealisticgeneediting.data.GenomePatch;
import com.morerealisticgeneediting.genome.Genome;
import com.morerealisticgeneediting.genome.GenomeRepository;
import com.morerealisticgeneediting.genome.crispr.CrisprEngine;
import com.morerealisticgeneediting.item.ModItems;
import com.morerealisticgeneediting.item.GenomeSampleItem;
import com.morerealisticgeneediting.network.s2c.*;
import com.morerealisticgeneediting.project.ServerProjectManager;
import com.morerealisticgeneediting.screens.*;
import com.morerealisticgeneediting.security.GenomeRequestPolicy;
import com.morerealisticgeneediting.security.RateLimiters;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import java.util.*;

/** Versioned, bounded, server-authoritative lab requests. No remote service is queried on the tick thread. */
public final class C2SPackets {
    private C2SPackets() {}
    private static boolean registered;
    public static void register() {
        if (registered) return;
        registered=true;
        PayloadTypeRegistry.playC2S().register(C2SRequestGenomeSlicePacket.ID,C2SRequestGenomeSlicePacket.CODEC);
        PayloadTypeRegistry.playC2S().register(C2SRequestMotifSearchPacket.ID,C2SRequestMotifSearchPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(C2SOpenGeneInsertionScreenPacket.ID,C2SOpenGeneInsertionScreenPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(C2SPerformGeneInsertionPacket.ID,C2SPerformGeneInsertionPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(C2SPerformGeneKnockoutPacket.ID,C2SPerformGeneKnockoutPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(C2SStartProjectPacket.ID,C2SStartProjectPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(C2SCompleteProjectPacket.ID,C2SCompleteProjectPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(S2CSendGenomeSlicePacket.ID,S2CSendGenomeSlicePacket.CODEC);
        PayloadTypeRegistry.playS2C().register(S2CSendMotifSearchResultsPacket.ID,S2CSendMotifSearchResultsPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(S2CPlayerProjectStatePacket.ID,S2CPlayerProjectStatePacket.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(C2SRequestGenomeSlicePacket.ID,(p,c)->c.server().execute(()->slice(c.player(),p)));
        ServerPlayNetworking.registerGlobalReceiver(C2SRequestMotifSearchPacket.ID,(p,c)->c.server().execute(()->motif(c.player(),p)));
        ServerPlayNetworking.registerGlobalReceiver(C2SOpenGeneInsertionScreenPacket.ID,(p,c)->c.server().execute(()->openInsertion(c.player(),p)));
        ServerPlayNetworking.registerGlobalReceiver(C2SPerformGeneInsertionPacket.ID,(p,c)->c.server().execute(()->edit(c.player(),p.genomeId(),p.syncId(),p.revision(),p.pamPosition(),p.sequence(),true)));
        ServerPlayNetworking.registerGlobalReceiver(C2SPerformGeneKnockoutPacket.ID,(p,c)->c.server().execute(()->edit(c.player(),p.genomeId(),p.syncId(),p.revision(),p.pamPosition(),null,false)));
        ServerPlayNetworking.registerGlobalReceiver(C2SStartProjectPacket.ID,(p,c)->c.server().execute(()->{if(RateLimiters.PROJECT_OPS.tryAcquire(c.player().getUuid()))ServerProjectManager.startProject(c.player(),p.projectId());}));
        ServerPlayNetworking.registerGlobalReceiver(C2SCompleteProjectPacket.ID,(p,c)->c.server().execute(()->{if(RateLimiters.PROJECT_OPS.tryAcquire(c.player().getUuid()))ServerProjectManager.completeProject(c.player(),p.projectId());}));
        ServerPlayConnectionEvents.DISCONNECT.register((handler,server)->resetLimits(handler.player.getUuid()));
        ServerLifecycleEvents.SERVER_STOPPED.register(server->{RateLimiters.GENOME_SLICE.clear();RateLimiters.MOTIF_SEARCH.clear();RateLimiters.GENE_EDITING.clear();RateLimiters.PROJECT_OPS.clear();});
    }
    private static void resetLimits(UUID player){RateLimiters.GENOME_SLICE.reset(player);RateLimiters.MOTIF_SEARCH.reset(player);RateLimiters.GENE_EDITING.reset(player);RateLimiters.PROJECT_OPS.reset(player);}
    private static Genome authorize(ServerPlayerEntity player,UUID id,int syncId,long revision) {
        if (!(player.currentScreenHandler instanceof GenomeSessionScreenHandler menu) || !menu.canUse(player)
            || !GenomeRequestPolicy.matches(id,menu.genomeId,syncId,menu.syncId,revision,menu.revision)) return null;
        return GenomeRepository.get(player.getServer()).findOwned(id,player.getUuid()).filter(g->g.getRevision()==revision).orElse(null);
    }
    private static void slice(ServerPlayerEntity player,C2SRequestGenomeSlicePacket p) {
        if(!RateLimiters.GENOME_SLICE.tryAcquire(player.getUuid()))return;
        Genome g=authorize(player,p.genomeId(),p.syncId(),p.revision());
        if(g==null||!GenomeRequestPolicy.validRange(p.start(),p.length(),g.getTotalLength(),GenomeRequestPolicy.MAX_SLICE))return;
        ServerPlayNetworking.send(player,new S2CSendGenomeSlicePacket(p.genomeId(),p.syncId(),p.revision(),g.getTotalLength(),p.start(),g.getSequence(p.start(),p.length())));
    }
    private static void motif(ServerPlayerEntity player,C2SRequestMotifSearchPacket p) {
        if(!RateLimiters.MOTIF_SEARCH.tryAcquire(player.getUuid()))return;
        Genome g=authorize(player,p.genomeId(),p.syncId(),p.revision());
        if(g==null||!GenomeRequestPolicy.validDna(p.motif(),GenomeRequestPolicy.MAX_MOTIF)||p.start()<0||p.start()>=g.getTotalLength())return;
        int length=(int)Math.min(GenomeRequestPolicy.MAX_SCAN,g.getTotalLength()-p.start());
        String sequence=g.getSequence(p.start(),length), query=p.motif().toUpperCase(Locale.ROOT);
        List<Long> hits=new ArrayList<>();boolean truncated=false;
        for(int at=sequence.indexOf(query);at>=0;at=sequence.indexOf(query,at+1)) {
            if(hits.size()==GenomeRequestPolicy.MAX_HITS){truncated=true;break;}
            hits.add(p.start()+at);
        }
        ServerPlayNetworking.send(player,new S2CSendMotifSearchResultsPacket(p.genomeId(),p.syncId(),p.revision(),p.start(),length,hits.stream().mapToLong(Long::longValue).toArray(),truncated));
    }
    private static CrisprEngine.TargetSite target(Genome genome,long pamPosition,CrisprEngine engine) {
        if(pamPosition<20||pamPosition>genome.getTotalLength()-3)return null;
        String pam=genome.getSequence(pamPosition,3);
        if(!pam.endsWith("GG"))return null;
        long start=Math.max(0,pamPosition-32);
        int length=(int)Math.min(96,genome.getTotalLength()-start);
        return engine.findPamSites(genome,start,length,CrisprEngine.SPCAS9_PAM).stream()
            .filter(t->t.position()==pamPosition&&t.protospacer().equals(genome.getSequence(pamPosition-20,20)))
            .findFirst().orElse(null);
    }
    private static void openInsertion(ServerPlayerEntity player,C2SOpenGeneInsertionScreenPacket p) {
        Genome genome=authorize(player,p.genomeId(),p.syncId(),p.revision());
        if(genome==null||!(player.currentScreenHandler instanceof GenomeTerminalScreenHandler menu))return;
        if(target(genome,p.pamPosition(),new CrisprEngine(0))==null)return;
        GeneInsertionOpenData data=new GeneInsertionOpenData(p.genomeId(),menu.terminalPos,genome.getTotalLength(),p.revision(),p.pamPosition(),menu.portable);
        player.openHandledScreen(new ExtendedScreenHandlerFactory<GeneInsertionOpenData>() {
            public GeneInsertionOpenData getScreenOpeningData(ServerPlayerEntity player){return data;}
            public Text getDisplayName(){return Text.translatable("screen.mrge.edit_title");}
            public ScreenHandler createMenu(int syncId,PlayerInventory inventory,PlayerEntity player){return new GeneInsertionScreenHandler(syncId,inventory,data);}
        });
    }
    private static ItemStack find(PlayerInventory inventory,Item item) {
        for(int i=0;i<inventory.size();i++){ItemStack stack=inventory.getStack(i);if(stack.isOf(item)&&!stack.isEmpty())return stack;}
        return ItemStack.EMPTY;
    }
    private static ItemStack template(PlayerInventory inventory,String sequence) {
        for(int i=0;i<inventory.size();i++) {
            ItemStack stack=inventory.getStack(i);
            if(stack.isOf(ModItems.AMPLIFIED_GENE_FRAGMENT)&&sequence.equalsIgnoreCase(stack.getOrDefault(DataComponentTypes.CUSTOM_DATA,NbtComponent.DEFAULT).copyNbt().getString("sequence")))return stack;
        }
        return ItemStack.EMPTY;
    }
    private static long referenceBoundary(Genome genome,long editedPosition) {
        return editedPosition==genome.getTotalLength()?genome.getReferenceLength():genome.editedToReference(editedPosition);
    }
    private static Genome apply(Genome genome,EditOutcome outcome) {
        if(outcome.getType()==EditOutcome.Type.NO_CHANGE)return genome;
        long editedStart=outcome.getPosition();
        long start=referenceBoundary(genome,editedStart);
        GenomePatch patch=new GenomePatch(genome.getUUID());
        if(outcome.getType()==EditOutcome.Type.DELETION||outcome.getType()==EditOutcome.Type.REPLACEMENT) {
            if(outcome.size()<=0||editedStart<0||outcome.size()>genome.getTotalLength()-editedStart)throw new IllegalArgumentException("Invalid repair interval");
            // A second edit must not silently cross an already inserted/deleted interval.
            long last=genome.editedToReference(editedStart+outcome.size()-1);
            if(last-start+1!=outcome.size())throw new IllegalArgumentException("Repair overlaps a previous structural edit");
            patch.addDeletion(start,outcome.size());
        }
        if(outcome.getType()==EditOutcome.Type.INSERTION||outcome.getType()==EditOutcome.Type.REPLACEMENT)patch.addInsertion(start,outcome.getSequence());
        if(outcome.getType()==EditOutcome.Type.POINT_MUTATION) {
            int base="ACGT".indexOf(outcome.getSequence().toUpperCase(Locale.ROOT));
            if(base<0)throw new IllegalArgumentException("Invalid replacement base");patch.addPointMutation(start,(byte)base);
        }
        if(outcome.getType()==EditOutcome.Type.COMPLEX_INDEL)throw new IllegalArgumentException("Complex repair is not represented by a single patch");
        return genome.applyPatch(patch);
    }
    private static void edit(ServerPlayerEntity player,UUID id,int syncId,long revision,long pamPosition,String sequence,boolean hdr) {
        if(!RateLimiters.GENE_EDITING.tryAcquire(player.getUuid()))return;
        Genome genome=authorize(player,id,syncId,revision);
        if(genome==null||!(player.currentScreenHandler instanceof GeneInsertionScreenHandler menu)||menu.getKnockoutPosition()!=pamPosition)return;
        if(hdr&&!GenomeRequestPolicy.validDna(sequence,GenomeRequestPolicy.MAX_TEMPLATE))return;
        ItemStack enzyme=find(player.getInventory(),ModItems.CRISPR_CAS9),guide=find(player.getInventory(),ModItems.GUIDE_RNA);
        ItemStack donor=hdr?template(player.getInventory(),sequence):ItemStack.EMPTY;
        if(enzyme.isEmpty()||guide.isEmpty()||(hdr&&donor.isEmpty())) {player.sendMessage(Text.translatable("message.mrge.missing_edit_materials"),false);return;}
        CrisprEngine engine=new CrisprEngine(player.getRandom().nextLong());
        CrisprEngine.TargetSite site=target(genome,pamPosition,engine);
        if(site==null)return;
        try {
            CrisprEngine.EditingResult result=engine.performEditing(genome,site,hdr?sequence.toUpperCase(Locale.ROOT):null);
            Genome updated=apply(genome,result.primaryOutcome());
            GenomeRepository.get(player.getServer()).put(updated);
            for (ItemStack held : List.of(player.getMainHandStack(), player.getOffHandStack())) {
                if (id.equals(GenomeSampleItem.getGenomeId(held))) {
                    var data = GenomeSampleItem.readData(held);
                    data.putLong("Revision", updated.getRevision());
                    data.putLong("GenomeLength", updated.getTotalLength());
                    GenomeSampleItem.writeData(held, data);
                }
            }
            enzyme.decrement(1);guide.decrement(1);if(hdr)donor.decrement(1);
            player.getInventory().markDirty();
            if(result.primaryOutcome().getType()!=EditOutcome.Type.NO_CHANGE)ServerProjectManager.recordEdited(player,id,updated.getRevision());
            player.sendMessage(Text.translatable("message.mrge.edit_result",result.primaryOutcome().getSummary()),false);
            player.closeHandledScreen();
        } catch(IllegalArgumentException ex) {
            player.sendMessage(Text.translatable("message.mrge.edit_conflict"),false);
        } catch(IllegalStateException ex) {
            player.sendMessage(Text.translatable("message.mrge.storage_full"),false);
        }
    }
}
