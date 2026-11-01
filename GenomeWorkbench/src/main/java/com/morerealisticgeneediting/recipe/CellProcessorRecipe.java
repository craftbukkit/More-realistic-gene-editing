package com.morerealisticgeneediting.recipe;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.recipe.*;
import net.minecraft.recipe.input.RecipeInput;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.World;
import java.util.List;

public final class CellProcessorRecipe implements Recipe<CellProcessorRecipe.Input> {
    private final List<Ingredient> ingredients;
    private final ItemStack output;
    public CellProcessorRecipe(List<Ingredient> ingredients,ItemStack output) {
        if(ingredients.size()!=2 || output.isEmpty()) throw new IllegalArgumentException("Cell processor needs two inputs and an output");
        this.ingredients=List.copyOf(ingredients);this.output=output.copy();
    }
    public record Input(ItemStack sample,ItemStack reagent) implements RecipeInput {
        @Override public ItemStack getStackInSlot(int index) { return switch(index) { case 0->sample;case 1->reagent;default->throw new IndexOutOfBoundsException(index); }; }
        @Override public int getSize() { return 2; }
    }
    @Override public boolean matches(Input input,World world) { return ingredients.get(0).test(input.sample()) && ingredients.get(1).test(input.reagent()); }
    @Override public ItemStack craft(Input input,RegistryWrapper.WrapperLookup lookup) { return output.copy(); }
    @Override public boolean fits(int width,int height) { return width*height>=2; }
    @Override public ItemStack getResult(RegistryWrapper.WrapperLookup lookup) { return output.copy(); }
    public ItemStack getOutput() { return output.copy(); }
    @Override public DefaultedList<Ingredient> getIngredients() { DefaultedList<Ingredient> list=DefaultedList.of();list.addAll(ingredients);return list; }
    @Override public RecipeSerializer<?> getSerializer() { return Serializer.INSTANCE; }
    @Override public RecipeType<?> getType() { return Type.INSTANCE; }
    public static final class Type implements RecipeType<CellProcessorRecipe> {
        public static final Type INSTANCE=new Type(); public static final String ID="cell_processing";
    }
    public static final class Serializer implements RecipeSerializer<CellProcessorRecipe> {
        public static final Serializer INSTANCE=new Serializer();public static final String ID="cell_processing";
        private static final MapCodec<CellProcessorRecipe> CODEC=RecordCodecBuilder.mapCodec(instance->instance.group(
            Ingredient.DISALLOW_EMPTY_CODEC.listOf().fieldOf("ingredients").forGetter(recipe->recipe.ingredients),
            ItemStack.CODEC.fieldOf("output").forGetter(recipe->recipe.output)
        ).apply(instance,CellProcessorRecipe::new));
        private static final PacketCodec<RegistryByteBuf,CellProcessorRecipe> PACKET_CODEC=new PacketCodec<>() {
            public CellProcessorRecipe decode(RegistryByteBuf buf) {
                return new CellProcessorRecipe(List.of(Ingredient.PACKET_CODEC.decode(buf),Ingredient.PACKET_CODEC.decode(buf)),ItemStack.PACKET_CODEC.decode(buf));
            }
            public void encode(RegistryByteBuf buf,CellProcessorRecipe value) {
                Ingredient.PACKET_CODEC.encode(buf,value.ingredients.get(0));Ingredient.PACKET_CODEC.encode(buf,value.ingredients.get(1));ItemStack.PACKET_CODEC.encode(buf,value.output);
            }
        };
        @Override public MapCodec<CellProcessorRecipe> codec() { return CODEC; }
        @Override public PacketCodec<RegistryByteBuf,CellProcessorRecipe> packetCodec() { return PACKET_CODEC; }
    }
}
