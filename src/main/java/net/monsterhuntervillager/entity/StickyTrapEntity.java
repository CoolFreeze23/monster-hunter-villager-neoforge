package net.monsterhuntervillager.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.monsterhuntervillager.hunter.HunterCombat;

/** Glues whatever steps on it in place (the bigger the victim, the weaker the hold). */
public class StickyTrapEntity extends AbstractTrapEntity {
    public StickyTrapEntity(EntityType<? extends StickyTrapEntity> type, Level level) {
        super(type, level);
    }

    @Override
    public TrapKind kind() {
        return TrapKind.STICKY;
    }

    @Override
    protected void affectVictims() {
        Vec3 center = position();
        for (LivingEntity victim : level().getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(0.3), e -> !e.getType().is(TRAPS))) {
            inUse = 20;
            double drag = 120.0F / (victim.getBbWidth() * victim.getBbHeight());
            Vec3 motion = victim.getDeltaMovement();
            victim.setDeltaMovement(motion.x / drag, motion.y / drag, motion.z / drag);
            HunterCombat.markTrapped(victim);
            if (HunterCombat.isMoving(victim)) {
                wear += 1.0;
                if (random.nextDouble() <= 0.04 && level() instanceof ServerLevel server) {
                    spawnCrit(server);
                }
            }
        }
    }
}
