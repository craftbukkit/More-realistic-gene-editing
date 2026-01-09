package com.morerealisticgeneediting.client.gui.screen;

import com.morerealisticgeneediting.client.gui.widget.GenomeVisualizer;
import com.morerealisticgeneediting.genome.Genome;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

public class GenomeWorkbenchScreen extends Screen {

    private GenomeVisualizer genomeVisualizer;
    private Genome testGenome; // In a real scenario, this would be loaded dynamically

    public GenomeWorkbenchScreen() {
        super(Text.of("Genome Workbench"));
        // Create a test genome
        String testSequence = "AGCT".repeat(100);
        this.testGenome = Genome.createFromUnpackedSequence(testSequence);
    }

    @Override
    protected void init() {
        super.init();
        int visualizerWidth = this.width - 20;
        int visualizerHeight = this.height - 40;
        this.genomeVisualizer = new GenomeVisualizer(10, 20, visualizerWidth, visualizerHeight, testGenome);
        this.addSelectableChild(this.genomeVisualizer);
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices);
        this.genomeVisualizer.render(matrices, mouseX, mouseY, delta);
        drawCenteredText(matrices, this.textRenderer, this.title, this.width / 2, 8, 0xFFFFFF);
        super.render(matrices, mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
