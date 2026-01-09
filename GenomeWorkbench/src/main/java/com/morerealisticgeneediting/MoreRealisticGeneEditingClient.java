package com.morerealisticgeneediting;

import com.morerealisticgeneediting.client.gui.screen.ElectrophoresisTankScreen;
import com.morerealisticgeneediting.client.gui.screen.IncubatorScreen;
import com.morerealisticgeneediting.client.gui.screen.PcrMachineScreen;
import com.morerealisticgeneediting.client.gui.screen.SequencerScreen;
import com.morerealisticgeneediting.network.ClientPacketHandler;
import com.morerealisticgeneediting.screen.CellProcessorScreen;
import com.morerealisticgeneediting.screen.EthicsCaseScreen;
import com.morerealisticgeneediting.screen.ModScreenHandlers;
import com.morerealisticgeneediting.screens.GeneInsertionScreen;
import com.morerealisticgeneediting.screens.GenomeTerminalScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.screenhandler.v1.ScreenRegistry;

public class MoreRealisticGeneEditingClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ScreenRegistry.register(ModScreenHandlers.CELL_PROCESSOR_SCREEN_HANDLER, CellProcessorScreen::new);
        ScreenRegistry.register(ModScreenHandlers.SEQUENCER_SCREEN_HANDLER, SequencerScreen::new);
        ScreenRegistry.register(ModScreenHandlers.PCR_MACHINE_SCREEN_HANDLER, PcrMachineScreen::new);
        ScreenRegistry.register(ModScreenHandlers.INCUBATOR_SCREEN_HANDLER, IncubatorScreen::new);
        ScreenRegistry.register(ModScreenHandlers.ELECTROPHORESIS_TANK_SCREEN_HANDLER, ElectrophoresisTankScreen::new);
        ScreenRegistry.register(ModScreenHandlers.GENOME_TERMINAL_SCREEN_HANDLER, GenomeTerminalScreen::new);
        ScreenRegistry.register(ModScreenHandlers.GENE_INSERTION_SCREEN_HANDLER, GeneInsertionScreen::new);
        ScreenRegistry.register(ModScreenHandlers.ETHICS_CASE_SCREEN_HANDLER, EthicsCaseScreen::new);

        // Register client-side packet handlers
        ClientPacketHandler.register();
    }
}
