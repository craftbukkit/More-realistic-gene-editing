package com.morerealisticgeneediting.client.gui.equipment;

import com.morerealisticgeneediting.MoreRealisticGeneEditing;
import com.morerealisticgeneediting.block.entity.ThermalCyclerBlockEntityV2;
import com.morerealisticgeneediting.screen.equipment.ThermalCyclerScreenHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * Thermal Cycler GUI Screen
 * 
 * Features:
 * - Temperature display with color coding
 * - Cycle counter
 * - Phase indicator
 * - PCR protocol configuration
 * - qPCR curve display (T3 only)
 */
public class ThermalCyclerScreenV2 extends HandledScreen<ThermalCyclerScreenHandler> {

    private static final Identifier TEXTURE = new Identifier(MoreRealisticGeneEditing.MOD_ID,
        "textures/gui/thermal_cycler_gui.png");

    // GUI dimensions
    private static final int GUI_WIDTH = 256;
    private static final int GUI_HEIGHT = 200;
    
    // Element positions
    private static final int TEMP_DISPLAY_X = 150;
    private static final int TEMP_DISPLAY_Y = 20;
    private static final int CYCLE_DISPLAY_X = 150;
    private static final int CYCLE_DISPLAY_Y = 50;
    private static final int PHASE_DISPLAY_X = 150;
    private static final int PHASE_DISPLAY_Y = 70;
    
    // qPCR curve display
    private static final int CURVE_X = 160;
    private static final int CURVE_Y = 90;
    private static final int CURVE_WIDTH = 80;
    private static final int CURVE_HEIGHT = 50;

    private final ThermalCyclerBlockEntityV2 blockEntity;
    
    // Widgets
    private ButtonWidget startButton;
    private ButtonWidget stopButton;

    public ThermalCyclerScreenV2(ThermalCyclerScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.blockEntity = handler.getBlockEntity();
        this.backgroundWidth = GUI_WIDTH;
        this.backgroundHeight = GUI_HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        
        int x = (width - backgroundWidth) / 2;
        int y = (height - backgroundHeight) / 2;

        // Start button
        startButton = ButtonWidget.builder(Text.literal("Start PCR"), button -> {
            // Send packet to start PCR
            sendStartPacket();
        }).dimensions(x + 10, y + 100, 60, 20).build();
        addDrawableChild(startButton);

        // Stop button
        stopButton = ButtonWidget.builder(Text.literal("Stop"), button -> {
            // Send packet to stop PCR
            sendStopPacket();
        }).dimensions(x + 75, y + 100, 60, 20).build();
        addDrawableChild(stopButton);
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        // Draw background texture
        int x = (width - backgroundWidth) / 2;
        int y = (height - backgroundHeight) / 2;
        
        context.fill(x, y, x + backgroundWidth, y + backgroundHeight, 0xFF404040);
        context.fill(x + 2, y + 2, x + backgroundWidth - 2, y + backgroundHeight - 2, 0xFF303030);

        // Draw slot backgrounds
        drawSlotBackgrounds(context, x, y);

        // Draw temperature indicator
        drawTemperatureDisplay(context, x, y);

        // Draw cycle counter
        drawCycleDisplay(context, x, y);

        // Draw phase indicator
        drawPhaseDisplay(context, x, y);

        // Draw qPCR curve (if applicable)
        if (blockEntity.isRealTimeMode()) {
            drawQPCRCurve(context, x, y);
        }

        // Draw heating block visualization
        drawHeatingBlock(context, x, y);
    }

    private void drawSlotBackgrounds(DrawContext context, int x, int y) {
        int slotBg = 0xFF505050;
        int slotBorder = 0xFF707070;
        
        // Input slots
        String[] slotLabels = {"Template", "Primer F", "Primer R", "Polymerase", "dNTPs", "Output", "Buffer", "Tube"};
        int slotX = x + 8;
        int slotY = y + 20;
        
        for (int i = 0; i < 8; i++) {
            int sx = slotX + (i % 4) * 22;
            int sy = slotY + (i / 4) * 40;
            
            context.fill(sx - 1, sy - 1, sx + 17, sy + 17, slotBorder);
            context.fill(sx, sy, sx + 16, sy + 16, slotBg);
            
            // Label
            context.drawText(textRenderer, slotLabels[i].substring(0, Math.min(3, slotLabels[i].length())), 
                sx, sy + 18, 0xAAAAAA, false);
        }
    }

    private void drawTemperatureDisplay(DrawContext context, int x, int y) {
        float currentTemp = blockEntity.getCurrentTemp();
        float targetTemp = blockEntity.getTargetTemp();
        
        // Temperature box
        int tx = x + TEMP_DISPLAY_X;
        int ty = y + TEMP_DISPLAY_Y;
        
        context.fill(tx, ty, tx + 90, ty + 25, 0xFF202020);
        
        // Temperature color based on value
        int tempColor = getTemperatureColor(currentTemp);
        
        // Current temperature
        String tempText = String.format("%.1f°C", currentTemp);
        context.drawText(textRenderer, tempText, tx + 5, ty + 4, tempColor, false);
        
        // Target temperature
        String targetText = String.format("→ %.0f°C", targetTemp);
        context.drawText(textRenderer, targetText, tx + 5, ty + 14, 0x888888, false);
        
        // Temperature bar
        int barWidth = (int) (80 * (currentTemp / 100.0f));
        context.fill(tx + 5, ty + 22, tx + 5 + barWidth, ty + 24, tempColor);
    }

    private int getTemperatureColor(float temp) {
        if (temp >= 90) return 0xFFFF3333;      // Red (denature)
        if (temp >= 70) return 0xFFFFAA33;      // Orange (extension)
        if (temp >= 50) return 0xFFFFFF33;      // Yellow (annealing)
        if (temp >= 30) return 0xFF33FF33;      // Green (room temp)
        return 0xFF3333FF;                       // Blue (cold)
    }

    private void drawCycleDisplay(DrawContext context, int x, int y) {
        int cx = x + CYCLE_DISPLAY_X;
        int cy = y + CYCLE_DISPLAY_Y;
        
        int currentCycle = blockEntity.getCurrentCycle();
        int totalCycles = blockEntity.getTotalCycles();
        
        String cycleText = String.format("Cycle: %d / %d", currentCycle, totalCycles);
        context.drawText(textRenderer, cycleText, cx, cy, 0xFFFFFF, false);
        
        // Progress bar
        int progressWidth = (int) (80 * ((float) currentCycle / totalCycles));
        context.fill(cx, cy + 10, cx + 80, cy + 14, 0xFF404040);
        context.fill(cx, cy + 10, cx + progressWidth, cy + 14, 0xFF00FF00);
    }

    private void drawPhaseDisplay(DrawContext context, int x, int y) {
        int px = x + PHASE_DISPLAY_X;
        int py = y + PHASE_DISPLAY_Y;
        
        ThermalCyclerBlockEntityV2.PcrPhase phase = blockEntity.getCurrentPhase();
        String phaseText = getPhaseDisplayName(phase);
        int phaseColor = getPhaseColor(phase);
        
        context.drawText(textRenderer, "Phase: " + phaseText, px, py, phaseColor, false);
    }

    private String getPhaseDisplayName(ThermalCyclerBlockEntityV2.PcrPhase phase) {
        return switch (phase) {
            case IDLE -> "Idle";
            case INITIAL_DENATURATION -> "Init. Denature";
            case DENATURATION -> "Denature (95°C)";
            case ANNEALING -> "Annealing";
            case EXTENSION -> "Extension (72°C)";
            case FINAL_EXTENSION -> "Final Ext.";
            case HOLD -> "Hold (4°C)";
            case COMPLETE -> "Complete";
        };
    }

    private int getPhaseColor(ThermalCyclerBlockEntityV2.PcrPhase phase) {
        return switch (phase) {
            case IDLE -> 0x888888;
            case INITIAL_DENATURATION, DENATURATION -> 0xFF3333;
            case ANNEALING -> 0xFFFF33;
            case EXTENSION, FINAL_EXTENSION -> 0xFFAA33;
            case HOLD -> 0x3333FF;
            case COMPLETE -> 0x33FF33;
        };
    }

    private void drawQPCRCurve(DrawContext context, int x, int y) {
        int cx = x + CURVE_X;
        int cy = y + CURVE_Y;
        
        // Background
        context.fill(cx, cy, cx + CURVE_WIDTH, cy + CURVE_HEIGHT, 0xFF101010);
        context.drawBorder(context, cx, cy, CURVE_WIDTH, CURVE_HEIGHT, 0xFF404040);
        
        // Title
        context.drawText(textRenderer, "qPCR", cx + 2, cy + 2, 0xFFFFFF, false);
        
        // Draw curve
        float[] data = blockEntity.getFluorescenceData();
        if (data != null && data.length > 0) {
            // Find max for scaling
            float maxF = 1;
            for (float f : data) {
                if (f > maxF) maxF = f;
            }
            
            // Draw data points
            int pointsToShow = Math.min(data.length, blockEntity.getCurrentCycle() + 1);
            for (int i = 1; i < pointsToShow; i++) {
                int x1 = cx + (i - 1) * CURVE_WIDTH / data.length;
                int x2 = cx + i * CURVE_WIDTH / data.length;
                int y1 = cy + CURVE_HEIGHT - (int) ((data[i - 1] / maxF) * (CURVE_HEIGHT - 10));
                int y2 = cy + CURVE_HEIGHT - (int) ((data[i] / maxF) * (CURVE_HEIGHT - 10));
                
                context.fill(x2 - 1, y2 - 1, x2 + 1, y2 + 1, 0xFF00FF00);
            }
        }
    }

    private void drawHeatingBlock(DrawContext context, int x, int y) {
        // Visual representation of the thermal block
        int bx = x + 10;
        int by = y + 130;
        int bw = 130;
        int bh = 50;
        
        // Block body
        int blockColor = getTemperatureColor(blockEntity.getCurrentTemp());
        context.fill(bx, by, bx + bw, by + bh, blockColor);
        
        // Well positions (simplified 8-strip representation)
        for (int i = 0; i < 8; i++) {
            int wx = bx + 10 + i * 15;
            int wy = by + 10;
            context.fill(wx, wy, wx + 12, wy + 30, 0xFF202020);
        }
        
        // Label
        context.drawText(textRenderer, "Heating Block", bx, by + bh + 2, 0xAAAAAA, false);
    }

    private void drawBorder(DrawContext context, int x, int y, int width, int height, int color) {
        context.fill(x, y, x + width, y + 1, color);
        context.fill(x, y + height - 1, x + width, y + height, color);
        context.fill(x, y, x + 1, y + height, color);
        context.fill(x + width - 1, y, x + width, y + height, color);
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        context.drawText(textRenderer, title, titleX, titleY, 0xFFFFFF, false);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        drawMouseoverTooltip(context, mouseX, mouseY);
        
        // Update button states
        boolean running = blockEntity.getCurrentPhase() != ThermalCyclerBlockEntityV2.PcrPhase.IDLE &&
                         blockEntity.getCurrentPhase() != ThermalCyclerBlockEntityV2.PcrPhase.COMPLETE;
        startButton.active = !running;
        stopButton.active = running;
    }

    private void sendStartPacket() {
        // TODO: Send C2S packet to start PCR
    }

    private void sendStopPacket() {
        // TODO: Send C2S packet to stop PCR
    }
}
