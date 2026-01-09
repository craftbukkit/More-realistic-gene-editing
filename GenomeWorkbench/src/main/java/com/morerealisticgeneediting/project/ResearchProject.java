package com.morerealisticgeneediting.project;

import java.util.List;

/**
 * Represents a single research project that a player can undertake.
 * This is an immutable data structure, typically loaded from JSON.
 *
 * @param id          A unique identifier for the project (e.g., "beginner_sequencing").
 * @param title       The display name of the project.
 * @param description A short text explaining the background and goals of the project.
 * @param objectives  A list of tasks the player must complete.
 * @param rewards     A list of rewards the player receives upon completion.
 */
public record ResearchProject(
    String id,
    String title,
    String description,
    List<String> objectives, // For now, simple text descriptions. Will be evolved into concrete tasks.
    List<String> rewards      // For now, simple text descriptions. Will be evolved into concrete rewards.
) {}
