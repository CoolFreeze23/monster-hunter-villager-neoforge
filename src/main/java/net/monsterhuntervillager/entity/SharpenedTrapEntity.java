package net.monsterhuntervillager.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.monsterhuntervillager.hunter.HunterCombat;

/** Cuts whatever struggles in it, harder the more wounded the victim already is, and slows it. */
public class SharpenedTrapEntity extends AbstractTrapEntity {
    public SharpenedTrapEntity(EntityType<? extends SharpenedTrapEntity> type, Level level) {
        super(type, level);
    }

    @Override
    public TrapKind kind() {
        return TrapKind.SHARPENED;
    }

    @Override
    protected void affectVictims() {
        Vec3 center = position();
        for (LivingEntity victim : level().getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(0.3), e -> !e.getType().is(TRAPS))) {
            inUse = 20;
            if (!HunterCombat.isMoving(victim)) {
                continue;
            }
            if (!level().isClientSide && isAttackable()) {
                victim.hurt(trapDamage(), (float) (1.0 + (victim.getMaxHealth() - victim.getHealth()) * 0.2));
                victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 1));
            }
            wear += 1.0 + (victim.getMaxHealth() - victim.getHealth()) * 0.3;
            if (random.nextDouble() <= 0.04 && level() instanceof ServerLevel server) {
                spawnCrit(server);
            }
        }
    }
}
