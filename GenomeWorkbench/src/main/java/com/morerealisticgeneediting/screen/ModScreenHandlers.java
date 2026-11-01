package com.morerealisticgeneediting.screen;

import com.morerealisticgeneediting.MoreRealisticGeneEditing;
import com.morerealisticgeneediting.screens.*;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;

/** Single registry shared by client constructors and server menu factories. */
public class ModScreenHandlers {
    public static final ScreenHandlerType<LabEquipmentScreenHandler> LAB_EQUIPMENT = simple("lab_equipment", LabEquipmentScreenHandler::new);
    public static final ScreenHandlerType<CellProcessorScreenHandler> CELL_PROCESSOR_SCREEN_HANDLER = simple("cell_processor", CellProcessorScreenHandler::new);
    public static final ScreenHandlerType<SequencerScreenHandler> SEQUENCER_SCREEN_HANDLER = simple("sequencer", SequencerScreenHandler::new);
    public static final ScreenHandlerType<PcrMachineScreenHandler> PCR_MACHINE_SCREEN_HANDLER = simple("pcr_machine", PcrMachineScreenHandler::new);
    public static final ScreenHandlerType<IncubatorScreenHandler> INCUBATOR_SCREEN_HANDLER = simple("incubator", IncubatorScreenHandler::new);
    public static final ScreenHandlerType<ElectrophoresisTankScreenHandler> ELECTROPHORESIS_TANK_SCREEN_HANDLER = simple("electrophoresis_tank", ElectrophoresisTankScreenHandler::new);
    public static final ScreenHandlerType<GenomeTerminalScreenHandler> GENOME_TERMINAL_SCREEN_HANDLER = register("genome_terminal", new ExtendedScreenHandlerType<>(GenomeTerminalScreenHandler::new, TerminalOpenData.CODEC));
    public static final ScreenHandlerType<GeneInsertionScreenHandler> GENE_INSERTION_SCREEN_HANDLER = register("gene_insertion", new ExtendedScreenHandlerType<>(GeneInsertionScreenHandler::new, GeneInsertionOpenData.CODEC));
    public static final ScreenHandlerType<EthicsCaseScreenHandler> ETHICS_CASE_SCREEN_HANDLER = register("ethics_case", new ExtendedScreenHandlerType<>(EthicsCaseScreenHandler::new, EthicsCaseOpenData.CODEC));
    private static <T extends ScreenHandler> ScreenHandlerType<T> simple(String id, ScreenHandlerType.Factory<T> factory) {
        return register(id, new ScreenHandlerType<>(factory, FeatureFlags.VANILLA_FEATURES));
    }
    private static <T extends ScreenHandler> ScreenHandlerType<T> register(String id, ScreenHandlerType<T> type) {
        return Registry.register(Registries.SCREEN_HANDLER, Identifier.of(MoreRealisticGeneEditing.MOD_ID, id), type);
    }
    public static void registerAllScreenHandlers() { }
}
