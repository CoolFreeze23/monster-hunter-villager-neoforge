package net.monsterhuntervillager.recipe;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.monsterhuntervillager.registry.ModRecipes;

import java.util.List;

/**
 * A Hunter's Workbench recipe: trap prototype + addition = trap. The original only used these
 * JSON files to show recipes in JEI and hard-coded the actual crafting in a screen-tick procedure;
 * the workbench now crafts from them, so datapacks can add or change trap recipes.
 */
public record HuntersWorkbenchRecipe(Ingredient prototype, Ingredient addition, ItemStack output) implements Recipe<HuntersWorkbenchInput> {
    @Override
    public boolean matches(HuntersWorkbenchInput input, Level level) {
        return prototype.test(input.prototype()) && addition.test(input.addition());
    }

    @Override
    public ItemStack assemble(HuntersWorkbenchInput input, HolderLookup.Provider registries) {
        return output.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return output;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return NonNullList.of(Ingredient.EMPTY, prototype, addition);
    }

    /** Kept out of the vanilla recipe book, which has no category for it. */
    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.HUNTERS_WORKBENCH_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.HUNTERS_WORKBENCH.get();
    }

    public static final class Serializer implements RecipeSerializer<HuntersWorkbenchRecipe> {
        // Same JSON shape as the original: {"ingredients": [prototype, addition], "output": {...}}
        private static final MapCodec<HuntersWorkbenchRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Ingredient.CODEC_NONEMPTY.listOf().fieldOf("ingredients")
                        .flatXmap(Serializer::exactlyTwo, Serializer::exactlyTwo)
                        .forGetter(recipe -> List.of(recipe.prototype(), recipe.addition())),
                ItemStack.STRICT_CODEC.fieldOf("output").forGetter(HuntersWorkbenchRecipe::output)
        ).apply(instance, (ingredients, output) -> new HuntersWorkbenchRecipe(ingredients.get(0), ingredients.get(1), output)));

        private static final StreamCodec<RegistryFriendlyByteBuf, HuntersWorkbenchRecipe> STREAM_CODEC = StreamCodec.composite(
                Ingredient.CONTENTS_STREAM_CODEC, HuntersWorkbenchRecipe::prototype,
                Ingredient.CONTENTS_STREAM_CODEC, HuntersWorkbenchRecipe::addition,
                ItemStack.STREAM_CODEC, HuntersWorkbenchRecipe::output,
                HuntersWorkbenchRecipe::new);

        private static DataResult<List<Ingredient>> exactlyTwo(List<Ingredient> ingredients) {
            return ingredients.size() == 2
                    ? DataResult.success(ingredients)
                    : DataResult.error(() -> "Hunter's Workbench recipes take exactly 2 ingredients, got " + ingredients.size());
        }

        @Override
        public MapCodec<HuntersWorkbenchRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, HuntersWorkbenchRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
