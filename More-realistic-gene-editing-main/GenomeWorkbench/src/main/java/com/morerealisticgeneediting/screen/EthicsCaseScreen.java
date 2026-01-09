package com.morerealisticgeneediting.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.morerealisticgeneediting.MoreRealisticGeneEditing;
import com.morerealisticgeneediting.ethics.EthicsCase;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.List;

public class EthicsCaseScreen extends HandledScreen<EthicsCaseScreenHandler> {

    private static final Identifier TEXTURE = new Identifier(MoreRealisticGeneEditing.MOD_ID, "textures/gui/ethics_case_background.png");
    private EthicsCase.EthicsOption selectedOption = null;

    public EthicsCaseScreen(EthicsCaseScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = 256;
        this.backgroundHeight = 180;
    }

    @Override
    protected void init() {
        super.init();
        titleX = (backgroundWidth - textRenderer.getWidth(title)) / 2;
        updateWidgets();
    }

    private void updateWidgets() {
        clearChildren();
        int x = (width - backgroundWidth) / 2;
        int y = (height - backgroundHeight) / 2;

        if (selectedOption == null) {
            // Initial view: show options
            List<EthicsCase.EthicsOption> options = handler.ethicsCase.options();
            int buttonHeight = 20;
            int buttonY = y + 70;
            for (EthicsCase.EthicsOption option : options) {
                addDrawableChild(new ButtonWidget.Builder(Text.of(option.text()), button -> {
                    this.selectedOption = option;
                    updateWidgets();
                }).dimensions(x + 20, buttonY, 216, buttonHeight).build());
                buttonY += buttonHeight + 5;
            }
        } else {
            // Consequence view: show consequence and close button
            addDrawableChild(new ButtonWidget.Builder(Text.of("Close"), button -> {
                this.close();
            }).dimensions(x + (backgroundWidth / 2) - 50, y + 150, 100, 20).build());
        }
    }

    @Override
    protected void drawBackground(MatrixStack matrices, float delta, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);
        int x = (width - backgroundWidth) / 2;
        int y = (height - backgroundHeight) / 2;
        drawTexture(matrices, x, y, 0, 0, backgroundWidth, backgroundHeight);
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices);
        super.render(matrices, mouseX, mouseY, delta);
        drawForeground(matrices, mouseX, mouseY);
    }

    @Override
    protected void drawForeground(MatrixStack matrices, int mouseX, int mouseY) {
        int x = (width - backgroundWidth) / 2;
        int y = (height - backgroundHeight) / 2;

        if (selectedOption == null) {
            // Draw title and description
            textRenderer.draw(matrices, handler.ethicsCase.title(), x + titleX, y + 10, 4210752);
            textRenderer.drawTrimmed(Text.of(handler.ethicsCase.description()), x + 20, y + 25, 216, 3, 4210752);
        } else {
            // Draw consequence text
            textRenderer.draw(matrices, "Consequence:", x + titleX, y + 10, 4210752);
            textRenderer.drawTrimmed(Text.of(selectedOption.consequence()), x + 20, y + 25, 216, 8, 4210752);
        }
    }
}
