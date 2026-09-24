package net.monsterhuntervillager.hunter;

import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.monsterhuntervillager.registry.ModAttachments;

/** Combat rules shared by traps, the hunter and the Hunter's Knife. */
public final class HunterCombat {
    /** How long a victim counts as trapped after a trap last held it. */
    private static final long TRAPPED_TICKS = 5;
    private static final Vec3 STUCK = new Vec3(0.25, 0.05, 0.25);

    private HunterCombat() {
    }

    public static void markTrapped(LivingEntity entity) {
        if (!entity.level().isClientSide) {
            entity.setData(ModAttachments.TRAPPED_UNTIL, entity.level().getGameTime() + TRAPPED_TICKS);
        }
    }

    public static boolean isTrapped(Entity entity) {
        return entity.hasData(ModAttachments.TRAPPED_UNTIL)
                && entity.getData(ModAttachments.TRAPPED_UNTIL) > entity.level().getGameTime();
    }

    /** Whether the entity has any velocity at all (the traps only wear while their victim struggles). */
    public static boolean isMoving(Entity entity) {
        Vec3 motion = entity.getDeltaMovement();
        return motion.x != 0.0 || motion.y != 0.0 || motion.z != 0.0;
    }

    /**
     * The Hunter's Knife bonus: half a heart plus 1-6% of the target's max health, or two hearts plus
     * 5-8% (and the target held in place) if a trap has it. Always slows the target for two seconds.
     */
    public static void knifeHit(LivingEntity target, LivingEntity attacker) {
        if (target.level().isClientSide) {
            return;
        }
        DamageSource source = target.level().damageSources().source(DamageTypes.PLAYER_ATTACK, attacker);
        float maxHealth = target.getMaxHealth();
        if (isTrapped(target)) {
            target.hurt(source, (float) (4.0 + maxHealth * Mth.nextDouble(attacker.getRandom(), 0.05, 0.08)));
            target.makeStuckInBlock(Blocks.AIR.defaultBlockState(), STUCK);
        } else {
            target.hurt(source, (float) (1.0 + maxHealth * Mth.nextDouble(attacker.getRandom(), 0.01, 0.06)));
        }
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0));
    }
}
