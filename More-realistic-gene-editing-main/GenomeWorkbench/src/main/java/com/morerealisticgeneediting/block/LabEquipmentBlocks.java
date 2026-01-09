package com.morerealisticgeneediting.block;

import com.morerealisticgeneediting.MoreRealisticGeneEditing;
import com.morerealisticgeneediting.equipment.EquipmentTier;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

import static com.morerealisticgeneediting.item.ModItems.MRGE_GROUP_KEY;

/**
 * Laboratory Equipment Blocks Registration
 * 
 * Based on real laboratory equipment organized by tier:
 * - Tier 1 (基础): Basic/DIY equipment
 * - Tier 2 (进阶): Professional benchtop equipment
 * - Tier 3 (尖端): High-end automated systems
 */
public class LabEquipmentBlocks {

    // Store all registered blocks for easy iteration
    public static final Map<String, Block> BLOCKS = new LinkedHashMap<>();

    // ========== 离心与提取系统 (Centrifugation & Extraction) ==========
    
    public static final Block PALM_CENTRIFUGE = registerLabBlock("palm_centrifuge",
            LabEquipmentBlock::new, EquipmentTier.BASIC,
            "D1008 Palm Micro Centrifuge - DLAB");
    
    public static final Block REFRIGERATED_CENTRIFUGE = registerLabBlock("refrigerated_centrifuge",
            LabEquipmentBlock::new, EquipmentTier.ADVANCED,
            "LX-165T2R Refrigerated Centrifuge - Haier");
    
    public static final Block ULTRACENTRIFUGE = registerLabBlock("ultracentrifuge",
            LabEquipmentBlock::new, EquipmentTier.ELITE,
            "Optima XPN-100 Ultracentrifuge - Beckman Coulter");

    // ========== 热循环与扩增系统 (Thermal Cycling & Amplification) ==========
    
    public static final Block OPEN_PCR = registerLabBlock("open_pcr",
            LabEquipmentBlock::new, EquipmentTier.BASIC,
            "OpenPCR / Chai Open qPCR - Open Source");
    
    public static final Block THERMAL_CYCLER = registerLabBlock("thermal_cycler",
            LabEquipmentBlock::new, EquipmentTier.ADVANCED,
            "T100 Thermal Cycler - Bio-Rad");
    
    public static final Block QPCR_SYSTEM = registerLabBlock("qpcr_system",
            LabEquipmentBlock::new, EquipmentTier.ELITE,
            "LightCycler 480 II - Roche");

    // ========== 电泳与分离系统 (Electrophoresis & Separation) ==========
    
    public static final Block PORTABLE_ELECTROPHORESIS = registerLabBlock("portable_electrophoresis",
            LabEquipmentBlock::new, EquipmentTier.BASIC,
            "blueGel Electrophoresis - Amplyus/Bento");
    
    public static final Block VERTICAL_ELECTROPHORESIS = registerLabBlock("vertical_electrophoresis",
            LabEquipmentBlock::new, EquipmentTier.ADVANCED,
            "DYCZ-24DN Vertical Electrophoresis - Beijing Liuyi");
    
    public static final Block AUTO_ELECTROPHORESIS = registerLabBlock("auto_electrophoresis",
            LabEquipmentBlock::new, EquipmentTier.ELITE,
            "4200 TapeStation - Agilent");

    // ========== 基因测序系统 (Gene Sequencing) ==========
    
    public static final Block BENCHTOP_SEQUENCER = registerLabBlock("benchtop_sequencer",
            LabEquipmentBlock::new, EquipmentTier.ADVANCED,
            "DNBSEQ-G99 - MGI Tech");

    // ========== 移液与液体处理工作站 (Liquid Handling) ==========
    
    public static final Block AUTO_PIPETTING_STATION = registerLabBlock("auto_pipetting_station",
            LabEquipmentBlock::new, EquipmentTier.ADVANCED,
            "epMotion 5075 - Eppendorf");
    
    public static final Block PIPETTING_ROBOT = registerLabBlock("pipetting_robot",
            LabEquipmentBlock::new, EquipmentTier.ELITE,
            "Andrew+ Robot - Waters/Andrew Alliance");

    // ========== 培养与生物反应系统 (Culture & Bioreactor) ==========
    
    public static final Block DIY_INCUBATOR = registerLabBlock("diy_incubator",
            LabEquipmentBlock::new, EquipmentTier.BASIC,
            "DIY Styrofoam Incubator - Garage Bio");
    
    public static final Block SHAKING_INCUBATOR = registerLabBlock("shaking_incubator",
            LabEquipmentBlock::new, EquipmentTier.ADVANCED,
            "THZ-300C Shaking Incubator - Yiheng");
    
    public static final Block MODULAR_BIOREACTOR = registerLabBlock("modular_bioreactor",
            LabEquipmentBlock::new, EquipmentTier.ELITE,
            "Ambr 250 Modular - Sartorius");

    // ========== 生物安全柜与洁净环境 (Biosafety & Clean Environment) ==========
    
    public static final Block LAMINAR_FLOW_HOOD = registerLabBlock("laminar_flow_hood",
            LabEquipmentBlock::new, EquipmentTier.BASIC,
            "Simple Laminar Flow Hood - Generic");
    
    public static final Block BIOSAFETY_CABINET = registerLabBlock("biosafety_cabinet",
            LabEquipmentBlock::new, EquipmentTier.ADVANCED,
            "Class II Biosafety Cabinet - Esco Airstream");
    
    public static final Block ISOLATOR_WORKSTATION = registerLabBlock("isolator_workstation",
            LabEquipmentBlock::new, EquipmentTier.ELITE,
            "Isoclean Isolator - Esco Pharma");

    // ========== 分光光度计与定量分析 (Spectrophotometry & Quantification) ==========
    
    public static final Block VISIBLE_SPECTROPHOTOMETER = registerLabBlock("visible_spectrophotometer",
            LabEquipmentBlock::new, EquipmentTier.BASIC,
            "721 Visible Spectrophotometer - Generic");
    
    public static final Block NANODROP = registerLabBlock("nanodrop",
            LabEquipmentBlock::new, EquipmentTier.ADVANCED,
            "NanoDrop One - Thermo Scientific");
    
    public static final Block HT_SPECTROPHOTOMETER = registerLabBlock("ht_spectrophotometer",
            LabEquipmentBlock::new, EquipmentTier.ELITE,
            "Lunatic - Unchained Labs");

    // ========== 电穿孔与转化系统 (Electroporation & Transformation) ==========
    
    public static final Block ELECTROPORATOR = registerLabBlock("electroporator",
            LabEquipmentBlock::new, EquipmentTier.ADVANCED,
            "Gene Pulser Xcell - Bio-Rad");

    // ========== 超低温存储系统 (Ultra-low Temperature Storage) ==========
    
    public static final Block DRY_ICE_CONTAINER = registerLabBlock("dry_ice_container",
            LabEquipmentBlock::new, EquipmentTier.BASIC,
            "Dry Ice Storage Container - Generic");

    // ========== 其他实验设备 (Additional Equipment) ==========
    
    public static final Block GENOME_TERMINAL = registerLabBlock("genome_terminal",
            LabEquipmentBlock::new, EquipmentTier.ADVANCED,
            "Genome Analysis Terminal");
    
    public static final Block CELL_PROCESSOR = registerLabBlock("cell_processor",
            LabEquipmentBlock::new, EquipmentTier.ADVANCED,
            "Cell Processing Unit");

    // ========== Registration Methods ==========

    private static Block registerLabBlock(String name, 
            Function<AbstractBlock.Settings, Block> factory,
            EquipmentTier tier, String description) {
        
        AbstractBlock.Settings settings = AbstractBlock.Settings.create()
                .strength(2.0f + tier.getLevel(), 6.0f)
                .sounds(BlockSoundGroup.METAL)
                .requiresTool()
                .luminance(state -> tier == EquipmentTier.ELITE ? 7 : (tier == EquipmentTier.ADVANCED ? 3 : 0))
                .nonOpaque();
        
        Block block = factory.apply(settings);
        
        // Register block
        Identifier id = Identifier.of(MoreRealisticGeneEditing.MOD_ID, name);
        Registry.register(Registries.BLOCK, id, block);
        
        // Register block item
        Item.Settings itemSettings = new Item.Settings();
        BlockItem blockItem = new BlockItem(block, itemSettings);
        Registry.register(Registries.ITEM, id, blockItem);
        
        BLOCKS.put(name, block);
        
        MoreRealisticGeneEditing.LOGGER.debug("Registered lab equipment: {} (Tier {})", name, tier.getLevel());
        
        return block;
    }

    /**
     * Register all blocks to item groups
     */
    public static void registerBlocks() {
        MoreRealisticGeneEditing.LOGGER.info("Registering {} laboratory equipment blocks", BLOCKS.size());
        
        // Add all blocks to custom item group
        ItemGroupEvents.modifyEntriesEvent(MRGE_GROUP_KEY).register(content -> {
            BLOCKS.values().forEach(block -> content.add(block));
        });
    }

    /**
     * Get all blocks of a specific tier
     */
    public static Map<String, Block> getBlocksByTier(EquipmentTier tier) {
        Map<String, Block> result = new LinkedHashMap<>();
        // This would need tier metadata stored - simplified version
        return result;
    }
}
