package net.monsterhuntervillager.registry;

import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

public final class ModTabs {
    private ModTabs() {
    }

    public static void buildContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(ModItems.HUNTERS_TABLE.get());
        } else if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            event.accept(ModItems.STICKY_TRAP_ITEM.get());
            event.accept(ModItems.SHARPENED_TRAP_ITEM.get());
            event.accept(ModItems.SOUL_CHAIN_TRAP_ITEM.get());
            event.accept(ModItems.HUNTERS_KNIFE.get());
        } else if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(ModItems.TRAP_PROTOTYPE.get());
        }
    }
}
