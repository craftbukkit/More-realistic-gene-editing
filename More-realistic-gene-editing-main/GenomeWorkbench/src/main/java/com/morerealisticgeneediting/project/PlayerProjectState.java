package com.morerealisticgeneediting.project;

import java.util.Collections;
import java.util.Set;

/**
 * An immutable record representing the state of a player's research projects.
 *
 * @param activeProjectIds    A set of IDs for projects the player is currently working on.
 * @param completedProjectIds A set of IDs for projects the player has successfully completed.
 */
public record PlayerProjectState(
    Set<String> activeProjectIds,
    Set<String> completedProjectIds
) {
    /**
     * A convenient constant for a player's initial, empty state.
     */
    public static final PlayerProjectState EMPTY = new PlayerProjectState(
        Collections.emptySet(),
        Collections.emptySet()
    );
}
