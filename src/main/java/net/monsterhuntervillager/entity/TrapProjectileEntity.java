package net.monsterhuntervillager.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

/** A trap in flight. It deploys the matching trap where it lands, owned by whoever threw it. */
public class TrapProjectileEntity extends AbstractArrow implements ItemSupplier {
    private TrapKind kind;
    private ItemStack displayStack;

    public TrapProjectileEntity(EntityType<? extends TrapProjectileEntity> type, Level level) {
        super(type, level);
    }

    private TrapProjectileEntity(TrapKind kind, LivingEntity shooter, Level level) {
        super(kind.projectileType(), shooter, level, new ItemStack(kind.item()), null);
    }

    public static TrapProjectileEntity create(TrapKind kind, Level level, LivingEntity shooter) {
        TrapProjectileEntity projectile = new TrapProjectileEntity(kind, shooter, level);
        projectile.setBaseDamage(0.0);
        projectile.setSilent(true);
        // Never leave a pickable item behind: the trap itself is what comes back.
        projectile.pickup = Pickup.DISALLOWED;
        return projectile;
    }

    public TrapKind kind() {
        if (kind == null) {
            kind = TrapKind.byProjectileType(getType());
        }
        return kind;
    }

    @Override
    public ItemStack getItem() {
        if (displayStack == null) {
            displayStack = new ItemStack(kind().item());
        }
        return displayStack;
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return new ItemStack(kind().item());
    }

    @Override
    public void tick() {
        super.tick();
        if (inGround) {
            discard();
        }
    }

    @Override
    protected void doPostHurtEffects(LivingEntity target) {
        super.doPostHurtEffects(target);
        target.setArrowCount(target.getArrowCount() - 1);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        Entity victim = result.getEntity();
        Entity owner = getOwner();
        if (owner == null) {
            return;
        }
        // Runs on both sides like the original so a player hit in the face is slowed client-side too.
        victim.makeStuckInBlock(Blocks.AIR.defaultBlockState(), AbstractTrapEntity.STUCK);
        if (level() instanceof ServerLevel server) {
            BlockPos at = BlockPos.containing(
                    victim.getX() + victim.getBbWidth() * Mth.nextDouble(random, -0.4, 0.4),
                    victim.getY(),
                    victim.getZ() + victim.getBbWidth() * Mth.nextDouble(random, -0.4, 0.4));
            deploy(server, at, BlockPos.containing(getX(), getY(), getZ()), owner);
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        Entity owner = getOwner();
        if (owner == null || !(level() instanceof ServerLevel server)) {
            return;
        }
        BlockPos above = result.getBlockPos().above();
        if (!server.getBlockState(above).canOcclude()) {
            deploy(server, above, above, owner);
        } else {
            BlockPos here = BlockPos.containing(getX(), getY(), getZ());
            deploy(server, here, here, owner);
        }
    }

    private void deploy(ServerLevel level, BlockPos at, BlockPos soundAt, Entity owner) {
        spawnOwnedTrap(level, kind().trapType(), at, owner);
        level.playSound(null, soundAt, kind().landingSound(), SoundSource.NEUTRAL, 1.0F, kind().landingPitch());
        level.playSound(null, soundAt, SoundEvents.CHAIN_PLACE, SoundSource.NEUTRAL, 1.0F, 0.0F);
        discard();
    }

    /**
     * Spawns the trap with its owner already set, so the owner travels with the trap's spawn data.
     * Setting it after spawning left clients one tick where the trap had no owner, and a soul chain
     * trap would chain (and tug) the player who had just thrown it.
     */
    private <T extends AbstractTrapEntity> void spawnOwnedTrap(ServerLevel level, EntityType<T> type, BlockPos at, Entity owner) {
        type.spawn(level, trap -> {
            trap.setYRot((float) Mth.nextDouble(random, -180.0, 180.0));
            trap.setYBodyRot((float) Mth.nextDouble(random, -180.0, 180.0));
            trap.setYHeadRot((float) Mth.nextDouble(random, -180.0, 180.0));
            trap.setDeltaMovement(0.0, 0.0, 0.0);
            trap.setOwner(owner);
        }, at, MobSpawnType.MOB_SUMMONED, false, false);
    }
}
