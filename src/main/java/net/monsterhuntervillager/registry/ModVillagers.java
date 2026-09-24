package net.monsterhuntervillager.registry;

import com.google.common.collect.ImmutableSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.monsterhuntervillager.MonsterHunterVillager;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModVillagers {
    public static final DeferredRegister<PoiType> POI_TYPES = DeferredRegister.create(Registries.POINT_OF_INTEREST_TYPE, MonsterHunterVillager.MODID);
    public static final DeferredRegister<VillagerProfession> PROFESSIONS = DeferredRegister.create(Registries.VILLAGER_PROFESSION, MonsterHunterVillager.MODID);

    public static final DeferredHolder<PoiType, PoiType> MONSTER_HUNTER_POI = POI_TYPES.register("monster_hunter",
            () -> new PoiType(ImmutableSet.copyOf(ModBlocks.HUNTERS_TABLE.get().getStateDefinition().getPossibleStates()), 1, 1));

    public static final DeferredHolder<VillagerProfession, VillagerProfession> MONSTER_HUNTER = PROFESSIONS.register("monster_hunter",
            () -> new VillagerProfession(
                    MonsterHunterVillager.MODID + ":monster_hunter",
                    poi -> poi.is(MONSTER_HUNTER_POI.getKey()),
                    poi -> poi.is(MONSTER_HUNTER_POI.getKey()),
                    ImmutableSet.of(),
                    ImmutableSet.of(),
                    SoundEvents.VILLAGER_WORK_CARTOGRAPHER));

    private ModVillagers() {
    }
}
