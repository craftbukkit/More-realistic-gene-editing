package com.morerealisticgeneediting.project;

/**
 * A client-side singleton for managing the player's project state.
 */
public class ClientProjectManager {
    private static PlayerProjectState currentState = PlayerProjectState.EMPTY;

    /**
     * Updates the client-side state. This is called when a packet is received
     * from the server.
     * @param newState The new state from the server.
     */
    public static void setState(PlayerProjectState newState) {
        currentState = newState;
    }

    /**
     * Checks if a project is currently active.
     * @param projectId The ID of the project.
     * @return True if the project is in the active set, false otherwise.
     */
    public static boolean isProjectActive(String projectId) {
        return currentState.activeProjectIds().contains(projectId);
    }

    /**
     * Checks if a project has been completed.
     * @param projectId The ID of the project.
     * @return True if the project is in the completed set, false otherwise.
     */
    public static boolean isProjectCompleted(String projectId) {
        return currentState.completedProjectIds().contains(projectId);
    }
}
