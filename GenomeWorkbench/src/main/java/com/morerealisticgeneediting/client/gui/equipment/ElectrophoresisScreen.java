package com.morerealisticgeneediting.client.gui.equipment;

import com.morerealisticgeneediting.MoreRealisticGeneEditing;
import com.morerealisticgeneediting.screen.equipment.ElectrophoresisScreenHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * GUI Screen for Electrophoresis equipment
 */
public class ElectrophoresisScreen extends HandledScreen<ElectrophoresisScreenHandler> {
    
    private static final Identifier TEXTURE = Identifier.of(MoreRealisticGeneEditing.MOD_ID,
            "textures/gui/electrophoresis_gui.png");
    
    public ElectrophoresisScreen(ElectrophoresisScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundHeight = 166;
        this.backgroundWidth = 176;
    }
    
    @Override
    protected void init() {
        super.init();
        titleX = (backgroundWidth - textRenderer.getWidth(title)) / 2;
    }
    
    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int x = (width - backgroundWidth) / 2;
        int y = (height - backgroundHeight) / 2;
        
        context.drawTexture(TEXTURE, x, y, 0, 0, backgroundWidth, backgroundHeight);
        
        // Draw gel visualization
        if (handler.isRunning()) {
            drawGelVisualization(context, x + 75, y + 17);
        }
        
        // Draw progress bar
        int progress = (int)(handler.getProgressPercent() * 70);
        context.fill(x + 75, y + 60, x + 75 + progress, y + 65, 0xFF4CAF50);
    }
    
    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        context.drawText(textRenderer, title, titleX, titleY, 0x404040, false);
        context.drawText(textRenderer, playerInventoryTitle, playerInventoryTitleX, playerInventoryTitleY, 0x404040, false);
        
        // Draw voltage
        String voltageText = handler.getVoltage() + "V";
        context.drawText(textRenderer, voltageText, 130, 17, 0x404040, false);
        
        // Draw gel percentage
        String gelText = handler.getGelPercent() + "%";
        context.drawText(textRenderer, gelText, 130, 27, 0x404040, false);
        
        // Draw run time
        int time = handler.getRunTime();
        String timeText = (time / 60) + ":" + String.format("%02d", time % 60);
        context.drawText(textRenderer, timeText, 130, 37, 0x404040, false);
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        drawMouseoverTooltip(context, mouseX, mouseY);
    }
    
    private void drawGelVisualization(DrawContext context, int x, int y) {
        // Gel background (agarose gel appearance)
        context.fill(x, y, x + 70, y + 40, 0xFF224466);
        
        // Draw wells at top
        for (int i = 0; i < 6; i++) {
            context.fill(x + 5 + i * 11, y + 2, x + 12 + i * 11, y + 6, 0xFF113355);
        }
        
        // Draw bands based on progress (simplified visualization)
        float progress = handler.getProgressPercent();
        if (progress > 0) {
            // Simulate DNA bands migrating down
            int bandY = y + 8 + (int)(progress * 28);
            
            // Draw ladder bands (first lane)
            drawLadderBands(context, x + 5, y + 8, progress);
            
            // Draw sample bands (lanes 2-6)
            for (int lane = 1; lane < 6; lane++) {
                int laneX = x + 5 + lane * 11;
                // Simulate different fragment sizes
                int bandOffset = (int)(Math.sin(lane * 1.5) * 5);
                context.fill(laneX, bandY + bandOffset, laneX + 7, bandY + bandOffset + 3, 0xFF88FF88);
            }
        }
    }
    
    private void drawLadderBands(DrawContext context, int x, int startY, float progress) {
        // Simulate DNA ladder with multiple bands
        int[] bandSizes = {100, 200, 300, 400, 500, 750, 1000, 1500, 2000, 3000};
        
        for (int size : bandSizes) {
            // Smaller fragments migrate faster
            float migrationFactor = 1.0f / (float)Math.log10(size + 1);
            int bandY = startY + (int)(progress * 30 * migrationFactor);
            
            if (bandY < startY + 35) {
                // Brighter bands for reference sizes
                int brightness = (size == 500 || size == 1000) ? 0xFFFFFF88 : 0xFFCCCC66;
                context.fill(x, bandY, x + 7, bandY + 2, brightness);
            }
        }
    }
}
