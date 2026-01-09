package com.morerealisticgeneediting.client.gui.equipment;

import com.morerealisticgeneediting.MoreRealisticGeneEditing;
import com.morerealisticgeneediting.screen.equipment.ThermalCyclerScreenHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * GUI Screen for Thermal Cycler / PCR equipment
 */
public class ThermalCyclerScreen extends HandledScreen<ThermalCyclerScreenHandler> {
    
    private static final Identifier TEXTURE = Identifier.of(MoreRealisticGeneEditing.MOD_ID,
            "textures/gui/thermal_cycler_gui.png");
    
    public ThermalCyclerScreen(ThermalCyclerScreenHandler handler, PlayerInventory inventory, Text title) {
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
        
        // Draw progress arrow
        if (handler.isRunning()) {
            int progress = (int) (handler.getCycleProgress() * 24);
            context.drawTexture(TEXTURE, x + 79, y + 34, 176, 0, progress + 1, 17);
        }
        
        // Draw temperature graph
        drawTemperatureGraph(context, x + 7, y + 7);
        
        // Draw cycle counter
        drawCycleCounter(context, x + 130, y + 7);
    }
    
    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        context.drawText(textRenderer, title, titleX, titleY, 0x404040, false);
        context.drawText(textRenderer, playerInventoryTitle, playerInventoryTitleX, playerInventoryTitleY, 0x404040, false);
        
        // Draw current step name
        String stepName = handler.getCurrentStepName();
        int stepColor = getStepColor(handler.getCurrentStep());
        context.drawText(textRenderer, stepName, 100, 55, stepColor, false);
        
        // Draw temperature
        String tempText = String.format("%.1f°C", handler.getCurrentTemperature());
        context.drawText(textRenderer, tempText, 100, 65, 0x404040, false);
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        drawMouseoverTooltip(context, mouseX, mouseY);
    }
    
    private void drawTemperatureGraph(DrawContext context, int x, int y) {
        // Background
        context.fill(x, y, x + 50, y + 50, 0xFF222222);
        
        // Temperature zones with colors
        // Denaturation zone (94-98°C) - red
        int denatY = y + 5;
        context.fill(x + 2, denatY, x + 48, denatY + 10, 0x44FF4444);
        
        // Extension zone (72°C) - green  
        int extY = y + 20;
        context.fill(x + 2, extY, x + 48, extY + 10, 0x4444FF44);
        
        // Annealing zone (50-65°C) - blue
        int annealY = y + 35;
        context.fill(x + 2, annealY, x + 48, annealY + 10, 0x444488FF);
        
        // Current temperature indicator
        float currentTemp = handler.getCurrentTemperature();
        int tempY = y + 45 - (int)((currentTemp - 20) / 80.0f * 40);
        tempY = Math.max(y + 2, Math.min(y + 48, tempY));
        context.fill(x, tempY - 1, x + 50, tempY + 1, 0xFFFFFF00);
        
        // Labels
        context.drawText(textRenderer, "D", x + 52, denatY + 1, 0xFFFF4444, false);
        context.drawText(textRenderer, "E", x + 52, extY + 1, 0xFF44FF44, false);
        context.drawText(textRenderer, "A", x + 52, annealY + 1, 0xFF4488FF, false);
    }
    
    private void drawCycleCounter(DrawContext context, int x, int y) {
        // Cycle progress circle
        int currentCycle = handler.getCurrentCycle();
        int totalCycles = handler.getTotalCycles();
        
        // Background circle
        context.fill(x, y, x + 40, y + 40, 0xFF333333);
        
        // Progress arc (simplified as filled rectangle for now)
        float progress = handler.getCycleProgress();
        int progressHeight = (int)(progress * 36);
        context.fill(x + 2, y + 38 - progressHeight, x + 38, y + 38, 0xFF4CAF50);
        
        // Cycle text
        String cycleText = currentCycle + "/" + totalCycles;
        int textWidth = textRenderer.getWidth(cycleText);
        context.drawText(textRenderer, cycleText, x + 20 - textWidth / 2, y + 16, 0xFFFFFFFF, false);
    }
    
    private int getStepColor(int step) {
        return switch (step) {
            case 1 -> 0xFFFF4444; // Denaturation - red
            case 2 -> 0xFF4488FF; // Annealing - blue
            case 3 -> 0xFF44FF44; // Extension - green
            case 4 -> 0xFF44FF44; // Final extension - green
            default -> 0xFF888888; // Idle - gray
        };
    }
}
