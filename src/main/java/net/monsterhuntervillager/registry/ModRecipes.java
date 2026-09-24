package net.monsterhuntervillager.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.monsterhuntervillager.MonsterHunterVillager;
import net.monsterhuntervillager.recipe.HuntersWorkbenchRecipe;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModRecipes {
    public static final DeferredRegister<RecipeType<?>> TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, MonsterHunterVillager.MODID);
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, MonsterHunterVillager.MODID);

    // The original id is kept so existing datapack recipes keep loading.
    public static final DeferredHolder<RecipeType<?>, RecipeType<HuntersWorkbenchRecipe>> HUNTERS_WORKBENCH =
            TYPES.register("hunters_workbench_jei", () -> RecipeType.simple(MonsterHunterVillager.id("hunters_workbench_jei")));
    public static final DeferredHolder<RecipeSerializer<?>, HuntersWorkbenchRecipe.Serializer> HUNTERS_WORKBENCH_SERIALIZER =
            SERIALIZERS.register("hunters_workbench_jei", HuntersWorkbenchRecipe.Serializer::new);

    private ModRecipes() {
    }
}
