package com.morerealisticgeneediting.item;

import com.morerealisticgeneediting.MoreRealisticGeneEditing;
import com.morerealisticgeneediting.item.custom.GeneSyringe;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class ModItems {

    // ========== Laboratory Equipment Items ==========
    
    public static final Item GENOME_SAMPLE = registerItem("genome_sample",
            new GenomeSampleItem(new Item.Settings().maxCount(1)));

    public static final Item DNA_SAMPLE = registerItem("dna_sample",
            new Item(new Item.Settings().maxCount(1)));

    public static final Item GENOME_WORKBENCH = registerItem("genome_workbench",
            new GenomeWorkbenchItem(new Item.Settings()));

    // ========== PCR & Molecular Biology Items ==========
    
    public static final Item PRIMER = registerItem("primer",
            new Item(new Item.Settings().maxCount(64)));

    public static final Item PCR_TUBE = registerItem("pcr_tube",
            new Item(new Item.Settings().maxCount(16)));

    public static final Item AMPLIFIED_GENE_FRAGMENT = registerItem("amplified_gene_fragment",
            new Item(new Item.Settings().maxCount(1)));

    // ========== Cloning & Vector Items ==========
    
    public static final Item RESTRICTION_ENZYME = registerItem("restriction_enzyme",
            new Item(new Item.Settings().maxCount(64)));

    public static final Item PLASMID_VECTOR = registerItem("plasmid_vector",
            new Item(new Item.Settings().maxCount(1)));

    public static final Item DNA_LIGASE = registerItem("dna_ligase",
            new Item(new Item.Settings().maxCount(64)));

    public static final Item RECOMBINANT_PLASMID = registerItem("recombinant_plasmid",
            new Item(new Item.Settings().maxCount(1)));

    // ========== Gene Editing Items ==========
    
    public static final Item GENE_SYRINGE = registerItem("gene_syringe",
            new GeneSyringe(new Item.Settings().maxDamage(32)));
    
    public static final Item CRISPR_CAS9 = registerItem("crispr_cas9",
            new Item(new Item.Settings().maxCount(16)));
    
    public static final Item GUIDE_RNA = registerItem("guide_rna",
            new Item(new Item.Settings().maxCount(64)));
    
    public static final Item CAS9_GRNA_COMPLEX = registerItem("cas9_grna_complex",
            new Item(new Item.Settings().maxCount(1)));

    // ========== Delivery System Items ==========
    
    public static final Item LIPID_NANOPARTICLE = registerItem("lipid_nanoparticle",
            new Item(new Item.Settings().maxCount(16)));
    
    public static final Item ELECTROPORATION_CUVETTE = registerItem("electroporation_cuvette",
            new Item(new Item.Settings().maxCount(8)));

    // ========== Cell Culture Items ==========
    
    public static final Item CELL_CULTURE_FLASK = registerItem("cell_culture_flask",
            new Item(new Item.Settings().maxCount(8)));
    
    public static final Item GROWTH_MEDIUM = registerItem("growth_medium",
            new Item(new Item.Settings().maxCount(64)));
    
    public static final Item PETRI_DISH = registerItem("petri_dish",
            new Item(new Item.Settings().maxCount(16)));

    // ========== Analysis & Quality Control Items ==========
    
    public static final Item SEQUENCING_CHIP = registerItem("sequencing_chip",
            new Item(new Item.Settings().maxCount(8)));
    
    public static final Item AGAROSE_GEL = registerItem("agarose_gel",
            new Item(new Item.Settings().maxCount(16)));
    
    public static final Item DNA_LADDER = registerItem("dna_ladder",
            new Item(new Item.Settings().maxCount(64)));

    // ========== Custom Item Group ==========
    
    public static final RegistryKey<ItemGroup> MRGE_GROUP_KEY = RegistryKey.of(
            RegistryKeys.ITEM_GROUP,
            Identifier.of(MoreRealisticGeneEditing.MOD_ID, "mrge_items")
    );

    public static final ItemGroup MRGE_GROUP = FabricItemGroup.builder()
            .icon(() -> new ItemStack(GENOME_SAMPLE))
            .displayName(Text.translatable("itemGroup.morerealisticgeneediting.mrge_items"))
            .build();

    // ========== Registration Methods ==========
    
    private static Item registerItem(String name, Item item) {
        return Registry.register(Registries.ITEM,
                Identifier.of(MoreRealisticGeneEditing.MOD_ID, name), item);
    }

    public static void registerModItems() {
        MoreRealisticGeneEditing.LOGGER.info("Registering Mod Items for " + MoreRealisticGeneEditing.MOD_ID);
        
        // Register the custom item group
        Registry.register(Registries.ITEM_GROUP, MRGE_GROUP_KEY, MRGE_GROUP);
        
        // Add items to the custom group
        ItemGroupEvents.modifyEntriesEvent(MRGE_GROUP_KEY).register(content -> {
            // Laboratory Equipment
            content.add(GENOME_SAMPLE);
            content.add(DNA_SAMPLE);
            content.add(GENOME_WORKBENCH);
            
            // PCR & Molecular Biology
            content.add(PRIMER);
            content.add(PCR_TUBE);
            content.add(AMPLIFIED_GENE_FRAGMENT);
            
            // Cloning & Vector
            content.add(RESTRICTION_ENZYME);
            content.add(PLASMID_VECTOR);
            content.add(DNA_LIGASE);
            content.add(RECOMBINANT_PLASMID);
            
            // Gene Editing
            content.add(GENE_SYRINGE);
            content.add(CRISPR_CAS9);
            content.add(GUIDE_RNA);
            content.add(CAS9_GRNA_COMPLEX);
            
            // Delivery System
            content.add(LIPID_NANOPARTICLE);
            content.add(ELECTROPORATION_CUVETTE);
            
            // Cell Culture
            content.add(CELL_CULTURE_FLASK);
            content.add(GROWTH_MEDIUM);
            content.add(PETRI_DISH);
            
            // Analysis & QC
            content.add(SEQUENCING_CHIP);
            content.add(AGAROSE_GEL);
            content.add(DNA_LADDER);
        });
        
        // Also add some items to vanilla Ingredients group for discoverability
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.INGREDIENTS).register(content -> {
            content.add(GENOME_SAMPLE);
            content.add(CRISPR_CAS9);
            content.add(GUIDE_RNA);
        });
    }
}
