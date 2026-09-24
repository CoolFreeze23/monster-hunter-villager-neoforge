package net.monsterhuntervillager.compat.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.monsterhuntervillager.MonsterHunterVillager;
import net.monsterhuntervillager.recipe.HuntersWorkbenchRecipe;
import net.monsterhuntervillager.registry.ModItems;

/**
 * Uses only API that every JEI 19 release has (a background drawable rather than the newer
 * width/height/draw trio), so the plugin works with whichever JEI a 1.21.1 pack ships.
 */
public class HuntersWorkbenchCategory implements IRecipeCategory<RecipeHolder<HuntersWorkbenchRecipe>> {
    private final IDrawable background;
    private final IDrawable icon;

    public HuntersWorkbenchCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createDrawable(MonsterHunterVillager.id("textures/screens/hunters_table_jei_slots_only.png"), 0, 0, 112, 88);
        this.icon = guiHelper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(ModItems.HUNTERS_TABLE.get()));
    }

    @Override
    public RecipeType<RecipeHolder<HuntersWorkbenchRecipe>> getRecipeType() {
        return MonsterHunterJeiPlugin.HUNTERS_WORKBENCH;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.monster_hunter_villager.hunters_workbench");
    }

    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<HuntersWorkbenchRecipe> holder, IFocusGroup focuses) {
        HuntersWorkbenchRecipe recipe = holder.value();
        builder.addSlot(RecipeIngredientRole.INPUT, 17, 56).addIngredients(recipe.prototype());
        builder.addSlot(RecipeIngredientRole.INPUT, 17, 17).addIngredients(recipe.addition());
        builder.addSlot(RecipeIngredientRole.OUTPUT, 80, 36).addItemStack(recipe.getResultItem(RegistryAccess.EMPTY));
    }

}
