package net.monsterhuntervillager.compat.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.monsterhuntervillager.MonsterHunterVillager;
import net.monsterhuntervillager.recipe.HuntersWorkbenchRecipe;
import net.monsterhuntervillager.registry.ModItems;
import net.monsterhuntervillager.registry.ModRecipes;

/** Shows the Hunter's Workbench trap recipes in JEI. Loaded by JEI only when JEI is installed. */
@JeiPlugin
public class MonsterHunterJeiPlugin implements IModPlugin {
    public static final RecipeType<RecipeHolder<HuntersWorkbenchRecipe>> HUNTERS_WORKBENCH =
            RecipeType.createRecipeHolderType(MonsterHunterVillager.id("hunters_workbench_jei"));

    @Override
    public ResourceLocation getPluginUid() {
        return MonsterHunterVillager.id("jei_plugin");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new HuntersWorkbenchCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        var level = Minecraft.getInstance().level;
        if (level != null) {
            registration.addRecipes(HUNTERS_WORKBENCH, level.getRecipeManager().getAllRecipesFor(ModRecipes.HUNTERS_WORKBENCH.get()));
        }
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(ModItems.HUNTERS_TABLE.get(), HUNTERS_WORKBENCH);
    }
}
