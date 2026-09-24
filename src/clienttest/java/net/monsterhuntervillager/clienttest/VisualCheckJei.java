package net.monsterhuntervillager.clienttest;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.ResourceLocation;
import net.monsterhuntervillager.MonsterHunterVillager;
import net.monsterhuntervillager.compat.jei.MonsterHunterJeiPlugin;
import net.neoforged.fml.ModList;

import java.util.List;

/** Grabs JEI's runtime so the visual check can open the Hunter's Workbench recipe page. */
@JeiPlugin
public class VisualCheckJei implements IModPlugin {
    private static volatile IJeiRuntime runtime;

    @Override
    public ResourceLocation getPluginUid() {
        return MonsterHunterVillager.id("visual_check");
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
    }

    /** Sets JEI's item-list search text (no-op without JEI). */
    static void filter(String text) {
        if (ModList.get().isLoaded("jei") && runtime != null) {
            runtime.getIngredientFilter().setFilterText(text);
        }
    }

    /** Opens the workbench recipe page; false if JEI isn't installed or not ready. */
    static boolean showWorkbenchRecipes() {
        if (!ModList.get().isLoaded("jei") || runtime == null) {
            return false;
        }
        runtime.getRecipesGui().showTypes(List.of(MonsterHunterJeiPlugin.HUNTERS_WORKBENCH));
        return true;
    }
}
