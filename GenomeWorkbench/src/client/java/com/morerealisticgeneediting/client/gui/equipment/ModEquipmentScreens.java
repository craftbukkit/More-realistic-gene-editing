package com.morerealisticgeneediting.client.gui.equipment;
import com.morerealisticgeneediting.screen.equipment.ModEquipmentScreenHandlers;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
public final class ModEquipmentScreens {
    public static void registerScreens(){
        HandledScreens.register(ModEquipmentScreenHandlers.CENTRIFUGE_SCREEN_HANDLER,CentrifugeScreen::new);
        HandledScreens.register(ModEquipmentScreenHandlers.THERMAL_CYCLER_SCREEN_HANDLER,ThermalCyclerScreen::new);
        HandledScreens.register(ModEquipmentScreenHandlers.ELECTROPHORESIS_SCREEN_HANDLER,ElectrophoresisScreen::new);
        HandledScreens.register(ModEquipmentScreenHandlers.SPECTROPHOTOMETER_SCREEN_HANDLER,(h,i,t)->new EquipmentReadoutScreen<>(h,i,t,"button.mrge.blank","button.mrge.measure"));
        HandledScreens.register(ModEquipmentScreenHandlers.ELECTROPORATOR_SCREEN_HANDLER,(h,i,t)->new EquipmentReadoutScreen<>(h,i,t,"button.mrge.pulse"));
        HandledScreens.register(ModEquipmentScreenHandlers.SEQUENCER_SCREEN_HANDLER,(h,i,t)->new EquipmentReadoutScreen<>(h,i,t,"button.mrge.start_a","button.mrge.start_b","button.mrge.reset_a","button.mrge.reset_b"));
        HandledScreens.register(ModEquipmentScreenHandlers.INCUBATOR_EQUIPMENT_SCREEN_HANDLER,(h,i,t)->new EquipmentReadoutScreen<>(h,i,t));
        HandledScreens.register(ModEquipmentScreenHandlers.BIOSAFETY_SCREEN_HANDLER,(h,i,t)->new EquipmentReadoutScreen<>(h,i,t));
    }
}
