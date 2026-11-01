package com.morerealisticgeneediting.client.gui.screen;

import com.morerealisticgeneediting.screen.LabEquipmentScreenHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;

/** Controls follow synchronized machine kind, including the first property update after opening. */
public final class GenericLaboratoryScreen extends LaboratoryScreen<LabEquipmentScreenHandler> {
    private final ButtonWidget[] controls = new ButtonWidget[3];

    public GenericLaboratoryScreen(LabEquipmentScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        backgroundWidth = 320;
    }

    @Override protected void init() {
        super.init();
        for (int i = 0; i < controls.length; i++) {
            int action = i;
            controls[i] = addDrawableChild(ButtonWidget.builder(Text.empty(), button -> {
                if (client != null && client.interactionManager != null)
                    client.interactionManager.clickButton(handler.syncId, action);
            }).dimensions(x + 185, y + 66 + i * 24, 126, 20).build());
        }
        updateControls();
    }

    private void updateControls() {
        String[] actions = switch (handler.getMachineKind()) {
            case 3 -> new String[]{"start", "stop", "next_protocol"};
            case 4 -> new String[]{"harvest"};
            case 5 -> new String[]{"reset"};
            case 6 -> new String[]{"airflow", "uv"};
            default -> new String[0];
        };
        for (int i = 0; i < controls.length; i++) {
            controls[i].visible = i < actions.length;
            controls[i].active = i < actions.length && !(handler.getMachineKind() == 5 && handler.getStatus() != 0);
            if (i < actions.length) controls[i].setMessage(Text.translatable("button.mrge." + actions[i]));
        }
    }

    @Override public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        updateControls();
        super.render(context, mouseX, mouseY, delta);
    }

    @Override protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        super.drawForeground(context, mouseX, mouseY);
        Text status = Text.translatable(handler.getStatus() == 0 ? "screen.mrge.idle" : "screen.mrge.running");
        context.drawText(textRenderer, status, 185, 26, 0x183E4E, false);
        if (handler.getMaxProgress() > 0) {
            int percent = Math.min(100, Math.max(0, (int) (100L * handler.getProgress() / handler.getMaxProgress())));
            context.drawText(textRenderer, percent + "%", 185, 40, 0x183E4E, false);
        }
        if (handler.getMachineKind() == 3) {
            context.drawText(textRenderer, textRenderer.trimToWidth(Text.translatable("screen.mrge.protocol_" + handler.getDetail()).getString(), 126), 185, 54, 0x183E4E, false);
        }
    }
}
