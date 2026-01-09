package com.morerealisticgeneediting;

import com.morerealisticgeneediting.block.LabEquipmentBlocks;
import com.morerealisticgeneediting.block.entity.ModBlockEntities;
import com.morerealisticgeneediting.block.ModBlocks;
import com.morerealisticgeneediting.command.EthicsCommand;
import com.morerealisticgeneediting.ethics.EthicsCasebook;
import com.morerealisticgeneediting.genome.Genome;
import com.morerealisticgeneediting.genome.provider.GenomeProviderRegistry;
import com.morerealisticgeneediting.item.LabEquipmentItems;
import com.morerealisticgeneediting.item.ModItems;
import com.morerealisticgeneediting.network.ServerPacketHandler;
import com.morerealisticgeneediting.project.ProjectRegistry;
import com.morerealisticgeneediting.project.ServerProjectManager;
import com.morerealisticgeneediting.recipe.ModRecipes;
import com.morerealisticgeneediting.screen.ModScreenHandlers;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MoreRealisticGeneEditing implements ModInitializer {

    public static final String MOD_ID = "morerealisticgeneediting";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final Map<UUID, Genome> genomeCache = new HashMap<>();

    @Override
    public void onInitialize() {
        LOGGER.info("===========================================");
        LOGGER.info("  More Realistic Gene Editing Mod v0.2.0");
        LOGGER.info("  Initializing...");
        LOGGER.info("===========================================");
        
        // ========== Phase 1: Core Registration ==========
        LOGGER.info("[1/8] Registering core items...");
        ModItems.registerModItems();
        
        LOGGER.info("[2/8] Registering laboratory equipment items...");
        LabEquipmentItems.registerItems();
        
        LOGGER.info("[3/8] Registering core blocks...");
        ModBlocks.registerModBlocks();
        
        LOGGER.info("[4/8] Registering laboratory equipment blocks...");
        LabEquipmentBlocks.registerBlocks();
        
        LOGGER.info("[5/8] Registering block entities...");
        ModBlockEntities.registerBlockEntities();
        
        LOGGER.info("[6/8] Registering screens and recipes...");
        ModScreenHandlers.registerAllScreenHandlers();
        ModRecipes.registerRecipes();

        // ========== Phase 2: Data Systems ==========
        LOGGER.info("[7/8] Initializing data systems...");
        EthicsCasebook.initialize();
        ProjectRegistry.initialize();
        
        // ========== Phase 3: Network & Events ==========
        LOGGER.info("[8/8] Setting up network and events...");
        
        // Register network packet handlers
        ServerPacketHandler.register();
        
        // Register commands using v2 API
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            EthicsCommand.register(dispatcher);
        });

        // Register the available genome providers
        GenomeProviderRegistry.registerDefaults();

        // Register player event handlers for the project system
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerProjectManager.onPlayerJoin(handler.player);
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerProjectManager.onPlayerLeave(handler.player);
        });
        
        // Print summary
        int totalBlocks = LabEquipmentBlocks.BLOCKS.size();
        int totalItems = LabEquipmentItems.ITEMS.size();
        
        LOGGER.info("===========================================");
        LOGGER.info("  Initialization Complete!");
        LOGGER.info("  - Laboratory Blocks: {}", totalBlocks);
        LOGGER.info("  - Laboratory Items: {}", totalItems);
        LOGGER.info("===========================================");
    }
}
