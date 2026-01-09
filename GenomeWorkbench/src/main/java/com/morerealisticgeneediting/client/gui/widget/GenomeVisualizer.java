package com.morerealisticgeneediting.client.gui.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import com.morerealisticgeneediting.genome.Genome;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.Element;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Matrix4f;

public class GenomeVisualizer extends DrawableHelper implements Drawable, Element {

    private Genome genome;
    private final TextRenderer textRenderer;

    private double scrollX = 0;
    private double zoom = 10.0; // 1 character = 10 pixels

    private final int x, y, width, height;
    private boolean isDragging = false;

    private static final int RULER_HEIGHT = 20;

    public GenomeVisualizer(int x, int y, int width, int height, Genome genome) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.genome = genome;
        this.textRenderer = MinecraftClient.getInstance().textRenderer;
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        fill(matrices, x, y, x + width, y + height, 0xFF202020); // Dark background

        RenderSystem.enableScissor(x, y, width, height);

        if (genome != null && genome.getSequence() != null && !genome.getSequence().isEmpty()) {
            renderRuler(matrices);
            renderSequence(matrices);
        }

        RenderSystem.disableScissor();
    }

    private void renderRuler(MatrixStack matrices) {
        fill(matrices, x, y, x + width, y + RULER_HEIGHT, 0xFF303030);

        int firstVisibleBase = (int) (scrollX / zoom);
        int lastVisibleBase = (int) ((scrollX + width) / zoom);

        // Determine tick spacing based on zoom level
        int majorTickSpacing = 1;
        if (zoom < 0.2) majorTickSpacing = 1000;
        else if (zoom < 1) majorTickSpacing = 100;
        else if (zoom < 5) majorTickSpacing = 50;
        else if (zoom < 15) majorTickSpacing = 10;

        for (int i = firstVisibleBase - (firstVisibleBase % majorTickSpacing); i <= lastVisibleBase; i += majorTickSpacing) {
            float tickX = (float) (x + i * zoom - scrollX);
            if (tickX >= x && tickX <= x + width) {
                if (i % (majorTickSpacing * 10) == 0) {
                    fill(matrices, (int) tickX, y, (int) tickX + 1, y + 10, 0xFFFFFFFF);
                    textRenderer.draw(matrices, String.valueOf(i), tickX + 2, y + 2, 0xFFFFFFFF);
                } else {
                    fill(matrices, (int) tickX, y, (int) tickX + 1, y + 5, 0xFFA0A0A0);
                }
            }
        }
    }

    private void renderSequence(MatrixStack matrices) {
        char[] sequence = genome.getSequence().toCharArray();
        int sequenceLength = sequence.length;

        int firstVisibleBase = (int) (scrollX / zoom);
        int lastVisibleBase = (int) ((scrollX + width) / zoom);

        int sequenceY = y + RULER_HEIGHT + 5;

        for (int i = Math.max(0, firstVisibleBase); i <= Math.min(sequenceLength - 1, lastVisibleBase); i++) {
            char base = sequence[i];
            float charX = (float) (x + i * zoom - scrollX);

            // Only draw if the character is visible
            if (charX + zoom > x && charX < x + width) {
                int color = 0xFFFFFFFF; // White
                switch (base) {
                    case 'A': color = 0xFF80FF80; break; // Green
                    case 'C': color = 0xFF8080FF; break; // Blue
                    case 'G': color = 0xFFFFD700; break; // Gold
                    case 'T': color = 0xFFFF8080; break; // Red
                    case 'N': color = 0xFF808080; break; // Gray
                }
                // Draw character only if zoom level is high enough
                if (zoom > 5) {
                    textRenderer.draw(matrices, String.valueOf(base), charX, sequenceY, color);
                }
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isMouseOver(mouseX, mouseY) && button == 0) {
            isDragging = true;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            isDragging = false;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (isDragging) {
            scrollX -= deltaX;
            clampScroll();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (isMouseOver(mouseX, mouseY)) {
            double oldZoom = zoom;
            double zoomFactor = 1.1;
            if (amount > 0) {
                zoom *= zoomFactor;
            } else {
                zoom /= zoomFactor;
            }
            zoom = MathHelper.clamp(zoom, 0.1, 50);

            double mouseOffset = mouseX - x;
            scrollX = (scrollX + mouseOffset) * (zoom / oldZoom) - mouseOffset;
            clampScroll();

            return true;
        }
        return false;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private void clampScroll() {
        if (genome == null || genome.getSequence() == null) return;
        double sequenceWidth = genome.getSequence().length() * zoom;
        if (sequenceWidth < width) {
            scrollX = (sequenceWidth - width) / 2.0; // Center if sequence is smaller than view
        } else {
            scrollX = MathHelper.clamp(scrollX, 0, sequenceWidth - width);
        }
    }

    public void setGenome(Genome genome) {
        this.genome = genome;
        this.scrollX = 0;
        this.zoom = 10.0;
        clampScroll();
    }
}
