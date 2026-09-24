package net.monsterhuntervillager;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.monsterhuntervillager.registry.ModAttachments;
import net.monsterhuntervillager.registry.ModBlocks;
import net.monsterhuntervillager.registry.ModEntities;
import net.monsterhuntervillager.registry.ModItems;
import net.monsterhuntervillager.registry.ModMenus;
import net.monsterhuntervillager.registry.ModRecipes;
import net.monsterhuntervillager.registry.ModTabs;
import net.monsterhuntervillager.registry.ModVillagers;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(MonsterHunterVillager.MODID)
public final class MonsterHunterVillager {
    public static final String MODID = "monster_hunter_villager";
    public static final Logger LOGGER = LogUtils.getLogger();

    public MonsterHunterVillager(IEventBus modBus) {
        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModEntities.ENTITIES.register(modBus);
        ModMenus.MENUS.register(modBus);
        ModRecipes.TYPES.register(modBus);
        ModRecipes.SERIALIZERS.register(modBus);
        ModVillagers.POI_TYPES.register(modBus);
        ModVillagers.PROFESSIONS.register(modBus);
        ModAttachments.ATTACHMENTS.register(modBus);

        modBus.addListener(ModEntities::registerAttributes);
        modBus.addListener(ModTabs::buildContents);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
