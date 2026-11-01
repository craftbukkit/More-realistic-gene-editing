package com.morerealisticgeneediting.client.gui.equipment;
import com.morerealisticgeneediting.client.gui.screen.LaboratoryScreen;
import com.morerealisticgeneediting.screen.equipment.*;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import java.util.*;
/** Readouts use synchronized menu properties; absent measurements are not drawn as observed values. */
public class EquipmentReadoutScreen<T extends ScreenHandler> extends LaboratoryScreen<T> {
    public EquipmentReadoutScreen(T handler,PlayerInventory inventory,Text title,String... actions){super(handler,inventory,title,actions);backgroundWidth=Math.max(backgroundWidth,320);}
    private String number(double value,String unit){return Double.isFinite(value)&&value>=0?String.format(Locale.ROOT,"%.2f %s",value,unit):Text.translatable("screen.mrge.not_observed").getString();}
    @Override protected void drawForeground(DrawContext context,int mouseX,int mouseY){
        super.drawForeground(context,mouseX,mouseY);
        List<String> lines=new ArrayList<>();
        if(handler instanceof CentrifugeScreenHandler h){lines.add(h.getRpm()+" RPM");lines.add(number(h.getTemperature(),"°C"));lines.add(Math.round(h.getProgressPercent()*100)+"%");}
        else if(handler instanceof ThermalCyclerScreenHandler h){lines.add(number(h.getCurrentTemperature(),"°C"));lines.add(h.getCurrentCycle()+" / "+h.getTotalCycles());lines.add(h.getCurrentStepName());}
        else if(handler instanceof ElectrophoresisScreenHandler h){lines.add(h.getVoltage()+" V");lines.add(number(h.getGelPercent(),"%"));lines.add(Math.round(h.getProgressPercent()*100)+"%");lines.add(Text.translatable("screen.mrge.results_on_report").getString());}
        else if(handler instanceof SpectrophotometerScreenHandler h){lines.add(h.getWavelength()+" nm");lines.add("A: "+number(h.getAbsorbance(),""));lines.add(number(h.getConcentration(),"ng/µL"));}
        else if(handler instanceof ElectroporatorScreenHandler h){lines.add(Math.round(h.getProgressPercent()*100)+"%");lines.add(Text.translatable("screen.mrge.results_on_report").getString());}
        else if(handler instanceof com.morerealisticgeneediting.screen.equipment.SequencerScreenHandler h){lines.add(Math.round(h.getProgressPercent()*100)+"%");lines.add("Reads: "+h.getReadsGenerated());lines.add("Q: "+number(h.getQualityScore(),""));lines.add(Text.translatable("screen.mrge.results_on_report").getString());}
        else if(handler instanceof IncubatorEquipmentScreenHandler h){lines.add(number(h.getTemperature(),"°C"));lines.add(number(h.getCO2Level(),"% CO₂"));lines.add(Math.round(h.getProgressPercent()*100)+"%");}
        else if(handler instanceof BiosafetyScreenHandler h){lines.add("HEPA: "+h.getHepaStatus());lines.add(Text.translatable("screen.mrge.teaching_model").getString());}
        int row=27;for(String line:lines){context.drawText(textRenderer,textRenderer.trimToWidth(line,128),185,row,0x183E4E,false);row+=14;}
    }
}
