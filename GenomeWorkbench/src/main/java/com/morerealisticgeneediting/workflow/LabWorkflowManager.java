package com.morerealisticgeneediting.workflow;

import com.morerealisticgeneediting.equipment.EquipmentTier;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.*;

/**
 * Laboratory Workflow Manager
 * 
 * Manages the scientific workflow and equipment interactions for gene editing.
 * Defines valid input/output relationships between different laboratory equipment.
 * 
 * Standard Gene Editing Workflow:
 * 1. Sample Collection → Genome Sample
 * 2. DNA Extraction (Centrifuge) → DNA Sample
 * 3. PCR Amplification (Thermal Cycler) → Amplified Fragment
 * 4. Gel Electrophoresis (Electrophoresis Tank) → Verified Fragment
 * 5. Cloning/Ligation → Recombinant Plasmid
 * 6. Transformation/Transfection (Electroporator/Incubator) → Modified Cells
 * 7. Sequencing (Sequencer) → Sequence Data
 * 8. Analysis (Genome Terminal) → Edit Results
 */
public class LabWorkflowManager {
    
    // Workflow stage definitions
    public enum WorkflowStage {
        SAMPLE_COLLECTION("Sample Collection", "Collect biological samples from organisms"),
        DNA_EXTRACTION("DNA Extraction", "Extract DNA from samples using centrifugation"),
        PCR_AMPLIFICATION("PCR Amplification", "Amplify target gene regions"),
        GEL_ANALYSIS("Gel Analysis", "Verify PCR products by size"),
        CLONING("Cloning", "Insert genes into vectors"),
        TRANSFORMATION("Transformation", "Introduce DNA into cells"),
        SELECTION("Selection", "Select successfully transformed cells"),
        SEQUENCING("Sequencing", "Verify edited sequences"),
        ANALYSIS("Analysis", "Analyze editing results");
        
        public final String name;
        public final String description;
        
        WorkflowStage(String name, String description) {
            this.name = name;
            this.description = description;
        }
    }
    
    /**
     * Recipe definition for laboratory processes
     */
    public record LabRecipe(
        String id,
        String name,
        WorkflowStage stage,
        List<ItemStack> inputs,
        List<ItemStack> outputs,
        int processingTime,     // Ticks
        EquipmentTier minTier,
        float successRate,
        String description
    ) {
        public boolean matches(List<ItemStack> providedInputs) {
            if (providedInputs.size() < inputs.size()) return false;
            
            for (ItemStack required : inputs) {
                boolean found = false;
                for (ItemStack provided : providedInputs) {
                    if (ItemStack.areItemsEqual(required, provided) && 
                        provided.getCount() >= required.getCount()) {
                        found = true;
                        break;
                    }
                }
                if (!found) return false;
            }
            return true;
        }
    }
    
    // Registry of all lab recipes
    private static final Map<String, LabRecipe> RECIPES = new LinkedHashMap<>();
    private static final Map<WorkflowStage, List<LabRecipe>> RECIPES_BY_STAGE = new EnumMap<>(WorkflowStage.class);
    
    /**
     * Equipment capabilities - what each equipment type can process
     */
    public enum EquipmentType {
        CENTRIFUGE(Set.of(WorkflowStage.DNA_EXTRACTION)),
        THERMAL_CYCLER(Set.of(WorkflowStage.PCR_AMPLIFICATION)),
        ELECTROPHORESIS(Set.of(WorkflowStage.GEL_ANALYSIS)),
        SEQUENCER(Set.of(WorkflowStage.SEQUENCING)),
        INCUBATOR(Set.of(WorkflowStage.TRANSFORMATION, WorkflowStage.SELECTION)),
        ELECTROPORATOR(Set.of(WorkflowStage.TRANSFORMATION)),
        BIOSAFETY_CABINET(Set.of(WorkflowStage.SAMPLE_COLLECTION, WorkflowStage.CLONING)),
        SPECTROPHOTOMETER(Set.of(WorkflowStage.GEL_ANALYSIS)),
        GENOME_TERMINAL(Set.of(WorkflowStage.ANALYSIS));
        
        public final Set<WorkflowStage> supportedStages;
        
        EquipmentType(Set<WorkflowStage> stages) {
            this.supportedStages = stages;
        }
        
        public boolean canProcess(WorkflowStage stage) {
            return supportedStages.contains(stage);
        }
    }
    
    /**
     * Register a lab recipe
     */
    public static void registerRecipe(LabRecipe recipe) {
        RECIPES.put(recipe.id(), recipe);
        RECIPES_BY_STAGE.computeIfAbsent(recipe.stage(), k -> new ArrayList<>()).add(recipe);
    }
    
    /**
     * Get all recipes for a workflow stage
     */
    public static List<LabRecipe> getRecipesForStage(WorkflowStage stage) {
        return RECIPES_BY_STAGE.getOrDefault(stage, Collections.emptyList());
    }
    
    /**
     * Get recipe by ID
     */
    public static Optional<LabRecipe> getRecipe(String id) {
        return Optional.ofNullable(RECIPES.get(id));
    }
    
    /**
     * Find matching recipe for given inputs
     */
    public static Optional<LabRecipe> findMatchingRecipe(WorkflowStage stage, List<ItemStack> inputs) {
        return getRecipesForStage(stage).stream()
                .filter(recipe -> recipe.matches(inputs))
                .findFirst();
    }
    
    /**
     * Check if an item can be used in a workflow stage
     */
    public static boolean isValidInput(Item item, WorkflowStage stage) {
        return getRecipesForStage(stage).stream()
                .flatMap(recipe -> recipe.inputs().stream())
                .anyMatch(input -> input.isOf(item));
    }
    
    /**
     * Get the next workflow stages after completing a stage
     */
    public static List<WorkflowStage> getNextStages(WorkflowStage current) {
        return switch (current) {
            case SAMPLE_COLLECTION -> List.of(WorkflowStage.DNA_EXTRACTION);
            case DNA_EXTRACTION -> List.of(WorkflowStage.PCR_AMPLIFICATION);
            case PCR_AMPLIFICATION -> List.of(WorkflowStage.GEL_ANALYSIS);
            case GEL_ANALYSIS -> List.of(WorkflowStage.CLONING, WorkflowStage.SEQUENCING);
            case CLONING -> List.of(WorkflowStage.TRANSFORMATION);
            case TRANSFORMATION -> List.of(WorkflowStage.SELECTION);
            case SELECTION -> List.of(WorkflowStage.SEQUENCING);
            case SEQUENCING -> List.of(WorkflowStage.ANALYSIS);
            case ANALYSIS -> List.of();  // End of workflow
        };
    }
    
    /**
     * Calculate success rate modifier based on equipment tier and recipe requirements
     */
    public static float calculateSuccessRate(LabRecipe recipe, EquipmentTier equipmentTier) {
        float baseRate = recipe.successRate();
        
        // Bonus for using higher tier equipment
        int tierDiff = equipmentTier.getLevel() - recipe.minTier().getLevel();
        float tierBonus = tierDiff > 0 ? tierDiff * 0.1f : 0;
        
        return Math.min(1.0f, baseRate + tierBonus + equipmentTier.getSuccessRateBonus());
    }
    
    /**
     * Calculate processing time based on equipment tier
     */
    public static int calculateProcessingTime(LabRecipe recipe, EquipmentTier equipmentTier) {
        return (int) (recipe.processingTime() * equipmentTier.getSpeedMultiplier());
    }
    
    /**
     * Workflow validation result
     */
    public record ValidationResult(
        boolean valid,
        String message,
        List<String> missingInputs,
        List<String> suggestions
    ) {
        public static ValidationResult success() {
            return new ValidationResult(true, "Ready to process", List.of(), List.of());
        }
        
        public static ValidationResult failure(String message, List<String> missing) {
            return new ValidationResult(false, message, missing, List.of());
        }
    }
    
    /**
     * Validate if a process can be started
     */
    public static ValidationResult validateProcess(
            EquipmentType equipment,
            List<ItemStack> inputs,
            EquipmentTier tier) {
        
        // Find applicable stages
        Set<WorkflowStage> stages = equipment.supportedStages;
        
        for (WorkflowStage stage : stages) {
            Optional<LabRecipe> recipe = findMatchingRecipe(stage, inputs);
            if (recipe.isPresent()) {
                LabRecipe r = recipe.get();
                
                // Check tier requirement
                if (tier.getLevel() < r.minTier().getLevel()) {
                    return new ValidationResult(
                        false,
                        "Equipment tier too low",
                        List.of(),
                        List.of("Requires " + r.minTier().getId() + " tier or higher")
                    );
                }
                
                return ValidationResult.success();
            }
        }
        
        // No matching recipe found
        List<String> suggestions = new ArrayList<>();
        for (WorkflowStage stage : stages) {
            List<LabRecipe> stageRecipes = getRecipesForStage(stage);
            if (!stageRecipes.isEmpty()) {
                suggestions.add("Try adding: " + stageRecipes.get(0).inputs().get(0).getName().getString());
            }
        }
        
        return new ValidationResult(
            false,
            "No valid recipe found",
            List.of("Check required inputs"),
            suggestions
        );
    }
    
    /**
     * Initialize default recipes
     */
    public static void initializeRecipes() {
        // Recipes will be registered here or loaded from data packs
        // Example recipes are defined in recipe JSON files
    }
}
