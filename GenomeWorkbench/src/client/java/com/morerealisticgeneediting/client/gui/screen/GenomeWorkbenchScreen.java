package com.morerealisticgeneediting.client.gui.screen;
import com.morerealisticgeneediting.client.gui.widget.GenomeVisualizer;
import com.morerealisticgeneediting.genome.Genome;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
/** Optional read-only viewer. Gameplay opens the server-owned terminal session instead. */
public class GenomeWorkbenchScreen extends Screen {
    private final Genome genome;
    private GenomeVisualizer viewer;
    public GenomeWorkbenchScreen(){this(null);}
    public GenomeWorkbenchScreen(Genome genome){super(Text.translatable("screen.mrge.genome_title"));this.genome=genome;}
    @Override protected void init(){viewer=new GenomeVisualizer(10,28,width-20,height-40,genome);addSelectableChild(viewer);}
    @Override public void render(DrawContext context,int mouseX,int mouseY,float delta){renderBackground(context,mouseX,mouseY,delta);viewer.render(context,mouseX,mouseY,delta);context.drawCenteredTextWithShadow(textRenderer,title,width/2,8,0xFFFFFF);super.render(context,mouseX,mouseY,delta);}
    @Override public boolean shouldPause(){return false;}
}
