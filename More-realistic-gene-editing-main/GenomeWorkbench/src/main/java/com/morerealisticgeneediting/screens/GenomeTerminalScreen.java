package com.morerealisticgeneediting.screens;

import com.mojang.blaze3d.systems.RenderSystem;
import com.morerealisticgeneediting.MoreRealisticGeneEditing;
import com.morerealisticgeneediting.genome.GenomeSlice;
import com.morerealisticgeneediting.network.c2s.C2SOpenGeneInsertionScreenPacket;
import com.morerealisticgeneediting.network.c2s.C2SRequestGenomeSlicePacket;
import com.morerealisticgeneediting.network.c2s.C2SRequestMotifSearchPacket;
import com.morerealisticgeneediting.project.ProjectRegistry;
import com.morerealisticgeneediting.project.ResearchProject;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class GenomeTerminalScreen extends HandledScreen<GenomeTerminalScreenHandler> {
    private static final Identifier TEXTURE = new Identifier(MoreRealisticGeneEditing.MOD_ID, "textures/gui/genome_terminal.png");
    private final String currentGenomeIdentifier;

    // --- Tab management ---
    private enum Tab { GENOME, PROJECTS }
    private Tab currentTab = Tab.GENOME;
    private ButtonWidget genomeTabButton;
    private ButtonWidget projectsTabButton;

    // --- Genome Tab UI & State ---
    private GenomeSlice genomeSlice;
    private TextFieldWidget motifSearchField;
    private List<Long> motifHits = new ArrayList<>();
    private double genomeScrollOffset = 0.0;
    private final List<Integer> pamSites = new ArrayList<>();
    private Integer selectedPamSite = null;
    private ButtonWidget findPamsButton;
    private ButtonWidget insertionButton;
    private ButtonWidget searchButton;

    // --- Projects Tab UI & State ---
    private List<ResearchProject> projects = new ArrayList<>();
    private List<ButtonWidget> startProjectButtons = new ArrayList<>();
    private double projectScrollOffset = 0.0;

    // --- Common UI Properties ---
    private boolean isDraggingScrollbar = false;
    private static final int SCROLLBAR_WIDTH = 8;
    private static final int SCROLLBAR_HEIGHT = 152;
    private static final int SCROLLBAR_X = 158;
    private static final int SCROLLBAR_Y = 18;
    private static final int VIEW_X = 10;
    private static final int VIEW_Y = 18;
    private static final int VIEW_WIDTH = 145;
    private static final int VIEW_HEIGHT = 150;

    public GenomeTerminalScreen(GenomeTerminalScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = 176;
        this.backgroundHeight = 222;
        this.currentGenomeIdentifier = handler.genomeId.toString();
    }

    @Override
    protected void init() {
        super.init();
        // --- Tab Buttons ---
        this.genomeTabButton = this.addDrawableChild(new ButtonWidget(this.x, this.y - 20, 60, 20, Text.of("Genome"), button -> setCurrentTab(Tab.GENOME)));
        this.projectsTabButton = this.addDrawableChild(new ButtonWidget(this.x + 62, this.y - 20, 60, 20, Text.of("Projects"), button -> setCurrentTab(Tab.PROJECTS)));

        // --- Genome Tab Widgets ---
        this.motifSearchField = new TextFieldWidget(this.textRenderer, this.x + 8, this.y + 176, 100, 18, Text.of("Motif..."));
        this.addSelectableChild(this.motifSearchField);
        this.searchButton = this.addDrawableChild(new ButtonWidget(this.x + 112, this.y + 176, 60, 20, Text.of("Search"), button -> C2SRequestMotifSearchPacket.send(this.currentGenomeIdentifier, this.motifSearchField.getText())));
        this.findPamsButton = this.addDrawableChild(new ButtonWidget(this.x + 8, this.y + 198, 80, 20, Text.of("Find PAMs"), button -> findPamSites()));
        this.insertionButton = this.addDrawableChild(new ButtonWidget(this.x + 92, this.y + 198, 80, 20, Text.of("Proceed"), button -> proceedToInsertion()));

        // --- Projects Tab Widgets ---
        this.projects = ProjectRegistry.getAllProjects();
        this.startProjectButtons.clear();
        for (int i = 0; i < this.projects.size(); i++) {
            int buttonX = this.x + VIEW_X + VIEW_WIDTH - 50;
            int buttonY = this.y + VIEW_Y + (i * 60) + 40; // Positioned within the project entry
            ButtonWidget startButton = new ButtonWidget(buttonX, buttonY, 50, 20, Text.of("Start"), button -> {
                // Logic to start a project will be implemented later
            });
            startButton.active = false; // Disabled for now
            this.startProjectButtons.add(startButton);
            this.addDrawableChild(startButton);
        }

        setCurrentTab(Tab.GENOME);
        requestSlice(0);
    }

    private void setCurrentTab(Tab tab) {
        this.currentTab = tab;
        this.genomeTabButton.active = (tab != Tab.GENOME);
        this.projectsTabButton.active = (tab != Tab.PROJECTS);

        // Toggle visibility of genome tab widgets
        this.motifSearchField.setVisible(tab == Tab.GENOME);
        this.searchButton.visible = (tab == Tab.GENOME);
        this.findPamsButton.visible = (tab == Tab.GENOME);
        this.insertionButton.visible = (tab == Tab.GENOME);

        // Toggle visibility of project tab widgets
        this.startProjectButtons.forEach(b -> b.visible = (tab == Tab.PROJECTS));
    }

    @Override
    protected void drawBackground(MatrixStack matrices, float delta, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);
        drawTexture(matrices, this.x, this.y, 0, 0, this.backgroundWidth, this.backgroundHeight);

        // Draw scrollbar based on the current tab
        if (currentTab == Tab.GENOME && getGenomeMaxScroll() > 0) {
            int thumbHeight = getGenomeScrollbarThumbHeight();
            int thumbY = (int) (SCROLLBAR_Y + (SCROLLBAR_HEIGHT - thumbHeight) * this.genomeScrollOffset);
            drawTexture(matrices, this.x + SCROLLBAR_X, this.y + thumbY, 176, 0, SCROLLBAR_WIDTH, thumbHeight);
        } else if (currentTab == Tab.PROJECTS && getProjectsMaxScroll() > 0) {
            int thumbHeight = getProjectsScrollbarThumbHeight();
            int thumbY = (int) (SCROLLBAR_Y + (SCROLLBAR_HEIGHT - thumbHeight) * this.projectScrollOffset);
            drawTexture(matrices, this.x + SCROLLBAR_X, this.y + thumbY, 176, 0, SCROLLBAR_WIDTH, thumbHeight);
        }
    }

    @Override
    protected void drawForeground(MatrixStack matrices, int mouseX, int mouseY) {
        this.textRenderer.draw(matrices, this.title, (float)this.titleX, (float)this.titleY, 4210752);

        switch (currentTab) {
            case GENOME -> drawGenomeTab(matrices, mouseX, mouseY);
            case PROJECTS -> drawProjectsTab(matrices, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) return true;

        if (button == 0) { // Left-click
             if (isMouseOverScrollbar(mouseX, mouseY)) {
                isDraggingScrollbar = true;
                return true;
            }
            if (currentTab == Tab.GENOME) {
                return handleGenomeTabClick(mouseX, mouseY);
            }
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            isDraggingScrollbar = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (isDraggingScrollbar) {
            switch (currentTab) {
                case GENOME -> {
                    double scrollableHeight = SCROLLBAR_HEIGHT - getGenomeScrollbarThumbHeight();
                    if (scrollableHeight > 0) {
                        double delta = deltaY / scrollableHeight;
                        this.genomeScrollOffset = MathHelper.clamp(this.genomeScrollOffset + delta, 0.0, 1.0);
                        requestSlice((long) (genomeScrollOffset * getGenomeMaxScroll()));
                    }
                }
                case PROJECTS -> {
                    double scrollableHeight = SCROLLBAR_HEIGHT - getProjectsScrollbarThumbHeight();
                    if (scrollableHeight > 0) {
                        double delta = deltaY / scrollableHeight;
                        this.projectScrollOffset = MathHelper.clamp(this.projectScrollOffset + delta, 0.0, 1.0);
                    }
                }
            }
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        switch (currentTab) {
            case GENOME -> {
                if (getGenomeMaxScroll() > 0) {
                    double linesToScroll = -amount;
                    double scrollAmount = (linesToScroll * 10) / (getGenomeMaxScroll() * 15);
                    this.genomeScrollOffset = MathHelper.clamp(this.genomeScrollOffset + scrollAmount, 0.0, 1.0);
                    requestSlice((long) (this.genomeScrollOffset * getGenomeMaxScroll()));
                    return true;
                }
            }
            case PROJECTS -> {
                if (getProjectsMaxScroll() > 0) {
                    this.projectScrollOffset = MathHelper.clamp(this.projectScrollOffset - (amount / getProjectsMaxScroll()), 0.0, 1.0);
                    return true;
                }
            }
        }
        return false;
    }

    // --- Tab-Specific Drawing Logic ---

    private void drawGenomeTab(MatrixStack matrices, int mouseX, int mouseY) {
        final int SEQUENCE_LINE_HEIGHT = 10;
        final int BASES_PER_LINE = 15;
        final int FONT_WIDTH = 6;

        if (genomeSlice != null) {
            String sequence = genomeSlice.getSequence();
            long sliceStart = genomeSlice.getStart();
            int numLines = VIEW_HEIGHT / SEQUENCE_LINE_HEIGHT;

            for (int i = 0; i < numLines; i++) {
                int lineStartIdx = i * BASES_PER_LINE;
                if (lineStartIdx >= sequence.length()) break;

                String line = sequence.substring(lineStartIdx, Math.min(lineStartIdx + BASES_PER_LINE, sequence.length()));

                for (int j = 0; j < line.length(); j++) {
                    long absolutePos = sliceStart + lineStartIdx + j;
                    int charX = VIEW_X + j * (FONT_WIDTH + 2);
                    int charY = VIEW_Y + i * SEQUENCE_LINE_HEIGHT;

                    if (Collections.binarySearch(motifHits, absolutePos) >= 0) {
                        fill(matrices, charX - 1, charY - 1, charX + FONT_WIDTH, charY + SEQUENCE_LINE_HEIGHT - 1, 0x80FFFF00);
                    }
                    if (pamSites.contains(lineStartIdx + j)) {
                        fill(matrices, charX - 1, charY - 1, charX + FONT_WIDTH, charY + SEQUENCE_LINE_HEIGHT - 1, 0x8000FF00);
                    }
                    if (selectedPamSite != null && (lineStartIdx + j > selectedPamSite - 20) && (lineStartIdx + j < selectedPamSite)) {
                        fill(matrices, charX - 1, charY - 1, charX + FONT_WIDTH, charY + SEQUENCE_LINE_HEIGHT - 1, 0x80ADD8E6);
                    }

                    this.textRenderer.draw(matrices, String.valueOf(line.charAt(j)), charX, charY, 4210752);
                }
            }
        }
    }

    private void drawProjectsTab(MatrixStack matrices, int mouseX, int mouseY) {
        final int PROJECT_ENTRY_HEIGHT = 60;
        int yOffset = (int) -(projectScrollOffset * getProjectsMaxScroll());

        for (int i = 0; i < projects.size(); i++) {
            ResearchProject project = projects.get(i);
            int entryY = VIEW_Y + yOffset + (i * PROJECT_ENTRY_HEIGHT);

            if (entryY > VIEW_Y - PROJECT_ENTRY_HEIGHT && entryY < VIEW_Y + VIEW_HEIGHT) {
                textRenderer.draw(matrices, Text.of(project.title()).asOrderedText(), VIEW_X, entryY, 0xFFFFFF);
                List<Text> wrappedDesc = textRenderer.wrapLines(Text.of(project.description()), VIEW_WIDTH).stream().map(s -> (Text)Text.of(s.getString())).collect(Collectors.toList());
                for (int j = 0; j < wrappedDesc.size() && j < 2; j++) {
                    textRenderer.draw(matrices, wrappedDesc.get(j).asOrderedText(), VIEW_X, entryY + 12 + (j * 10), 0xA0A0A0);
                }

                ButtonWidget button = startProjectButtons.get(i);
                button.y = this.y + entryY + 35;
                button.visible = (this.currentTab == Tab.PROJECTS);
            }
            else {
                 startProjectButtons.get(i).visible = false;
            }
        }
    }

    // --- Tab-Specific Interaction Logic ---

    private boolean handleGenomeTabClick(double mouseX, double mouseY) {
        if (isMouseOverView(mouseX, mouseY)) {
            final int SEQUENCE_LINE_HEIGHT = 10;
            final int BASES_PER_LINE = 15;
            final int FONT_WIDTH = 6;

            int lineIndex = (int) ((mouseY - (this.y + VIEW_Y)) / SEQUENCE_LINE_HEIGHT);
            int charIndexInLine = (int) ((mouseX - (this.x + VIEW_X)) / (FONT_WIDTH + 2));

            if (lineIndex >= 0 && charIndexInLine >= 0 && charIndexInLine < BASES_PER_LINE) {
                int siteIndex = lineIndex * BASES_PER_LINE + charIndexInLine;
                if (pamSites.contains(siteIndex)) {
                    setSelectedPamSite(siteIndex);
                    return true;
                }
            }
        }
        return false;
    }

    // --- Scroll & View Helpers ---

    private boolean isMouseOverView(double mouseX, double mouseY) {
        return mouseX >= this.x + VIEW_X && mouseX < this.x + VIEW_X + VIEW_WIDTH &&
                mouseY >= this.y + VIEW_Y && mouseY < this.y + VIEW_Y + VIEW_HEIGHT;
    }

    private boolean isMouseOverScrollbar(double mouseX, double mouseY) {
        return mouseX >= this.x + SCROLLBAR_X && mouseX < this.x + SCROLLBAR_X + SCROLLBAR_WIDTH &&
               mouseY >= this.y + SCROLLBAR_Y && mouseY < this.y + SCROLLBAR_Y + SCROLLBAR_HEIGHT;
    }

    private long getGenomeMaxScroll() {
        if (genomeSlice == null) return 0;
        long totalLength = genomeSlice.getTotalGenomeLength();
        long viewLength = (long) (VIEW_HEIGHT / 10) * 15;
        return Math.max(0, totalLength - viewLength);
    }

    private int getGenomeScrollbarThumbHeight() {
        if (genomeSlice == null || genomeSlice.getTotalGenomeLength() <= 0) return SCROLLBAR_HEIGHT;
        long viewLength = (long) (VIEW_HEIGHT / 10) * 15;
        return MathHelper.clamp((int) (viewLength / (float)genomeSlice.getTotalGenomeLength() * SCROLLBAR_HEIGHT), 8, SCROLLBAR_HEIGHT);
    }

    private double getProjectsMaxScroll() {
        int totalContentHeight = projects.size() * 60;
        return Math.max(0, totalContentHeight - VIEW_HEIGHT);
    }

    private int getProjectsScrollbarThumbHeight() {
        if (projects.isEmpty()) return SCROLLBAR_HEIGHT;
        double maxScroll = getProjectsMaxScroll();
        if (maxScroll <= 0) return SCROLLBAR_HEIGHT;
        return MathHelper.clamp((int) (VIEW_HEIGHT / (float)(VIEW_HEIGHT + maxScroll) * SCROLLBAR_HEIGHT), 8, SCROLLBAR_HEIGHT);
    }

    // --- Genome-Specific Methods ---

    public void setGenomeSlice(GenomeSlice slice) {
        if (!this.currentGenomeIdentifier.equals(slice.getGenomeIdentifier())) {
            this.motifHits.clear();
        }
        this.genomeSlice = slice;
        this.pamSites.clear();
        this.setSelectedPamSite(null);
        if (getGenomeMaxScroll() > 0) {
            this.genomeScrollOffset = (double) slice.getStart() / getGenomeMaxScroll();
        } else {
            this.genomeScrollOffset = 0;
        }
    }

    public void setMotifHits(List<Long> hits) {
        this.motifHits = hits;
        Collections.sort(this.motifHits);
    }

    private void proceedToInsertion() {
        if (selectedPamSite != null && genomeSlice != null) {
            long absolutePamPosition = genomeSlice.getStart() + selectedPamSite;
            C2SOpenGeneInsertionScreenPacket.send(this.currentGenomeIdentifier, absolutePamPosition);
            setSelectedPamSite(null);
        }
    }

    private void findPamSites() {
        if (this.genomeSlice == null) return;
        this.pamSites.clear();
        this.setSelectedPamSite(null);
        String sequence = this.genomeSlice.getSequence();
        for (int i = 0; i < sequence.length() - 2; i++) {
            if (sequence.charAt(i + 1) == 'G' && sequence.charAt(i + 2) == 'G') {
                this.pamSites.add(i);
            }
        }
    }

    private void setSelectedPamSite(Integer pamSite) {
        this.selectedPamSite = pamSite;
        this.insertionButton.active = pamSite != null && (pamSite >= 20);
    }

    private void requestSlice(long start) {
        int lines = VIEW_HEIGHT / 10;
        int length = lines * 15;
        C2SRequestGenomeSlicePacket.send(this.currentGenomeIdentifier, start, length);
    }
}
