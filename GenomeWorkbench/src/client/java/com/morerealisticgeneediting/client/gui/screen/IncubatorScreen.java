package com.morerealisticgeneediting.client.gui.screen;
import com.morerealisticgeneediting.client.gui.screen.LaboratoryScreen;
import com.morerealisticgeneediting.screen.IncubatorScreenHandler;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
public class IncubatorScreen extends LaboratoryScreen<IncubatorScreenHandler> {
    public IncubatorScreen(IncubatorScreenHandler handler,PlayerInventory inventory,Text title){super(handler,inventory,title, "button.mrge.start");}
}
