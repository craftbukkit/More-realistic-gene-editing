package com.morerealisticgeneediting.project;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ProjectStatePersistence extends PersistentState {
    private final Map<UUID, PlayerProjectState> playerStates = new HashMap<>();

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        NbtList playersNbt = new NbtList();
        playerStates.forEach((uuid, state) -> {
            NbtCompound playerNbt = new NbtCompound();
            playerNbt.putUuid("UUID", uuid);

            NbtList activeProjects = new NbtList();
            state.activeProjectIds().forEach(id -> activeProjects.add(NbtString.of(id)));
            playerNbt.put("ActiveProjects", activeProjects);

            NbtList completedProjects = new NbtList();
            state.completedProjectIds().forEach(id -> completedProjects.add(NbtString.of(id)));
            playerNbt.put("CompletedProjects", completedProjects);

            playersNbt.add(playerNbt);
        });
        nbt.put("PlayerProjectStates", playersNbt);
        return nbt;
    }

    public static ProjectStatePersistence createFromNbt(NbtCompound nbt) {
        ProjectStatePersistence state = new ProjectStatePersistence();
        NbtList playersNbt = nbt.getList("PlayerProjectStates", NbtCompound.COMPOUND_TYPE);

        for (int i = 0; i < playersNbt.size(); i++) {
            NbtCompound playerNbt = playersNbt.getCompound(i);
            UUID uuid = playerNbt.getUuid("UUID");

            Set<String> activeProjects = new HashSet<>();
            NbtList activeProjectsNbt = playerNbt.getList("ActiveProjects", NbtList.STRING_TYPE);
            activeProjectsNbt.forEach(tag -> activeProjects.add(tag.asString()));

            Set<String> completedProjects = new HashSet<>();
            NbtList completedProjectsNbt = playerNbt.getList("CompletedProjects", NbtList.STRING_TYPE);
            completedProjectsNbt.forEach(tag -> completedProjects.add(tag.asString()));
            
            state.playerStates.put(uuid, new PlayerProjectState(activeProjects, completedProjects));
        }
        return state;
    }

    public static ProjectStatePersistence getServerState(MinecraftServer server) {
        PersistentStateManager persistentStateManager = server.getOverworld().getPersistentStateManager();
        // The first argument is the identifier of the persistent state.
        // The second is a factory for creating a new state if it doesn't exist.
        // The third is a factory for loading the state from NBT.
        return persistentStateManager.getOrCreate(
                ProjectStatePersistence::createFromNbt,
                ProjectStatePersistence::new,
                "mrge_project_state");
    }

    public PlayerProjectState getPlayerState(UUID playerUuid) {
        return playerStates.getOrDefault(playerUuid, PlayerProjectState.EMPTY);
    }
    
    public void setPlayerState(UUID playerUuid, PlayerProjectState state) {
        playerStates.put(playerUuid, state);
        markDirty(); // Mark the state as needing to be saved
    }
}
