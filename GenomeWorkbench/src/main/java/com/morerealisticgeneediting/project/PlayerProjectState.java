package com.morerealisticgeneediting.project;
import java.util.Set;
public record PlayerProjectState(Set<String> activeProjectIds, Set<String> completedProjectIds) {
    public static final PlayerProjectState EMPTY = new PlayerProjectState(Set.of(), Set.of());
    public PlayerProjectState {
        activeProjectIds = Set.copyOf(activeProjectIds);
        completedProjectIds = Set.copyOf(completedProjectIds);
        if (!java.util.Collections.disjoint(activeProjectIds, completedProjectIds))
            throw new IllegalArgumentException("A project cannot be active and completed");
    }
    public boolean isActive(String id) { return activeProjectIds.contains(id); }
    public boolean isCompleted(String id) { return completedProjectIds.contains(id); }
}
