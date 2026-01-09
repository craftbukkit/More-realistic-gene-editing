package com.morerealisticgeneediting.screens;

import com.morerealisticgeneediting.MoreRealisticGeneEditing;
import net.fabricmc.fabric.api.screenhandler.v1.ScreenHandlerRegistry;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;

public class ModScreenHandlers {
    public static ScreenHandlerType<GenomeTerminalScreenHandler> GENOME_TERMINAL_SCREEN_HANDLER;

    public static void registerScreenHandlers() {
        GENOME_TERMINAL_SCREEN_HANDLER = ScreenHandlerRegistry.registerSimple(new Identifier(MoreRealisticGeneEditing.MOD_ID, "genome_terminal"), GenomeTerminalScreenHandler::new);
    }
}
