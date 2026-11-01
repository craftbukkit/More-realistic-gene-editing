package com.morerealisticgeneediting.screen;
import com.morerealisticgeneediting.ethics.EthicsCase;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
public class EthicsCaseScreen extends HandledScreen<EthicsCaseScreenHandler> {
    private EthicsCase.EthicsOption selected;
    private int textScroll;
    public EthicsCaseScreen(EthicsCaseScreenHandler handler,PlayerInventory inventory,Text title){super(handler,inventory,title);backgroundWidth=320;backgroundHeight=240;}
    @Override protected void init(){super.init();buildChoices();}
    private void buildChoices(){
        clearChildren();if(handler.ethicsCase==null)return;
        if(selected==null){
            for(int i=0;i<handler.ethicsCase.options().size();i++) {
                int choice=i;EthicsCase.EthicsOption option=handler.ethicsCase.options().get(i);
                addDrawableChild(ButtonWidget.builder(Text.literal(option.text()),b->{if(client!=null&&client.interactionManager!=null){client.interactionManager.clickButton(handler.syncId,choice);selected=option;textScroll=0;buildChoices();}}).tooltip(Tooltip.of(Text.literal(option.text()))).dimensions(x+8,y+110+i*25,304,20).build());
            }
        }else addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"),b->close()).dimensions(x+110,y+211,100,20).build());
    }
    @Override protected void drawBackground(DrawContext context,float delta,int mouseX,int mouseY){context.fill(x,y,x+backgroundWidth,y+backgroundHeight,0xFF18313E);}
    @Override protected void drawForeground(DrawContext context,int mouseX,int mouseY){
        if(handler.ethicsCase==null){context.drawText(textRenderer,Text.translatable("screen.mrge.no_case"),8,8,0xFFFFFF,false);return;}
        context.drawText(textRenderer,textRenderer.trimToWidth(handler.ethicsCase.title(),304),8,8,0xFFFFFF,false);
        context.enableScissor(x+8,y+28,x+312,y+(selected==null?102:200));
        try{context.drawTextWrapped(textRenderer,Text.literal(selected==null?handler.ethicsCase.description():selected.consequence()),8,28-textScroll,304,0xF1F6F9);}finally{context.disableScissor();}
    }
    @Override public boolean mouseScrolled(double mouseX,double mouseY,double horizontal,double vertical){
        if(handler.ethicsCase!=null && mouseX>=x+8 && mouseX<x+312 && mouseY>=y+28 && mouseY<y+(selected==null?102:200)){
            String content=selected==null?handler.ethicsCase.description():selected.consequence();
            int viewport=selected==null?74:172;
            int max=Math.max(0,textRenderer.wrapLines(Text.literal(content),304).size()*textRenderer.fontHeight-viewport);
            textScroll=Math.max(0,Math.min(max,textScroll-(int)(vertical*18)));return true;
        }
        return super.mouseScrolled(mouseX,mouseY,horizontal,vertical);
    }
    @Override public void render(DrawContext context,int mouseX,int mouseY,float delta){renderBackground(context,mouseX,mouseY,delta);super.render(context,mouseX,mouseY,delta);}
}
