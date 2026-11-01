package com.morerealisticgeneediting.client.gui.equipment;
import com.morerealisticgeneediting.screen.equipment.CentrifugeScreenHandler;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
public class CentrifugeScreen extends EquipmentReadoutScreen<CentrifugeScreenHandler> {
    public CentrifugeScreen(CentrifugeScreenHandler handler,PlayerInventory inventory,Text title){super(handler,inventory,title);}
}
