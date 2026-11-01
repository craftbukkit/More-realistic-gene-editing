package com.morerealisticgeneediting.client.gui.equipment;

import com.morerealisticgeneediting.client.gui.screen.LaboratoryScreen;
import com.morerealisticgeneediting.screen.equipment.ElectrophoresisScreenHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;

/** Only completed, server-synchronized virtual migration positions produce visible bands. */
public class ElectrophoresisScreen extends LaboratoryScreen<ElectrophoresisScreenHandler> {
    public ElectrophoresisScreen(ElectrophoresisScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title, "button.mrge.start", "button.mrge.reset");
        backgroundWidth = 390;
    }
    @Override protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        super.drawForeground(context, mouseX, mouseY);
        context.drawText(textRenderer, handler.getVoltage() + " V · " + handler.getGelPercent() + "% · " + Math.round(handler.getProgressPercent() * 100) + "%", 185, 25, 0x183E4E, false);
        if (handler.getPhase() != 6) {
            context.drawTextWrapped(textRenderer, Text.translatable("screen.mrge.waiting_bands"), 185, 44, 195, 0x183E4E);
            return;
        }
        context.fill(185, 50, 383, 151, 0xFF102C38);
        for (int lane = 0; lane < ElectrophoresisScreenHandler.LANES; lane++) {
            int left = 188 + lane * 21;
            context.drawText(textRenderer, lane == 0 ? "L" : Integer.toString(lane), left + 4, 39, 0x183E4E, false);
            for (int band = 0; band < handler.getBandCount(lane); band++) {
                float position = handler.getBandPosition(lane, band);
                if (!Float.isFinite(position) || position < 0 || position > 100) continue;
                int top = 51 + Math.round(position * 0.97f);
                context.fill(left, top, left + 16, top + 2, 0xFF74E6ED);
            }
        }
        context.drawText(textRenderer, textRenderer.trimToWidth(Text.translatable("screen.mrge.virtual_gel").getString(), 195), 185, 154, 0x183E4E, false);
    }
}
