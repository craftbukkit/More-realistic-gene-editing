package com.morerealisticgeneediting.screens;
import com.morerealisticgeneediting.network.c2s.*;
import com.morerealisticgeneediting.security.GenomeRequestPolicy;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
public class GeneInsertionScreen extends HandledScreen<GeneInsertionScreenHandler> {
    private TextFieldWidget sequence;
    public GeneInsertionScreen(GeneInsertionScreenHandler handler,PlayerInventory inventory,Text title){super(handler,inventory,title);backgroundWidth=320;backgroundHeight=150;}
    @Override protected void init(){
        super.init();
        sequence=new TextFieldWidget(textRenderer,x+8,y+44,304,20,Text.translatable("screen.mrge.donor"));sequence.setMaxLength(GenomeRequestPolicy.MAX_TEMPLATE);addDrawableChild(sequence);
        addDrawableChild(ButtonWidget.builder(Text.translatable("button.mrge.template_edit"),b->{if(GenomeRequestPolicy.validDna(sequence.getText(),GenomeRequestPolicy.MAX_TEMPLATE))ClientPlayNetworking.send(new C2SPerformGeneInsertionPacket(handler.genomeId,handler.syncId,handler.revision,handler.getKnockoutPosition(),sequence.getText()));}).dimensions(x+8,y+72,148,20).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("button.mrge.indel_edit"),b->ClientPlayNetworking.send(new C2SPerformGeneKnockoutPacket(handler.genomeId,handler.syncId,handler.revision,handler.getKnockoutPosition()))).dimensions(x+164,y+72,148,20).build());
    }
    @Override protected void drawBackground(DrawContext context,float delta,int mouseX,int mouseY){context.fill(x,y,x+backgroundWidth,y+backgroundHeight,0xFFDFE6E9);context.fill(x,y,x+backgroundWidth,y+18,0xFF183E4E);}
    @Override protected void drawForeground(DrawContext context,int mouseX,int mouseY){
        context.drawText(textRenderer,title,8,5,0xFFFFFF,false);
        context.drawText(textRenderer,"SpCas9 (+) PAM: "+handler.getKnockoutPosition()+" · r"+handler.revision,8,27,0x183E4E,false);
        context.drawTextWrapped(textRenderer,Text.translatable("screen.mrge.edit_materials"),8,100,304,0x183E4E);
    }
    @Override public void render(DrawContext context,int mouseX,int mouseY,float delta){renderBackground(context,mouseX,mouseY,delta);super.render(context,mouseX,mouseY,delta);}
}
