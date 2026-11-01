package com.morerealisticgeneediting.screen.equipment;

import com.morerealisticgeneediting.MoreRealisticGeneEditing;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.util.Identifier;

/**
 * Registry for all laboratory equipment screen handlers.
 */
public class ModEquipmentScreenHandlers {
    
    // ========== Screen Handler Types ==========
    
    public static final ScreenHandlerType<CentrifugeScreenHandler> CENTRIFUGE_SCREEN_HANDLER =
            Registry.register(Registries.SCREEN_HANDLER,
                    Identifier.of(MoreRealisticGeneEditing.MOD_ID, "centrifuge"),
                    new ScreenHandlerType<>(CentrifugeScreenHandler::new, FeatureFlags.VANILLA_FEATURES));
    
    public static final ScreenHandlerType<ThermalCyclerScreenHandler> THERMAL_CYCLER_SCREEN_HANDLER =
            Registry.register(Registries.SCREEN_HANDLER,
                    Identifier.of(MoreRealisticGeneEditing.MOD_ID, "thermal_cycler"),
                    new ScreenHandlerType<>(ThermalCyclerScreenHandler::new, FeatureFlags.VANILLA_FEATURES));
    
    public static final ScreenHandlerType<ElectrophoresisScreenHandler> ELECTROPHORESIS_SCREEN_HANDLER =
            Registry.register(Registries.SCREEN_HANDLER,
                    Identifier.of(MoreRealisticGeneEditing.MOD_ID, "electrophoresis"),
                    new ScreenHandlerType<>(ElectrophoresisScreenHandler::new, FeatureFlags.VANILLA_FEATURES));
    
    public static final ScreenHandlerType<SequencerScreenHandler> SEQUENCER_SCREEN_HANDLER =
            Registry.register(Registries.SCREEN_HANDLER,
                    Identifier.of(MoreRealisticGeneEditing.MOD_ID, "sequencer_equipment"),
                    new ScreenHandlerType<>(SequencerScreenHandler::new, FeatureFlags.VANILLA_FEATURES));
    
    public static final ScreenHandlerType<SpectrophotometerScreenHandler> SPECTROPHOTOMETER_SCREEN_HANDLER =
            Registry.register(Registries.SCREEN_HANDLER,
                    Identifier.of(MoreRealisticGeneEditing.MOD_ID, "spectrophotometer"),
                    new ScreenHandlerType<>(SpectrophotometerScreenHandler::new, FeatureFlags.VANILLA_FEATURES));
    
    public static final ScreenHandlerType<IncubatorEquipmentScreenHandler> INCUBATOR_EQUIPMENT_SCREEN_HANDLER =
            Registry.register(Registries.SCREEN_HANDLER,
                    Identifier.of(MoreRealisticGeneEditing.MOD_ID, "incubator_equipment"),
                    new ScreenHandlerType<>(IncubatorEquipmentScreenHandler::new, FeatureFlags.VANILLA_FEATURES));
    
    public static final ScreenHandlerType<ElectroporatorScreenHandler> ELECTROPORATOR_SCREEN_HANDLER =
            Registry.register(Registries.SCREEN_HANDLER,
                    Identifier.of(MoreRealisticGeneEditing.MOD_ID, "electroporator"),
                    new ScreenHandlerType<>(ElectroporatorScreenHandler::new, FeatureFlags.VANILLA_FEATURES));
    
    public static final ScreenHandlerType<BiosafetyScreenHandler> BIOSAFETY_SCREEN_HANDLER =
            Registry.register(Registries.SCREEN_HANDLER,
                    Identifier.of(MoreRealisticGeneEditing.MOD_ID, "biosafety_cabinet"),
                    new ScreenHandlerType<>(BiosafetyScreenHandler::new, FeatureFlags.VANILLA_FEATURES));
    
    /**
     * Initialize all screen handlers
     */
    public static void registerScreenHandlers() {
        MoreRealisticGeneEditing.LOGGER.info("Registering equipment screen handlers");
    }
}
