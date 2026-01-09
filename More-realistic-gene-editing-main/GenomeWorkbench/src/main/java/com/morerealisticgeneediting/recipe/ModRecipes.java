package com.morerealisticgeneediting.recipe;

import com.morerealisticgeneediting.MoreRealisticGeneEditing;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class ModRecipes {
    public static void registerRecipes() {
        Registry.register(Registry.RECIPE_SERIALIZER, new Identifier(MoreRealisticGeneEditing.MOD_ID, CellProcessorRecipe.Serializer.ID),
                CellProcessorRecipe.Serializer.INSTANCE);
        Registry.register(Registry.RECIPE_TYPE, new Identifier(MoreRealisticGeneEditing.MOD_ID, CellProcessorRecipe.Type.ID),
                CellProcessorRecipe.Type.INSTANCE);
    }
}
