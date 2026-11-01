package com.morerealisticgeneediting.recipe;
import com.morerealisticgeneediting.MoreRealisticGeneEditing;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
public final class ModRecipes {
    public static void registerRecipes() {
        Registry.register(Registries.RECIPE_SERIALIZER,Identifier.of(MoreRealisticGeneEditing.MOD_ID,CellProcessorRecipe.Serializer.ID),CellProcessorRecipe.Serializer.INSTANCE);
        Registry.register(Registries.RECIPE_TYPE,Identifier.of(MoreRealisticGeneEditing.MOD_ID,CellProcessorRecipe.Type.ID),CellProcessorRecipe.Type.INSTANCE);
    }
}
