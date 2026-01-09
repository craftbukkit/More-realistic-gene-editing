package com.morerealisticgeneediting.client.gui.equipment;

import com.morerealisticgeneediting.MoreRealisticGeneEditing;
import com.morerealisticgeneediting.screen.equipment.CentrifugeScreenHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * GUI Screen for Centrifuge equipment
 */
public class CentrifugeScreen extends HandledScreen<CentrifugeScreenHandler> {
    
    private static final Identifier TEXTURE = Identifier.of(MoreRealisticGeneEditing.MOD_ID, 
            "textures/gui/centrifuge_gui.png");
    
    // Tier-specific colors
    private static final int BASIC_COLOR = 0xFF4CAF50;
    private static final int ADVANCED_COLOR = 0xFF2196F3;
    private static final int ELITE_COLOR = 0xFF9C27B0;
    
    public CentrifugeScreen(CentrifugeScreenHandler handler, PlayerInventory inventory, Text title) {
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
        if (handler.isProcessing()) {
            int progress = (int) (handler.getProgressPercent() * 24);
            context.drawTexture(TEXTURE, x + 79, y + 34, 176, 0, progress + 1, 17);
        }
        
        // Draw RPM indicator
        drawRpmGauge(context, x + 7, y + 17, handler.getRpm(), handler.getTargetRpm(), getTierMaxRpm());
        
        // Draw temperature indicator (for advanced+ tiers)
        if (handler.getTier() >= 2) {
            drawTemperatureBar(context, x + 7, y + 55, handler.getTemperature());
        }
    }
    
    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        context.drawText(textRenderer, title, titleX, titleY, 0x404040, false);
        context.drawText(textRenderer, playerInventoryTitle, playerInventoryTitleX, playerInventoryTitleY, 0x404040, false);
        
        // Draw status text
        int tier = handler.getTier();
        String tierName = switch (tier) {
            case 1 -> "Basic";
            case 2 -> "Advanced";
            case 3 -> "Elite";
            default -> "Unknown";
        };
        
        context.drawText(textRenderer, tierName, 130, 6, getTierColor(), false);
        
        // Draw RPM text
        String rpmText = handler.getRpm() + " RPM";
        context.drawText(textRenderer, rpmText, 100, 58, 0x404040, false);
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        drawMouseoverTooltip(context, mouseX, mouseY);
    }
    
    private void drawRpmGauge(DrawContext context, int x, int y, int rpm, int targetRpm, int maxRpm) {
        // Background
        context.fill(x, y, x + 40, y + 30, 0xFF333333);
        
        // RPM bar
        int barHeight = (int) ((rpm / (float) maxRpm) * 26);
        int color = rpm > maxRpm * 0.8 ? 0xFFFF5555 : getTierColor();
        context.fill(x + 2, y + 28 - barHeight, x + 38, y + 28, color);
        
        // Target line
        int targetY = y + 28 - (int) ((targetRpm / (float) maxRpm) * 26);
        context.fill(x, targetY, x + 40, targetY + 1, 0xFFFFFF00);
    }
    
    private void drawTemperatureBar(DrawContext context, int x, int y, int temp) {
        // Temperature scale from -20 to 40°C
        int normalizedTemp = Math.max(-20, Math.min(40, temp));
        int barWidth = (int) (((normalizedTemp + 20) / 60.0f) * 40);
        
        // Background
        context.fill(x, y, x + 40, y + 8, 0xFF333333);
        
        // Temperature bar - blue for cold, red for hot
        int color = temp < 4 ? 0xFF4488FF : (temp > 30 ? 0xFFFF4444 : 0xFF44FF44);
        context.fill(x + 2, y + 2, x + 2 + barWidth, y + 6, color);
        
        // Temperature text
        String tempText = temp + "°C";
        context.drawText(textRenderer, tempText, x + 45, y, 0x404040, false);
    }
    
    private int getTierColor() {
        return switch (handler.getTier()) {
            case 1 -> BASIC_COLOR;
            case 2 -> ADVANCED_COLOR;
            case 3 -> ELITE_COLOR;
            default -> 0xFFFFFFFF;
        };
    }
    
    private int getTierMaxRpm() {
        return switch (handler.getTier()) {
            case 1 -> 6000;
            case 2 -> 20000;
            case 3 -> 100000;
            default -> 6000;
        };
    }
}
