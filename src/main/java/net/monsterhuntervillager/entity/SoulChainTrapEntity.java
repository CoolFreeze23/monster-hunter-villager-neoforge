package net.monsterhuntervillager.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.monsterhuntervillager.hunter.HunterCombat;

/**
 * Holds everything within 4 blocks except its owner, and drags everything within reach
 * toward itself along a chain of soul fire.
 *
 * <p>The original implemented the drag from the victims' side: every living entity in the
 * world, every tick, on both sides, ran up to fifteen sorted searches for nearby soul chain
 * traps (plus a second copy of the logic for players). That per-entity search was the bulk of
 * the mod's server load. Here each trap looks for its own victims instead, so the cost scales
 * with the number of soul chain traps, not the number of mobs.
 */
public class SoulChainTrapEntity extends AbstractTrapEntity {
    private static final double HOLD_RADIUS = 4.0;
    /** A victim is dragged by a trap whose box touches a 9x9x9 box around the victim. */
    private static final double DRAG_REACH = 9.0;
    private static final double DRAG_STRENGTH = 0.02;

    public SoulChainTrapEntity(EntityType<? extends SoulChainTrapEntity> type, Level level) {
        super(type, level);
    }

    @Override
    public TrapKind kind() {
        return TrapKind.SOUL_CHAIN;
    }

    @Override
    protected void affectVictims() {
        holdVictims();
        dragVictims();
    }

    private void holdVictims() {
        Vec3 center = position();
        boolean client = level().isClientSide;
        for (LivingEntity victim : level().getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(HOLD_RADIUS),
                e -> !e.getType().is(TRAPS) && !isOwnedBy(e))) {
            victim.makeStuckInBlock(Blocks.AIR.defaultBlockState(), STUCK);
            double drag = 100.0F / (victim.getBbWidth() * victim.getBbHeight());
            Vec3 motion = victim.getDeltaMovement();
            victim.setDeltaMovement(motion.x / drag, motion.y / drag, motion.z / drag);
            HunterCombat.markTrapped(victim);
            if (HunterCombat.isMoving(victim)) {
                wear += 1.0;
                if (random.nextDouble() <= 0.04 && level() instanceof ServerLevel server) {
                    spawnCrit(server);
                }
            }
            if (client) {
                for (int i = 0; i < 4; i++) {
                    double angle = Math.PI * 2 / 4 * i;
                    level().addParticle(ParticleTypes.SOUL_FIRE_FLAME,
                            getX() + Math.cos(angle) * 0.2, getY() + getBbHeight() * 0.9, getZ() + Math.sin(angle) * 0.2,
                            0.0, 0.01, 0.0);
                }
            }
        }
    }

    private void dragVictims() {
        boolean client = level().isClientSide;
        AABB search = getBoundingBox().inflate(DRAG_REACH / 2);
        for (LivingEntity victim : level().getEntitiesOfClass(LivingEntity.class, search, e -> e != this && !e.getType().is(TRAPS))) {
            AABB reach = AABB.ofSize(victim.position(), DRAG_REACH, DRAG_REACH, DRAG_REACH);
            if (!getBoundingBox().intersects(reach) || nearestTrapTo(victim, reach) != this || isOwnedBy(victim)) {
                continue;
            }
            // Players went through the drag twice in the original (living tick and player tick).
            int passes = victim instanceof Player ? 2 : 1;
            // Players move client-side, so only their own client can drag them; everything else is dragged by the server.
            boolean moves = victim instanceof Player player ? player.isLocalPlayer() : !client;
            if (moves) {
                double strength = DRAG_STRENGTH * passes;
                victim.setDeltaMovement(victim.getDeltaMovement().add(
                        (getX() - victim.getX()) * strength,
                        (getY() - victim.getY()) * strength,
                        (getZ() - victim.getZ()) * strength));
            }
            if (client) {
                for (int pass = 0; pass < passes; pass++) {
                    drawChain(victim);
                }
            }
        }
    }

    /** The victim is dragged only by the closest soul chain trap in reach. */
    private SoulChainTrapEntity nearestTrapTo(LivingEntity victim, AABB reach) {
        SoulChainTrapEntity nearest = null;
        double best = Double.MAX_VALUE;
        for (SoulChainTrapEntity trap : level().getEntitiesOfClass(SoulChainTrapEntity.class, reach)) {
            double distance = trap.distanceToSqr(victim.getX(), victim.getY(), victim.getZ());
            if (distance < best) {
                best = distance;
                nearest = trap;
            }
        }
        return nearest;
    }

    /**
     * The chain of flames from trap to victim and the ring around the victim. The original sent the
     * chain from the server as 20 particle packets per victim per tick; it is drawn client-side now.
     */
    private void drawChain(LivingEntity victim) {
        double trackX = getX() - victim.getX();
        double trackY = getY() - victim.getY() + getBbHeight() * 0.5 - victim.getBbHeight() * 0.5;
        double trackZ = getZ() - victim.getZ();
        double grow = 0.0;
        for (int i = 0; i < 20; i++) {
            level().addParticle(ParticleTypes.SOUL_FIRE_FLAME,
                    getX() + trackX * grow + random.nextGaussian() * 0.01,
                    getY() + getBbHeight() * 0.5 + trackY * grow + random.nextGaussian() * 0.01,
                    getZ() + trackZ * grow + random.nextGaussian() * 0.01,
                    0.0, 0.0, 0.0);
            grow -= 0.05;
        }

        double count = victim.getBbWidth() * 16.0F;
        double radius = victim.getBbWidth() * 0.6;
        for (int i = 0; i < count; i++) {
            double angle = Math.PI * 2 / count * i;
            level().addParticle(ParticleTypes.SOUL_FIRE_FLAME,
                    victim.getX() + Math.cos(angle) * radius,
                    victim.getY() + victim.getBbHeight() * 0.5,
                    victim.getZ() + Math.sin(angle) * radius,
                    0.0, 0.01, 0.0);
        }
    }
}
