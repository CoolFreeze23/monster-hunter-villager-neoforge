package net.monsterhuntervillager.hunter;

import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.monster.Stray;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.monsterhuntervillager.entity.AbstractTrapEntity;
import net.monsterhuntervillager.entity.SharpenedTrapEntity;
import net.monsterhuntervillager.entity.StickyTrapEntity;
import net.monsterhuntervillager.entity.TrapKind;
import net.monsterhuntervillager.entity.TrapProjectileEntity;
import net.monsterhuntervillager.registry.ModAttachments;
import net.monsterhuntervillager.registry.ModItems;
import net.monsterhuntervillager.registry.ModVillagers;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;

/**
 * The Monster Hunter's combat behaviour, run once per tick for each Monster Hunter villager.
 *
 * <p>Behaviour is the original's: spot a zombie, spider, skeleton, stray or slime within 15 blocks,
 * circle it, throw sticky and sharpened traps at it, knife it at close range, drink a healing
 * potion when low, and collect its own traps afterwards. What changed is the cost:
 * <ul>
 *   <li>the original ran on every living entity in the world, both sides, and decided whether
 *       the entity was a hunter by serialising the whole entity to NBT every tick;</li>
 *   <li>the current target was re-found every tick by scanning every entity in a 600-block cube;</li>
 *   <li>facing a target ran a {@code /tp ... facing entity} command through the command dispatcher;</li>
 *   <li>every random roll allocated a fresh {@code RandomSource}.</li>
 * </ul>
 * Now it runs server-side, for hunters only, with a direct UUID lookup and plain method calls.
 */
public final class MonsterHunterAI {
    private static final double QUARRY_RANGE = 300.0;
    private static final double SPOT_RANGE = 15.0;
    private static final double MELEE_REACH = 0.9;
    private static final double PICKUP_REACH = 0.75;
    private static final double THROW_REACH = 6.0;
    private static final Vec3 STUCK = new Vec3(0.25, 0.05, 0.25);

    private MonsterHunterAI() {
    }

    public static boolean isHunter(Villager villager) {
        return villager.getVillagerData().getProfession() == ModVillagers.MONSTER_HUNTER.get();
    }

    public static void tick(Villager hunter, ServerLevel level) {
        HunterState state = hunter.getData(ModAttachments.HUNTER_STATE);
        RandomSource random = hunter.getRandom();
        Vec3 center = hunter.position();

        LivingEntity quarry = findQuarry(level, state, center);
        if (quarry == null) {
            spotMonsters(hunter, level, state, center, random);
        }

        if (quarry != null) {
            engage(hunter, level, state, quarry, random);
        } else {
            clearHands(hunter);
            returnToTraps(hunter, level, center, random);
        }

        strikeInReach(hunter, level, state, quarry, center, random);
        tickCooldowns(state);
        collectOwnTraps(hunter, level, center);
    }

    // ---- target selection --------------------------------------------------------------------

    @Nullable
    private static LivingEntity findQuarry(ServerLevel level, HunterState state, Vec3 center) {
        if (state.quarry == null) {
            return null;
        }
        Entity entity = level.getEntity(state.quarry);
        if (!(entity instanceof LivingEntity living)
                || !entity.getBoundingBox().intersects(new AABB(center, center).inflate(QUARRY_RANGE))
                || entity instanceof Player player && (player.isCreative() || player.isSpectator())) {
            return null;
        }
        return living;
    }

    private static boolean isHuntable(Mob mob) {
        return (mob instanceof Zombie || mob instanceof Spider || mob instanceof Skeleton || mob instanceof Stray || mob instanceof Slime)
                && !mob.isInWaterOrBubble();
    }

    /**
     * Picks up monsters within 15 blocks that share the hunter's sky exposure (both under open sky, or
     * both under cover), walking toward the ones that don't. As in the original, every qualifying
     * monster overwrites the choice in order of distance, so the farthest one wins.
     */
    private static void spotMonsters(Villager hunter, ServerLevel level, HunterState state, Vec3 center, RandomSource random) {
        List<Mob> monsters = level.getEntitiesOfClass(Mob.class, new AABB(center, center).inflate(SPOT_RANGE), MonsterHunterAI::isHuntable);
        if (monsters.isEmpty()) {
            return;
        }
        monsters.sort(Comparator.comparingDouble(mob -> mob.distanceToSqr(center)));
        boolean hunterUnderSky = underSky(level, hunter);
        for (Mob monster : monsters) {
            if (underSky(level, monster) == hunterUnderSky) {
                state.quarry = monster.getUUID();
            } else if (random.nextDouble() <= 0.1) {
                hunter.getNavigation().moveTo(
                        monster.getX() + Mth.nextDouble(random, -1.0, 1.0),
                        monster.getY(),
                        monster.getZ() + Mth.nextDouble(random, -1.0, 1.0),
                        0.5);
            }
        }
    }

    private static boolean underSky(ServerLevel level, Entity entity) {
        return level.canSeeSkyFromBelowWater(BlockPos.containing(entity.getX(), entity.getY() + entity.getBbHeight() * 0.6, entity.getZ()));
    }

    // ---- engaging a target -------------------------------------------------------------------

    private static void engage(Villager hunter, ServerLevel level, HunterState state, LivingEntity quarry, RandomSource random) {
        if (state.trapCooldown == 0.0) {
            state.trapCooldown = Mth.nextDouble(random, 30.0, 100.0);
        }
        if (state.trapCooldown <= 0.0) {
            return;
        }
        state.trapCooldown -= 1.0;
        boolean healthy = hunter.getHealth() > hunter.getMaxHealth() / 3.0F;
        if (healthy) {
            circle(hunter, level, quarry, random);
        }
        if (state.trapCooldown < 20.0) {
            if (state.throwCooldown == 0.0) {
                if (healthy) {
                    trapQuarry(hunter, level, state, quarry);
                } else {
                    drinkHealingPotion(hunter, level, state, random);
                }
            }
        } else {
            clearHands(hunter);
        }
    }

    /** Keeps a few blocks off the target's line of sight, closer in once it is trapped. */
    private static void circle(Villager hunter, ServerLevel level, LivingEntity quarry, RandomSource random) {
        Vec3 look = quarry.getLookAngle();
        double width = quarry.getBbWidth();
        if (HunterCombat.isTrapped(quarry)) {
            if (noTrapAt(level, SharpenedTrapEntity.class, quarry)) {
                if (random.nextDouble() <= 0.35) {
                    hunter.getNavigation().moveTo(quarry.getX() + look.x * (3.5 + width), quarry.getY(), quarry.getZ() + look.z * (3.5 + width), 0.8);
                }
                return;
            }
            if (random.nextDouble() <= 0.05) {
                keepDistance(hunter, quarry, look, width, random);
            }
        } else if (random.nextDouble() <= 0.1) {
            keepDistance(hunter, quarry, look, width, random);
        }
    }

    private static void keepDistance(Villager hunter, LivingEntity quarry, Vec3 look, double width, RandomSource random) {
        hunter.getNavigation().moveTo(
                quarry.getX() + look.x * (4.5 + width) + Mth.nextDouble(random, -1.0, 1.0),
                quarry.getY(),
                quarry.getZ() + look.z * (4.5 + width) + Mth.nextDouble(random, -1.0, 1.0),
                0.8);
    }

    private static boolean noTrapAt(ServerLevel level, Class<? extends AbstractTrapEntity> trapClass, Entity target) {
        return level.getEntitiesOfClass(trapClass, AABB.ofSize(target.position(), 2.0, 2.0, 2.0)).isEmpty();
    }

    /** Within 6 blocks, throws a sticky trap at the target's feet, or a sharpened one if it is already stuck. */
    private static void trapQuarry(Villager hunter, ServerLevel level, HunterState state, LivingEntity quarry) {
        double dx = quarry.getX() - hunter.getX();
        double dz = quarry.getZ() - hunter.getZ();
        // The original took |dx| and |dz| by printing the number and deleting the minus sign,
        // which turned tiny offsets such as 1.0E-4 into 1.0E4. This is the intended value.
        double spread = (Math.abs(dz) + Math.abs(dx)) / 2.0;
        if (spread > THROW_REACH) {
            return;
        }
        if (noTrapAt(level, StickyTrapEntity.class, quarry)) {
            throwTrap(hunter, level, state, quarry, TrapKind.STICKY, spread);
        } else if (noTrapAt(level, SharpenedTrapEntity.class, quarry)) {
            throwTrap(hunter, level, state, quarry, TrapKind.SHARPENED, spread);
        }
    }

    private static void throwTrap(Villager hunter, ServerLevel level, HunterState state, LivingEntity quarry, TrapKind kind, double spread) {
        hunter.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(kind.item()));
        double x = hunter.getX();
        double y = hunter.getY();
        double z = hunter.getZ();
        boolean alreadyInFlight = !level.getEntitiesOfClass(TrapProjectileEntity.class, AABB.ofSize(hunter.position(), 20.0, 20.0, 20.0),
                projectile -> projectile.kind() == kind).isEmpty();
        if (!alreadyInFlight) {
            TrapProjectileEntity projectile = TrapProjectileEntity.create(kind, level, hunter);
            projectile.setPos(x, y + 1.3, z);
            projectile.shoot(
                    (quarry.getX() - x) * 0.2,
                    (quarry.getY() + quarry.getBbHeight() * 0.5 + spread * 0.6 + (quarry.getY() - y) / 2.2 - (y + 1.3)) * 0.2,
                    (quarry.getZ() - z) * 0.2,
                    0.6F, 3.0F);
            level.addFreshEntity(projectile);
            state.throwCooldown = 50.0;
        }
        hunter.makeStuckInBlock(Blocks.AIR.defaultBlockState(), STUCK);
        face(hunter, quarry);
    }

    private static void drinkHealingPotion(Villager hunter, ServerLevel level, HunterState state, RandomSource random) {
        hunter.setItemSlot(EquipmentSlot.MAINHAND, PotionContents.createItemStack(Items.POTION, Potions.STRONG_HEALING));
        state.throwCooldown = 50.0;
        level.playSound(null, hunter.blockPosition(), SoundEvents.WANDERING_TRADER_DRINK_POTION, SoundSource.NEUTRAL, 1.0F, 1.0F);
        hunter.addEffect(new MobEffectInstance(MobEffects.HEAL, 1, (int) Math.round(Mth.nextDouble(random, 0.0, 0.9)), false, false));
    }

    // ---- idle --------------------------------------------------------------------------------

    /** With nothing to hunt, wanders back to its own unsprung traps within 30 blocks. */
    private static void returnToTraps(Villager hunter, ServerLevel level, Vec3 center, RandomSource random) {
        AABB area = AABB.ofSize(center, 60.0, 60.0, 60.0);
        AbstractTrapEntity sharpened = nearest(level.getEntitiesOfClass(SharpenedTrapEntity.class, area), center);
        if (sharpened != null) {
            walkToOwnTrap(hunter, sharpened, 0.3, random);
            return;
        }
        AbstractTrapEntity sticky = nearest(level.getEntitiesOfClass(StickyTrapEntity.class, area), center);
        if (sticky != null) {
            walkToOwnTrap(hunter, sticky, 0.1, random);
        }
    }

    private static void walkToOwnTrap(Villager hunter, AbstractTrapEntity trap, double chance, RandomSource random) {
        if (trap.isOwnedBy(hunter) && !trap.isInWaterOrBubble() && !trap.isInUse() && random.nextDouble() <= chance) {
            hunter.getNavigation().moveTo(
                    trap.getX() + Mth.nextDouble(random, -1.0, 1.0),
                    trap.getY(),
                    trap.getZ() + Mth.nextDouble(random, -1.0, 1.0),
                    0.5);
        }
    }

    @Nullable
    private static <T extends Entity> T nearest(List<T> entities, Vec3 center) {
        T nearest = null;
        double best = Double.MAX_VALUE;
        for (T entity : entities) {
            double distance = entity.distanceToSqr(center);
            if (distance < best) {
                best = distance;
                nearest = entity;
            }
        }
        return nearest;
    }

    // ---- melee -------------------------------------------------------------------------------

    /** Knifes the nearest thing within reach that is the hunter's target or is targeting the hunter. */
    private static void strikeInReach(Villager hunter, ServerLevel level, HunterState state, @Nullable LivingEntity quarry, Vec3 center, RandomSource random) {
        if (state.meleeCooldown != 0.0) {
            return;
        }
        List<LivingEntity> near = level.getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(MELEE_REACH));
        near.sort(Comparator.comparingDouble(entity -> entity.distanceToSqr(center)));
        for (LivingEntity victim : near) {
            boolean targetingHunter = victim instanceof Mob mob && mob.getTarget() == hunter;
            if (targetingHunter || victim == quarry) {
                strike(hunter, level, victim, random);
                state.meleeCooldown = 60.0;
                return;
            }
        }
    }

    private static void strike(Villager hunter, ServerLevel level, LivingEntity victim, RandomSource random) {
        hunter.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.HUNTERS_KNIFE.get()));
        face(hunter, victim);
        // The original attributed this damage to the victim itself, which gave "slain by itself"
        // death messages and knocked the victim in a random direction; the hunter is the attacker.
        DamageSource source = level.damageSources().mobAttack(hunter);
        float maxHealth = victim.getMaxHealth();
        if (HunterCombat.isTrapped(victim)) {
            victim.hurt(source, (float) (8.0 + maxHealth * Mth.nextDouble(random, 0.05, 0.08)));
            victim.makeStuckInBlock(Blocks.AIR.defaultBlockState(), STUCK);
        } else {
            victim.hurt(source, (float) (5.0 + maxHealth * Mth.nextDouble(random, 0.01, 0.06)));
        }
        victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0));
        level.playSound(null, hunter.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.NEUTRAL, 1.0F, (float) Mth.nextDouble(random, 1.0, 1.5));
        Vec3 look = hunter.getLookAngle();
        level.sendParticles(ParticleTypes.SWEEP_ATTACK,
                hunter.getX() + look.x, hunter.getY() + hunter.getBbHeight() * 0.6, hunter.getZ() + look.z,
                1, 0.0, 0.0, 0.0, 0.0);
    }

    // ---- housekeeping ------------------------------------------------------------------------

    private static void tickCooldowns(HunterState state) {
        if (state.throwCooldown > 0.0) {
            state.throwCooldown -= 1.0;
        }
        if (state.meleeCooldown > 0.0) {
            state.meleeCooldown -= 1.0;
        }
        state.meleeCooldown = Math.max(state.meleeCooldown, 0.0);
        state.throwCooldown = Math.max(state.throwCooldown, 0.0);
        state.trapCooldown = Math.max(state.trapCooldown, 0.0);
    }

    /** Picks up its own sticky and sharpened traps it is standing on, once nothing is caught in them (or at once if it is caught itself). */
    private static void collectOwnTraps(Villager hunter, ServerLevel level, Vec3 center) {
        boolean hunterTrapped = HunterCombat.isTrapped(hunter);
        for (AbstractTrapEntity trap : level.getEntitiesOfClass(AbstractTrapEntity.class, new AABB(center, center).inflate(PICKUP_REACH))) {
            TrapKind kind = trap.kind();
            if ((kind != TrapKind.STICKY && kind != TrapKind.SHARPENED) || !trap.isOwnedBy(hunter) || (!hunterTrapped && trap.isInUse())) {
                continue;
            }
            hunter.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(kind.item()));
            level.playSound(null, BlockPos.containing(trap.getX(), trap.getY(), trap.getZ()), SoundEvents.ITEM_PICKUP, SoundSource.NEUTRAL, 1.0F, 1.0F);
            trap.discard();
        }
    }

    private static void clearHands(Villager hunter) {
        ItemStack held = hunter.getMainHandItem();
        if (held.is(ModItems.STICKY_TRAP_ITEM.get()) || held.is(ModItems.SHARPENED_TRAP_ITEM.get())
                || held.is(ModItems.HUNTERS_KNIFE.get()) || held.is(Items.POTION)) {
            hunter.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        }
    }

    /**
     * Turns to face the target. Reproduces what the original's {@code tp @s ~ ~ ~ facing entity}
     * command did to the hunter (plant it, stop its path, kill vertical motion) without running a command.
     */
    private static void face(Villager hunter, Entity target) {
        hunter.lookAt(EntityAnchorArgument.Anchor.FEET, target.position());
        hunter.setDeltaMovement(hunter.getDeltaMovement().multiply(1.0, 0.0, 1.0));
        hunter.setOnGround(true);
        hunter.getNavigation().stop();
    }

    // ---- being attacked ----------------------------------------------------------------------

    /**
     * Whoever hurts a hunter becomes its quarry (creative and spectator players excepted), and a
     * moving projectile is dodged half the time with a puff of cloud and a sideways hop.
     */
    public static void onAttacked(Villager hunter, ServerLevel level, LivingIncomingDamageEvent event) {
        Entity attacker = event.getSource().getEntity();
        Entity direct = event.getSource().getDirectEntity();
        if (!(attacker instanceof LivingEntity) || direct == null
                || attacker instanceof Player player && (player.isCreative() || player.isSpectator())) {
            return;
        }
        hunter.getData(ModAttachments.HUNTER_STATE).quarry = attacker.getUUID();

        RandomSource random = hunter.getRandom();
        if (direct instanceof Projectile projectile && projectile.getDeltaMovement().length() > 0.0 && random.nextDouble() <= 0.5) {
            event.setCanceled(true);
            for (int i = 0; i < 10; i++) {
                level.sendParticles(ParticleTypes.CLOUD,
                        hunter.getX() + hunter.getBbWidth() * Mth.nextDouble(random, -0.4, 0.4),
                        hunter.getY() + hunter.getBbHeight() * Mth.nextDouble(random, 0.1, 1.0),
                        hunter.getZ() + hunter.getBbWidth() * Mth.nextDouble(random, -0.4, 0.4),
                        1, 0.0, 0.0, 0.0, 0.01);
            }
            hunter.setDeltaMovement(Mth.nextDouble(random, -1.0, 1.0), 0.4, Mth.nextDouble(random, -1.0, 1.0));
        }
    }
}
