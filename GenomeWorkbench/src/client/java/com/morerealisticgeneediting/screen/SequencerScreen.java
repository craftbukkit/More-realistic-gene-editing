package com.morerealisticgeneediting.screen;
import com.morerealisticgeneediting.client.gui.screen.LaboratoryScreen;
import com.morerealisticgeneediting.screen.SequencerScreenHandler;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
public class SequencerScreen extends LaboratoryScreen<SequencerScreenHandler> {
    public SequencerScreen(SequencerScreenHandler handler,PlayerInventory inventory,Text title){super(handler,inventory,title, "button.mrge.start");}
}
