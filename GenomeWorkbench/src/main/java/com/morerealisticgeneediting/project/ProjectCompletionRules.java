package com.morerealisticgeneediting.project;
import java.util.Collection;
public final class ProjectCompletionRules {
    private ProjectCompletionRules() { }
    public static boolean mayStart(ResearchProject project, PlayerProjectState state) {
        return project != null && !project.isReferenceOnly() && !state.isActive(project.id())
            && !state.isCompleted(project.id()) && state.completedProjectIds().containsAll(project.prerequisites());
    }
    public static boolean mayComplete(ResearchProject project, PlayerProjectState state,
                                      Collection<ExperimentEvidence> evidence, boolean readIntroduction) {
        if (project == null || !state.isActive(project.id()) || project.isReferenceOnly()
            || !state.completedProjectIds().containsAll(project.prerequisites())) return false;
        return switch (project.completionMode()) {
            case "introduction" -> readIntroduction;
            case "sample_edit_verify" -> evidence.stream().anyMatch(ExperimentEvidence::complete);
            default -> false;
        };
    }
}
