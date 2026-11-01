package com.morerealisticgeneediting.project;

import net.minecraft.nbt.*;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;
import java.util.*;

public class ProjectStatePersistence extends PersistentState {
    private static final int MAX_SAMPLES_PER_PLAYER = 32;
    private final Map<UUID, PlayerProjectState> playerStates = new HashMap<>();
    private final Map<UUID, LinkedHashMap<UUID, ExperimentEvidence>> evidence = new HashMap<>();
    private final Set<UUID> introductions = new HashSet<>();
    private static final Type<ProjectStatePersistence> TYPE = new Type<>(ProjectStatePersistence::new, ProjectStatePersistence::fromNbt, null);

    @Override public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        nbt.putInt("Schema",2);
        NbtList players = new NbtList();
        Set<UUID> ids = new HashSet<>(playerStates.keySet()); ids.addAll(evidence.keySet()); ids.addAll(introductions);
        for (UUID uuid : ids) {
            NbtCompound entry = new NbtCompound(); entry.putUuid("UUID",uuid);
            PlayerProjectState state=getPlayerState(uuid);
            entry.put("ActiveProjects",strings(state.activeProjectIds()));
            entry.put("CompletedProjects",strings(state.completedProjectIds()));
            entry.putBoolean("IntroductionRead",introductions.contains(uuid));
            NbtList records=new NbtList();
            for (ExperimentEvidence e : getEvidence(uuid)) {
                NbtCompound row=new NbtCompound(); row.putUuid("Sample",e.sampleId());
                row.putLong("Sampled",e.sampledRevision()); row.putLong("Edited",e.editedRevision()); row.putLong("Verified",e.verifiedRevision()); records.add(row);
            }
            entry.put("Evidence",records); players.add(entry);
        }
        nbt.put("PlayerProjectStates",players); return nbt;
    }
    private static NbtList strings(Set<String> values) { NbtList out=new NbtList(); values.forEach(s->out.add(NbtString.of(s))); return out; }
    private static Set<String> ids(NbtList list) {
        Set<String> out=new HashSet<>();
        for (int i=0;i<Math.min(list.size(),256);i++) { String id=list.getString(i); if(id.matches("[a-z0-9_]{1,80}")) out.add(id); }
        return out;
    }
    public static ProjectStatePersistence fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        ProjectStatePersistence out=new ProjectStatePersistence();
        NbtList players=nbt.getList("PlayerProjectStates",NbtElement.COMPOUND_TYPE);
        for (int i=0;i<players.size();i++) {
            NbtCompound row=players.getCompound(i); if(!row.containsUuid("UUID"))continue;
            UUID id=row.getUuid("UUID");
            Set<String> completed=nbt.getInt("Schema")>=2 ? ids(row.getList("CompletedProjects",NbtElement.STRING_TYPE)) : Set.of();
            Set<String> active=ids(row.getList("ActiveProjects",NbtElement.STRING_TYPE));active.removeAll(completed);
            out.playerStates.put(id,new PlayerProjectState(active,completed));
            if (row.getBoolean("IntroductionRead"))out.introductions.add(id);
            NbtList records=row.getList("Evidence",NbtElement.COMPOUND_TYPE);
            for(int j=0;j<Math.min(records.size(),MAX_SAMPLES_PER_PLAYER);j++) {
                NbtCompound e=records.getCompound(j); if(!e.containsUuid("Sample"))continue;
                try { out.putEvidence(id,new ExperimentEvidence(e.getUuid("Sample"),e.getLong("Sampled"),e.getLong("Edited"),e.getLong("Verified"))); }
                catch(IllegalArgumentException ignored) { /* Corrupt evidence cannot unlock a task. */ }
            }
        }
        return out;
    }
    public static ProjectStatePersistence getServerState(MinecraftServer server) {
        return server.getOverworld().getPersistentStateManager().getOrCreate(TYPE,"mrge_project_state");
    }
    public PlayerProjectState getPlayerState(UUID id) {return playerStates.getOrDefault(id,PlayerProjectState.EMPTY);}
    public void setPlayerState(UUID id,PlayerProjectState state) {playerStates.put(id,state);markDirty();}
    public void acknowledgeIntroduction(UUID id) {introductions.add(id);markDirty();}
    public boolean hasReadIntroduction(UUID id) {return introductions.contains(id);}
    public Collection<ExperimentEvidence> getEvidence(UUID id) {return List.copyOf(evidence.getOrDefault(id,new LinkedHashMap<>()).values());}
    public Optional<ExperimentEvidence> getEvidence(UUID id,UUID sample) {return Optional.ofNullable(evidence.getOrDefault(id,new LinkedHashMap<>()).get(sample));}
    public void putEvidence(UUID id,ExperimentEvidence value) {
        LinkedHashMap<UUID,ExperimentEvidence> records=evidence.computeIfAbsent(id,k->new LinkedHashMap<>());
        if(!records.containsKey(value.sampleId()) && records.size()>=MAX_SAMPLES_PER_PLAYER)records.remove(records.keySet().iterator().next());
        records.put(value.sampleId(),value);markDirty();
    }
}
