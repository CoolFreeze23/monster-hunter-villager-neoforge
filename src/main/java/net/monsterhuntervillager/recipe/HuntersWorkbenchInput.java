package net.monsterhuntervillager.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

/** The Hunter's Workbench inputs: the trap prototype (bottom slot) and the addition (top slot). */
public record HuntersWorkbenchInput(ItemStack prototype, ItemStack addition) implements RecipeInput {
    @Override
    public ItemStack getItem(int index) {
        return switch (index) {
            case 0 -> prototype;
            case 1 -> addition;
            default -> throw new IllegalArgumentException("No item for index " + index);
        };
    }

    @Override
    public int size() {
        return 2;
    }
}
