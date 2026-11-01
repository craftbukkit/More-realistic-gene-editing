package com.morerealisticgeneediting.client.gui.screen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
/** Texture-independent lab panel: inventory positions remain the server menu's source of truth. */
public class LaboratoryScreen<T extends ScreenHandler> extends HandledScreen<T> {
    private final String[] actions;
    public LaboratoryScreen(T handler,PlayerInventory inventory,Text title,String... actions) {
        super(handler,inventory,title);this.actions=actions;
        this.backgroundWidth=Math.max(176,handler.slots.stream().mapToInt(s->s.x+24).max().orElse(176));
        this.backgroundHeight=Math.max(166,handler.slots.stream().mapToInt(s->s.y+24).max().orElse(166));
    }
    @Override protected void init() {
        super.init();
        for(int i=0;i<actions.length;i++) {
            int id=i;
            addDrawableChild(ButtonWidget.builder(Text.translatable(actions[i]),b->{if(client!=null&&client.interactionManager!=null)client.interactionManager.clickButton(handler.syncId,id);})
                .dimensions(x+i*70,y-24,68,20).build());
        }
    }
    @Override protected void drawBackground(DrawContext context,float delta,int mouseX,int mouseY) {
        context.fill(x,y,x+backgroundWidth,y+backgroundHeight,0xFFDFE6E9);
        context.fill(x,y,x+backgroundWidth,y+17,0xFF183E4E);
        for(Slot slot:handler.slots){context.fill(x+slot.x-1,y+slot.y-1,x+slot.x+17,y+slot.y+17,0xFF627680);context.fill(x+slot.x,y+slot.y,x+slot.x+16,y+slot.y+16,0xFFAEBCC3);}
    }
    @Override protected void drawForeground(DrawContext context,int mouseX,int mouseY) {
        context.drawText(textRenderer,title,8,5,0xFFFFFF,false);
    }
    @Override public void render(DrawContext context,int mouseX,int mouseY,float delta) {
        renderBackground(context,mouseX,mouseY,delta);super.render(context,mouseX,mouseY,delta);drawMouseoverTooltip(context,mouseX,mouseY);
    }
}
