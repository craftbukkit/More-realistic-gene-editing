package com.morerealisticgeneediting.project;

import com.morerealisticgeneediting.network.s2c.S2CPlayerProjectStatePacket;
import com.morerealisticgeneediting.genome.GenomeRepository;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import java.util.HashSet;
import java.util.UUID;

public final class ServerProjectManager {
    private static ProjectStatePersistence store(ServerPlayerEntity player) {return ProjectStatePersistence.getServerState(player.getServer());}
    public static void onPlayerJoin(ServerPlayerEntity player) {S2CPlayerProjectStatePacket.send(player,getPlayerState(player));}
    public static void onPlayerLeave(ServerPlayerEntity player) { }
    public static PlayerProjectState getPlayerState(ServerPlayerEntity player) {return store(player).getPlayerState(player.getUuid());}
    public static void startProject(ServerPlayerEntity player,String id) {
        ProjectStatePersistence store=store(player); PlayerProjectState state=getPlayerState(player);
        ResearchProject project=ProjectRegistry.getProject(id);
        if(!ProjectCompletionRules.mayStart(project,state)) {player.sendMessage(Text.translatable("message.morerealisticgeneediting.project_unavailable"),false);return;}
        HashSet<String> active=new HashSet<>(state.activeProjectIds());active.add(id);
        PlayerProjectState updated=new PlayerProjectState(active,state.completedProjectIds());store.setPlayerState(player.getUuid(),updated);
        S2CPlayerProjectStatePacket.send(player,updated);
    }
    public static void completeProject(ServerPlayerEntity player,String id) {
        ProjectStatePersistence store=store(player); PlayerProjectState state=getPlayerState(player);
        ResearchProject project=ProjectRegistry.getProject(id);
        if(!ProjectCompletionRules.mayComplete(project,state,store.getEvidence(player.getUuid()),store.hasReadIntroduction(player.getUuid()))) {
            player.sendMessage(Text.translatable("message.morerealisticgeneediting.project_evidence_missing"),false);return;
        }
        HashSet<String> active=new HashSet<>(state.activeProjectIds());active.remove(id);
        HashSet<String> completed=new HashSet<>(state.completedProjectIds());completed.add(id);
        PlayerProjectState updated=new PlayerProjectState(active,completed);store.setPlayerState(player.getUuid(),updated);
        S2CPlayerProjectStatePacket.send(player,updated);
        player.sendMessage(Text.translatable("message.morerealisticgeneediting.project_completed",project.title()),false);
    }
    public static void acknowledgeIntroduction(ServerPlayerEntity player) {store(player).acknowledgeIntroduction(player.getUuid());}
    private static boolean ownsCurrentRevision(ServerPlayerEntity player,UUID sample,long revision) {
        return revision >= 0 && GenomeRepository.get(player.getServer()).findOwned(sample,player.getUuid())
            .filter(genome -> genome.getRevision() == revision).isPresent();
    }
    public static void recordSampled(ServerPlayerEntity player,UUID sample,long revision) {
        if (!ownsCurrentRevision(player,sample,revision)) return;
        ProjectStatePersistence store=store(player);
        if(store.getEvidence(player.getUuid(),sample).isEmpty())store.putEvidence(player.getUuid(),ExperimentEvidence.sampled(sample,revision));
    }
    public static void recordEdited(ServerPlayerEntity player,UUID sample,long revision) {
        if (!ownsCurrentRevision(player,sample,revision)) return;
        ProjectStatePersistence store=store(player);
        store.getEvidence(player.getUuid(),sample).ifPresent(e->store.putEvidence(player.getUuid(),e.edited(revision)));
    }
    /** Call only after a completed server-run sequence concordance validation of this exact revision. */
    public static void recordVerified(ServerPlayerEntity player,UUID sample,long revision) {
        if (!ownsCurrentRevision(player,sample,revision)) return;
        ProjectStatePersistence store=store(player);
        store.getEvidence(player.getUuid(),sample).ifPresent(e->store.putEvidence(player.getUuid(),e.verified(revision)));
    }
}
