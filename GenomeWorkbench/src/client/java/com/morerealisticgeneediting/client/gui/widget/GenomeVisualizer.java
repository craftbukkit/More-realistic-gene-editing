package com.morerealisticgeneediting.client.gui.widget;
import com.morerealisticgeneediting.genome.Genome;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.util.math.MathHelper;
/** A bounded viewer of a supplied sample. DrawContext converts GUI scissor coordinates for every GUI scale. */
public class GenomeVisualizer implements Drawable,Element {
    private Genome genome;
    private final int x,y,width,height;
    private double scroll,zoom=10;
    private boolean dragging,focused;
    public GenomeVisualizer(int x,int y,int width,int height,Genome genome){this.x=x;this.y=y;this.width=width;this.height=height;this.genome=genome;}
    @Override public void render(DrawContext context,int mouseX,int mouseY,float delta){
        TextRenderer font=MinecraftClient.getInstance().textRenderer;
        context.fill(x,y,x+width,y+height,0xFF18313E);
        context.enableScissor(x,y,x+width,y+height);
        try {
            if(genome==null)return;
            long first=Math.max(0,(long)(scroll/zoom));
            int count=(int)Math.min(4096,Math.min(Math.ceil(width/zoom)+2,Math.max(0,genome.getTotalLength()-first)));
            if(count<=0)return;
            String sequence=genome.getSequence(first,count);
            for(int i=0;i<sequence.length();i++){
                int px=(int)(x+(first+i)*zoom-scroll);
                if((first+i)%10==0)context.drawText(font,Long.toString(first+i),px,y+3,0xD1E8EF,false);
                if(zoom>=6)context.drawText(font,String.valueOf(sequence.charAt(i)),px,y+25,0xFFFFFF,false);
            }
        }finally{context.disableScissor();}
    }
    @Override public boolean mouseClicked(double mx,double my,int button){dragging=button==0&&isMouseOver(mx,my);return dragging;}
    @Override public boolean mouseReleased(double mx,double my,int button){boolean was=dragging;dragging=false;return was;}
    @Override public boolean mouseDragged(double mx,double my,int button,double dx,double dy){if(!dragging)return false;scroll-=dx;clamp();return true;}
    @Override public boolean mouseScrolled(double mx,double my,double horizontal,double vertical){
        if(!isMouseOver(mx,my))return false;
        double old=zoom;zoom=MathHelper.clamp(zoom*Math.pow(1.1,vertical),0.2,50);scroll=(scroll+mx-x)*(zoom/old)-(mx-x);clamp();return true;
    }
    private void clamp(){scroll=MathHelper.clamp(scroll,0,Math.max(0,(genome==null?0:genome.getTotalLength()*zoom)-width));}
    @Override public boolean isMouseOver(double mx,double my){return mx>=x&&my>=y&&mx<x+width&&my<y+height;}
    @Override public void setFocused(boolean focused){this.focused=focused;}
    @Override public boolean isFocused(){return focused;}
    public void setGenome(Genome genome){this.genome=genome;scroll=0;zoom=10;}
}
