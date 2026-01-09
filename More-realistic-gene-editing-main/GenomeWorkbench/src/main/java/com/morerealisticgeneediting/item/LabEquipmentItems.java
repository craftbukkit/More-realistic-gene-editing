package com.morerealisticgeneediting.item;

import com.morerealisticgeneediting.MoreRealisticGeneEditing;
import com.morerealisticgeneediting.equipment.EquipmentTier;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.morerealisticgeneediting.item.ModItems.MRGE_GROUP_KEY;

/**
 * Laboratory Equipment Items Registration
 * 
 * Hand-held tools and consumables for gene editing workflows.
 * Organized by category and tier.
 */
public class LabEquipmentItems {
    
    // Store all registered items
    public static final Map<String, Item> ITEMS = new LinkedHashMap<>();
    
    // ========== 测序设备 (Sequencing - Hand-held) ==========
    
    public static final Item PORTABLE_SEQUENCER = registerItem("portable_sequencer",
            new Item(new Item.Settings().maxCount(1).rarity(Rarity.UNCOMMON)),
            EquipmentTier.BASIC, "MinION Mk1C - Oxford Nanopore");
    
    // ========== 移液工具 (Pipetting Tools) ==========
    
    public static final Item MANUAL_PIPETTE = registerItem("manual_pipette",
            new Item(new Item.Settings().maxCount(1).maxDamage(500)),
            EquipmentTier.BASIC, "Manual Micropipette");
    
    public static final Item MULTICHANNEL_PIPETTE = registerItem("multichannel_pipette",
            new Item(new Item.Settings().maxCount(1).maxDamage(300)),
            EquipmentTier.ADVANCED, "8-Channel Pipette");
    
    public static final Item ELECTRONIC_PIPETTE = registerItem("electronic_pipette",
            new Item(new Item.Settings().maxCount(1).maxDamage(1000).rarity(Rarity.UNCOMMON)),
            EquipmentTier.ADVANCED, "Electronic Pipette");
    
    // ========== 移液器耗材 (Pipette Consumables) ==========
    
    public static final Item PIPETTE_TIP_BOX = registerItem("pipette_tip_box",
            new Item(new Item.Settings().maxCount(16)),
            EquipmentTier.BASIC, "96-well Pipette Tip Box");
    
    public static final Item FILTER_TIP_BOX = registerItem("filter_tip_box",
            new Item(new Item.Settings().maxCount(16)),
            EquipmentTier.ADVANCED, "Filter Pipette Tips");
    
    // ========== PCR耗材 (PCR Consumables) ==========
    
    public static final Item PCR_TUBE_STRIP = registerItem("pcr_tube_strip",
            new Item(new Item.Settings().maxCount(64)),
            EquipmentTier.BASIC, "8-Tube PCR Strip");
    
    public static final Item PCR_PLATE_96 = registerItem("pcr_plate_96",
            new Item(new Item.Settings().maxCount(16)),
            EquipmentTier.ADVANCED, "96-Well PCR Plate");
    
    public static final Item PCR_PLATE_384 = registerItem("pcr_plate_384",
            new Item(new Item.Settings().maxCount(8).rarity(Rarity.UNCOMMON)),
            EquipmentTier.ELITE, "384-Well PCR Plate");
    
    // ========== 试剂 (Reagents) ==========
    
    public static final Item TAQ_POLYMERASE = registerItem("taq_polymerase",
            new Item(new Item.Settings().maxCount(16)),
            EquipmentTier.BASIC, "Taq DNA Polymerase");
    
    public static final Item HF_POLYMERASE = registerItem("hf_polymerase",
            new Item(new Item.Settings().maxCount(16).rarity(Rarity.UNCOMMON)),
            EquipmentTier.ADVANCED, "High-Fidelity Polymerase");
    
    public static final Item DNTPS = registerItem("dntps",
            new Item(new Item.Settings().maxCount(64)),
            EquipmentTier.BASIC, "dNTP Mix");
    
    public static final Item PCR_BUFFER = registerItem("pcr_buffer",
            new Item(new Item.Settings().maxCount(64)),
            EquipmentTier.BASIC, "PCR Buffer 10X");
    
    public static final Item MGCL2 = registerItem("mgcl2",
            new Item(new Item.Settings().maxCount(64)),
            EquipmentTier.BASIC, "MgCl2 Solution");
    
    // ========== 电泳耗材 (Electrophoresis Consumables) ==========
    
    public static final Item AGAROSE_POWDER = registerItem("agarose_powder",
            new Item(new Item.Settings().maxCount(64)),
            EquipmentTier.BASIC, "Agarose Powder");
    
    public static final Item TAE_BUFFER = registerItem("tae_buffer",
            new Item(new Item.Settings().maxCount(64)),
            EquipmentTier.BASIC, "TAE Buffer 50X");
    
    public static final Item TBE_BUFFER = registerItem("tbe_buffer",
            new Item(new Item.Settings().maxCount(64)),
            EquipmentTier.BASIC, "TBE Buffer 10X");
    
    public static final Item LOADING_DYE = registerItem("loading_dye",
            new Item(new Item.Settings().maxCount(64)),
            EquipmentTier.BASIC, "6X Loading Dye");
    
    public static final Item ETHIDIUM_BROMIDE = registerItem("ethidium_bromide",
            new Item(new Item.Settings().maxCount(16)),
            EquipmentTier.BASIC, "Ethidium Bromide (Caution: Mutagenic)");
    
    public static final Item SYBR_SAFE = registerItem("sybr_safe",
            new Item(new Item.Settings().maxCount(16).rarity(Rarity.UNCOMMON)),
            EquipmentTier.ADVANCED, "SYBR Safe DNA Stain");
    
    public static final Item DNA_LADDER_100BP = registerItem("dna_ladder_100bp",
            new Item(new Item.Settings().maxCount(32)),
            EquipmentTier.BASIC, "100bp DNA Ladder");
    
    public static final Item DNA_LADDER_1KB = registerItem("dna_ladder_1kb",
            new Item(new Item.Settings().maxCount(32)),
            EquipmentTier.BASIC, "1kb DNA Ladder");
    
    public static final Item DNA_LADDER_1KB_PLUS = registerItem("dna_ladder_1kb_plus",
            new Item(new Item.Settings().maxCount(32).rarity(Rarity.UNCOMMON)),
            EquipmentTier.ADVANCED, "1kb Plus DNA Ladder");
    
    // ========== 克隆试剂 (Cloning Reagents) ==========
    
    public static final Item ECORI = registerItem("ecori",
            new Item(new Item.Settings().maxCount(16)),
            EquipmentTier.BASIC, "EcoRI Restriction Enzyme");
    
    public static final Item HINDIII = registerItem("hindiii",
            new Item(new Item.Settings().maxCount(16)),
            EquipmentTier.BASIC, "HindIII Restriction Enzyme");
    
    public static final Item BAMHI = registerItem("bamhi",
            new Item(new Item.Settings().maxCount(16)),
            EquipmentTier.BASIC, "BamHI Restriction Enzyme");
    
    public static final Item NOTI = registerItem("noti",
            new Item(new Item.Settings().maxCount(16)),
            EquipmentTier.ADVANCED, "NotI Restriction Enzyme");
    
    public static final Item T4_DNA_LIGASE = registerItem("t4_dna_ligase",
            new Item(new Item.Settings().maxCount(16)),
            EquipmentTier.BASIC, "T4 DNA Ligase");
    
    public static final Item LIGASE_BUFFER = registerItem("ligase_buffer",
            new Item(new Item.Settings().maxCount(64)),
            EquipmentTier.BASIC, "T4 Ligase Buffer 10X");
    
    // ========== CRISPR试剂 (CRISPR Reagents) ==========
    
    public static final Item CAS9_PROTEIN = registerItem("cas9_protein",
            new Item(new Item.Settings().maxCount(8).rarity(Rarity.UNCOMMON)),
            EquipmentTier.ADVANCED, "SpCas9 Protein");
    
    public static final Item CAS9_NICKASE = registerItem("cas9_nickase",
            new Item(new Item.Settings().maxCount(8).rarity(Rarity.RARE)),
            EquipmentTier.ELITE, "Cas9 Nickase (D10A)");
    
    public static final Item CAS12A = registerItem("cas12a",
            new Item(new Item.Settings().maxCount(8).rarity(Rarity.RARE)),
            EquipmentTier.ELITE, "Cas12a (Cpf1) Protein");
    
    public static final Item SGRNA_SCAFFOLD = registerItem("sgrna_scaffold",
            new Item(new Item.Settings().maxCount(32)),
            EquipmentTier.ADVANCED, "sgRNA Scaffold");
    
    public static final Item TRACRRNA = registerItem("tracrrna",
            new Item(new Item.Settings().maxCount(32)),
            EquipmentTier.ADVANCED, "tracrRNA");
    
    public static final Item RNP_COMPLEX = registerItem("rnp_complex",
            new Item(new Item.Settings().maxCount(4).rarity(Rarity.RARE)),
            EquipmentTier.ELITE, "Cas9-gRNA RNP Complex");
    
    // ========== 递送系统 (Delivery Systems) ==========
    
    public static final Item LIPOFECTAMINE = registerItem("lipofectamine",
            new Item(new Item.Settings().maxCount(8).rarity(Rarity.UNCOMMON)),
            EquipmentTier.ADVANCED, "Lipofectamine Reagent");
    
    public static final Item LNP_REAGENT = registerItem("lnp_reagent",
            new Item(new Item.Settings().maxCount(8).rarity(Rarity.RARE)),
            EquipmentTier.ELITE, "LNP Formulation Kit");
    
    public static final Item ELECTROPORATION_BUFFER = registerItem("electroporation_buffer",
            new Item(new Item.Settings().maxCount(32)),
            EquipmentTier.ADVANCED, "Electroporation Buffer");
    
    // ========== 细胞培养 (Cell Culture) ==========
    
    public static final Item DMEM_MEDIUM = registerItem("dmem_medium",
            new Item(new Item.Settings().maxCount(16)),
            EquipmentTier.BASIC, "DMEM Culture Medium");
    
    public static final Item RPMI_MEDIUM = registerItem("rpmi_medium",
            new Item(new Item.Settings().maxCount(16)),
            EquipmentTier.BASIC, "RPMI-1640 Medium");
    
    public static final Item FBS = registerItem("fbs",
            new Item(new Item.Settings().maxCount(8)),
            EquipmentTier.BASIC, "Fetal Bovine Serum");
    
    public static final Item PENICILLIN_STREP = registerItem("penicillin_strep",
            new Item(new Item.Settings().maxCount(16)),
            EquipmentTier.BASIC, "Penicillin-Streptomycin");
    
    public static final Item TRYPSIN_EDTA = registerItem("trypsin_edta",
            new Item(new Item.Settings().maxCount(16)),
            EquipmentTier.BASIC, "Trypsin-EDTA 0.25%");
    
    public static final Item PBS = registerItem("pbs",
            new Item(new Item.Settings().maxCount(64)),
            EquipmentTier.BASIC, "PBS (Phosphate Buffered Saline)");
    
    // ========== 培养耗材 (Culture Consumables) ==========
    
    public static final Item T25_FLASK = registerItem("t25_flask",
            new Item(new Item.Settings().maxCount(16)),
            EquipmentTier.BASIC, "T-25 Culture Flask");
    
    public static final Item T75_FLASK = registerItem("t75_flask",
            new Item(new Item.Settings().maxCount(8)),
            EquipmentTier.BASIC, "T-75 Culture Flask");
    
    public static final Item T175_FLASK = registerItem("t175_flask",
            new Item(new Item.Settings().maxCount(4)),
            EquipmentTier.ADVANCED, "T-175 Culture Flask");
    
    public static final Item WELL_PLATE_6 = registerItem("well_plate_6",
            new Item(new Item.Settings().maxCount(16)),
            EquipmentTier.BASIC, "6-Well Plate");
    
    public static final Item WELL_PLATE_24 = registerItem("well_plate_24",
            new Item(new Item.Settings().maxCount(16)),
            EquipmentTier.BASIC, "24-Well Plate");
    
    public static final Item WELL_PLATE_96 = registerItem("well_plate_96",
            new Item(new Item.Settings().maxCount(16)),
            EquipmentTier.ADVANCED, "96-Well Plate");
    
    // ========== 提取试剂盒 (Extraction Kits) ==========
    
    public static final Item DNA_EXTRACTION_KIT = registerItem("dna_extraction_kit",
            new Item(new Item.Settings().maxCount(8)),
            EquipmentTier.BASIC, "DNA Extraction Kit (50 preps)");
    
    public static final Item RNA_EXTRACTION_KIT = registerItem("rna_extraction_kit",
            new Item(new Item.Settings().maxCount(8).rarity(Rarity.UNCOMMON)),
            EquipmentTier.ADVANCED, "RNA Extraction Kit (50 preps)");
    
    public static final Item PLASMID_MINIPREP_KIT = registerItem("plasmid_miniprep_kit",
            new Item(new Item.Settings().maxCount(8)),
            EquipmentTier.BASIC, "Plasmid Miniprep Kit");
    
    public static final Item PLASMID_MAXIPREP_KIT = registerItem("plasmid_maxiprep_kit",
            new Item(new Item.Settings().maxCount(4).rarity(Rarity.UNCOMMON)),
            EquipmentTier.ADVANCED, "Plasmid Maxiprep Kit");
    
    public static final Item GEL_EXTRACTION_KIT = registerItem("gel_extraction_kit",
            new Item(new Item.Settings().maxCount(8)),
            EquipmentTier.BASIC, "Gel Extraction Kit");
    
    public static final Item PCR_CLEANUP_KIT = registerItem("pcr_cleanup_kit",
            new Item(new Item.Settings().maxCount(8)),
            EquipmentTier.BASIC, "PCR Cleanup Kit");
    
    // ========== 样本存储 (Sample Storage) ==========
    
    public static final Item CRYOVIAL = registerItem("cryovial",
            new Item(new Item.Settings().maxCount(64)),
            EquipmentTier.BASIC, "Cryogenic Vial 2mL");
    
    public static final Item MICROCENTRIFUGE_TUBE = registerItem("microcentrifuge_tube",
            new Item(new Item.Settings().maxCount(64)),
            EquipmentTier.BASIC, "1.5mL Microcentrifuge Tube");
    
    public static final Item FREEZER_BOX = registerItem("freezer_box",
            new Item(new Item.Settings().maxCount(16)),
            EquipmentTier.BASIC, "81-Well Freezer Box");
    
    // ========== 安全装备 (Safety Equipment) ==========
    
    public static final Item LAB_COAT = registerItem("lab_coat",
            new Item(new Item.Settings().maxCount(1)),
            EquipmentTier.BASIC, "Laboratory Coat");
    
    public static final Item NITRILE_GLOVES = registerItem("nitrile_gloves",
            new Item(new Item.Settings().maxCount(64)),
            EquipmentTier.BASIC, "Nitrile Gloves (Box)");
    
    public static final Item SAFETY_GOGGLES = registerItem("safety_goggles",
            new Item(new Item.Settings().maxCount(1)),
            EquipmentTier.BASIC, "Safety Goggles");
    
    public static final Item FACE_SHIELD = registerItem("face_shield",
            new Item(new Item.Settings().maxCount(1)),
            EquipmentTier.ADVANCED, "Face Shield");
    
    // ========== Registration Methods ==========
    
    private static Item registerItem(String name, Item item, EquipmentTier tier, String description) {
        Identifier id = Identifier.of(MoreRealisticGeneEditing.MOD_ID, name);
        Registry.register(Registries.ITEM, id, item);
        ITEMS.put(name, item);
        return item;
    }
    
    /**
     * Register all items and add to item groups
     */
    public static void registerItems() {
        MoreRealisticGeneEditing.LOGGER.info("Registering {} laboratory equipment items", ITEMS.size());
        
        // Add all items to custom item group
        ItemGroupEvents.modifyEntriesEvent(MRGE_GROUP_KEY).register(content -> {
            ITEMS.values().forEach(content::add);
        });
    }
}
