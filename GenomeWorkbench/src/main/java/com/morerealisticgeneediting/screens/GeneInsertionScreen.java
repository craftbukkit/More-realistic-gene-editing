package com.morerealisticgeneediting.screens;

import com.mojang.blaze3d.systems.RenderSystem;
import com.morerealisticgeneediting.MoreRealisticGeneEditing;
import com.morerealisticgeneediting.network.c2s.C2SPerformGeneInsertionPacket;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class GeneInsertionScreen extends HandledScreen<GeneInsertionScreenHandler> {
    private static final Identifier TEXTURE = new Identifier(MoreRealisticGeneEditing.MOD_ID, "textures/gui/gene_insertion.png");
    private TextFieldWidget geneSequenceField;
    private final String genomeId;
    private final long knockoutPosition;

    public GeneInsertionScreen(GeneInsertionScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = 176;
        this.backgroundHeight = 100;
        this.genomeId = handler.getGenomeId();
        this.knockoutPosition = handler.getKnockoutPosition();
    }

    @Override
    protected void init() {
        super.init();
        this.geneSequenceField = new TextFieldWidget(this.textRenderer, this.x + 8, this.y + 20, 160, 20, Text.of("Gene Sequence"));
        this.addSelectableChild(this.geneSequenceField);

        this.addDrawableChild(new ButtonWidget(this.x + 8, this.y + 45, 160, 20, Text.of("Insert Gene"), button -> {
            String geneSequence = this.geneSequenceField.getText();
            if (isValidDna(geneSequence)) {
                C2SPerformGeneInsertionPacket.send(this.genomeId, this.knockoutPosition, geneSequence);
                this.client.player.closeScreen();
            }
        }));
    }

    private boolean isValidDna(String sequence) {
        return sequence.matches("^[ATCG]+$");
    }

    @Override
    protected void drawBackground(MatrixStack matrices, float delta, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);
        drawTexture(matrices, this.x, this.y, 0, 0, this.backgroundWidth, this.backgroundHeight);
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices);
        super.render(matrices, mouseX, mouseY, delta);
        this.geneSequenceField.render(matrices, mouseX, mouseY, delta);
    }
}
