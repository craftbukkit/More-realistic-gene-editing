package com.morerealisticgeneediting.client.gui.equipment;
import com.morerealisticgeneediting.screen.equipment.ThermalCyclerScreenHandler;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
public class ThermalCyclerScreen extends EquipmentReadoutScreen<ThermalCyclerScreenHandler> {
    public ThermalCyclerScreen(ThermalCyclerScreenHandler handler,PlayerInventory inventory,Text title){super(handler,inventory,title,"button.mrge.start","button.mrge.stop");}
}
