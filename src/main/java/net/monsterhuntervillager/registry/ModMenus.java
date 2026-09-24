package net.monsterhuntervillager.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.monsterhuntervillager.MonsterHunterVillager;
import net.monsterhuntervillager.menu.HuntersTableMenu;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, MonsterHunterVillager.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<HuntersTableMenu>> HUNTERS_TABLE =
            MENUS.register("hunters_table_gui", () -> IMenuTypeExtension.create(HuntersTableMenu::new));

    private ModMenus() {
    }
}
