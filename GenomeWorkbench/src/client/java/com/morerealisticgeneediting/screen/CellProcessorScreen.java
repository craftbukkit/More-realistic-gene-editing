package com.morerealisticgeneediting.screen;
import com.morerealisticgeneediting.client.gui.screen.LaboratoryScreen;
import com.morerealisticgeneediting.screen.CellProcessorScreenHandler;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
public class CellProcessorScreen extends LaboratoryScreen<CellProcessorScreenHandler> {
    public CellProcessorScreen(CellProcessorScreenHandler handler,PlayerInventory inventory,Text title){super(handler,inventory,title, "button.mrge.start");}
}
