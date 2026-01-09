package com.morerealisticgeneediting.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class SequencerScreen extends HandledScreen<SequencerScreenHandler> {
    private static final Identifier TEXTURE = new Identifier("morerealisticgeneediting", "textures/gui/sequencer_gui.png");

    public SequencerScreen(SequencerScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }

    @Override
    protected void init() {
        super.init();
        titleX = (backgroundWidth - textRenderer.getWidth(title)) / 2;
    }

    @Override
    protected void drawBackground(MatrixStack matrices, float delta, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);
        int x = (width - backgroundWidth) / 2;
        int y = (height - backgroundHeight) / 2;
        drawTexture(matrices, x, y, 0, 0, backgroundWidth, backgroundHeight);

        if (handler.getProgress() > 0) {
            int progress = handler.getProgress();
            int maxProgress = handler.getMaxProgress();
            int arrowWidth = 24; // Width of the arrow in the texture
            int progressWidth = (int) (((float) progress / maxProgress) * arrowWidth);
            drawTexture(matrices, x + 80, y + 35, 176, 14, progressWidth, 18);
        }
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices);
        super.render(matrices, mouseX, mouseY, delta);
        drawMouseoverTooltip(matrices, x, y);
    }
}
