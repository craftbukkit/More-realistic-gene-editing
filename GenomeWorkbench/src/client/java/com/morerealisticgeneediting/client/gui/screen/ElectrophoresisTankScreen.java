package com.morerealisticgeneediting.client.gui.screen;
import com.morerealisticgeneediting.client.gui.screen.LaboratoryScreen;
import com.morerealisticgeneediting.screen.ElectrophoresisTankScreenHandler;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
public class ElectrophoresisTankScreen extends LaboratoryScreen<ElectrophoresisTankScreenHandler> {
    public ElectrophoresisTankScreen(ElectrophoresisTankScreenHandler handler,PlayerInventory inventory,Text title){super(handler,inventory,title, "button.mrge.start");}
}
