package com.morerealisticgeneediting.client.gui.equipment;
import com.morerealisticgeneediting.screen.equipment.ModEquipmentScreenHandlers;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
public final class ModEquipmentScreens {
    private static <T extends ScreenHandler> void readout(ScreenHandlerType<T> type,String... actions){
        HandledScreens.<T,EquipmentReadoutScreen<T>>register(type,(handler,inventory,title)->new EquipmentReadoutScreen<>(handler,inventory,title,actions));
    }
    public static void registerScreens(){
        HandledScreens.register(ModEquipmentScreenHandlers.CENTRIFUGE_SCREEN_HANDLER,CentrifugeScreen::new);
        HandledScreens.register(ModEquipmentScreenHandlers.THERMAL_CYCLER_SCREEN_HANDLER,ThermalCyclerScreen::new);
        HandledScreens.register(ModEquipmentScreenHandlers.ELECTROPHORESIS_SCREEN_HANDLER,ElectrophoresisScreen::new);
        readout(ModEquipmentScreenHandlers.SPECTROPHOTOMETER_SCREEN_HANDLER,"button.mrge.blank","button.mrge.measure");
        readout(ModEquipmentScreenHandlers.ELECTROPORATOR_SCREEN_HANDLER,"button.mrge.pulse","button.mrge.cancel");
        readout(ModEquipmentScreenHandlers.SEQUENCER_SCREEN_HANDLER,"button.mrge.start_a","button.mrge.start_b","button.mrge.reset_a","button.mrge.reset_b");
        readout(ModEquipmentScreenHandlers.INCUBATOR_EQUIPMENT_SCREEN_HANDLER);
        readout(ModEquipmentScreenHandlers.BIOSAFETY_SCREEN_HANDLER);
    }
}
