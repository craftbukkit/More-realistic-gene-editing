package com.morerealisticgeneediting.screen;

import com.morerealisticgeneediting.MoreRealisticGeneEditing;
import com.morerealisticgeneediting.ethics.EthicsCase;
import com.morerealisticgeneediting.screens.GeneInsertionScreenHandler;
import com.morerealisticgeneediting.screens.GenomeTerminalScreenHandler;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.fabricmc.fabric.api.screenhandler.v1.ScreenHandlerRegistry;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;

public class ModScreenHandlers {
    public static ScreenHandlerType<CellProcessorScreenHandler> CELL_PROCESSOR_SCREEN_HANDLER;
    public static ScreenHandlerType<SequencerScreenHandler> SEQUENCER_SCREEN_HANDLER;
    public static ScreenHandlerType<PcrMachineScreenHandler> PCR_MACHINE_SCREEN_HANDLER;
    public static ScreenHandlerType<IncubatorScreenHandler> INCUBATOR_SCREEN_HANDLER;
    public static ScreenHandlerType<ElectrophoresisTankScreenHandler> ELECTROPHORESIS_TANK_SCREEN_HANDLER;
    public static ScreenHandlerType<GenomeTerminalScreenHandler> GENOME_TERMINAL_SCREEN_HANDLER;
    public static ScreenHandlerType<GeneInsertionScreenHandler> GENE_INSERTION_SCREEN_HANDLER;
    public static ScreenHandlerType<EthicsCaseScreenHandler> ETHICS_CASE_SCREEN_HANDLER;


    public static void registerAllScreenHandlers() {
        CELL_PROCESSOR_SCREEN_HANDLER = ScreenHandlerRegistry.registerSimple(new Identifier(MoreRealisticGeneEditing.MOD_ID, "cell_processor"),
                (syncId, inventory) -> new CellProcessorScreenHandler(syncId, inventory));

        SEQUENCER_SCREEN_HANDLER = ScreenHandlerRegistry.registerSimple(new Identifier(MoreRealisticGeneEditing.MOD_ID, "sequencer"),
                (syncId, inventory) -> new SequencerScreenHandler(syncId, inventory));

        PCR_MACHINE_SCREEN_HANDLER = ScreenHandlerRegistry.registerSimple(new Identifier(MoreRealisticGeneEditing.MOD_ID, "pcr_machine"),
                (syncId, inventory) -> new PcrMachineScreenHandler(syncId, inventory));

        INCUBATOR_SCREEN_HANDLER = ScreenHandlerRegistry.registerSimple(new Identifier(MoreRealisticGeneEditing.MOD_ID, "incubator"),
                (syncId, inventory) -> new IncubatorScreenHandler(syncId, inventory));

        ELECTROPHORESIS_TANK_SCREEN_HANDLER = ScreenHandlerRegistry.registerSimple(new Identifier(MoreRealisticGeneEditing.MOD_ID, "electrophoresis_tank"),
                (syncId, inventory) -> new ElectrophoresisTankScreenHandler(syncId, inventory));

        GENOME_TERMINAL_SCREEN_HANDLER = new ExtendedScreenHandlerType<>(GenomeTerminalScreenHandler::new);
        ScreenHandlerRegistry.register(new Identifier(MoreRealisticGeneEditing.MOD_ID, "genome_terminal"), GENOME_TERMINAL_SCREEN_HANDLER);

        GENE_INSERTION_SCREEN_HANDLER = new ExtendedScreenHandlerType<>(GeneInsertionScreenHandler::new);
        ScreenHandlerRegistry.register(new Identifier(MoreRealisticGeneEditing.MOD_ID, "gene_insertion"), GENE_INSERTION_SCREEN_HANDLER);

        ETHICS_CASE_SCREEN_HANDLER = new ExtendedScreenHandlerType<>((syncId, inv, buf) -> new EthicsCaseScreenHandler(syncId, inv, EthicsCase.from(buf)));
        ScreenHandlerRegistry.register(new Identifier(MoreRealisticGeneEditing.MOD_ID, "ethics_case"), ETHICS_CASE_SCREEN_HANDLER);
    }
}
