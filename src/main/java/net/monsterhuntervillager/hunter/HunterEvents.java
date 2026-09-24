package net.monsterhuntervillager.hunter;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.Villager;
import net.monsterhuntervillager.MonsterHunterVillager;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * The only world-wide hooks the mod has. Each one leaves on a single {@code instanceof} for anything
 * that is not a villager, so a world full of mobs costs next to nothing.
 */
@EventBusSubscriber(modid = MonsterHunterVillager.MODID)
public final class HunterEvents {
    private HunterEvents() {
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Pre event) {
        if (event.getEntity() instanceof Villager villager
                && villager.level() instanceof ServerLevel level
                && MonsterHunterAI.isHunter(villager)) {
            MonsterHunterAI.tick(villager, level);
        }
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof Villager villager
                && villager.level() instanceof ServerLevel level
                && MonsterHunterAI.isHunter(villager)) {
            MonsterHunterAI.onAttacked(villager, level, event);
        }
    }
}
