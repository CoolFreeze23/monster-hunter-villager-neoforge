package net.monsterhuntervillager.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.monsterhuntervillager.MonsterHunterVillager;
import net.monsterhuntervillager.entity.AbstractTrapEntity;
import net.monsterhuntervillager.entity.EmptyTrapEntity;
import net.monsterhuntervillager.entity.SharpenedTrapEntity;
import net.monsterhuntervillager.entity.SoulChainTrapEntity;
import net.monsterhuntervillager.entity.StickyTrapEntity;
import net.monsterhuntervillager.entity.TrapProjectileEntity;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, MonsterHunterVillager.MODID);

    // Placed traps. MONSTER category as in the original; being persistent, they never count
    // toward the natural-spawning mob cap.
    public static final DeferredHolder<EntityType<?>, EntityType<StickyTrapEntity>> STICKY_TRAP =
            trap("sticky_trap", StickyTrapEntity::new, 0.8F, 0.25F);
    public static final DeferredHolder<EntityType<?>, EntityType<SoulChainTrapEntity>> SOUL_CHAIN_TRAP =
            trap("soul_chain_trap", SoulChainTrapEntity::new, 0.4F, 0.4F);
    public static final DeferredHolder<EntityType<?>, EntityType<EmptyTrapEntity>> EMPTY_TRAP =
            trap("empty_trap", EmptyTrapEntity::new, 0.8F, 0.25F);
    public static final DeferredHolder<EntityType<?>, EntityType<SharpenedTrapEntity>> SHARPENED_TRAP =
            trap("sharpened_trap", SharpenedTrapEntity::new, 0.8F, 0.25F);

    // Thrown traps; registry names kept from the original so commands and datapacks still match.
    public static final DeferredHolder<EntityType<?>, EntityType<TrapProjectileEntity>> STICKY_TRAP_PROJECTILE =
            projectile("projectile_sticky_trap_projectile_projectile");
    public static final DeferredHolder<EntityType<?>, EntityType<TrapProjectileEntity>> SOUL_CHAIN_TRAP_PROJECTILE =
            projectile("projectile_soul_chain_trap_projectile");
    public static final DeferredHolder<EntityType<?>, EntityType<TrapProjectileEntity>> SHARPENED_TRAP_PROJECTILE =
            projectile("projectile_sharpened_trap_projectile");

    private ModEntities() {
    }

    private static <T extends AbstractTrapEntity> DeferredHolder<EntityType<?>, EntityType<T>> trap(
            String name, EntityType.EntityFactory<T> factory, float width, float height) {
        return ENTITIES.register(name, () -> EntityType.Builder.of(factory, MobCategory.MONSTER)
                .sized(width, height)
                .fireImmune()
                .clientTrackingRange(8)
                .updateInterval(3)
                .build(name));
    }

    private static DeferredHolder<EntityType<?>, EntityType<TrapProjectileEntity>> projectile(String name) {
        return ENTITIES.register(name, () -> EntityType.Builder.<TrapProjectileEntity>of(TrapProjectileEntity::new, MobCategory.MISC)
                .sized(0.5F, 0.5F)
                .clientTrackingRange(4)
                .updateInterval(1)
                .build(name));
    }

    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(STICKY_TRAP.get(), AbstractTrapEntity.createAttributes().build());
        event.put(SOUL_CHAIN_TRAP.get(), AbstractTrapEntity.createAttributes().build());
        event.put(EMPTY_TRAP.get(), AbstractTrapEntity.createAttributes().build());
        event.put(SHARPENED_TRAP.get(), AbstractTrapEntity.createAttributes().build());
    }
}
