package com.morerealisticgeneediting.project;

import java.util.List;

/** A teaching task or a reference case; descriptions are not executable clinical protocols. */
public record ResearchProject(String id, String title, String description, List<String> objectives,
                              List<String> rewards, List<String> prerequisites, String completionMode,
                              List<String> sources) {
    public ResearchProject {
        if (id == null || !id.matches("[a-z0-9_]{1,80}")) throw new IllegalArgumentException("Invalid project id");
        if (title == null || description == null) throw new IllegalArgumentException("Missing project text");
        objectives = objectives == null ? List.of() : List.copyOf(objectives);
        rewards = rewards == null ? List.of() : List.copyOf(rewards);
        prerequisites = prerequisites == null ? List.of() : List.copyOf(prerequisites);
        sources = sources == null ? List.of() : List.copyOf(sources);
        completionMode = completionMode == null ? "reference_only" : completionMode;
        if (!List.of("introduction", "sample_edit_verify", "reference_only").contains(completionMode))
            throw new IllegalArgumentException("Unknown completion mode");
    }
    public ResearchProject(String id, String title, String description, List<String> objectives, List<String> rewards) {
        this(id,title,description,objectives,rewards,List.of(),"reference_only",List.of());
    }
    public boolean isReferenceOnly() { return "reference_only".equals(completionMode); }
}
