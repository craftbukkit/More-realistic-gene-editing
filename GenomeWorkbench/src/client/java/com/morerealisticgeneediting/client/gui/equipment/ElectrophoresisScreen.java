package com.morerealisticgeneediting.client.gui.equipment;
import com.morerealisticgeneediting.screen.equipment.ElectrophoresisScreenHandler;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
public class ElectrophoresisScreen extends EquipmentReadoutScreen<ElectrophoresisScreenHandler> {
    public ElectrophoresisScreen(ElectrophoresisScreenHandler handler,PlayerInventory inventory,Text title){super(handler,inventory,title,"button.mrge.start","button.mrge.stop");}
}
