package com.morerealisticgeneediting.client.gui.screen;
import com.morerealisticgeneediting.client.gui.screen.LaboratoryScreen;
import com.morerealisticgeneediting.screen.PcrMachineScreenHandler;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
public class PcrMachineScreen extends LaboratoryScreen<PcrMachineScreenHandler> {
    public PcrMachineScreen(PcrMachineScreenHandler handler,PlayerInventory inventory,Text title){super(handler,inventory,title, "button.mrge.start");}
}
