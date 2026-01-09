package com.morerealisticgeneediting.client.gui.equipment;

import com.morerealisticgeneediting.screen.equipment.*;
import net.minecraft.client.gui.screen.ingame.HandledScreens;

/**
 * Client-side registration of equipment screens
 */
public class ModEquipmentScreens {
    
    public static void registerScreens() {
        HandledScreens.register(ModEquipmentScreenHandlers.CENTRIFUGE_SCREEN_HANDLER, CentrifugeScreen::new);
        HandledScreens.register(ModEquipmentScreenHandlers.THERMAL_CYCLER_SCREEN_HANDLER, ThermalCyclerScreen::new);
        HandledScreens.register(ModEquipmentScreenHandlers.ELECTROPHORESIS_SCREEN_HANDLER, ElectrophoresisScreen::new);
        // Additional screens would be registered here
    }
}
