package net.monsterhuntervillager.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.monsterhuntervillager.MonsterHunterVillager;
import net.neoforged.neoforge.fluids.FluidType;
import org.jetbrains.annotations.Nullable;

/**
 * A trap lying on the ground. Traps are (immobile) mobs so they can be hit, pushed by nothing,
 * and rendered with an entity model. Each trap wears out as it holds or damages things, and
 * disappears after 3000 ticks, when no player is within 400 blocks, or when buried in a block.
 */
public abstract class AbstractTrapEntity extends PathfinderMob {
    public static final TagKey<EntityType<?>> TRAPS = TagKey.create(Registries.ENTITY_TYPE, MonsterHunterVillager.id("traps"));
    public static final ResourceKey<DamageType> TRAP_DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE, MonsterHunterVillager.id("trap"));

    private static final EntityDataAccessor<String> DATA_OWNER = SynchedEntityData.defineId(AbstractTrapEntity.class, EntityDataSerializers.STRING);
    private static final int DESPAWN_AGE = 3000;
    private static final double PLAYER_PRESENCE_RANGE = 400.0;
    protected static final Vec3 STUCK = new Vec3(0.25, 0.05, 0.25);

    /** Accumulated wear; the trap breaks at {@link TrapKind#breakThreshold()}. */
    protected double wear;
    /** Ticks left during which something is (or was just) standing in the trap. */
    protected int inUse;
    private int age;

    protected AbstractTrapEntity(EntityType<? extends AbstractTrapEntity> type, Level level) {
        super(type, level);
        this.xpReward = 0;
        setNoAi(false);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.MAX_HEALTH, 10.0)
                .add(Attributes.ARMOR, 0.0)
                .add(Attributes.ATTACK_DAMAGE, 3.0)
                .add(Attributes.FOLLOW_RANGE, 16.0);
    }

    /** The kind of trap, or {@code null} for the inert preview trap. */
    @Nullable
    public abstract TrapKind kind();

    /** Applies this trap's effect to whatever is caught in it. Runs on both sides. */
    protected abstract void affectVictims();

    // ---- owner -------------------------------------------------------------------------------

    public String ownerId() {
        return entityData.get(DATA_OWNER);
    }

    public void setOwner(Entity owner) {
        entityData.set(DATA_OWNER, owner.getStringUUID());
    }

    public boolean isOwnedBy(Entity entity) {
        return entity.getStringUUID().equals(ownerId());
    }

    public boolean isInUse() {
        return inUse > 0;
    }

    // ---- ticking -----------------------------------------------------------------------------

    @Override
    public void baseTick() {
        super.baseTick();
        if (kind() != null) {
            trapTick();
        }
    }

    private void trapTick() {
        affectVictims();
        setHealth(getMaxHealth());
        if (inUse > 0) {
            inUse--;
        }

        boolean server = !level().isClientSide;
        if (server && wear >= kind().breakThreshold()) {
            shatter();
        }
        if (server && age >= DESPAWN_AGE) {
            discard();
        }
        age++;

        setDeltaMovement(0.0, getDeltaMovement().y - 0.5, 0.0);

        if (server && !playerWithinRange()) {
            discard();
        }
        if (server && isInWall()) {
            discard();
            ItemEntity drop = new ItemEntity(level(), getX(), getY(), getZ(), new ItemStack(kind().item()));
            drop.setPickUpDelay(10);
            level().addFreshEntity(drop);
        }

        // Traps never turn: pin body and head to the placement yaw.
        setYBodyRot(getYRot());
        setYHeadRot(getYRot());
        yRotO = getYRot();
        xRotO = getXRot();
        yBodyRotO = getYRot();
        yHeadRotO = getYRot();
    }

    /** Whether any player is inside the 800-block cube the original checked, without an 800-block entity search. */
    private boolean playerWithinRange() {
        AABB range = AABB.ofSize(position(), PLAYER_PRESENCE_RANGE * 2, PLAYER_PRESENCE_RANGE * 2, PLAYER_PRESENCE_RANGE * 2);
        for (Player player : level().players()) {
            if (player.getBoundingBox().intersects(range)) {
                return true;
            }
        }
        return false;
    }

    /** Breaks the trap: gone, with a wither-crunch and a burst of crit particles. */
    protected void shatter() {
        discard();
        level().playSound(null, BlockPos.containing(getX(), getY(), getZ()), SoundEvents.WITHER_BREAK_BLOCK, SoundSource.NEUTRAL, 1.0F, 1.0F);
        if (level() instanceof ServerLevel server) {
            for (int i = 0; i < 20; i++) {
                spawnCrit(server);
            }
        }
    }

    protected void spawnCrit(ServerLevel level) {
        level.sendParticles(ParticleTypes.CRIT,
                getX() + Mth.nextDouble(random, -0.3, 0.3),
                getY() + Mth.nextDouble(random, 0.0, 0.3),
                getZ() + Mth.nextDouble(random, -0.3, 0.3),
                1, 0.1, 0.1, 0.1, 0.1);
    }

    protected DamageSource trapDamage() {
        return level().damageSources().source(TRAP_DAMAGE);
    }

    // ---- interaction -------------------------------------------------------------------------

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // Traps refill their health every tick, so a normal death never completes; /kill removes them outright.
        if (source.is(DamageTypes.GENERIC_KILL)) {
            discard();
            return true;
        }
        // Every blow (even one the trap then ignores) rattles the chain and wears the trap.
        if (kind() != null && level() instanceof ServerLevel server) {
            server.playSound(null, BlockPos.containing(getX(), getY(), getZ()), SoundEvents.CHAIN_HIT, SoundSource.NEUTRAL, 1.0F, 0.5F);
            wear += 10.0;
            for (int i = 0; i < 6; i++) {
                spawnCrit(server);
            }
        }
        if (ignoresDamage(source)) {
            return false;
        }
        return super.hurt(source, amount);
    }

    private static boolean ignoresDamage(DamageSource source) {
        Entity direct = source.getDirectEntity();
        return source.is(DamageTypes.IN_FIRE)
                || direct instanceof AbstractArrow
                || direct instanceof Player
                || direct instanceof ThrownPotion
                || direct instanceof AreaEffectCloud
                || source.is(DamageTypes.FALL)
                || source.is(DamageTypes.CACTUS)
                || source.is(DamageTypes.DROWN)
                || source.is(DamageTypes.LIGHTNING_BOLT)
                || source.is(DamageTypes.EXPLOSION)
                || source.is(DamageTypes.TRIDENT)
                || source.is(DamageTypes.FALLING_ANVIL)
                || source.is(DamageTypes.DRAGON_BREATH)
                || source.is(DamageTypes.WITHER)
                || source.is(DamageTypes.WITHER_SKULL);
    }

    /** The owner right-clicking the trap picks it back up, unless it is too worn, in which case it breaks. */
    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        InteractionResult vanilla = super.mobInteract(player, hand);
        if (kind() == null) {
            return vanilla;
        }
        if (!level().isClientSide && isOwnedBy(player)) {
            if (wear < kind().reclaimThreshold()) {
                ItemEntity drop = new ItemEntity(level(), getX(), getY(), getZ(), new ItemStack(kind().item()));
                drop.setPickUpDelay(4);
                level().addFreshEntity(drop);
                level().playSound(null, BlockPos.containing(getX(), getY(), getZ()), SoundEvents.ITEM_PICKUP, SoundSource.NEUTRAL, 1.0F, 1.0F);
                discard();
            } else {
                shatter();
            }
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    // ---- physics -----------------------------------------------------------------------------

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public boolean canDrownInFluidType(FluidType type) {
        return false;
    }

    @Override
    public boolean isPushedByFluid(FluidType type) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(Entity entity) {
    }

    @Override
    protected void pushEntities() {
    }

    // ---- persistence -------------------------------------------------------------------------

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_OWNER, "");
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("Dataowner", ownerId());
        tag.putDouble("TrapWear", wear);
        tag.putInt("TrapInUse", inUse);
        tag.putInt("TrapAge", age);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Dataowner")) {
            entityData.set(DATA_OWNER, tag.getString("Dataowner"));
        }
        wear = tag.getDouble("TrapWear");
        inUse = tag.getInt("TrapInUse");
        age = tag.getInt("TrapAge");
    }
}
