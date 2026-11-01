package com.morerealisticgeneediting;
import com.morerealisticgeneediting.client.gui.screen.*;
import com.morerealisticgeneediting.client.gui.equipment.ModEquipmentScreens;
import com.morerealisticgeneediting.network.ClientPacketHandler;
import com.morerealisticgeneediting.screen.*;
import com.morerealisticgeneediting.screens.GenomeTerminalScreen;
import com.morerealisticgeneediting.screens.GeneInsertionScreen;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
public class MoreRealisticGeneEditingClient implements ClientModInitializer {
    public void onInitializeClient(){
        HandledScreens.register(ModScreenHandlers.CELL_PROCESSOR_SCREEN_HANDLER,CellProcessorScreen::new);
        HandledScreens.register(ModScreenHandlers.SEQUENCER_SCREEN_HANDLER,SequencerScreen::new);
        HandledScreens.register(ModScreenHandlers.PCR_MACHINE_SCREEN_HANDLER,PcrMachineScreen::new);
        HandledScreens.register(ModScreenHandlers.INCUBATOR_SCREEN_HANDLER,IncubatorScreen::new);
        HandledScreens.register(ModScreenHandlers.ELECTROPHORESIS_TANK_SCREEN_HANDLER,ElectrophoresisTankScreen::new);
        HandledScreens.register(ModScreenHandlers.GENOME_TERMINAL_SCREEN_HANDLER,GenomeTerminalScreen::new);
        HandledScreens.register(ModScreenHandlers.GENE_INSERTION_SCREEN_HANDLER,GeneInsertionScreen::new);
        HandledScreens.register(ModScreenHandlers.ETHICS_CASE_SCREEN_HANDLER,EthicsCaseScreen::new);
        ModEquipmentScreens.registerScreens();ClientPacketHandler.register();
    }
}
