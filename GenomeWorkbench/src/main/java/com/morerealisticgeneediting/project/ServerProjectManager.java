package com.morerealisticgeneediting.project;

import com.morerealisticgeneediting.network.s2c.S2CPlayerProjectStatePacket;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ServerProjectManager {

    public static void onPlayerJoin(ServerPlayerEntity player) {
        ProjectStatePersistence serverState = ProjectStatePersistence.getServerState(player.getServer());
        PlayerProjectState playerState = serverState.getPlayerState(player.getUuid());
        S2CPlayerProjectStatePacket.send(player, playerState);
    }

    public static void onPlayerLeave(ServerPlayerEntity player) {
        // State is persisted automatically.
    }

    public static PlayerProjectState getPlayerState(ServerPlayerEntity player) {
        ProjectStatePersistence serverState = ProjectStatePersistence.getServerState(player.getServer());
        return serverState.getPlayerState(player.getUuid());
    }

    public static void startProject(ServerPlayerEntity player, String projectId) {
        ProjectStatePersistence serverState = ProjectStatePersistence.getServerState(player.getServer());
        PlayerProjectState currentState = serverState.getPlayerState(player.getUuid());

        ResearchProject project = ProjectRegistry.getProject(projectId);

        if (project == null || currentState.isActive(projectId) || currentState.isCompleted(projectId)) {
            return;
        }

        Set<String> newActiveProjects = new HashSet<>(currentState.activeProjectIds());
        newActiveProjects.add(projectId);

        PlayerProjectState newState = new PlayerProjectState(
            newActiveProjects,
            currentState.completedProjectIds()
        );

        serverState.setPlayerState(player.getUuid(), newState);
        S2CPlayerProjectStatePacket.send(player, newState);
    }

    public static void completeProject(ServerPlayerEntity player, String projectId) {
        ProjectStatePersistence serverState = ProjectStatePersistence.getServerState(player.getServer());
        PlayerProjectState currentState = serverState.getPlayerState(player.getUuid());

        if (!currentState.isActive(projectId)) {
            return; // Can't complete a project that isn't active.
        }

        Set<String> newActiveProjects = new HashSet<>(currentState.activeProjectIds());
        newActiveProjects.remove(projectId);

        Set<String> newCompletedProjects = new HashSet<>(currentState.completedProjectIds());
        newCompletedProjects.add(projectId);

        PlayerProjectState newState = new PlayerProjectState(
            newActiveProjects,
            newCompletedProjects
        );

        serverState.setPlayerState(player.getUuid(), newState);
        S2CPlayerProjectStatePacket.send(player, newState);
    }
}
