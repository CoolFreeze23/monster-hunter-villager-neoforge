package net.monsterhuntervillager.entity;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.monsterhuntervillager.registry.ModEntities;
import net.monsterhuntervillager.registry.ModItems;

import java.util.function.Supplier;

/**
 * The three deployable traps. The original had a separate copy of every item, projectile,
 * landing procedure and pickup procedure per trap; the numbers that actually differed live here.
 */
public enum TrapKind {
    STICKY(ModItems.STICKY_TRAP_ITEM::get, ModEntities.STICKY_TRAP::get, ModEntities.STICKY_TRAP_PROJECTILE::get,
            0.6F, () -> SoundEvents.SLIME_BLOCK_PLACE, 1.0F, 250.0, 166.0),
    SHARPENED(ModItems.SHARPENED_TRAP_ITEM::get, ModEntities.SHARPENED_TRAP::get, ModEntities.SHARPENED_TRAP_PROJECTILE::get,
            0.6F, () -> SoundEvents.ARMOR_EQUIP_CHAIN.value(), 0.5F, 250.0, 166.0),
    SOUL_CHAIN(ModItems.SOUL_CHAIN_TRAP_ITEM::get, ModEntities.SOUL_CHAIN_TRAP::get, ModEntities.SOUL_CHAIN_TRAP_PROJECTILE::get,
            0.7F, () -> SoundEvents.SOUL_ESCAPE.value(), 1.0F, 400.0, 286.0);

    private final Supplier<Item> item;
    private final Supplier<? extends EntityType<? extends AbstractTrapEntity>> trapType;
    private final Supplier<EntityType<TrapProjectileEntity>> projectileType;
    private final float throwSpeed;
    private final Supplier<SoundEvent> landingSound;
    private final float landingPitch;
    private final double breakThreshold;
    private final double reclaimThreshold;

    TrapKind(Supplier<Item> item,
             Supplier<? extends EntityType<? extends AbstractTrapEntity>> trapType,
             Supplier<EntityType<TrapProjectileEntity>> projectileType,
             float throwSpeed,
             Supplier<SoundEvent> landingSound,
             float landingPitch,
             double breakThreshold,
             double reclaimThreshold) {
        this.item = item;
        this.trapType = trapType;
        this.projectileType = projectileType;
        this.throwSpeed = throwSpeed;
        this.landingSound = landingSound;
        this.landingPitch = landingPitch;
        this.breakThreshold = breakThreshold;
        this.reclaimThreshold = reclaimThreshold;
    }

    public Item item() {
        return item.get();
    }

    public EntityType<? extends AbstractTrapEntity> trapType() {
        return trapType.get();
    }

    public EntityType<TrapProjectileEntity> projectileType() {
        return projectileType.get();
    }

    /** Speed the trap is thrown at, by players and by hunters alike. */
    public float throwSpeed() {
        return throwSpeed;
    }

    public SoundEvent landingSound() {
        return landingSound.get();
    }

    public float landingPitch() {
        return landingPitch;
    }

    /** Wear at which the trap snaps on its own. */
    public double breakThreshold() {
        return breakThreshold;
    }

    /** Wear below which the owner gets the item back when picking the trap up; at or above it the trap just breaks. */
    public double reclaimThreshold() {
        return reclaimThreshold;
    }

    public static TrapKind byProjectileType(EntityType<?> type) {
        for (TrapKind kind : values()) {
            if (kind.projectileType() == type) {
                return kind;
            }
        }
        throw new IllegalStateException("Not a trap projectile: " + type);
    }

    public static TrapKind byItem(Item item) {
        for (TrapKind kind : values()) {
            if (kind.item() == item) {
                return kind;
            }
        }
        return null;
    }
}
